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

import su.weavedwires.iroh.vpn.error.NativeError;
import su.weavedwires.iroh.vpn.error.NativeErrorListener;
import su.weavedwires.iroh.vpn.proxy.ProxyController;
import su.weavedwires.iroh.vpn.proxy.ProxyService;

public class MainActivity extends AppCompatActivity implements NativeErrorListener {
    private EditText relayAddressField;
    private EditText endpointKeyField;
    private Button enableButton;
    private TextView statusText;
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
        findViewById(R.id.proxy_text).setOnClickListener(this::goToProxyConfig);

        SharedPreferences prefs = getSharedPreferences(getString(R.string.prefs_name), MODE_PRIVATE);
        relayAddressField.setText(prefs.getString(getString(R.string.relay_address), ""));
        endpointKeyField.setText(prefs.getString(getString(R.string.endpoint_key), ""));

        proxyController = ((IrohProxyApp) getApplication()).getProxyController();
    }

    @Override
    protected void onStart() {
        super.onStart();
        proxyController.addListener(this);
        syncState();
    }

    private void syncState() {
        boolean isRunning = proxyController.isRunning();
        if (isRunning) {
            setUIEnabled();
        } else if (proxyController.getLastError() != null) {
            setUIError(proxyController.getLastError());
        } else {
            setUIDisabled();
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
        start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS) {
            start();
        }
    }

    private void start() {
        saveInputs();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, ProxyService.class).setAction(getString(R.string.action_start)));
        } else {
            startService(new Intent(this, ProxyService.class).setAction(getString(R.string.action_start)));
        }

        setUIEnabled();
    }

    private void disable() {
        stopService(new Intent(this, ProxyService.class).setAction(getString(R.string.action_stop)));
        setUIDisabled();
    }

    @Override
    public void onNativeProcessExited(NativeError error) {
        getMainExecutor().execute(
                () -> setUIError(error)
        );
    }

    private void setUIEnabled() {
        enabled.set(true);
        enableButton.setText(R.string.disable);
        statusText.setText(R.string.enabled);
        statusText.setTextColor(ContextCompat.getColor(this, R.color.enabled));
    }

    private void setUIDisabled() {
        enabled.set(false);
        enableButton.setText(R.string.enable);
        statusText.setText(R.string.disabled);
        statusText.setTextColor(ContextCompat.getColor(this, R.color.disabled));
    }

    private void setUIError(NativeError error) {
        enabled.set(false);
        enableButton.setText(R.string.enable);
        statusText.setText(error.getCode() + ": " + error.getDescription());
        statusText.setTextColor(ContextCompat.getColor(this, R.color.disabled));
    }

    @Override
    protected void onStop() {
        super.onStop();
        proxyController.removeListener(this);
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
