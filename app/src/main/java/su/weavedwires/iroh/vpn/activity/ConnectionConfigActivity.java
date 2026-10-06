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

import su.weavedwires.iroh.vpn.R;
import su.weavedwires.iroh.vpn.constant.Constant;
import su.weavedwires.iroh.vpn.model.IrohSocksLink;

public class ConnectionConfigActivity extends AppCompatActivity {

    private TextInputEditText nameInput;
    private TextInputEditText ticketInput;
    private TextInputEditText userInput;
    private TextInputEditText passwordInput;
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

        Intent result = new Intent()
                .putExtra(Constant.EXTRA_INDEX, editIndex)
                .putExtra(Constant.EXTRA_DELETED, false)
                .putExtra(Constant.EXTRA_NAME, name)
                .putExtra(Constant.EXTRA_USER, user)
                .putExtra(Constant.EXTRA_PASSWORD, password)
                .putExtra(Constant.EXTRA_TICKET, ticket);
        setResult(RESULT_OK, result);
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message)
                .setPositiveButton(R.string.remove, (dialog, which) -> {
                    Intent result = new Intent()
                            .putExtra(Constant.EXTRA_INDEX, editIndex)
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
