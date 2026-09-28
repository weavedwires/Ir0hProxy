package su.weavedwires.iroh.vpn;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Settings {

    private final SharedPreferences prefs;

    public Settings(Context context) {
        prefs = context.getSharedPreferences(Constant.PREFS_NAME, Context.MODE_PRIVATE);
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

    public Mode getMode() {
        String stored = prefs.getString(Constant.MODE, null);
        if (stored == null) {
            return Mode.VPN;
        }
        Mode mode = enumByName(Mode.class, stored);
        if (mode != null) {
            return mode;
        }
        return Constant.LEGACY_MODE_PROXY.equals(stored) ? Mode.PROXY : Mode.VPN;
    }

    public void setMode(Mode mode) {
        prefs.edit().putString(Constant.MODE, mode.name()).apply();
    }

    public boolean isVpnMode() {
        return getMode() == Mode.VPN;
    }

    public boolean isEnabled() {
        return prefs.getBoolean(Constant.ENABLE, false);
    }

    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(Constant.ENABLE, enabled).apply();
    }

    public int getSelected() {
        return prefs.getInt(Constant.SELECTED, -1);
    }

    public void setSelected(int index) {
        prefs.edit().putInt(Constant.SELECTED, index).apply();
    }

    public String getLastError() {
        return prefs.getString(Constant.LAST_ERROR, null);
    }

    public void setLastError(String message) {
        prefs.edit().putString(Constant.LAST_ERROR, message).apply();
    }

    public void clearLastError() {
        prefs.edit().remove(Constant.LAST_ERROR).apply();
    }

    public void registerListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
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
