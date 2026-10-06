package su.weavedwires.iroh.vpn;

import static su.weavedwires.iroh.vpn.constant.Constant.IROH_BINARY_NAME;

import android.app.Application;
import android.widget.Toast;

import java.io.File;

import su.weavedwires.iroh.vpn.nativ.TunNativeTool;
import su.weavedwires.iroh.vpn.proxy.ProxyNativeTool;

public class IrohProxyApp extends Application {
    private final ThreadSafeSingleton<ProxyNativeTool> proxyRunner = new ThreadSafeSingleton<>();
    private final ThreadSafeSingleton<TunNativeTool> tun2Socks = new ThreadSafeSingleton<>();

    @Override
    public void onCreate() {
        super.onCreate();
        new Settings(this).ensureDefaultDnsServers();
    }

    public ProxyNativeTool getProxyRunner() {
        return proxyRunner.getOrCreateInstance(this::createProxyRunner);
    }

    public TunNativeTool getTun2Socks() {
        return tun2Socks.getOrCreateInstance(TunNativeTool::new);
    }

    private ProxyNativeTool createProxyRunner() {
        File binary = new File(getApplicationInfo().nativeLibraryDir, IROH_BINARY_NAME + ".so");
        if (!binary.isFile()) {
            String message = "native binary not found: " + binary.getAbsolutePath();
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            throw new RuntimeException(message);
        }
        return new ProxyNativeTool(getFilesDir(), binary);
    }
}
