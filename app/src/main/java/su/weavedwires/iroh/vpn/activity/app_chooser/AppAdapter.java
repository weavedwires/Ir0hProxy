package su.weavedwires.iroh.vpn.activity.app_chooser;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

import su.weavedwires.iroh.vpn.R;
import su.weavedwires.iroh.vpn.model.AppData;

public class AppAdapter extends ListAdapter<AppData, AppAdapter.Holder> {
    private static final DiffUtil.ItemCallback<AppData> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull AppData oldItem, @NonNull AppData newItem) {
            return Objects.equals(oldItem.getName(), newItem.getName());
        }

        @Override
        public boolean areContentsTheSame(@NonNull AppData oldItem, @NonNull AppData newItem) {
            return Objects.equals(oldItem.getIcon(), newItem.getIcon()) &&
                    Objects.equals(oldItem.isChecked(), newItem.isChecked());
        }
    };
    public AppAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        AppData app = getCurrentList().get(position);
        holder.bind(app);
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final TextView title;
        private final CheckBox select;

        Holder(@NonNull View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.app_icon);
            title = itemView.findViewById(R.id.app_title);
            select = itemView.findViewById(R.id.app_selected);
        }

        void bind(AppData app) {
            title.setText(app.getName());
            icon.setImageDrawable(app.getIcon());
            select.setChecked(app.isChecked());
            select.setOnClickListener(v -> app.flipChecked());
        }
    }
}
