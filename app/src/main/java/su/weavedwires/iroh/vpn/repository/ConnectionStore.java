package su.weavedwires.iroh.vpn.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import su.weavedwires.iroh.vpn.constant.Constant;
import su.weavedwires.iroh.vpn.model.Connection;

public class ConnectionStore {

    private final SharedPreferences prefs;
    private List<Connection> connections;

    public ConnectionStore(Context context) {
        prefs = getPrefs(context);
    }

    public List<Connection> load() {
        if (connections == null) {
            connections = read();
        }
        return connections;
    }

    public int add(Connection connection) {
        List<Connection> list = load();
        list.add(connection);
        persist();
        return list.size() - 1;
    }

    public void update(int index, Connection connection) {
        List<Connection> list = load();
        if (index >= 0 && index < list.size()) {
            list.set(index, connection);
            persist();
        }
    }

    public void delete(int index) {
        List<Connection> list = load();
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            persist();
        }
    }

    private List<Connection> read() {
        List<Connection> list = new ArrayList<>();
        String raw = prefs.getString(Constant.CONNECTIONS, null);
        if (raw != null && !raw.isEmpty()) {
            try {
                JSONArray array = new JSONArray(raw);
                for (int i = 0; i < array.length(); i++) {
                    list.add(Connection.fromJson(array.getJSONObject(i)));
                }
            } catch (JSONException ignored) {
            }
        }
        return list;
    }

    private void persist() {
        JSONArray array = new JSONArray();
        for (Connection connection : connections) {
            array.put(connection.toJson());
        }
        prefs.edit().putString(Constant.CONNECTIONS, array.toString()).apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        try {
            return getEncryptedRefs(context);
        } catch (Exception e) {
            return context.getSharedPreferences(Constant.CONNECTIONS_PREFS, Context.MODE_PRIVATE);
        }
    }

    @SuppressWarnings("deprecation")
    private static SharedPreferences getEncryptedRefs(Context context) throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        return EncryptedSharedPreferences.create(
                context,
                Constant.CONNECTIONS_PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }
}
