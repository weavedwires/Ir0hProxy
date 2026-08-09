package su.weavedwires.iroh.vpn;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ProxyConfigActivity extends AppCompatActivity {
    private EditText listenAddressField;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_proxy_config);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.proxy_config), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        listenAddressField = findViewById(R.id.listen_address);
        SharedPreferences prefs = getSharedPreferences(getString(R.string.prefs_name), MODE_PRIVATE);
        listenAddressField.setText(prefs.getString(getString(R.string.listen_address), ""));
    }

    @Override
    protected void onStop() {
        super.onStop();
        String listenAddress = listenAddressField.getText().toString().trim();
        SharedPreferences prefs = getSharedPreferences(getString(R.string.prefs_name), MODE_PRIVATE);
        var editor = prefs.edit();
        if (listenAddress.isEmpty()) {
            editor.remove(getString(R.string.listen_address));
        } else {
            editor.putString(getString(R.string.listen_address), listenAddress);
        }
        editor.apply();
    }
}
