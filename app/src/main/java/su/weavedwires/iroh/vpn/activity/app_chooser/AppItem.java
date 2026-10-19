package su.weavedwires.iroh.vpn.activity.app_chooser;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import lombok.Data;

@Data
public class AppItem {
    private final String name;
    private final String packageName;
    private final Drawable icon;

    public static AppItem fromInfo(ResolveInfo info, PackageManager pm) {
        return new AppItem(
                info.loadLabel(pm).toString(),
                info.activityInfo.applicationInfo.packageName,
                info.loadIcon(pm)
        );
    }
}
