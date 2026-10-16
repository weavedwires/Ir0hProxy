package su.weavedwires.iroh.vpn.proxy;

public interface ProxyStateListener {
    void onStarted();

    void onStopped();

    void onError(String error);
}
