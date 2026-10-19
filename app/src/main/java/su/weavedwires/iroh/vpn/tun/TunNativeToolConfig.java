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
        conf.append("misc:").append(System.lineSeparator());
        conf.append("  task-stack-size: ").append(taskStackSize).append(System.lineSeparator());
        conf.append("tunnel:").append(System.lineSeparator());
        conf.append("  mtu: ").append(mtu).append(System.lineSeparator());
        conf.append("socks5:").append(System.lineSeparator());
        conf.append("  port: ").append(socks5.getPort()).append(System.lineSeparator());
        conf.append("  address: '").append(socks5.getAddress().getHostAddress()).append("'").append(System.lineSeparator());
        conf.append("  udp: 'udp'").append(System.lineSeparator());
        if (hasCredentials()) {
            conf.append("  username: '").append(username).append("'").append(System.lineSeparator());
            conf.append("  password: '").append(password).append("'").append(System.lineSeparator());
        }
        conf.append("mapdns:").append(System.lineSeparator());
        conf.append("  address: ").append(mapDns.getAddress().getHostAddress()).append(System.lineSeparator());
        conf.append("  port: ").append(mapDns.getPort()).append(System.lineSeparator());
        conf.append("  network: ").append(mapDnsNetwork.getHostAddress()).append(System.lineSeparator());
        conf.append("  netmask: ").append(mapDnsNetmask.getHostAddress()).append(System.lineSeparator());
        conf.append("  cache-size: ").append(mapDnsCacheSize).append(System.lineSeparator());
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
