package su.weavedwires.iroh.vpn.nativ;

public final class TunStats {

    public static final TunStats EMPTY = new TunStats(0, 0, 0, 0);

    private final long txPackets;
    private final long txBytes;
    private final long rxPackets;
    private final long rxBytes;

    public TunStats(long txPackets, long txBytes, long rxPackets, long rxBytes) {
        this.txPackets = txPackets;
        this.txBytes = txBytes;
        this.rxPackets = rxPackets;
        this.rxBytes = rxBytes;
    }

    public long getTxPackets() {
        return txPackets;
    }

    public long getTxBytes() {
        return txBytes;
    }

    public long getRxPackets() {
        return rxPackets;
    }

    public long getRxBytes() {
        return rxBytes;
    }

    @Override
    public String toString() {
        return "TunStats{txPackets=" + txPackets
                + ", txBytes=" + txBytes
                + ", rxPackets=" + rxPackets
                + ", rxBytes=" + rxBytes + '}';
    }
}
