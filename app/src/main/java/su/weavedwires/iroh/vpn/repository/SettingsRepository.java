package su.weavedwires.iroh.vpn.repository;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import su.weavedwires.iroh.vpn.constant.Constant;
import su.weavedwires.iroh.vpn.constant.HostScope;
import su.weavedwires.iroh.vpn.constant.NetworkMode;

public class SettingsRepository {

    private final SharedPreferences prefs;

    public SettingsRepository(Context context) {
        prefs = context.getSharedPreferences(Constant.SETTINGS_PREFS, Context.MODE_PRIVATE);
    }

    public InetAddress getHost() {
        return getHostScope().address();
    }

    public HostScope getHostScope() {
        String stored = prefs.getString(Constant.HOST, null);
        if (stored == null) {
            return HostScope.LOCAL;
        }
        HostScope scope = enumByName(HostScope.class, stored);
        if (scope != null) {
            return scope;
        }
        if (Constant.LEGACY_PUBLIC_HOST.equals(stored)) {
            return HostScope.PUBLIC;
        }
        return HostScope.LOCAL;
    }

    public void setHostScope(HostScope scope) {
        prefs.edit().putString(Constant.HOST, scope.name()).apply();
    }

    public int getPort() {
        return prefs.getInt(Constant.PORT, Constant.DEFAULT_PORT);
    }

    public void setPort(int port) {
        prefs.edit().putInt(Constant.PORT, port).apply();
    }

    public List<InetAddress> getDnsServers() {
        List<InetAddress> servers = new ArrayList<>();
        String raw = prefs.getString(Constant.DNS, null);
        if (raw != null && !raw.isEmpty()) {
            try {
                JSONArray array = new JSONArray(raw);
                for (int i = 0; i < array.length(); i++) {
                    String value = array.optString(i);
                    if (value != null && !value.isEmpty()) {
                        try {
                            servers.add(InetAddress.getByName(value));
                        } catch (UnknownHostException ignored) {
                        }
                    }
                }
            } catch (JSONException ignored) {
            }
        }
        return servers;
    }

    public void setDnsServers(List<InetAddress> servers) {
        JSONArray array = new JSONArray();
        for (InetAddress server : servers) {
            array.put(server.getHostAddress());
        }
        prefs.edit().putString(Constant.DNS, array.toString()).apply();
    }

    public void ensureDefaultDnsServers() {
        if (prefs.contains(Constant.DNS)) {
            return;
        }
        List<InetAddress> servers = new ArrayList<>();
        for (String value : Constant.DEFAULT_DNS_SERVERS) {
            try {
                servers.add(InetAddress.getByName(value));
            } catch (UnknownHostException ignored) {
            }
        }
        if (!servers.isEmpty()) {
            setDnsServers(servers);
        }
    }

    public NetworkMode getMode() {
        String stored = prefs.getString(Constant.MODE, null);
        if (stored == null) {
            return NetworkMode.VPN;
        }
        NetworkMode mode = enumByName(NetworkMode.class, stored);
        if (mode != null) {
            return mode;
        }
        return Constant.LEGACY_MODE_PROXY.equals(stored) ? NetworkMode.PROXY : NetworkMode.VPN;
    }

    public void setMode(NetworkMode mode) {
        prefs.edit().putString(Constant.MODE, mode.name()).apply();
    }

    public boolean isVpnMode() {
        return getMode() == NetworkMode.VPN;
    }

    private static <T extends Enum<T>> T enumByName(Class<T> type, String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
