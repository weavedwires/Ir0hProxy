package su.weavedwires.iroh.vpn;

import java.util.function.Supplier;

public class ThreadSafeSingleton<T> {
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
