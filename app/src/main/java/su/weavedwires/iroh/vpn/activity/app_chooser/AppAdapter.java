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

import java.util.List;
import java.util.Objects;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import su.weavedwires.iroh.vpn.R;

public class AppAdapter extends ListAdapter<AppItem, AppAdapter.Holder> {
    private static final String PAYLOAD_SELECTION = "selection";
    private static final DiffUtil.ItemCallback<AppItem> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull AppItem oldItem, @NonNull AppItem newItem) {
            return Objects.equals(oldItem.getPackageName(), newItem.getPackageName());
        }

        @Override
        public boolean areContentsTheSame(@NonNull AppItem oldItem, @NonNull AppItem newItem) {
            return Objects.equals(oldItem.getName(), newItem.getName()) &&
                    Objects.equals(oldItem.getIcon(), newItem.getIcon());
        }
    };

    private final Runnable onSelected;

    @Getter
    @Setter
    private Set<String> selectedPackages;

    public AppAdapter(Runnable savePackages) {
        super(DIFF);
        this.onSelected = savePackages;
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
        AppItem app = getCurrentList().get(position);
        holder.bind(app);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.contains(PAYLOAD_SELECTION)) {
            AppItem app = getCurrentList().get(position);
            holder.bindSelection(app);
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    private void selectOrUnselectPackage(String packageName) {
        if (selectedPackages.contains(packageName)) {
            selectedPackages.remove(packageName);
        } else {
            selectedPackages.add(packageName);
        }
        onSelected.run();
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final TextView title;
        private final CheckBox select;
        private AppItem current;

        Holder(@NonNull View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.app_icon);
            title = itemView.findViewById(R.id.app_title);
            select = itemView.findViewById(R.id.app_selected);
            select.setOnClickListener(v -> selectOrUnselectPackage(current.getPackageName()));
        }

        void bind(AppItem app) {
            current = app;
            title.setText(app.getName());
            icon.setImageDrawable(app.getIcon());
            bindSelection(app);
        }

        void bindSelection(AppItem app) {
            select.setChecked(selectedPackages.contains(app.getPackageName()));
        }
    }
}
