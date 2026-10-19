package su.weavedwires.iroh.vpn.repository;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

import su.weavedwires.iroh.vpn.constant.FilterMode;
import su.weavedwires.iroh.vpn.constant.Constant;
import su.weavedwires.iroh.vpn.model.VpnAppFilter;

public class AppRepository {
    private final SharedPreferences prefs;
    private Set<String> packages;

    public AppRepository(Context context) {
        prefs = context.getSharedPreferences(Constant.APP_PREFS, Context.MODE_PRIVATE);
        packages = prefs.getStringSet(Constant.APPS, new HashSet<>());
    }

    public void setApps(Set<String> packages) {
        this.packages = packages;
        savePackages();
    }

    public Set<String> getApps() {
        return packages;
    }

    public void savePackages() {
        System.out.println(packages);
        prefs.edit().putStringSet(Constant.APPS, packages).apply();
    }

    public FilterMode getMode() {
        String enumStr = prefs.getString(Constant.APP_MODE, FilterMode.BLACKLIST.toString());
        return FilterMode.fromString(enumStr);
    }

    public void switchMode() {
        setMode(FilterMode.getAntagonist(getMode()));
    }

    public void setMode(FilterMode mode) {
        prefs.edit().putString(Constant.APP_MODE, mode.toString()).apply();
    }
}
