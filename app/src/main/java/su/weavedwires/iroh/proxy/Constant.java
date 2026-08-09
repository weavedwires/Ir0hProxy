package su.weavedwires.iroh.proxy;

import androidx.annotation.NonNull;

public enum Constant {
    TAG("IrohProxy"),
    PREFS_NAME("app_prefs"),
    RELAY_ADDRESS("intermediateAddress"),
    ENDPOINT_KEY("endpointKey"),
    LISTEN_ADDRESS("listenAddress");

    Constant(String str) {
        this.str = str;
    }

    private String str;

    @NonNull
    @Override
    public String toString() {
        return str;
    }

    public String str() {
        return toString();
    }
}
