package su.weavedwires.iroh.vpn.nativ;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class CmdBuilder {
    private final List<String> cmd = new ArrayList<>();

    public CmdBuilder add(Object part) {
        return add(part.toString());
    }

    public CmdBuilder add(String part) {
        cmd.add(part);
        return this;
    }

    public List<String> toList() {
        return cmd;
    }

    @NonNull
    @Override
    public String toString() {
        return String.join(" ", cmd);
    }
}
