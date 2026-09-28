package su.weavedwires.iroh.vpn;

import java.net.InetAddress;

public class Constant {
    public static final String PREFS_NAME = "PREFS_NAME";
    public static final String CONNECTIONS_PREFS = "CONNECTIONS_PREFS";
    public static final String CONNECTIONS = "CONNECTIONS";
    public static final String SELECTED = "SELECTED";
    public static final String ENABLE = "ENABLE";
    public static final String LAST_ERROR = "LAST_ERROR";
    public static final String MODE = "MODE";
    public static final String HOST = "HOST";
    public static final String PORT = "PORT";
    public static final String DNS = "DNS";
    public static final String IROH_BINARY_NAME = "dumbpipe";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String ACTION_START = "ACTION_START";
    public static final String EXTRA_TICKET = "EXTRA_TICKET";
    public static final String EXTRA_USER = "EXTRA_USER";
    public static final String EXTRA_PASSWORD = "EXTRA_PASSWORD";
    public static final String EXTRA_NAME = "EXTRA_NAME";
    public static final String EXTRA_INDEX = "EXTRA_INDEX";

    // Legacy string values kept only to migrate older SharedPreferences entries.
    public static final String LEGACY_MODE_VPN = "vpn";
    public static final String LEGACY_MODE_PROXY = "proxy";
    public static final String LEGACY_LOCAL_HOST = "127.0.0.1";
    public static final String LEGACY_PUBLIC_HOST = "0.0.0.0";

    public static final InetAddress LOCAL_HOST_ADDRESS = ipv4("127.0.0.1");
    public static final InetAddress PUBLIC_HOST_ADDRESS = ipv4("0.0.0.0");

    public static final int DEFAULT_PORT = 2081;

    public static final int TUN_MTU = 8500;
    public static final InetAddress TUN_IPV4_ADDRESS = ipv4("198.18.0.1");
    public static final int TUN_IPV4_PREFIX = 32;
    public static final InetAddress DNS_SERVER = ipv4("198.18.0.2");
    public static final int TASK_STACK_SIZE = 81920;
    public static final int MAPDNS_PORT = 53;
    public static final InetAddress MAPDNS_NETWORK = ipv4("240.0.0.0");
    public static final InetAddress MAPDNS_NETMASK = ipv4("240.0.0.0");
    public static final int MAPDNS_CACHE_SIZE = 10000;

    private Constant() {
    }

    private static InetAddress ipv4(String literal) {
        try {
            return InetAddress.getByName(literal);
        } catch (java.net.UnknownHostException e) {
            throw new IllegalStateException("Invalid IPv4 literal: " + literal, e);
        }
    }
}
