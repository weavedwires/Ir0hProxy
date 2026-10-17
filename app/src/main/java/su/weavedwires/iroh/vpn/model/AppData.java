package su.weavedwires.iroh.vpn.model;

import android.graphics.drawable.Drawable;

import lombok.Data;

@Data
public class AppData {
    private final String name;
    private final String packageName;
    private final Drawable icon;
    private boolean checked = false;

    public void flipChecked() {
        checked = !checked;
    }
}
