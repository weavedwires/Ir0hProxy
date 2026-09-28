package su.weavedwires.iroh.vpn.nativ;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.BooleanSupplier;

public final class PortProbe {

    private PortProbe() {
    }

    public static boolean waitForPort(InetSocketAddress address, long timeoutMs, BooleanSupplier isRunning) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (!isRunning.getAsBoolean()) {
                return false;
            }
            try (Socket socket = new Socket()) {
                socket.connect(address, 200);
                return true;
            } catch (IOException ignored) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }
}
