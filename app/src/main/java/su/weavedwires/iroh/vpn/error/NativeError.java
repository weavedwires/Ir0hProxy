package su.weavedwires.iroh.vpn.error;

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
}