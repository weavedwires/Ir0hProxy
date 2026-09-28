package su.weavedwires.iroh.vpn.nativ;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import su.weavedwires.iroh.vpn.nativ.error.NativeError;

public class BinaryNativeTool extends NativeTool {
    private final String tag = getClass().getSimpleName();
    private final File workDir;
    private final File binary;
    private final ExecutorService monitorExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean stopping;
    private Process process;

    public BinaryNativeTool(File workDir, File binary) {
        this.workDir = workDir;
        this.binary = binary;
    }

    protected void runProcess(CmdBuilder cmd) throws IOException {
        Log.i(tag, "starting: " + cmd);

        stopping = false;
        setRunning(true);
        clearError();

        ProcessBuilder pb = new ProcessBuilder(cmd.toList());
        pb.redirectErrorStream(true);
        pb.directory(workDir);
        process = pb.start();

        monitorProcess();
    }

    @Override
    public void stop() {
        stopping = true;
        if (process != null) {
            process.destroy();
            process = null;
        }
        setRunning(false);
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

        setRunning(false);

        if (code == 143) return;

        pushError(new NativeError(code, lastStr));
    }
}
