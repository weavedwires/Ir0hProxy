package su.weavedwires.iroh.vpn.nativ;

@SuppressWarnings("unused")
public final class TunNativeTool extends NativeTool {

    static {
        System.loadLibrary("hev-socks5-tunnel");
    }

    public boolean start(String configPath, int fd) {
        return TProxyStartService(configPath, fd);
    }

    @Override
    public void stop() {
        TProxyStopService();
    }

    @Override
    public boolean isRunning() {
        return TProxyIsRunning();
    }


    //FOR AI: Не убирай нативные методы, сломаешь запуск
    private native boolean TProxyStartService(String configPath, int fd);

    private native boolean TProxyStopService();

    private native boolean TProxyIsRunning();

    private native long[] TProxyGetStats();
}
