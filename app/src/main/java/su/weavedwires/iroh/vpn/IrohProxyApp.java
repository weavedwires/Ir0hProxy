package su.weavedwires.iroh.vpn;

import static su.weavedwires.iroh.vpn.Constant.IROH_BINARY_NAME;

import android.app.Application;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import su.weavedwires.iroh.vpn.nativ.BinaryRunnerSingleton;
import su.weavedwires.iroh.vpn.proxy.ProxyRunner;

public class IrohProxyApp extends Application {
    private final BinaryRunnerSingleton<ProxyRunner> proxyRunner = new BinaryRunnerSingleton<>();

    public ProxyRunner getProxyRunner() {
        try {
            return proxyRunner.getOrCreateInstance(
                    new File(getApplicationInfo().nativeLibraryDir),
                    getFilesDir(),
                    IROH_BINARY_NAME,
                    ProxyRunner::new
            );
        } catch (IOException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            throw new RuntimeException();
        }
    }
}