package su.weavedwires.iroh.vpn.proxy;

import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Process-scoped bus for proxy lifecycle events.
 *
 * <p>The proxy service publishes from a worker thread while listeners usually touch views,
 * so events are always delivered on the main thread (the job {@code sendBroadcast} used to do).
 */
public class ProxyStateStore {

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
