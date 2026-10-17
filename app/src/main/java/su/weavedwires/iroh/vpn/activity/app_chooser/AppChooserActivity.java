package su.weavedwires.iroh.vpn.activity.app_chooser;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.health.connect.datatypes.AppInfo;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.DynamicColors;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import su.weavedwires.iroh.vpn.R;
import su.weavedwires.iroh.vpn.model.AppData;

public class AppChooserActivity extends AppCompatActivity {
    private AppAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivityIfAvailable(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_app_chooser);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.apps_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        List<AppData> apps = getApps();

        RecyclerView appList = findViewById(R.id.apps_list);
        appList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AppAdapter();
        adapter.submitList(apps);

        appList.setAdapter(adapter);

    }

    @NonNull
    @SuppressLint("QueryPermissionsNeeded")
    private List<AppData> getApps() {
        var pm = getPackageManager();

        List<ApplicationInfo> appInfos = pm.getInstalledApplications(0);

        return appInfos.stream()
                .map(info -> toAppData(info, pm))
                .sorted(Comparator.comparing(AppData::getName))
                .collect(Collectors.toList());
    }

    @NonNull
    private static AppData toAppData(ApplicationInfo info, PackageManager pm) {
        return new AppData(info.loadLabel(pm).toString(), pm.getApplicationIcon(info));
    }
}
