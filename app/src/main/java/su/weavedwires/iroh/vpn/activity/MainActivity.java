package su.weavedwires.iroh.vpn.activity;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import su.weavedwires.iroh.vpn.Constant;
import su.weavedwires.iroh.vpn.R;
import su.weavedwires.iroh.vpn.Settings;
import su.weavedwires.iroh.vpn.model.Connection;
import su.weavedwires.iroh.vpn.model.ConnectionStore;
import su.weavedwires.iroh.vpn.model.IrohSocksLink;
import su.weavedwires.iroh.vpn.proxy.ProxyService;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATIONS = 1;
    private static final int REQUEST_VPN = 2;

    private Settings settings;
    private ConnectionStore connectionStore;

    private RecyclerView connectionList;
    private ConnectionView connectionView;
    private FloatingActionButton enableButton;
    private String lastShownError;

    private final SharedPreferences.OnSharedPreferenceChangeListener stateListener =
            (sp, key) -> {
                if (Constant.ENABLE.equals(key) || Constant.LAST_ERROR.equals(key)) {
                    runOnUiThread(this::syncState);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivityIfAvailable(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        settings = new Settings(this);
        connectionStore = new ConnectionStore(this);

        connectionList = findViewById(R.id.connection_list);
        enableButton = findViewById(R.id.enable_button);

        connectionView = new ConnectionView(new ConnectionView.OnConnectionSelectedListener() {
            @Override
            public void onConnectionSelected(int position) {
                selectConnection(position);
            }

            @Override
            public void onConnectionEdit(int position) {
                editConnection(position);
            }
        });
        connectionList.setLayoutManager(new LinearLayoutManager(this));
        connectionList.setAdapter(connectionView);

        enableButton.setOnClickListener(v -> toggleProxy());
        findViewById(R.id.settings_button).setOnClickListener(v -> openSettings());
        findViewById(R.id.add_button).setOnClickListener(this::showAddMenu);

        handleIncomingLink(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingLink(intent);
    }

    private void handleIncomingLink(Intent intent) {
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction()) || intent.getData() == null) {
            return;
        }
        String rawLink = intent.getData().toString();
        intent.setData(null);
        try {
            IrohSocksLink link = IrohSocksLink.parse(rawLink);
            connectionStore.add(link.toConnection());
            loadConnections();
        } catch (IllegalArgumentException e) {
            Snackbar.make(findViewById(R.id.main), R.string.invalid_link, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        settings.registerListener(stateListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConnections();
    }

    @Override
    protected void onStop() {
        super.onStop();
        settings.unregisterListener(stateListener);
    }

    private void loadConnections() {
        List<Connection> connections = connectionStore.load();
        connectionView.setConnections(connections);
        connectionView.setSelected(settings.getSelected());
        syncState();
    }

    private void selectConnection(int position) {
        settings.setSelected(position);
        connectionView.setSelected(position);
    }

    private void editConnection(int position) {
        Connection connection = connectionView.getConnection(position);
        if (connection == null) {
            return;
        }
        Intent intent = new Intent(this, ConnectionConfigActivity.class)
                .putExtra(Constant.EXTRA_INDEX, position)
                .putExtra(Constant.EXTRA_NAME, connection.getName())
                .putExtra(Constant.EXTRA_USER, connection.getUser())
                .putExtra(Constant.EXTRA_PASSWORD, connection.getPassword())
                .putExtra(Constant.EXTRA_TICKET, connection.getTicket());
        startActivity(intent);
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void showAddMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 0, 0, R.string.add_manually);
        popup.getMenu().add(0, 1, 1, R.string.add_from_clipboard);
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 0) {
                startActivity(new Intent(this, ConnectionConfigActivity.class));
            } else {
                pasteFromClipboard();
            }
            return true;
        });
        popup.show();
    }

    private void pasteFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = clipboard != null ? clipboard.getPrimaryClip() : null;
        if (clip == null || clip.getItemCount() == 0) {
            Snackbar.make(findViewById(R.id.main), R.string.clipboard_empty, Snackbar.LENGTH_SHORT).show();
            return;
        }
        String text = clip.getItemAt(0).coerceToText(this).toString();
        try {
            IrohSocksLink link = IrohSocksLink.parse(text);
            Intent intent = new Intent(this, ConnectionConfigActivity.class)
                    .putExtra(Constant.EXTRA_NAME, link.getName())
                    .putExtra(Constant.EXTRA_USER, link.getUser())
                    .putExtra(Constant.EXTRA_PASSWORD, link.getPassword())
                    .putExtra(Constant.EXTRA_TICKET, link.getTicket());
            startActivity(intent);
        } catch (IllegalArgumentException e) {
            Snackbar.make(findViewById(R.id.main), R.string.invalid_link, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void toggleProxy() {
        if (settings.isEnabled()) {
            stopProxy();
        } else {
            startProxy();
        }
    }

    private void startProxy() {
        Connection connection = connectionView.getSelectedConnection();
        if (connection == null) {
            Toast.makeText(this, R.string.no_connection_selected, Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
            return;
        }

        if (settings.isVpnMode()) {
            Intent prepare = VpnService.prepare(this);
            if (prepare != null) {
                startActivityForResult(prepare, REQUEST_VPN);
                return;
            }
        }

        Intent intent = new Intent(this, ProxyService.class)
                .setAction(Constant.ACTION_START)
                .putExtra(Constant.EXTRA_TICKET, connection.getTicket())
                .putExtra(Constant.EXTRA_USER, connection.getUser())
                .putExtra(Constant.EXTRA_PASSWORD, connection.getPassword());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        syncState();
    }

    private void stopProxy() {
        startService(new Intent(this, ProxyService.class).setAction(Constant.ACTION_STOP));
        syncState();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS) {
            startProxy();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VPN && resultCode == RESULT_OK) {
            startProxy();
        }
    }

    private void syncState() {
        boolean enabled = settings.isEnabled();
        updatePowerButton(enabled);

        String error = settings.getLastError();
        if (enabled) {
            lastShownError = null;
        } else if (error != null && !error.isEmpty() && !error.equals(lastShownError)) {
            lastShownError = error;
            Snackbar.make(findViewById(R.id.main), error, Snackbar.LENGTH_LONG).show();
        }
    }

    private void updatePowerButton(boolean enabled) {
        if (enabled) {
            enableButton.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.enabled)));
            enableButton.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.white)));
            enableButton.setContentDescription(getString(R.string.disable));
        } else {
            int surface = MaterialColors.getColor(enableButton,
                    com.google.android.material.R.attr.colorSurfaceContainerHighest);
            int onSurface = MaterialColors.getColor(enableButton,
                    com.google.android.material.R.attr.colorOnSurfaceVariant);
            enableButton.setBackgroundTintList(ColorStateList.valueOf(surface));
            enableButton.setImageTintList(ColorStateList.valueOf(onSurface));
            enableButton.setContentDescription(getString(R.string.enable));
        }
    }
}
