package su.weavedwires.iroh.vpn;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.IntentCompat;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import su.weavedwires.iroh.vpn.activity.main.MainActivity;
import su.weavedwires.iroh.vpn.constant.Constant;
import su.weavedwires.iroh.vpn.constant.FilterMode;
import su.weavedwires.iroh.vpn.constant.NetworkMode;
import su.weavedwires.iroh.vpn.model.Connection;
import su.weavedwires.iroh.vpn.model.VpnAppFilter;
import su.weavedwires.iroh.vpn.proxy.ProxyNativeTool;
import su.weavedwires.iroh.vpn.proxy.ProxyStateBus;
import su.weavedwires.iroh.vpn.repository.SettingsRepository;
import su.weavedwires.iroh.vpn.tun.TunNativeTool;
import su.weavedwires.iroh.vpn.tun.TunNativeToolConfig;

@SuppressLint("VpnServicePolicy")
public class ProxyService extends VpnService {
    private static final String TAG = "ProxyService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "iroh_vpn";

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private final AtomicBoolean destroyed = new AtomicBoolean(false);
    private final AtomicBoolean cleanupDone = new AtomicBoolean(false);

    private SettingsRepository settings;
    private ProxyNativeTool proxyRunner;
    private TunNativeTool tunNativeTool;
    private ProxyStateBus proxyStateBus;
    private ParcelFileDescriptor tunFd;
    private int localPort;
    private String proxyStartupError;
    private String tunStartupError;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        settings = new SettingsRepository(this);

        IrohProxyApp app = (IrohProxyApp) getApplication();
        proxyStateBus = app.getProxyStateStore();

        try {
            proxyRunner = app.getProxyRunner();
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to initialize proxy runner", e);
            proxyStartupError = getString(R.string.foreground_start_failed);
        }

        try {
            tunNativeTool = app.getTun2Socks();
        } catch (RuntimeException | LinkageError e) {
            Log.e(TAG, "failed to initialize tun2socks", e);
            tunStartupError = getString(R.string.foreground_start_failed);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.w(TAG, "received null intent, stopping service");
            stopping.set(true);
            startAsForeground(null);
            submit(this::shutdown);
            return START_NOT_STICKY;
        }

        if (Constant.ACTION_STOP.equals(intent.getAction())) {
            stopping.set(true);
            submit(this::shutdown);
            return START_NOT_STICKY;
        }

        if (!startAsForeground(intent)) {
            fail(getString(R.string.foreground_start_failed));
            return START_NOT_STICKY;
        }

        submit(() -> launch(intent));
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyed.set(true);
        stopping.set(true);
        worker.shutdownNow();

