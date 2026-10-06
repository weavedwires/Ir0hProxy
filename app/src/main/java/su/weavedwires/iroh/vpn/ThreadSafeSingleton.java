package su.weavedwires.iroh.vpn;

import java.util.function.Supplier;

import su.weavedwires.iroh.vpn.nativ.NativeTool;

public class ThreadSafeSingleton<T extends NativeTool> {
    private volatile T object;

    public T getOrCreateInstance(Supplier<T> constructor) {
        if (object == null) {
            synchronized (this) {
                if (object == null) {
                    object = constructor.get();
                }
            }
        }
        return object;
    }
}
