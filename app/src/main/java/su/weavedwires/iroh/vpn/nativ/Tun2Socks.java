package su.weavedwires.iroh.vpn.nativ;

public final class Tun2Socks {

    static {
        System.loadLibrary("hev-socks5-tunnel");
    }

    private static final Tun2Socks INSTANCE = new Tun2Socks();

    private Tun2Socks() {
    }

    public static Tun2Socks getInstance() {
        return INSTANCE;
    }

    public boolean start(String configPath, int fd) {
        return TProxyStartService(configPath, fd);
    }

    public boolean stop() {
        return TProxyStopService();
    }

    public boolean isRunning() {
        return TProxyIsRunning();
    }

    public TunStats getStats() {
        long[] raw = TProxyGetStats();
        if (raw == null || raw.length < 4) {
            return TunStats.EMPTY;
        }
        return new TunStats(raw[0], raw[1], raw[2], raw[3]);
    }

    private native boolean TProxyStartService(String configPath, int fd);

    private native boolean TProxyStopService();

    private native boolean TProxyIsRunning();

    private native long[] TProxyGetStats();
}
