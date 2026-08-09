package su.weavedwires.iroh.proxy;

import static su.weavedwires.iroh.proxy.Constant.ENDPOINT_KEY;
import static su.weavedwires.iroh.proxy.Constant.LISTEN_ADDRESS;
import static su.weavedwires.iroh.proxy.Constant.PREFS_NAME;
import static su.weavedwires.iroh.proxy.Constant.RELAY_ADDRESS;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import static su.weavedwires.iroh.proxy.Constant.*;

import androidx.core.app.NotificationCompat;

public class ProxyService extends Service implements ProxyController.ProxyListener {

    public static final String ACTION_START = "su.weavedwires.iroh.proxy.action.START";
    public static final String ACTION_STOP = "su.weavedwires.iroh.proxy.action.STOP";

    private static final String CHANNEL_ID = "iroh_proxy";
    private static final int NOTIFICATION_ID = 1;
    public static final String HTTPS = "https://";
    public static final String HTTP = "http://";
    public static final String WB_SERVER = "dnd.wb.ru";
    public static final String DEFAULT_SERVER_KEY = "Sw4RmQpRHs5AaWSh7bpiEbWkXK1ku+XgztNjLRoYYR8=";
    public static final String LOCALHOST_1081 = "127.0.0.1:1081";

    private ProxyController controller;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        controller = ((IrohProxyApp) getApplication()).getProxyController();
        controller.setListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            controller.stop();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME.str(), MODE_PRIVATE);
        String relayAddress = prefs.getString(RELAY_ADDRESS.str(), WB_SERVER);
        if (!relayAddress.startsWith(HTTP) || !relayAddress.startsWith(HTTPS)) {
            relayAddress = HTTPS + relayAddress;
        }
        String endpointKey = prefs.getString(ENDPOINT_KEY.str(), DEFAULT_SERVER_KEY);
        String listenAddress = prefs.getString(LISTEN_ADDRESS.str(), LOCALHOST_1081);

        startAsForeground();

        try {
            controller.start(relayAddress, endpointKey, listenAddress);
        } catch (Exception e) {
            Log.e(TAG.str(), "failed to start proxy", e);
            controller.stop();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onProcessExited(int code, String error) {
        mainHandler.post(() -> {
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        });
    }

    @Override
    public void onDestroy() {
        if (controller != null) {
            controller.setListener(null);
            controller.stop();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startAsForeground() {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification() {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setOngoing(true)
                .setOnlyAlertOnce(true);
        return b.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.notification_channel_description));
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }
}
