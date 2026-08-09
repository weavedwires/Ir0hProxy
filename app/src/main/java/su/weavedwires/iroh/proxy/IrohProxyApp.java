package su.weavedwires.iroh.proxy;

import android.app.Application;

public class IrohProxyApp extends Application {
    private ProxyController proxyController;

    public ProxyController getProxyController() {
        if (proxyController == null) {
            proxyController = new ProxyController(this);
        }
        return proxyController;
    }
}