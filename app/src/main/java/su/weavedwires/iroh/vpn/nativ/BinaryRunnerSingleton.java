package su.weavedwires.iroh.vpn.nativ;

import java.io.File;
import java.io.IOException;
import java.util.function.BiFunction;


public class BinaryRunnerSingleton<T extends BinaryRunner> {
    private volatile T object;

    public T getOrCreateInstance(File nativeLibraryDir, File filesDir, String binaryName, BiFunction<File, File, T> constructor) throws IOException {
        if (object == null) {
            synchronized (this) {
                if (object == null) {
                    object = createInstance(nativeLibraryDir, filesDir, binaryName, constructor);
                }
            }
        }
        return getInstance();
    }

    public T createInstance(File nativeLibraryDir, File filesDir, String binaryName, BiFunction<File, File, T> constructor) throws IOException {
        File binary = extractBinary(nativeLibraryDir, filesDir, binaryName);
        return constructor.apply(filesDir, binary);
    }

    public T getInstance() {
        return object;
    }

    private File extractBinary(File nativeLibraryDir, File workdir, String binaryName) throws IOException {
        return new BinaryExtractor(
                    nativeLibraryDir,
                    binaryName,
                    workdir
        ).extract();
    }
}
