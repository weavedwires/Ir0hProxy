package su.weavedwires.iroh.vpn.nativ;

import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import su.weavedwires.iroh.vpn.nativ.error.NativeError;

public class BinaryNativeTool extends NativeTool {
    private static final long GRACEFUL_STOP_TIMEOUT_MS = 2000L;
    private static final long FORCED_STOP_TIMEOUT_MS = 500L;
    private static final long EXIT_POLL_INTERVAL_MS = 50L;

    private final String tag = getClass().getSimpleName();
    private final File workDir;
    private final File binary;
    private final ExecutorService monitorExecutor = Executors.newSingleThreadExecutor();
    private Process process;
    private AtomicBoolean stopExpected;

    public BinaryNativeTool(File workDir, File binary) {
        this.workDir = workDir;
        this.binary = binary;
    }

    protected synchronized void runProcess(CmdBuilder cmd) throws IOException {
        Log.i(tag, "starting: " + cmd);

        if (process != null) {
            Log.w(tag, "process already exists, stopping it before start");
            stopLocked();
        }

        ProcessBuilder pb = new ProcessBuilder(cmd.toList());
        pb.redirectErrorStream(true);
        pb.directory(workDir);

        Process started = pb.start();
        AtomicBoolean expectedStop = new AtomicBoolean(false);
        process = started;
        stopExpected = expectedStop;
        setRunning(true);

        monitorExecutor.execute(() -> monitorLoop(started, expectedStop));
    }

    @Override
    public synchronized void stop() {
        stopLocked();
    }

    private void stopLocked() {
        Process p = process;
        AtomicBoolean expectedStop = stopExpected;
        process = null;
        stopExpected = null;

        if (expectedStop != null) {
            expectedStop.set(true);
        }
        if (p == null) {
            setRunning(false);
            return;
        }

        p.destroy();
        try {
            if (!waitForExit(p, GRACEFUL_STOP_TIMEOUT_MS)) {
                Log.w(tag, "process did not exit in time, killing it");
                destroyForcibly(p);
                waitForExit(p, FORCED_STOP_TIMEOUT_MS);
            }
        } finally {
            setRunning(false);
        }
    }

    private static boolean waitForExit(Process process, long timeoutMs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                return process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                process.exitValue();
                return true;
            } catch (IllegalThreadStateException ignored) {
                try {
                    Thread.sleep(EXIT_POLL_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private static void destroyForcibly(Process process) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            process.destroyForcibly();
        } else {
            // Process.destroyForcibly() is not available before API 26.
            process.destroy();
        }
    }

    private void monitorLoop(Process p, AtomicBoolean expectedStop) {
        int code;
        String lastStr = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lastStr = line;
                Log.d(binary.getName(), line);
            }
        } catch (IOException e) {
            Log.w(tag, "reading process output", e);
        }
        try {
            code = p.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            code = -1;
        }

        boolean current;
        synchronized (this) {
            current = process == p;
            if (current) {
                setRunning(false);
            }
        }

        if (!current || expectedStop.get() || code == 143) {
            return;
        }

        pushError(new NativeError(code, lastStr));
    }
}
