package su.weavedwires.iroh.vpn.activity.app_chooser;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.DynamicColors;

import su.weavedwires.iroh.vpn.R;

public class AppChooserActivity extends AppCompatActivity {
    private RecyclerView appList;

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

        var pm = getPackageManager();

        appList = findViewById(R.id.apps_list);

    }
}
