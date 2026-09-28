package su.weavedwires.iroh.vpn.nativ;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;

import org.junit.Test;

import su.weavedwires.iroh.vpn.Constant;

public class TunNativeToolConfigTest {

    @Test
    public void defaults_renderExpectedConfig() {
        TunNativeToolConfig config = TunNativeToolConfig.builder().build();
        String rendered = config.toConfigString();

        assertTrue(rendered.contains("task-stack-size: " + Constant.TASK_STACK_SIZE));
        assertTrue(rendered.contains("mtu: " + Constant.TUN_MTU));
        assertTrue(rendered.contains("port: " + Constant.DEFAULT_PORT));
        assertTrue(rendered.contains("address: '127.0.0.1'"));
        assertTrue(rendered.contains("address: " + Constant.DNS_SERVER.getHostAddress()));
        assertFalse(rendered.contains("username:"));
        assertFalse(rendered.contains("password:"));
    }

    @Test
    public void socksPort_isReflected() {
        TunNativeToolConfig config = TunNativeToolConfig.builder()
                .socks5(new InetSocketAddress(Constant.LOCAL_HOST_ADDRESS, 4321))
                .build();

        assertTrue(config.toConfigString().contains("port: 4321"));
    }

    @Test
    public void credentials_areIncludedWhenBothPresent() {
        TunNativeToolConfig config = TunNativeToolConfig.builder()
                .credentials("alice", "secret")
                .build();
        String rendered = config.toConfigString();

        assertTrue(rendered.contains("username: 'alice'"));
        assertTrue(rendered.contains("password: 'secret'"));
    }

    @Test
    public void credentials_areOmittedWhenPartial() {
        TunNativeToolConfig config = TunNativeToolConfig.builder()
                .credentials("alice", "")
                .build();
        String rendered = config.toConfigString();

        assertFalse(rendered.contains("username:"));
        assertFalse(rendered.contains("password:"));
    }
}
