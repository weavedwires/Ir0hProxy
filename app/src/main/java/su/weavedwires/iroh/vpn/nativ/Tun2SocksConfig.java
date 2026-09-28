package su.weavedwires.iroh.vpn.nativ;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import su.weavedwires.iroh.vpn.Constant;

public final class Tun2SocksConfig {

    private final int taskStackSize;
    private final int mtu;
    private final InetSocketAddress socks5;
    private final String username;
    private final String password;
    private final InetSocketAddress mapDns;
    private final InetAddress mapDnsNetwork;
    private final InetAddress mapDnsNetmask;
    private final int mapDnsCacheSize;

    private Tun2SocksConfig(Builder builder) {
        this.taskStackSize = builder.taskStackSize;
        this.mtu = builder.mtu;
        this.socks5 = builder.socks5;
        this.username = builder.username;
        this.password = builder.password;
        this.mapDns = builder.mapDns;
        this.mapDnsNetwork = builder.mapDnsNetwork;
        this.mapDnsNetmask = builder.mapDnsNetmask;
        this.mapDnsCacheSize = builder.mapDnsCacheSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String toConfigString() {
        StringBuilder conf = new StringBuilder();
        conf.append("misc:\n");
        conf.append("  task-stack-size: ").append(taskStackSize).append("\n");
        conf.append("tunnel:\n");
        conf.append("  mtu: ").append(mtu).append("\n");
        conf.append("  icmp: 'reply'\n");
        conf.append("socks5:\n");
        conf.append("  port: ").append(socks5.getPort()).append("\n");
        conf.append("  address: '").append(socks5.getAddress().getHostAddress()).append("'\n");
        conf.append("  udp: 'udp'\n");
        if (hasCredentials()) {
            conf.append("  username: '").append(username).append("'\n");
            conf.append("  password: '").append(password).append("'\n");
        }
        conf.append("mapdns:\n");
        conf.append("  address: ").append(mapDns.getAddress().getHostAddress()).append("\n");
        conf.append("  port: ").append(mapDns.getPort()).append("\n");
        conf.append("  network: ").append(mapDnsNetwork.getHostAddress()).append("\n");
        conf.append("  netmask: ").append(mapDnsNetmask.getHostAddress()).append("\n");
        conf.append("  cache-size: ").append(mapDnsCacheSize).append("\n");
        return conf.toString();
    }

    public void writeTo(File file) throws IOException {
        try (OutputStream out = new FileOutputStream(file, false)) {
            out.write(toConfigString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private boolean hasCredentials() {
        return username != null && !username.isEmpty()
                && password != null && !password.isEmpty();
    }

    public static final class Builder {
        private int taskStackSize = Constant.TASK_STACK_SIZE;
        private int mtu = Constant.TUN_MTU;
        private InetSocketAddress socks5 = new InetSocketAddress(Constant.LOCAL_HOST_ADDRESS, Constant.DEFAULT_PORT);
        private String username;
        private String password;
        private InetSocketAddress mapDns = new InetSocketAddress(Constant.DNS_SERVER, Constant.MAPDNS_PORT);
        private InetAddress mapDnsNetwork = Constant.MAPDNS_NETWORK;
        private InetAddress mapDnsNetmask = Constant.MAPDNS_NETMASK;
        private int mapDnsCacheSize = Constant.MAPDNS_CACHE_SIZE;

        public Builder taskStackSize(int value) {
            this.taskStackSize = value;
            return this;
        }

        public Builder mtu(int value) {
            this.mtu = value;
            return this;
        }

        public Builder socks5(InetSocketAddress value) {
            this.socks5 = value;
            return this;
        }

        public Builder credentials(String username, String password) {
            this.username = username;
            this.password = password;
            return this;
        }

        public Builder mapDns(InetSocketAddress value) {
            this.mapDns = value;
            return this;
        }

        public Builder mapDnsNetwork(InetAddress value) {
            this.mapDnsNetwork = value;
            return this;
        }

        public Builder mapDnsNetmask(InetAddress value) {
            this.mapDnsNetmask = value;
            return this;
        }

        public Builder mapDnsCacheSize(int value) {
            this.mapDnsCacheSize = value;
            return this;
        }

        public Tun2SocksConfig build() {
            return new Tun2SocksConfig(this);
        }
    }
}
