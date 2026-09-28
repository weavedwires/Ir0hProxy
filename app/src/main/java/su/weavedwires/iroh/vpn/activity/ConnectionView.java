package su.weavedwires.iroh.vpn.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import su.weavedwires.iroh.vpn.R;
import su.weavedwires.iroh.vpn.model.Connection;

public class ConnectionView extends RecyclerView.Adapter<ConnectionView.Holder> {

    public interface OnConnectionSelectedListener {
        void onConnectionSelected(int position);

        void onConnectionEdit(int position);
    }

    private final List<Connection> connections = new ArrayList<>();
    private final OnConnectionSelectedListener listener;
    private int selected = -1;

    public ConnectionView(OnConnectionSelectedListener listener) {
        this.listener = listener;
    }

    public void setConnections(List<Connection> list) {
        connections.clear();
        if (list != null) {
            connections.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void setSelected(int index) {
        int previous = selected;
        selected = index;
        if (previous >= 0 && previous < connections.size()) {
            notifyItemChanged(previous);
        }
        if (selected >= 0 && selected < connections.size()) {
            notifyItemChanged(selected);
        }
    }

    public Connection getSelectedConnection() {
        return getConnection(selected);
    }

    public Connection getConnection(int position) {
        if (position >= 0 && position < connections.size()) {
            return connections.get(position);
        }
        return null;
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
        Connection connection = connections.get(position);
        String name = connection.getName();
        holder.title.setText(name.isEmpty()
                ? holder.itemView.getContext().getString(R.string.no_name)
                : name);
        holder.selected.setChecked(position == selected);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConnectionSelected(holder.getBindingAdapterPosition());
            }
        });
        holder.edit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConnectionEdit(holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return connections.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final RadioButton selected;
        final ImageButton edit;

        Holder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.connection_title);
            selected = itemView.findViewById(R.id.connection_selected);
            edit = itemView.findViewById(R.id.connection_edit);
        }
    }
}
