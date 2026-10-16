package su.weavedwires.iroh.vpn.activity.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;
import java.util.function.Consumer;

import lombok.Getter;
import su.weavedwires.iroh.vpn.R;
import su.weavedwires.iroh.vpn.model.Connection;

public class ConnectionAdapter extends ListAdapter<Connection, ConnectionAdapter.Holder> {
    private final ButtonWatchdog watchdog = new ButtonWatchdog();
    private final Consumer<Connection> onSelect;
    private final Consumer<Connection> onEdit;

    @Getter
    private Connection selected;

    public void setSelected(Connection selected) {
        this.selected = selected;
        notifyItemChanged(getCurrentList().indexOf(selected));
    }

    private static final DiffUtil.ItemCallback<Connection> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Connection oldItem, @NonNull Connection newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Connection oldItem, @NonNull Connection newItem) {
            return Objects.equals(oldItem.getUser(), newItem.getUser()) &&
                    Objects.equals(oldItem.getPassword(), newItem.getPassword()) &&
                    Objects.equals(oldItem.getName(), newItem.getName()) &&
                    Objects.equals(oldItem.getTicket(), newItem.getTicket());
        }
    };

    public ConnectionAdapter(Consumer<Connection> onSelect, Consumer<Connection> onEdit) {
        super(DIFF);
        this.onSelect = onSelect;
        this.onEdit = onEdit;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_connection, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Connection connection = getCurrentList().get(position);
        holder.bind(connection);
    }

    class Holder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final RadioButton select;
        private final ImageButton edit;
        private Connection current;

        Holder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.connection_title);
            select = itemView.findViewById(R.id.connection_selected);
            edit = itemView.findViewById(R.id.connection_edit);

            select.setOnClickListener(c -> {
                watchdog.exchange(select);
                onSelect.accept(current);
            });
            edit.setOnClickListener(c -> onEdit.accept(current));
        }

        void bind(Connection connection) {
            title.setText(connection.getName());

            select.setChecked(Objects.equals(connection.getId(), getSelected().getId()));
            if (select.isChecked()) {
                watchdog.exchange(select);
            }

            current = connection;
            title.setText(connection.getName());
        }
    }

    static class ButtonWatchdog {
        RadioButton previousButton;

        public synchronized void exchange(RadioButton newButton) {
            if (previousButton != null) {
                previousButton.setChecked(false);
            }
            previousButton = newButton;
            if (newButton != null) {
                newButton.setChecked(true);
            }
        }
    }
}
