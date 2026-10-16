package su.weavedwires.iroh.vpn.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicLong;

import lombok.Data;

@Data
public class Connection implements Parcelable {
    private final long id;
    private final String name;
    private final String user;
    private final String password;
    private final String ticket;

    public Connection(String name, String user, String password, String ticket) {
        this(IdGenerator.getUnusedId(), name, user, password, ticket);
    }

    public Connection(long id, String name, String user, String password, String ticket) {
        IdGenerator.notifyUsed(id);
        this.id = id;
        this.name = name;
        this.user = user;
        this.password = password;
        this.ticket = ticket;
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        try {
            object.put("id", getId());
            object.put("name", getName());
            object.put("user", getUser());
            object.put("password", getPassword());
            object.put("ticket", getTicket());
        } catch (JSONException e) {
            Log.w("Connection", "Can`t encode object");
        }
        return object;
    }

    public static Connection fromJson(JSONObject object) {
        return new Connection(
                object.optLong("id"),
                object.optString("name"),
                object.optString("user"),
                object.optString("password"),
                object.optString("ticket")
        );
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeString(user);
        dest.writeString(password);
        dest.writeString(ticket);
    }

    public static final Creator<Connection> CREATOR = new Creator<>() {
        @Override
        public Connection createFromParcel(Parcel in) {
            return new Connection(
                    in.readLong(),
                    in.readString(),
                    in.readString(),
                    in.readString(),
                    in.readString()
            );
        }

        @Override
        public Connection[] newArray(int size) {
            return new Connection[size];
        }
    };

    private static class IdGenerator {
        private static final AtomicLong COUNTER = new AtomicLong();

        public static long getUnusedId() {
            return COUNTER.incrementAndGet();
        }

        public static void notifyUsed(long usedId) {
            COUNTER.accumulateAndGet(usedId, Math::max);
        }
    }
}
