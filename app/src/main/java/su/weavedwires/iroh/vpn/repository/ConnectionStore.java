package su.weavedwires.iroh.vpn.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import su.weavedwires.iroh.vpn.constant.Constant;
import su.weavedwires.iroh.vpn.model.Connection;

public class ConnectionStore {
    private final SharedPreferences prefs;
    private LinkedHashMap<Long, Connection> connections;

    public ConnectionStore(Context context) {
        prefs = getPrefs(context);
    }

    public List<Connection> getList() {
        load();
        return new ArrayList<>(connections.values());
    }

    private void load() {
        connections = read();
    }

    public int size() {
        return connections.size();
    }

    public boolean isEmpty() {
        load();
        return connections.isEmpty();
    }

    public boolean contains(long id) {
        load();
        return connections.containsKey(id);
    }

    public Connection getById(long id) {
        load();
        return connections.get(id);
    }

    public long add(Connection connection) {
        load();
        connections.put(connection.getId(), connection);
        persist();
        return connection.getId();
    }

    public void update(long id, Connection connection) {
        load();
        connections.put(id, connection);
        persist();
    }

    public void delete(long id) {
        load();
        connections.remove(id);
        persist();
    }

    public void setSelected(Connection connection) {
        prefs.edit().putString(Constant.SELECTED, String.valueOf(connection.getId())).apply();
    }

    public Connection getSelected() {
        try {
            return getById(Long.parseLong(prefs.getString(Constant.SELECTED, "")));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LinkedHashMap<Long, Connection> read() {
        LinkedHashMap<Long, Connection> map = new LinkedHashMap<>();
        Set<String> raw = prefs.getStringSet(Constant.CONNECTIONS, new HashSet<>());
        try {
            for (String connStr : raw) {
                Connection connection = Connection.fromJson(new JSONObject(connStr));
                map.put(connection.getId(), connection);
            }
        } catch (JSONException e) {
            Log.w("ConnectionStore", "Can`t decode object", e);
        }
        return map;
    }

    private void persist() {
        Set<String> strSet = new HashSet<>();
        for (Connection connection : connections.values()) {
            strSet.add(connection.toJson().toString());
        }
        prefs.edit().putStringSet(Constant.CONNECTIONS, strSet).apply();
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
