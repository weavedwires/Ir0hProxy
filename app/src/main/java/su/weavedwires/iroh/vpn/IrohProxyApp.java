package su.weavedwires.iroh.vpn;

import android.app.Application;

import su.weavedwires.iroh.vpn.proxy.ProxyController;

public class IrohProxyApp extends Application {
    private ProxyController proxyController;

    public ProxyController getProxyController() {
        if (proxyController == null) {
            synchronized (this) {
                if (proxyController == null) {
                    proxyController = new ProxyController(this);
                }
            }
        }
        return proxyController;
    }
}