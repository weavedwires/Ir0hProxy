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

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import su.weavedwires.iroh.vpn.R;
import su.weavedwires.iroh.vpn.model.Connection;

public class ConnectionAdapter extends ListAdapter<Connection, ConnectionAdapter.Holder> {
    private static final String PAYLOAD_SELECTION = "selection";
    private static final long NO_SELECTION = -1;
    private static final DiffUtil.ItemCallback<Connection> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Connection oldItem, @NonNull Connection newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Connection oldItem, @NonNull Connection newItem) {
            return Objects.equals(oldItem.getUser(), newItem.getUser()) &&
                    Objects.equals(oldItem.getPassword(), newItem.getPassword()) &&
                    Objects.equals(oldItem.getName(), newItem.getName()) &&
                    Objects.equals(oldItem.getTicket(), newItem.getTicket());
        }
    };

    private final Consumer<Connection> onSelect;
    private final Consumer<Connection> onEdit;

    private long selectedId = NO_SELECTION;

    public ConnectionAdapter(Consumer<Connection> onSelect, Consumer<Connection> onEdit) {
        super(DIFF);
        this.onSelect = onSelect;
        this.onEdit = onEdit;
    }

    public void setSelected(Connection selected) {
        long newId = selected == null ? NO_SELECTION : selected.getId();
        if (newId == selectedId) {
            return;
        }
        long oldId = selectedId;
        selectedId = newId;
        notifySelectionChanged(oldId);
        notifySelectionChanged(newId);
    }

    public Connection getSelected() {
        int position = indexOf(selectedId);
        return position < 0 ? null : getCurrentList().get(position);
    }

    private void notifySelectionChanged(long id) {
        int position = indexOf(id);
        if (position >= 0) {
            notifyItemChanged(position, PAYLOAD_SELECTION);
        }
    }

    private int indexOf(long id) {
        if (id == NO_SELECTION) {
            return -1;
        }
        List<Connection> list = getCurrentList();
        for (int position = 0; position < list.size(); position++) {
            if (list.get(position).getId() == id) {
                return position;
            }
        }
        return -1;
    }

    private boolean isSelected(Connection connection) {
        return connection.getId() == selectedId;
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

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.contains(PAYLOAD_SELECTION)) {
            Connection connection = getCurrentList().get(position);
            holder.bind(connection);
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
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

            select.setOnClickListener(c -> onSelect.accept(current));
            edit.setOnClickListener(c -> onEdit.accept(current));
        }

        void bind(Connection connection) {
            current = connection;
            title.setText(connection.getName());
            bindSelection(connection);
        }

        void bindSelection(Connection connection) {
            select.setChecked(isSelected(connection));
        }
    }
}
