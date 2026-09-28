package su.weavedwires.iroh.vpn.nativ.error;

public interface NativeErrorListener {
    void onNativeProcessExited(NativeError error);
}