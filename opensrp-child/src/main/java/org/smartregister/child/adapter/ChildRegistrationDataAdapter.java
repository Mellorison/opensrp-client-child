package org.smartregister.child.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.smartregister.child.R;
import org.smartregister.child.domain.KeyValueItem;

import java.util.List;

/**
 * Created by ndegwamartin on 2019-06-11.
 */
public class ChildRegistrationDataAdapter extends RecyclerView.Adapter<ChildRegistrationDataAdapter.ViewHolder> {
    // declaring some fields.
    private List<KeyValueItem> detailsList;

    // A constructor.
    public ChildRegistrationDataAdapter(List<KeyValueItem> detailsList) {
        this.detailsList = detailsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parentViewGroup, int i) {
        View itemView = LayoutInflater.from(parentViewGroup.getContext())
                .inflate(R.layout.key_value_data_recycler_view_item, parentViewGroup, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        KeyValueItem contact = detailsList.get(position);
        viewHolder.keyText.setText(contact.getKey());
        viewHolder.valueText.setText(contact.getValue());
    }

    @Override
    public int getItemCount() {
        return detailsList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        public TextView keyText, valueText;

        public ViewHolder(View itemView) {
            super(itemView);
            keyText = itemView.findViewById(R.id.key);
            valueText = itemView.findViewById(R.id.value);
        }

    }
}
