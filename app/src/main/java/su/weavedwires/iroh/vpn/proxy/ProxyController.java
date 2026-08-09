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

import su.weavedwires.iroh.vpn.error.NativeError;
import su.weavedwires.iroh.vpn.error.NativeErrorListener;
import su.weavedwires.iroh.vpn.R;

public class ProxyController {
    private static final String TAG = ProxyController.class.getSimpleName();
    private final Context context;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService monitorExecutor = Executors.newSingleThreadExecutor();
    private final List<NativeErrorListener> errorListeners = new ArrayList<>();
    private volatile NativeError lastError;
    private volatile boolean stopping;

    private Process process;

    public ProxyController(Context context) {
        this.context = context;
    }

    public boolean isRunning() {
        return running.get();
    }

    public NativeError getLastError() {
        return lastError;
    }

    public void addListener(NativeErrorListener listener) {
        errorListeners.add(listener);
    }

    public void removeListener(NativeErrorListener listener) {
        errorListeners.remove(listener);
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

        Log.i(TAG, "starting: " + String.join(" ", cmd));

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
            Log.w(TAG, "could not set executable bit on " + dest);
        }
        return dest;
    }

    private void monitorProcess() {
        final Process p = process;
        monitorExecutor.execute(() -> monitorLoop(p));
    }

    private void monitorLoop(final Process p) {
        int code;
        StringBuilder error = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            Log.d(TAG, line);
            error.append(line);
            while (reader.ready()) {
                line = reader.readLine();
                Log.d(TAG, line);
                error.append(System.lineSeparator()).append(line);
            }
        } catch (IOException e) {
            if (stopping) {
                Log.d(TAG, "process stopped");
            } else {
                Log.w(TAG, "error reading process output", e);
            }
        }
        try {
            code = p.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            code = -1;
        }
        Log.d(TAG, "iroh-socks exited with code " + code);

        running.set(false);

        if (code == 143) return;

        lastError = new NativeError(code, error.toString());
        for (NativeErrorListener l : errorListeners) {
            l.onNativeProcessExited(lastError);
        }
    }
}
