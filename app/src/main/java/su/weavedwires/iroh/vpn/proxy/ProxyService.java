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

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import su.weavedwires.iroh.vpn.IrohProxyApp;
import su.weavedwires.iroh.vpn.R;
import su.weavedwires.iroh.vpn.Settings;
import su.weavedwires.iroh.vpn.activity.MainActivity;
import su.weavedwires.iroh.vpn.constant.Constant;
import su.weavedwires.iroh.vpn.constant.Mode;
import su.weavedwires.iroh.vpn.nativ.TunNativeTool;
import su.weavedwires.iroh.vpn.nativ.TunNativeToolConfig;
import su.weavedwires.iroh.vpn.nativ.error.NativeError;
import su.weavedwires.iroh.vpn.nativ.error.NativeErrorListener;

public class ProxyService extends VpnService implements NativeErrorListener {

    private static final String TAG = "ProxyService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "iroh_vpn";

    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private Settings settings;
    private ProxyNativeTool proxyRunner;
    private TunNativeTool tunNativeTool;
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && Constant.ACTION_STOP.equals(intent.getAction())) {
            worker.execute(this::stopProxy);
            return START_NOT_STICKY;
        }
        startAsForeground();
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

        String ticket = intent != null ? intent.getStringExtra(Constant.EXTRA_TICKET) : null;
        String username = intent != null ? intent.getStringExtra(Constant.EXTRA_USER) : null;
        String password = intent != null ? intent.getStringExtra(Constant.EXTRA_PASSWORD) : null;

        if (ticket == null || ticket.isEmpty()) {
            fail("ticket missing");
            return;
        }

        boolean vpnMode = settings.getMode() == Mode.VPN;
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
            if (!startTun2Socks(username, password)) {
                fail("failed to start tun2socks");
                return;
            }
        }

        broadcast(Constant.ACTION_PROXY_STARTED, null);
    }

    private void stopProxy() {
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopNativeTools();
        broadcast(Constant.ACTION_PROXY_STOPPED, null);
        stopSelf();
    }

    private void fail(String message) {
        Log.e(TAG, message);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopNativeTools();
        stopSelf();
        broadcast(Constant.ACTION_PROXY_ERROR, message);
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

    private void broadcast(String action, String error) {
        Intent intent = new Intent(action).setPackage(getPackageName());
        if (error != null) {
            intent.putExtra(Constant.EXTRA_ERROR, error);
        }
        sendBroadcast(intent);
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
                    .credentials(username, password)
                    .build();
            config.writeTo(configFile);
        } catch (IOException e) {
            Log.e(TAG, "failed to write tun2socks config", e);
            return false;
        }
        return tunNativeTool.start(configFile.getAbsolutePath(), tunFd.getFd());
    }

    private void startAsForeground() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
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
