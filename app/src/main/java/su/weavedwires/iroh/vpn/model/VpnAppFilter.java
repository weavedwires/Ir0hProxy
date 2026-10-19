package su.weavedwires.iroh.vpn.model;

import android.content.pm.PackageManager;
import android.net.VpnService.Builder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import lombok.Data;
import su.weavedwires.iroh.vpn.constant.FilterMode;

@Data
public class VpnAppFilter implements Parcelable {
    private static final String TAG = "VpnAppFilter";
    private final String[] packages;
    private final FilterMode mode;

    public VpnAppFilter(String[] packages, FilterMode mode) {
        this.packages = packages;
        this.mode = mode;
    }

    public void apply(Builder vpn) {
        switch (mode) {
            case BLACKLIST -> addApps(vpn::addDisallowedApplication);
            case WHITELIST -> addApps(vpn::addAllowedApplication);
        }
    }

    private void addApps(PackageConsumer registry) {
        for (String packageName :packages) {
            addApp(registry, packageName);
        }
    }

    private static void addApp(PackageConsumer registry, String packageName) {
        try {
            registry.add(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "app " + packageName + " not found, and not added to vpn configuration");
        }
    }

    public boolean isTriggersWhitelist() {
        return packages.length > 0 && FilterMode.WHITELIST.equals(mode);
    }

    interface PackageConsumer {
        void add(String packageName) throws PackageManager.NameNotFoundException;
    }

    public static final Creator<VpnAppFilter> CREATOR = new Creator<>() {
        @Override
        public VpnAppFilter createFromParcel(Parcel in) {
            return new VpnAppFilter(
                    in.createStringArray(),
                    FilterMode.fromString(in.readString())
            );
        }

        @Override
        public VpnAppFilter[] newArray(int size) {
            return new VpnAppFilter[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeStringArray(packages);
        parcel.writeString(mode.toString());
    }
}
