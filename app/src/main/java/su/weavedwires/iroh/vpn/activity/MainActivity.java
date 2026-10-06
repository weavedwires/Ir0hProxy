package su.weavedwires.iroh.vpn.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
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

import su.weavedwires.iroh.vpn.IrohProxyApp;
import su.weavedwires.iroh.vpn.R;
import su.weavedwires.iroh.vpn.Settings;
import su.weavedwires.iroh.vpn.constant.Constant;
import su.weavedwires.iroh.vpn.model.Connection;
import su.weavedwires.iroh.vpn.model.ConnectionStore;
import su.weavedwires.iroh.vpn.model.IrohSocksLink;
import su.weavedwires.iroh.vpn.proxy.ProxyService;

public class MainActivity extends AppCompatActivity {

    private Settings settings;
    private ConnectionStore connectionStore;

    private RecyclerView connectionList;
    private ConnectionView connectionView;
    private FloatingActionButton enableButton;
    private String lastShownError;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> startProxy());

    private final ActivityResultLauncher<Intent> vpnPrepareLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    startProxy();
                }
            });

    private final ActivityResultLauncher<Intent> editLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    applyEditResult(result.getData());
                }
            });

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Constant.ACTION_PROXY_STARTED.equals(action)) {
                lastShownError = null;
                updatePowerButton(true);
            } else if (Constant.ACTION_PROXY_STOPPED.equals(action)) {
                updatePowerButton(false);
            } else if (Constant.ACTION_PROXY_ERROR.equals(action)) {
                updatePowerButton(false);
                showError(intent.getStringExtra(Constant.EXTRA_ERROR));
            }
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

        initConnections();
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
            IrohSocksLink link = new IrohSocksLink(rawLink);
            addConnection(link.toConnection());
        } catch (IllegalArgumentException e) {
            Snackbar.make(findViewById(R.id.main), R.string.invalid_link, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constant.ACTION_PROXY_STARTED);
        filter.addAction(Constant.ACTION_PROXY_STOPPED);
        filter.addAction(Constant.ACTION_PROXY_ERROR);
        ContextCompat.registerReceiver(this, stateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        syncState();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(stateReceiver);
    }

    private void initConnections() {
        connectionView.setConnections(connectionStore.load());
        int selected = settings.getSelected();
        if (connectionView.getItemCount() > 0
                && (selected < 0 || selected >= connectionView.getItemCount())) {
            selected = 0;
            settings.setSelected(selected);
        }
        connectionView.setSelected(selected);
    }

    private void applyEditResult(Intent data) {
        if (data.getBooleanExtra(Constant.EXTRA_DELETED, false)) {
            deleteConnection(data.getIntExtra(Constant.EXTRA_INDEX, -1));
            return;
        }
        Connection connection = new Connection(
                data.getStringExtra(Constant.EXTRA_NAME),
                data.getStringExtra(Constant.EXTRA_USER),
                data.getStringExtra(Constant.EXTRA_PASSWORD),
                data.getStringExtra(Constant.EXTRA_TICKET));
        int index = data.getIntExtra(Constant.EXTRA_INDEX, -1);
        if (index >= 0) {
            connectionStore.update(index, connection);
            connectionView.notifyItemChanged(index);
        } else {
            addConnection(connection);
        }
    }

    private void addConnection(Connection connection) {
        boolean wasEmpty = connectionView.getItemCount() == 0;
        int index = connectionStore.add(connection);
        connectionView.notifyItemInserted(index);
        if (wasEmpty) {
            settings.setSelected(index);
            connectionView.setSelected(index);
        }
    }

    private void deleteConnection(int index) {
        int size = connectionView.getItemCount();
        if (index < 0 || index >= size) {
            return;
        }
        int selected = settings.getSelected();
        int newSelected = selected;
        if (size == 1) {
            newSelected = -1;
        } else if (index < selected) {
            newSelected = selected - 1;
        } else if (index == selected) {
            newSelected = Math.min(index, size - 2);
        }
        connectionStore.delete(index);
        connectionView.notifyItemRemoved(index);
        settings.setSelected(newSelected);
        connectionView.setSelected(newSelected);
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
        editLauncher.launch(intent);
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
                editLauncher.launch(new Intent(this, ConnectionConfigActivity.class));
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
            IrohSocksLink link = new IrohSocksLink(text);
            Intent intent = new Intent(this, ConnectionConfigActivity.class)
                    .putExtra(Constant.EXTRA_NAME, link.getName())
                    .putExtra(Constant.EXTRA_USER, link.getUser())
                    .putExtra(Constant.EXTRA_PASSWORD, link.getPassword())
                    .putExtra(Constant.EXTRA_TICKET, link.getTicket());
            editLauncher.launch(intent);
        } catch (IllegalArgumentException e) {
            Snackbar.make(findViewById(R.id.main), R.string.invalid_link, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void toggleProxy() {
        if (isProxyRunning()) {
            stopProxy();
        } else {
            startProxy();
        }
    }

    private boolean isProxyRunning() {
        try {
            IrohProxyApp app = (IrohProxyApp) getApplication();
            if (!app.getProxyRunner().isRunning()) {
                return false;
            }
            return !settings.isVpnMode() || app.getTun2Socks().isRunning();
        } catch (RuntimeException e) {
            return false;
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
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }

        if (settings.isVpnMode()) {
            Intent prepare = VpnService.prepare(this);
            if (prepare != null) {
                vpnPrepareLauncher.launch(prepare);
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
    }

    private void stopProxy() {
        startService(new Intent(this, ProxyService.class).setAction(Constant.ACTION_STOP));
    }

    private void syncState() {
        boolean running = isProxyRunning();
        updatePowerButton(running);
        if (running) {
            lastShownError = null;
        }
    }

    private void showError(String error) {
        if (error == null || error.isEmpty() || error.equals(lastShownError)) {
            return;
        }
        lastShownError = error;
        Snackbar.make(findViewById(R.id.main), error, Snackbar.LENGTH_LONG).show();
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
