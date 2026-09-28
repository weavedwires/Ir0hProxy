package su.weavedwires.iroh.vpn.proxy;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import su.weavedwires.iroh.vpn.nativ.BinaryNativeTool;
import su.weavedwires.iroh.vpn.nativ.CmdBuilder;

public class ProxyNativeTool extends BinaryNativeTool {
    private final File binary;

    public ProxyNativeTool(File workDir, File binary) {
        super(workDir, binary);
        this.binary = binary;
    }

    public void start(InetSocketAddress bindAddress, List<InetAddress> dnsServers, String ticket) throws IOException {
        CmdBuilder cmd = new CmdBuilder()
                .add(binary.getAbsolutePath())
                .add("connect-tcp")
                .add("--addr")
                .add(bindAddress.getAddress().getHostAddress() + ":" + bindAddress.getPort());

        if (dnsServers != null && !dnsServers.isEmpty()) {
            cmd.add("--dns-server")
                    .add(joinHosts(dnsServers));
        }

        cmd.add(ticket);

        runProcess(cmd);
    }

    private static String joinHosts(List<InetAddress> dnsServers) {
        List<String> hosts = new ArrayList<>(dnsServers.size());
        for (InetAddress address : dnsServers) {
            hosts.add(address.getHostAddress());
        }
        return String.join(",", hosts);
    }
}
