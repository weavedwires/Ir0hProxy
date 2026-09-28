package su.weavedwires.iroh.vpn.nativ;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import su.weavedwires.iroh.vpn.nativ.error.NativeError;
import su.weavedwires.iroh.vpn.nativ.error.NativeErrorListener;

public class BinaryRunner {
    private final String tag = getClass().getSimpleName();
    private final File workDir;
    private final File binary;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService monitorExecutor = Executors.newSingleThreadExecutor();
    private final List<NativeErrorListener> errorListeners = new ArrayList<>();
    private volatile NativeError lastError;
    private volatile boolean stopping;
    private Process process;

    public BinaryRunner(File workDir, File binary) {
        this.workDir = workDir;
        this.binary = binary;
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

    protected void runProcess(CmdBuilder cmd) throws IOException {
        Log.i(tag, "starting: " + cmd);

        stopping = false;
        running.set(true);
        lastError = null;

        ProcessBuilder pb = new ProcessBuilder(cmd.toList());
        pb.redirectErrorStream(true);
        pb.directory(workDir);
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

    private void monitorProcess() {
        final Process p = process;
        monitorExecutor.execute(() -> monitorLoop(p));
    }

    private void monitorLoop(final Process p) {
        int code;
        String lastStr = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            lastStr = reader.readLine();
            while (reader.ready()) {
                lastStr = reader.readLine();
                Log.d(binary.getName(), lastStr);
            }
        } catch (IOException e) {
            if (stopping) {
                Log.d(tag, "process stopped");
            } else {
                Log.w(tag, "lastStr reading process output", e);
            }
        }
        try {
            code = p.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            code = -1;
        }

        running.set(false);

        if (code == 143) return;

        pushError(new NativeError(code, lastStr));
    }

    private void pushError(NativeError error) {
        lastError = error;
        for (NativeErrorListener l : errorListeners) {
            l.onNativeProcessExited(lastError);
        }
    }
}
