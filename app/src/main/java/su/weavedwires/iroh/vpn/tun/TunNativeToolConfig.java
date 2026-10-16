package su.weavedwires.iroh.vpn.tun;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import lombok.Builder;

import su.weavedwires.iroh.vpn.constant.Constant;

@Builder
public class TunNativeToolConfig {

    @Builder.Default
    private final int taskStackSize = Constant.TASK_STACK_SIZE;
    @Builder.Default
    private final int mtu = Constant.TUN_MTU;
    @Builder.Default
    private final InetSocketAddress socks5 =
            new InetSocketAddress(Constant.LOCAL_HOST_ADDRESS, Constant.DEFAULT_PORT);
    private final String username;
    private final String password;
    @Builder.Default
    private final InetSocketAddress mapDns =
            new InetSocketAddress(Constant.DNS_SERVER, Constant.MAPDNS_PORT);
    @Builder.Default
    private final InetAddress mapDnsNetwork = Constant.MAPDNS_NETWORK;
    @Builder.Default
    private final InetAddress mapDnsNetmask = Constant.MAPDNS_NETMASK;
    @Builder.Default
    private final int mapDnsCacheSize = Constant.MAPDNS_CACHE_SIZE;

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
}
