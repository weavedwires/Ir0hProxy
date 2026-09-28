package su.weavedwires.iroh.vpn.nativ.error;

import androidx.annotation.NonNull;

public class NativeError {
    public NativeError(int code, String description) {
        this.code = code;
        this.description = description;
    }
    private final int code;
    private final String description;

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("exited with code %d: %s", code, description);
    }
}