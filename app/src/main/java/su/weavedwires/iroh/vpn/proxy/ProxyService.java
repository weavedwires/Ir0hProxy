package su.weavedwires.iroh.vpn.proxy;

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

import su.weavedwires.iroh.vpn.IrohProxyApp;
import su.weavedwires.iroh.vpn.R;
import su.weavedwires.iroh.vpn.activity.main.MainActivity;
import su.weavedwires.iroh.vpn.constant.Constant;
import su.weavedwires.iroh.vpn.constant.Mode;
import su.weavedwires.iroh.vpn.model.Connection;
import su.weavedwires.iroh.vpn.nativ.error.NativeError;
import su.weavedwires.iroh.vpn.nativ.error.NativeErrorListener;
import su.weavedwires.iroh.vpn.repository.Settings;
import su.weavedwires.iroh.vpn.tun.TunNativeTool;
import su.weavedwires.iroh.vpn.tun.TunNativeToolConfig;

public class ProxyService extends VpnService implements NativeErrorListener {
    private static final String TAG = "ProxyService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "iroh_vpn";

    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private Settings settings;
    private ProxyNativeTool proxyRunner;
    private TunNativeTool tunNativeTool;
    private ProxyStateBus proxyStateBus;
    private ParcelFileDescriptor tunFd;
    private int localPort;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        settings = new Settings(this);
        proxyRunner = ((IrohProxyApp) getApplication()).getProxyRunner();
        proxyRunner.addListener(this);
        tunNativeTool = ((IrohProxyApp) getApplication()).getTun2Socks();
        proxyStateBus = ((IrohProxyApp) getApplication()).getProxyStateStore();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && Constant.ACTION_STOP.equals(intent.getAction())) {
            worker.execute(this::stopProxy);
            return START_NOT_STICKY;
        }
        startAsForeground(intent);
        worker.execute(() -> startProxy(intent));
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (proxyRunner != null) {
            proxyRunner.removeListener(this);
        }
        worker.shutdown();
    }

    @Override
    public void onRevoke() {
        worker.execute(this::stopProxy);
        super.onRevoke();
    }

    @Override
    public void onNativeError(NativeError error) {
        Log.e(TAG, error.toString());
        worker.execute(() -> fail(error.toString()));
    }

    private void startProxy(Intent intent) {
        if (proxyRunner.isRunning()) {
            return;
        }

        Connection connection = getConnection(intent);
        String ticket = connection != null ? connection.getTicket() : null;
        if (ticket == null || ticket.isEmpty()) {
            fail("ticket missing");
            return;
        }

        boolean vpnMode = isVpnMode();
        localPort = settings.getPort();
        InetSocketAddress bindAddress = new InetSocketAddress(settings.getHost(), localPort);

        try {
            proxyRunner.start(bindAddress, settings.getDnsServers(), ticket);
        } catch (IOException e) {
            Log.e(TAG, "failed to start dumbpipe", e);
            fail("failed to start dumbpipe");
            return;
        }

        if (vpnMode) {
            if (!establishVpn()) {
                fail("failed to establish VPN");
                return;
            }
            if (!startTun2Socks(connection.getUser(), connection.getPassword())) {
                fail("failed to start tun2socks");
                return;
            }
        }

        proxyStateBus.notifyStarted();
    }

    private static Connection getConnection(Intent intent) {
        return intent == null
                ? null
                : IntentCompat.getParcelableExtra(intent, Constant.EXTRA_CONNECTION, Connection.class);
    }

    private void stopProxy() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        stopNativeTools();
        proxyStateBus.notifyStopped();
        stopSelf();
    }

    private void fail(String message) {
        Log.e(TAG, message);
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        stopNativeTools();
        stopSelf();
        proxyStateBus.notifyError(message);
    }

    private void stopNativeTools() {
        tunNativeTool.stop();

        if (tunFd != null) {
            try {
                tunFd.close();
            } catch (IOException ignored) {
            }
            tunFd = null;
        }

        if (proxyRunner != null) {
            proxyRunner.stop();
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private boolean establishVpn() {
        Builder builder = new Builder();
        builder.setBlocking(false);
        builder.setMtu(Constant.TUN_MTU);
        builder.addAddress(Constant.TUN_IPV4_ADDRESS.getHostAddress(), Constant.TUN_IPV4_PREFIX);
        builder.addRoute("0.0.0.0", 0);
        builder.addDnsServer(Constant.DNS_SERVER.getHostAddress());
        builder.setSession("iroh-vpn");
        try {
            builder.addDisallowedApplication(getPackageName());
        } catch (Exception ignored) {
        }
        tunFd = builder.establish();
        return tunFd != null;
    }

    private boolean startTun2Socks(String username, String password) {
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

    private void startAsForeground(Intent startIntent) {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = makeNotification(startIntent, pendingIntent);

        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
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
        return settings.getMode() == Mode.VPN;
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
