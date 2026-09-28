package su.weavedwires.iroh.vpn.model;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import su.weavedwires.iroh.vpn.Constant;

public class ConnectionStore {

    private final SharedPreferences prefs;

    public ConnectionStore(Context context) {
        prefs = encryptedPrefs(context);
    }

    public List<Connection> load() {
        List<Connection> connections = new ArrayList<>();
        String raw = prefs.getString(Constant.CONNECTIONS, null);
        if (raw != null && !raw.isEmpty()) {
            try {
                JSONArray array = new JSONArray(raw);
                for (int i = 0; i < array.length(); i++) {
                    connections.add(Connection.fromJson(array.getJSONObject(i)));
                }
            } catch (JSONException ignored) {
            }
        }
        return connections;
    }

    public void save(List<Connection> connections) {
        JSONArray array = new JSONArray();
        for (Connection connection : connections) {
            array.put(connection.toJson());
        }
        prefs.edit().putString(Constant.CONNECTIONS, array.toString()).apply();
    }

    public void add(Connection connection) {
        List<Connection> connections = load();
        connections.add(connection);
        save(connections);
    }

    public void update(int index, Connection connection) {
        List<Connection> connections = load();
        if (index >= 0 && index < connections.size()) {
            connections.set(index, connection);
            save(connections);
        }
    }

    public void delete(int index) {
        List<Connection> connections = load();
        if (index >= 0 && index < connections.size()) {
            connections.remove(index);
            save(connections);
        }
    }

    private static SharedPreferences encryptedPrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    context,
                    Constant.CONNECTIONS_PREFS,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            return context.getSharedPreferences(Constant.CONNECTIONS_PREFS, Context.MODE_PRIVATE);
        }
    }
}
