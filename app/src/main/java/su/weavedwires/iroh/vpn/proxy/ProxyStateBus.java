package su.weavedwires.iroh.vpn.proxy;

import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProxyStateBus {

    private final List<ProxyStateListener> listeners = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void addListener(ProxyStateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ProxyStateListener listener) {
        listeners.remove(listener);
    }

    public void notifyStarted() {
        mainHandler.post(() -> {
            for (ProxyStateListener listener : listeners) {
                listener.onStarted();
            }
        });
    }

    public void notifyStopped() {
        mainHandler.post(() -> {
            for (ProxyStateListener listener : listeners) {
                listener.onStopped();
            }
        });
    }

    public void notifyError(String error) {
        mainHandler.post(() -> {
            for (ProxyStateListener listener : listeners) {
                listener.onError(error);
            }
        });
    }
}
