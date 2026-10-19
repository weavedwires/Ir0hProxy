package su.weavedwires.iroh.vpn.constant;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public enum FilterMode {
    BLACKLIST,
    WHITELIST;

    private static final Map<String, FilterMode> LOOKUP = new HashMap<>();
    static {
        for (FilterMode value : values()) {
            LOOKUP.put(value.toString(), value);
        }
    }
    public static FilterMode fromString(String s) {
        return LOOKUP.get(s);
    }

    private static final Map<FilterMode, FilterMode> antagonists = new EnumMap<>(FilterMode.class);
    static {
        antagonists.put(values()[0], values()[1]);
        antagonists.put(values()[1], values()[0]);
    }
    public static FilterMode getAntagonist(FilterMode current) {
        return antagonists.get(current);
    }
}
