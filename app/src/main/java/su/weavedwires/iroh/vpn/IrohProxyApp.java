package su.weavedwires.iroh.vpn;

import static su.weavedwires.iroh.vpn.constant.Constant.IROH_BINARY_NAME;

import android.app.Application;
import android.util.Log;

import java.io.File;

import su.weavedwires.iroh.vpn.proxy.ProxyNativeTool;
import su.weavedwires.iroh.vpn.proxy.ProxyStateBus;
import su.weavedwires.iroh.vpn.repository.SettingsRepository;
import su.weavedwires.iroh.vpn.tun.TunNativeTool;

public class IrohProxyApp extends Application {
    private static final String TAG = "IrohProxyApp";

    private final ThreadSafeSingleton<ProxyNativeTool> proxyRunner = new ThreadSafeSingleton<>();
    private final ThreadSafeSingleton<TunNativeTool> tun2Socks = new ThreadSafeSingleton<>();
    private final ThreadSafeSingleton<ProxyStateBus> proxyStateStore = new ThreadSafeSingleton<>();

    @Override
    public void onCreate() {
        super.onCreate();
        new SettingsRepository(this).ensureDefaultDnsServers();
    }

    public ProxyNativeTool getProxyRunner() {
        return proxyRunner.getOrCreateInstance(this::createProxyRunner);
    }

    public TunNativeTool getTun2Socks() {
        return tun2Socks.getOrCreateInstance(TunNativeTool::new);
    }

    public ProxyStateBus getProxyStateStore() {
        return proxyStateStore.getOrCreateInstance(ProxyStateBus::new);
    }

    private ProxyNativeTool createProxyRunner() {
        File binary = new File(getApplicationInfo().nativeLibraryDir, IROH_BINARY_NAME + ".so");
        if (!binary.isFile()) {
            String message = "native binary not found: " + binary.getAbsolutePath();
            Log.e(TAG, message);
            throw new RuntimeException(message);
        }
        return new ProxyNativeTool(getFilesDir(), binary);
    }
}
