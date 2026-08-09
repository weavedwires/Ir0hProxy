package su.weavedwires.iroh.vpn.error;

public interface NativeErrorListener {
    void onNativeProcessExited(NativeError error);
}