        if (cleanupDone.compareAndSet(false, true)) {
            stopNativeTools();
            proxyStateBus.notifyStopped();
        }
    }

    @Override
    public void onRevoke() {
        stopping.set(true);
        submit(this::shutdown);
        super.onRevoke();
    }

    private void submit(Runnable task) {
        if (destroyed.get() || worker.isShutdown()) {
            Log.w(TAG, "ignoring task: worker is shut down");
            return;
        }
        try {
            worker.execute(task);
        } catch (RejectedExecutionException e) {
            Log.w(TAG, "task rejected: service is shutting down", e);
        }
    }

    private void launch(Intent intent) {
        try {
            launchInternal(intent);
        } catch (RuntimeException | LinkageError e) {
            Log.e(TAG, "failed to start proxy", e);
            fail(getString(R.string.foreground_start_failed));
        }
    }

    private void launchInternal(Intent intent) {
        if (stopping.get() || destroyed.get()) {
            return;
        }

        if (proxyRunner == null) {
            fail(proxyStartupError != null ? proxyStartupError : getString(R.string.foreground_start_failed));
            return;
        }

        if (proxyRunner.isRunning()) {
            Log.i(TAG, "proxy is already running");
            return;
        }

        Connection connection = getConnection(intent);
        String ticket = connection != null ? connection.getTicket() : null;
        if (ticket == null || ticket.isEmpty()) {
            fail("ticket missing");
            return;
        }

        switch (settings.getMode()) {
            case VPN -> startVpnMode(connection, ticket, getAppFilter(intent));
            case PROXY -> startProxyMode(ticket);
        }
    }

    private void startVpnMode(Connection connection, String ticket, VpnAppFilter filter) {
        if (tunNativeTool == null) {
            fail(tunStartupError != null ? tunStartupError : getString(R.string.foreground_start_failed));
            return;
        }

        if (!startTunnel(ticket)) {
            return;
        }

        try {
            if (!establishVpn(filter)) {
                fail("failed to establish VPN");
                return;
            }
            if (abortIfStopping()) {
                return;
            }
            if (!startTun2Socks(connection.getUser(), connection.getPassword())) {
                fail("failed to start tun2socks");
                return;
            }
        } catch (RuntimeException | LinkageError e) {
            Log.e(TAG, "failed to initialize vpn", e);
            fail(getString(R.string.foreground_start_failed));
            return;
        }

        publishStarted();
    }

    private void startProxyMode(String ticket) {
        if (!startTunnel(ticket)) {
            return;
        }
        publishStarted();
    }

    /**
     * Starts the dumbpipe tunnel shared by both VPN and proxy-only modes.
     */
    private boolean startTunnel(String ticket) {
        localPort = settings.getPort();
        InetSocketAddress bindAddress = new InetSocketAddress(settings.getHost(), localPort);

        try {
            proxyRunner.start(bindAddress, settings.getDnsServers(), ticket);
        } catch (IOException e) {
            Log.e(TAG, "failed to start dumbpipe", e);
            fail("failed to start dumbpipe");
            return false;
        }

        return !abortIfStopping();
    }

    private void publishStarted() {
        if (abortIfStopping()) {
            return;
        }

        proxyStateBus.notifyStarted();

        // A stop/destroy can race with the last check above. Make sure the UI
        // does not stay in the "started" state for an already stopped service.
        if (stopping.get() || destroyed.get()) {
            proxyStateBus.notifyStopped();
        }
    }

    /**
     * Stops native tools if a stop was requested while {@link #launchInternal(Intent)}
     * was still starting them. The worker is single threaded, so without this check a
     * queued shutdown could run before the tun fd/process state is published.
     */
    private boolean abortIfStopping() {
        if (!stopping.get()) {
            return false;
        }
        stopNativeTools();
        return true;
    }

    private static Connection getConnection(Intent intent) {
        if (intent == null) {
            return null;
        }
        return IntentCompat.getParcelableExtra(intent, Constant.EXTRA_CONNECTION, Connection.class);
    }

    private static VpnAppFilter getAppFilter(Intent intent) {
        VpnAppFilter filter = intent == null ? null
                : IntentCompat.getParcelableExtra(intent, Constant.EXTRA_APP_FILTER, VpnAppFilter.class);
        if (filter == null || filter.getPackages() == null) {
            return new VpnAppFilter(new String[0], FilterMode.BLACKLIST);
        }
        return filter;
    }

    private void shutdown() {
        stopping.set(true);
        if (!cleanupDone.compareAndSet(false, true)) {
            return;
        }
        stopForegroundSafely();
        stopNativeTools();
        proxyStateBus.notifyStopped();
        stopSelf();
    }

    private void fail(String message) {
        stopping.set(true);
        Log.e(TAG, message);
        if (!cleanupDone.compareAndSet(false, true)) {
            return;
        }
        stopForegroundSafely();
        stopNativeTools();
        stopSelf();
        proxyStateBus.notifyError(message);
    }

    private void stopForegroundSafely() {
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        } catch (RuntimeException e) {
            Log.w(TAG, "failed to stop foreground", e);
        }
    }

    private synchronized void stopNativeTools() {
        if (tunNativeTool != null) {
            try {
                tunNativeTool.stop();
            } catch (RuntimeException | LinkageError e) {
                Log.w(TAG, "failed to stop tun2socks", e);
            }
        }

        ParcelFileDescriptor fd = tunFd;
        tunFd = null;
        if (fd != null) {
            try {
                fd.close();
            } catch (IOException ignored) {
            }
        }

        if (proxyRunner != null) {
            try {
                proxyRunner.stop();
            } catch (RuntimeException e) {
                Log.w(TAG, "failed to stop dumbpipe", e);
            }
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private boolean establishVpn(VpnAppFilter filter) {
        Builder builder = new Builder()
                .setBlocking(false)
                .setMtu(Constant.TUN_MTU)
                .addAddress(Constant.TUN_IPV4_ADDRESS.getHostAddress(), Constant.TUN_IPV4_PREFIX)
                .addRoute("0.0.0.0", 0)
                .addDnsServer(Constant.DNS_SERVER.getHostAddress())
                .setSession("iroh-vpn");

        addSelfToExceptions(filter, builder);
        filter.apply(builder);

        tunFd = builder.establish();
        return tunFd != null;
    }

    private void addSelfToExceptions(VpnAppFilter userFilter, Builder builder) {
        if (!userFilter.isTriggersWhitelist()) {
            new VpnAppFilter(new String[]{getPackageName()}, FilterMode.BLACKLIST).apply(builder);
        }
    }

    private synchronized boolean startTun2Socks(String username, String password) {
        if (tunNativeTool == null || tunFd == null) {
            Log.e(TAG, "cannot start tun2socks: native tool or tun fd is missing");
            return false;
        }

        File configFile = new File(getCacheDir(), "tproxy.conf");
        try {
            TunNativeToolConfig config = TunNativeToolConfig.builder()
                    .socks5(new InetSocketAddress(Constant.LOCAL_HOST_ADDRESS, localPort))
                    .username(username)
                    .password(password)
                    .build();
            config.writeTo(configFile);
        } catch (IOException e) {
            Log.e(TAG, "failed to write tun2socks config", e);
            return false;
        }
        return tunNativeTool.start(configFile.getAbsolutePath(), tunFd.getFd());
    }

    private boolean startAsForeground(Intent startIntent) {
        try {
            Intent openIntent = new Intent(this, MainActivity.class);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE);

            Notification notification = makeNotification(startIntent, pendingIntent);

            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to enter foreground", e);
            return false;
        }
    }

    @NonNull
    private Notification makeNotification(Intent rootIntent, PendingIntent pendingIntent) {
        int titleType = isVpnMode() ? R.string.notification_title_vpn : R.string.notification_title_proxy;
        String title = getString(titleType);

        String connectionName = null;
        Connection connection = getConnection(rootIntent);
        if (connection != null) {
            connectionName = connection.getName();
        }
        String text = (connectionName == null || connectionName.isEmpty()) ? getString(R.string.no_name) : connectionName;

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .build();
    }

    private boolean isVpnMode() {
        return settings.getMode() == NetworkMode.VPN;
    }

    private void createNotificationChannel() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }
    }
}
