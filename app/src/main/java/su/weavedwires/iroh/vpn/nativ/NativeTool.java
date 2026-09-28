package su.weavedwires.iroh.vpn.nativ;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import su.weavedwires.iroh.vpn.nativ.error.NativeError;
import su.weavedwires.iroh.vpn.nativ.error.NativeErrorListener;

public abstract class NativeTool {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<NativeErrorListener> errorListeners = new CopyOnWriteArrayList<>();
    private volatile NativeError lastError;

    public boolean isRunning() {
        return running.get();
    }

    public NativeError getLastError() {
        return lastError;
    }

    public void addListener(NativeErrorListener listener) {
        errorListeners.add(listener);
    }

    public void removeListener(NativeErrorListener listener) {
        errorListeners.remove(listener);
    }

    protected void setRunning(boolean value) {
        running.set(value);
    }

    protected void clearError() {
        lastError = null;
    }

    protected void pushError(NativeError error) {
        lastError = error;
        for (NativeErrorListener listener : errorListeners) {
            listener.onNativeError(error);
        }
    }

    public abstract void stop();
}
