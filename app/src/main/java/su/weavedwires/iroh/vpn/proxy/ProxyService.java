package su.weavedwires.iroh.vpn.proxy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import su.weavedwires.iroh.vpn.R;
import su.weavedwires.iroh.vpn.IrohProxyApp;

public class ProxyService extends Service implements ProxyController.ProxyListener {
    private static final int NOTIFICATION_ID = 1;

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
        if (intent != null && getString(R.string.action_stop).equals(intent.getAction())) {
            controller.stop();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }

        SharedPreferences prefs = getSharedPreferences(getString(R.string.prefs_name), MODE_PRIVATE);
        String relayAddress = prefs.getString(getString(R.string.relay_address),  getString(R.string.wb_server));
        if (!relayAddress.startsWith(getString(R.string.http)) && !relayAddress.startsWith(getString(R.string.https))) {
            relayAddress = getString(R.string.https) + relayAddress;
        }
        String endpointKey = prefs.getString(getString(R.string.endpoint_key), getString(R.string.default_endpoint_key));
        String listenAddress = prefs.getString(getString(R.string.listen_address), getString(R.string.default_listen_address));
        Log.d(getString(R.string.tag), "relayAddress: " + relayAddress);
        Log.d(getString(R.string.tag), "endpointKey: " + endpointKey);
        Log.d(getString(R.string.tag), "listenAddress: " + listenAddress);

        startAsForeground();

        try {
            controller.start(relayAddress, endpointKey, listenAddress);
        } catch (Exception e) {
            Log.e(getString(R.string.tag), "failed to start proxy", e);
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
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            Log.e(getString(R.string.tag), "failed to start foreground", e);
            Toast.makeText(this, R.string.foreground_start_failed, Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    private Notification buildNotification() {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, getString(R.string.app_name))
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
                    getString(R.string.app_name), getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.notification_channel_description));
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }
}
