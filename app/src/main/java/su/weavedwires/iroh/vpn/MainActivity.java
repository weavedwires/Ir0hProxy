package su.weavedwires.iroh.vpn;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.concurrent.atomic.AtomicBoolean;

import su.weavedwires.iroh.vpn.proxy.ProxyController;
import su.weavedwires.iroh.vpn.proxy.ProxyService;

public class MainActivity extends AppCompatActivity {
    private EditText relayAddressField;
    private EditText endpointKeyField;
    private Button enableButton;
    private TextView statusText;
    private TextView proxyText;

    private ProxyController proxyController;
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        relayAddressField = findViewById(R.id.relay_address);
        endpointKeyField = findViewById(R.id.endpoint_key);
        enableButton = findViewById(R.id.enable_button);
        enableButton.setOnClickListener(this::changeState);
        statusText = findViewById(R.id.status_text);
        proxyText = findViewById(R.id.proxy_text);
        proxyText.setOnClickListener(this::goToProxyConfig);

        proxyController = ((IrohProxyApp) getApplication()).getProxyController();

        SharedPreferences prefs = getSharedPreferences(getString(R.string.prefs_name), MODE_PRIVATE);
        relayAddressField.setText(prefs.getString(getString(R.string.relay_address), ""));
        endpointKeyField.setText(prefs.getString(getString(R.string.endpoint_key), ""));

        syncState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        syncState();
    }

    private void syncState() {
        boolean isRunning = proxyController.isRunning();
        enabled.set(isRunning);
        enableButton.setText(isRunning ? R.string.disable : R.string.enable);
        if (isRunning) {
            statusText.setText(R.string.enabled);
            statusText.setTextColor(ContextCompat.getColor(this, R.color.enabled));
        } else if (proxyController.getLastError() != null) {
            statusText.setText(proxyController.getLastError());
            statusText.setTextColor(ContextCompat.getColor(this, R.color.disabled));
        } else {
            statusText.setText(R.string.disabled);
            statusText.setTextColor(ContextCompat.getColor(this, R.color.disabled));
        }
    }

    private void changeState(View view) {
        if (enabled.get()) {
            disable();
        } else {
            enable();
        }
    }

    private void goToProxyConfig(View view) {
        startActivity(new Intent(this, ProxyConfigActivity.class));
    }

    private static final int REQUEST_NOTIFICATIONS = 1;
    private void enable() {
        if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS
            );
            return;
        }
        startProxy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS) {
            startProxy();
        }
    }

    private void startProxy() {
        saveInputs();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, ProxyService.class).setAction(getString(R.string.action_start)));
        } else {
            startService(new Intent(this, ProxyService.class).setAction(getString(R.string.action_start)));
        }

        enabled.set(true);
        enableButton.setText(R.string.disable);
        statusText.setText(R.string.enabled);
        statusText.setTextColor(ContextCompat.getColor(this, R.color.enabled));
    }

    private void disable() {
        stopService(new Intent(this, ProxyService.class).setAction(getString(R.string.action_stop)));

        enabled.set(false);
        enableButton.setText(R.string.enable);
        statusText.setText(R.string.disabled);
        statusText.setTextColor(ContextCompat.getColor(this, R.color.disabled));
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveInputs();
    }

    private void saveInputs() {
        var editor = getSharedPreferences(getString(R.string.prefs_name), MODE_PRIVATE).edit();

        String relayAddress = relayAddressField.getText().toString().trim();
        if (relayAddress.isEmpty()) {
            editor.remove(getString(R.string.relay_address));
        } else {
            editor.putString(getString(R.string.relay_address), relayAddress);
        }

        String endpointKey = endpointKeyField.getText().toString().trim();
        if (endpointKey.isEmpty()) {
            editor.remove(getString(R.string.endpoint_key));
        } else {
            editor.putString(getString(R.string.endpoint_key), endpointKey);
        }

        editor.apply();
    }
}
