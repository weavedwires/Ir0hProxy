package su.weavedwires.iroh.vpn;

import static su.weavedwires.iroh.vpn.Constant.IROH_BINARY_NAME;

import android.app.Application;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import su.weavedwires.iroh.vpn.nativ.BinaryRunnerSingleton;
import su.weavedwires.iroh.vpn.nativ.TunNativeTool;
import su.weavedwires.iroh.vpn.proxy.ProxyNativeTool;

public class IrohProxyApp extends Application {
    private final BinaryRunnerSingleton<ProxyNativeTool> proxyRunner = new BinaryRunnerSingleton<>();

    public ProxyNativeTool getProxyRunner() {
        try {
            return proxyRunner.getOrCreateInstance(
                    new File(getApplicationInfo().nativeLibraryDir),
                    getFilesDir(),
                    IROH_BINARY_NAME,
                    ProxyNativeTool::new
            );
        } catch (IOException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            throw new RuntimeException();
        }
    }

    public TunNativeTool getTun2Socks() {
        return TunNativeTool.getInstance();
    }

    public boolean isProxyRunning() {
        ProxyNativeTool runner = proxyRunner.getInstance();
        return runner != null && runner.isRunning();
    }
}