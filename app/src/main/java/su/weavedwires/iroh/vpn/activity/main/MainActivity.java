package su.weavedwires.iroh.vpn.activity.main;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import su.weavedwires.iroh.vpn.IrohProxyApp;
import su.weavedwires.iroh.vpn.R;
import su.weavedwires.iroh.vpn.activity.connection_config.ConnectionConfigActivity;
import su.weavedwires.iroh.vpn.activity.settings.SettingsActivity;
import su.weavedwires.iroh.vpn.constant.Constant;
import su.weavedwires.iroh.vpn.model.Connection;
import su.weavedwires.iroh.vpn.model.IrohSocksLink;
import su.weavedwires.iroh.vpn.ProxyService;
import su.weavedwires.iroh.vpn.proxy.ProxyStateBus;
import su.weavedwires.iroh.vpn.proxy.ProxyStateListener;
import su.weavedwires.iroh.vpn.repository.ConnectionRepository;
import su.weavedwires.iroh.vpn.repository.SettingsRepository;

public class MainActivity extends AppCompatActivity implements ProxyStateListener {
    private SettingsRepository settings;
    private ConnectionRepository repository;
    private ConnectionAdapter adapter;
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
                    editConnection(result.getData());
                }
            });

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

        settings = new SettingsRepository(this);
        repository = new ConnectionRepository(this);

        RecyclerView connectionList = findViewById(R.id.connection_list);

        adapter = new ConnectionAdapter(
                this::selectConnection,
                this::callConnectionEditor
        );
        adapter.setSelected(repository.getSelected());

        connectionList.setLayoutManager(new LinearLayoutManager(this));
        connectionList.setAdapter(adapter);

        adapter.submitList(repository.getList());

        enableButton = findViewById(R.id.enable_button);
        enableButton.setOnClickListener(v -> toggleProxy());
        findViewById(R.id.settings_button).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.add_button).setOnClickListener(this::showAddMenu);

        handleIncomingLink(getIntent());

        ProxyStateBus proxyStateBus = ((IrohProxyApp) getApplication()).getProxyStateStore();
        getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                proxyStateBus.addListener(MainActivity.this);
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                proxyStateBus.removeListener(MainActivity.this);
            }
        });
    }

    @Override
    protected void onStart() {
        syncState();
        super.onStart();
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
            addConnection(link.toConnection());
        } catch (IllegalArgumentException e) {
            Snackbar.make(findViewById(R.id.main), R.string.invalid_link, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void callConnectionEditor(Connection connection) {
        Intent intent = new Intent(this, ConnectionConfigActivity.class)
                .putExtra(Constant.EXTRA_CONNECTION, connection);
        editLauncher.launch(intent);
    }

    private void addConnection(Connection newConnection) {
        boolean firstConnection = repository.isEmpty();
        repository.add(newConnection);
        if (firstConnection) {
            selectConnection(newConnection);
        }
        adapter.submitList(repository.getList());
    }

    private void editConnection(Intent data) {
        Connection connection = IntentCompat.getParcelableExtra(data, Constant.EXTRA_CONNECTION, Connection.class);
        if (connection == null) {
            return;
        }
        if (data.getBooleanExtra(Constant.EXTRA_DELETED, false)) {
            deleteConnection(connection.getId());
        } else if (repository.contains(connection.getId())) {
            updateConnection(connection);
        } else {
            addConnection(connection);
        }
    }

    private void updateConnection(Connection connection) {
        repository.update(connection.getId(), connection);
        adapter.submitList(repository.getList());
    }

    private void deleteConnection(long id) {
        repository.delete(id);
        if (repository.isEmpty()) {
            selectConnection(null);
        } else {
            selectConnection(repository.getList().get(0));
        }
        adapter.submitList(repository.getList());
    }

    private void selectConnection(Connection connection) {
        adapter.setSelected(connection);
        saveSelected(connection);
    }

    private void saveSelected(Connection connection) {
        repository.setSelected(connection);
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
            return ((IrohProxyApp) getApplication()).getProxyRunner().isRunning();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    private void startProxy() {
        Connection connection = adapter.getSelected();
        if (connection == null) {
            Snackbar.make(findViewById(R.id.main), R.string.no_connection_selected, Snackbar.LENGTH_SHORT).show();
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
                .setAction(Constant.ACTION_START);

        ContextCompat.startForegroundService(this, intent);
    }

    private void stopProxy() {
        startService(new Intent(this, ProxyService.class).setAction(Constant.ACTION_STOP));
    }

    @Override
    public void onStarted() {
        lastShownError = null;
        updatePowerButton(true);
    }

    @Override
    public void onStopped() {
        updatePowerButton(false);
    }

    @Override
    public void onError(String error) {
        updatePowerButton(false);
        showError(error);
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
