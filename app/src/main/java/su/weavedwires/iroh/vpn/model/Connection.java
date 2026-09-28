package su.weavedwires.iroh.vpn.model;

import org.json.JSONException;
import org.json.JSONObject;


public class Connection {
    private final String name;
    private final String user;
    private final String password;
    private final String ticket;

    public Connection(String name, String user, String password, String ticket) {
        this.name = name;
        this.user = user;
        this.password = password;
        this.ticket = ticket;
    }

    public String getName() {
        return name;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getTicket() {
        return ticket;
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
