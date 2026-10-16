package su.weavedwires.iroh.vpn.activity.connection_config;

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
import androidx.core.content.IntentCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.textfield.TextInputEditText;

import su.weavedwires.iroh.vpn.R;
import su.weavedwires.iroh.vpn.constant.Constant;
import su.weavedwires.iroh.vpn.model.Connection;
import su.weavedwires.iroh.vpn.model.IrohSocksLink;

public class ConnectionConfigActivity extends AppCompatActivity {
    private TextInputEditText nameInput;
    private TextInputEditText ticketInput;
    private TextInputEditText userInput;
    private TextInputEditText passwordInput;
    private Connection original;

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

        original = IntentCompat.getParcelableExtra(getIntent(), Constant.EXTRA_CONNECTION, Connection.class);

        nameInput = findViewById(R.id.name_input);
        ticketInput = findViewById(R.id.ticket_input);
        userInput = findViewById(R.id.user_input);
        passwordInput = findViewById(R.id.password_input);
        MaterialButton saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> save());

        MaterialButton deleteButton = findViewById(R.id.delete_button);
        deleteButton.setVisibility(original != null ? View.VISIBLE : View.GONE);
        deleteButton.setOnClickListener(v -> confirmDelete());

        MaterialButton shareButton = findViewById(R.id.share_button);
        shareButton.setOnClickListener(v -> share());

        prefill(original);
    }

    private void prefill(Connection connection) {
        if (connection == null) {
            return;
        }
        if (connection.getName() != null) {
            nameInput.setText(connection.getName());
        }
        if (connection.getTicket() != null) {
            ticketInput.setText(connection.getTicket());
        }
        if (connection.getUser() != null) {
            userInput.setText(connection.getUser());
        }
        if (connection.getPassword() != null) {
            passwordInput.setText(connection.getPassword());
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

        Intent result = new Intent()
                .putExtra(Constant.EXTRA_CONNECTION, new Connection(
                        original == null ? null : original.getId(),
                        name,
                        user,
                        password,
                        ticket))
                .putExtra(Constant.EXTRA_DELETED, false);
        setResult(RESULT_OK, result);
        finish();
    }

    private void confirmDelete() {
        if (original == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message)
                .setPositiveButton(R.string.remove, (dialog, which) -> {
                    Intent result = new Intent()
                            .putExtra(Constant.EXTRA_CONNECTION, original)
                            .putExtra(Constant.EXTRA_DELETED, true);
                    setResult(RESULT_OK, result);
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
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();

        Intent share = new Intent(Intent.ACTION_SEND);
        share.putExtra(Intent.EXTRA_TEXT, link.toString());
        share.setType("text/plain");
        try {
            startActivity(share);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_share_apps, Toast.LENGTH_LONG).show();
        }
    }

    private static String getText(TextInputEditText field) {
        return field.getText().toString().trim();
    }
}
