package su.weavedwires.iroh.proxy;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static su.weavedwires.iroh.proxy.Constant.*;

public class ProxyController {
    private static final String BINARY_NAME = "libiroh-socks.so";

    public interface ProxyListener {
        void onProcessExited(int code, String error);
    }

    private final Context context;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile String lastError;
    private volatile ProxyListener listener;

    private Process process;
    private Thread monitorThread;

    public ProxyController(Context context) {
        this.context = context;
    }

    public boolean isRunning() {
        return running.get();
    }

    public String getLastError() {
        return lastError;
    }

    public void setListener(ProxyListener listener) {
        this.listener = listener;
    }

    public void start(String relayAddress, String endpointKey, String listenAddress) throws IOException {
        File binary = extractBinary();
        List<String> cmd = new ArrayList<>();
        cmd.add(binary.getAbsolutePath());

        if (relayAddress != null && !relayAddress.isEmpty()) {
            cmd.add("-r");
            cmd.add(relayAddress);
        }

        cmd.add("client");

        if (endpointKey != null && !endpointKey.isEmpty()) {
            cmd.add("-k");
            cmd.add(endpointKey);
        }

        cmd.add("-l");
        cmd.add(listenAddress);

        Log.d(TAG.str(), "starting: " + String.join(" ", cmd));

        running.set(true);
        lastError = null;

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        pb.directory(context.getFilesDir());
        process = pb.start();

        monitorProcess();
    }

    public void stop() {
        if (process != null) {
            process.destroy();
            process = null;
        }
        running.set(false);
    }

    private File extractBinary() throws IOException {
        File src = new File(context.getApplicationInfo().nativeLibraryDir, BINARY_NAME);
        if (!src.exists()) {
            throw new IOException("native binary not found: " + src.getAbsolutePath());
        }

        File dest = new File(context.getFilesDir(), "iroh-socks");
        if (!dest.exists() || dest.length() != src.length()) {
            try (InputStream in = new FileInputStream(src);
                 OutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            }
        }
        if (!dest.setExecutable(true, false)) {
            Log.w(TAG.str(), "could not set executable bit on " + dest);
        }
        return dest;
    }

    private void monitorProcess() {
        monitorThread = new Thread(() -> {
            int code;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.d(TAG.str(), line);
                }
            } catch (IOException e) {
                Log.w(TAG.str(), "error reading process output", e);
            }
            try {
                code = process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                code = -1;
            }
            Log.d(TAG.str(), "iroh-socks exited with code " + code);

            String error = null;
            if (code != 0 && lastError == null) {
                error = "iroh-socks exited with code " + code;
                lastError = error;
            }
            running.set(false);

            ProxyListener l = listener;
            if (l != null) {
                l.onProcessExited(code, error);
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }
}
