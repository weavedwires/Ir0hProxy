package su.weavedwires.iroh.vpn.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import su.weavedwires.iroh.vpn.Constant;
import su.weavedwires.iroh.vpn.HostScope;
import su.weavedwires.iroh.vpn.Mode;
import su.weavedwires.iroh.vpn.R;
import su.weavedwires.iroh.vpn.Settings;

public class SettingsActivity extends AppCompatActivity {

    private MaterialSwitch hostSwitch;
    private TextInputEditText portInput;
    private MaterialSwitch vpnSwitch;
    private LinearLayout dnsContainer;
    private Settings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivityIfAvailable(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        settings = new Settings(this);

        hostSwitch = findViewById(R.id.host_switch);
        portInput = findViewById(R.id.port_input);
        vpnSwitch = findViewById(R.id.vpn_switch);
        dnsContainer = findViewById(R.id.dns_container);
        MaterialButton saveButton = findViewById(R.id.save_button);

        hostSwitch.setChecked(settings.getHostScope() == HostScope.PUBLIC);
        portInput.setText(Integer.toString(settings.getPort()));
        vpnSwitch.setChecked(settings.isVpnMode());

        for (InetAddress server : settings.getDnsServers()) {
            addDnsField(server.getHostAddress());
        }

        findViewById(R.id.add_dns).setOnClickListener(v -> addDnsField(""));
        saveButton.setOnClickListener(v -> save());
    }

    private void addDnsField(String value) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_dns, dnsContainer, false);
        TextInputEditText input = row.findViewById(R.id.dns_input);
        if (value != null) {
            input.setText(value);
        }
        ImageButton remove = row.findViewById(R.id.dns_remove);
        remove.setOnClickListener(v -> dnsContainer.removeView(row));
        dnsContainer.addView(row);
    }

    private void save() {
        settings.setHostScope(hostSwitch.isChecked() ? HostScope.PUBLIC : HostScope.LOCAL);
        settings.setPort(parsePort());
        settings.setDnsServers(collectDns());
        settings.setMode(vpnSwitch.isChecked() ? Mode.VPN : Mode.PROXY);
        finish();
    }

    private int parsePort() {
        String text = portInput.getText().toString().trim();
        try {
            int port = Integer.parseInt(text);
            if (port > 0 && port < 65536) {
                return port;
            }
        } catch (NumberFormatException ignored) {
        }
        return Constant.DEFAULT_PORT;
    }

    private List<InetAddress> collectDns() {
        List<InetAddress> servers = new ArrayList<>();
        for (int i = 0; i < dnsContainer.getChildCount(); i++) {
            View row = dnsContainer.getChildAt(i);
            TextInputEditText input = row.findViewById(R.id.dns_input);
            String value = input.getText().toString().trim();
            if (!value.isEmpty()) {
                try {
                    servers.add(InetAddress.getByName(value));
                } catch (UnknownHostException ignored) {
                }
            }
        }
        return servers;
    }
}
