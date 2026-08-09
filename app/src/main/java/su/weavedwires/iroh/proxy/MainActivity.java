package su.weavedwires.iroh.proxy;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "app_prefs";
    private static final String INTERMEDIATE_ADDR = "intermediateAddress";
    private static final String ENDPOINT_KEY = "endpointKey";

    private EditText intermediateAddressField;
    private EditText endpointKeyField;
    private Button enableButton;
    private TextView statusText;

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

        intermediateAddressField = findViewById(R.id.intermediate_address);
        endpointKeyField = findViewById(R.id.endpoint_key);
        enableButton = findViewById(R.id.enable_button);
        enableButton.setOnClickListener(this::changeState);
        statusText = findViewById(R.id.status_text);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        intermediateAddressField.setText(prefs.getString(INTERMEDIATE_ADDR, ""));
        endpointKeyField.setText(prefs.getString(ENDPOINT_KEY, ""));
    }

    private void changeState(View view) {
        if (enabled.get()) {
            disable();
        } else {
            enable();
        }
    }

    private void enable() {
        enabled.set(true);
        enableButton.setText(R.string.disable);
        statusText.setText(R.string.enabled);
        statusText.setTextColor(ContextCompat.getColor(this, R.color.enabled));
    }

    private void disable() {
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
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(INTERMEDIATE_ADDR, intermediateAddressField.getText().toString())
                .putString(ENDPOINT_KEY, endpointKeyField.getText().toString())
                .apply();
    }
}