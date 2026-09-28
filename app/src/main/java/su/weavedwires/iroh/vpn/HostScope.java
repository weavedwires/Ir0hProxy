package su.weavedwires.iroh.vpn;

import java.net.InetAddress;

public enum HostScope {
    LOCAL(Constant.LOCAL_HOST_ADDRESS),
    PUBLIC(Constant.PUBLIC_HOST_ADDRESS);

    private final InetAddress address;

    HostScope(InetAddress address) {
        this.address = address;
    }

    public InetAddress address() {
        return address;
    }
}
