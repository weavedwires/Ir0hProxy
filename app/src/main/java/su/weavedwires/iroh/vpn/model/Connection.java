package su.weavedwires.iroh.vpn.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Connection extends IrohSocksLink {

    public Connection(String name, String user, String password, String ticket) {
        super(name, user, password, ticket);
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        try {
            object.put("name", getName());
            object.put("user", getUser());
            object.put("password", getPassword());
            object.put("ticket", getTicket());
        } catch (JSONException ignored) {
        }
        return object;
    }

    public static Connection fromJson(JSONObject object) {
        return new Connection(
                object.optString("name"),
                object.optString("user"),
                object.optString("password"),
                object.optString("ticket")
        );
    }
}
