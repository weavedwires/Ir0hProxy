package su.weavedwires.iroh.vpn.activity;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.textfield.TextInputEditText;

import su.weavedwires.iroh.vpn.Constant;
import su.weavedwires.iroh.vpn.R;
import su.weavedwires.iroh.vpn.model.Connection;
import su.weavedwires.iroh.vpn.model.ConnectionStore;
import su.weavedwires.iroh.vpn.model.IrohSocksLink;

public class ConnectionConfigActivity extends AppCompatActivity {

    private TextInputEditText nameInput;
    private TextInputEditText ticketInput;
    private TextInputEditText userInput;
    private TextInputEditText passwordInput;
    private ConnectionStore connectionStore;
    private int editIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivityIfAvailable(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_connection_config);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.connection_config), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        connectionStore = new ConnectionStore(this);

        editIndex = getIntent() != null
                ? getIntent().getIntExtra(Constant.EXTRA_INDEX, -1)
                : -1;

        nameInput = findViewById(R.id.name_input);
        ticketInput = findViewById(R.id.ticket_input);
        userInput = findViewById(R.id.user_input);
        passwordInput = findViewById(R.id.password_input);
        MaterialButton saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> save());

        MaterialButton deleteButton = findViewById(R.id.delete_button);
        deleteButton.setVisibility(editIndex >= 0 ? View.VISIBLE : View.GONE);
        deleteButton.setOnClickListener(v -> confirmDelete());

        MaterialButton shareButton = findViewById(R.id.share_button);
        shareButton.setOnClickListener(v -> share());

        prefillFromExtras();
    }

    private void prefillFromExtras() {
        if (getIntent() == null) {
            return;
        }
        String name = getIntent().getStringExtra(Constant.EXTRA_NAME);
        String ticket = getIntent().getStringExtra(Constant.EXTRA_TICKET);
        String user = getIntent().getStringExtra(Constant.EXTRA_USER);
        String password = getIntent().getStringExtra(Constant.EXTRA_PASSWORD);
        if (name != null) {
            nameInput.setText(name);
        }
        if (ticket != null) {
            ticketInput.setText(ticket);
        }
        if (user != null) {
            userInput.setText(user);
        }
        if (password != null) {
            passwordInput.setText(password);
        }
    }

    private void save() {
        String name = getText(nameInput);
        String ticket = getText(ticketInput);
        if (ticket.isEmpty()) {
            Toast.makeText(this, R.string.ticket_required, Toast.LENGTH_SHORT).show();
            return;
        }
        String user = getText(userInput);
        String password = getText(passwordInput);

        Connection connection = new Connection(name, user, password, ticket);
        if (editIndex >= 0) {
            connectionStore.update(editIndex, connection);
        } else {
            connectionStore.add(connection);
        }
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message)
                .setPositiveButton(R.string.remove, (dialog, which) -> {
                    connectionStore.delete(editIndex);
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void share() {
        save();

        IrohSocksLink link = new IrohSocksLink(getText(nameInput), getText(userInput), getText(passwordInput), getText(ticketInput));

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("irohsocks-link", link.toString());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Скопировано в буфер обмена", Toast.LENGTH_SHORT).show();

        Intent share = new Intent(Intent.ACTION_SEND);
        share.putExtra(Intent.EXTRA_TEXT, link.toString());
        share.setType("text/plain");
        try {
            startActivity(share);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Нет подходящих приложений", Toast.LENGTH_LONG).show();
        }
    }

    private static String getText(TextInputEditText field) {
        return field.getText().toString().trim();
    }
}
