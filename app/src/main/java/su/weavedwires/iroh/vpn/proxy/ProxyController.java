package su.weavedwires.iroh.vpn.proxy;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import su.weavedwires.iroh.vpn.R;

public class ProxyController {

    public interface ProxyListener {
        void onProcessExited(int code, String error);
    }

    private final Context context;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService monitorExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "iroh-monitor");
                t.setDaemon(true);
                return t;
            });
    private volatile String lastError;
    private volatile ProxyListener listener;
    private volatile boolean stopping;

    private Process process;

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

        cmd.add("-r");
        cmd.add(relayAddress);

        cmd.add("client");

        cmd.add("-k");
        cmd.add(endpointKey);

        cmd.add("-l");
        cmd.add(listenAddress);

        Log.d(context.getString(R.string.tag), "starting: " + String.join(" ", cmd));

        stopping = false;
        running.set(true);
        lastError = null;

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        pb.directory(context.getFilesDir());
        process = pb.start();

        monitorProcess();
    }

    public void stop() {
        stopping = true;
        if (process != null) {
            process.destroy();
            process = null;
        }
        running.set(false);
    }

    private File extractBinary() throws IOException {
        File src = new File(context.getApplicationInfo().nativeLibraryDir, context.getString(R.string.libiroh_socks_so));
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
            Log.w(context.getString(R.string.tag), "could not set executable bit on " + dest);
        }
        return dest;
    }

    private void monitorProcess() {
        final Process p = process;
        monitorExecutor.execute(() -> monitorLoop(p));
    }

    private void monitorLoop(final Process p) {
        int code;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Log.d(context.getString(R.string.tag), line);
            }
        } catch (IOException e) {
            if (stopping) {
                Log.d(context.getString(R.string.tag), "process stopped");
            } else {
                Log.w(context.getString(R.string.tag), "error reading process output", e);
            }
        }
        try {
            code = p.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            code = -1;
        }
        Log.d(context.getString(R.string.tag), "iroh-socks exited with code " + code);

        if (this.process != p) {
            return;
        }

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
    }
}
