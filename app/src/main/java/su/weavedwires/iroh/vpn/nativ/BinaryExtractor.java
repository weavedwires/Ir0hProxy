package su.weavedwires.iroh.vpn.nativ;

import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BinaryExtractor {
    private final File srcDir;
    private final String name;
    private final File dstDir;

    public BinaryExtractor(File srcDir, String name, File dstDir) {
        this.srcDir = srcDir;
        this.name = name;
        this.dstDir = dstDir;
    }

    public File extract() throws IOException {
        if (!dstDir.exists()) {
            if (!dstDir.mkdirs()) {
                throw new IOException("cannot create workdir: " + dstDir.getAbsolutePath());
            }
        }

        File src = new File(srcDir, name + ".so");
        if (!src.exists()) {
            throw new IOException("native binary not found: " + src.getAbsolutePath());
        }

        File dest = new File(dstDir, name);

        if (!isEquals(dest, src)) {
            copy(src, dest);
        }

        if (!dest.setExecutable(true, false)) {
            throw new IOException("could not set executable bit on " + dest.getAbsolutePath());
        }

        return dest;
    }

    private void copy(File src, File dest) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dest)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                in.transferTo(out);
            } else {
                transferTo(in, out);
            }
        }
    }

    private void transferTo(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[65536];
        int read;
        while ((read = in.read(buffer, 0, 65536)) >= 0) {
            out.write(buffer, 0, read);
        }
    }

    private static boolean isEquals(File dest, File src) {
        if (!dest.isFile() || dest.length() != src.length()) {
            return false;
        }
        byte[] srcHash = sha256(src);
        byte[] destHash = sha256(dest);
        if (srcHash == null || destHash == null) {
            return false;
        }
        return MessageDigest.isEqual(srcHash, destHash);
    }

    private static byte[] sha256(File file) {
        try (InputStream in = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[65536];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
            return digest.digest();
        } catch (IOException | NoSuchAlgorithmException e) {
            return null;
        }
    }
}
