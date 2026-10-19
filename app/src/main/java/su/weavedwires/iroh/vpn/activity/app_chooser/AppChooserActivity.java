package su.weavedwires.iroh.vpn.activity.app_chooser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
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
import su.weavedwires.iroh.vpn.constant.FilterMode;
import su.weavedwires.iroh.vpn.repository.AppRepository;

public class AppChooserActivity extends AppCompatActivity {
    private AppRepository repository;
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
        repository = new AppRepository(this);

        SwitchCompat appModeStitch = findViewById(R.id.app_mode_switch);
        appModeStitch.setChecked(FilterMode.WHITELIST.equals(repository.getMode()));
        appModeStitch.setOnClickListener(v -> repository.switchMode());

        RecyclerView appList = findViewById(R.id.apps_list);
        appList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AppAdapter(repository::savePackages);
        adapter.setSelectedPackages(repository.getApps());
        adapter.submitList(getInstalledApps());

        appList.setAdapter(adapter);
    }

    @NonNull
    @SuppressLint("QueryPermissionsNeeded")
    private List<AppItem> getInstalledApps() {
        var pm = getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            resolveInfos = pm.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0)
            );
        } else {
            resolveInfos = pm.queryIntentActivities(intent, 0);
        }

        return resolveInfos.stream()
                .map(info -> AppItem.fromInfo(info, pm))
                .sorted(Comparator.comparing(AppItem::getName))
                .collect(Collectors.toList());
    }
}
