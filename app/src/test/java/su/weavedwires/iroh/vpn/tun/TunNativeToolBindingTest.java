package su.weavedwires.iroh.vpn.tun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Assume;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Guards the contract between {@code scripts/sync_sockstun.sh} and the Java JNI
 * bridge class.
 *
 * <p>{@code hev-jni.c} resolves the class to register its natives on at run time
 * from {@code -DPKGNAME/-DCLSNAME} (compiled into {@code libhev-socks5-tunnel.so}).
 * If the Java class is moved without updating the script (or vice versa),
 * {@code JNI_OnLoad}'s {@code FindClass} throws {@code ClassNotFoundException} and
 * {@code System.loadLibrary} kills the process. This test fails at build time in
 * that case.
 */
public class TunNativeToolBindingTest {

    private static final Pattern PKGNAME = Pattern.compile("^PKGNAME=\"?([^\"]+)\"?", Pattern.MULTILINE);
    private static final Pattern CLSNAME = Pattern.compile("^CLSNAME=\"?([^\"]+)\"?", Pattern.MULTILINE);

    @Test
    public void scriptPackageAndClass_matchJavaBridgeClass() throws Exception {
        Path script = repoRoot().resolve("scripts/sync_sockstun.sh");
        assertTrue("script not found: " + script, Files.isRegularFile(script));

        String source = new String(Files.readAllBytes(script), StandardCharsets.UTF_8);
        String pkg = firstGroup(PKGNAME, source);
        String cls = firstGroup(CLSNAME, source);

        assertNotNull("PKGNAME not found in " + script, pkg);
        assertNotNull("CLSNAME not found in " + script, cls);

        String fqn = pkg.replace('/', '.') + "." + cls;
        assertEquals("su.weavedwires.iroh.vpn.tun.TunNativeTool", fqn);

        // Initialize=false: loading the class must not run System.loadLibrary in the JVM.
        Class<?> bridge = Class.forName(fqn, false, TunNativeToolBindingTest.class.getClassLoader());
        assertTrue("TunNativeTool must extend NativeTool",
                su.weavedwires.iroh.vpn.nativ.NativeTool.class.isAssignableFrom(bridge));

        assertNativeMethod(bridge, "TProxyStartService", String.class, int.class);
        assertNativeMethod(bridge, "TProxyStopService");
        assertNativeMethod(bridge, "TProxyIsRunning");
        assertNativeMethod(bridge, "TProxyGetStats");
    }

    @Test
    public void builtLibraries_containRegisteredClassName() throws IOException {
        Path jniLibs = repoRoot().resolve("app/src/main/jniLibs");
        List<Path> libraries = new ArrayList<>();
        if (Files.isDirectory(jniLibs)) {
            try (Stream<Path> abis = Files.list(jniLibs)) {
                for (Path abi : (Iterable<Path>) abis::iterator) {
                    Path so = abi.resolve("libhev-socks5-tunnel.so");
                    if (Files.isRegularFile(so)) {
                        libraries.add(so);
                    }
                }
            }
        }
        // Binaries are gitignored; nothing to verify on a fresh clone.
        Assume.assumeFalse("no libhev-socks5-tunnel.so built yet", libraries.isEmpty());

        for (Path so : libraries) {
            // ISO-8859-1 maps bytes to chars 1:1, so contains() is a byte search.
            String blob = new String(Files.readAllBytes(so), StandardCharsets.ISO_8859_1);
            assertTrue(so + " was built for a different bridge class",
                    blob.contains("su/weavedwires/iroh/vpn/tun/TunNativeTool"));
        }
    }

    private static void assertNativeMethod(Class<?> type, String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = type.getDeclaredMethod(name, parameterTypes);
        assertTrue(name + " must be native", Modifier.isNative(method.getModifiers()));
    }

    private static String firstGroup(Pattern pattern, String source) {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Path repoRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("scripts/sync_sockstun.sh"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("repo root not found from " + Paths.get("").toAbsolutePath());
    }
}
