package org.smartregister.child.adapter;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.smartregister.child.R;
import org.smartregister.child.activity.BaseActivity;
import org.smartregister.child.view.SiblingPicture;

import java.util.List;

/**
 * Created by Jason Rogena - jrogena@ona.io on 09/05/2017.
 */

public class SiblingPictureAdapter extends RecyclerView.Adapter<SiblingPicture> {

    private final BaseActivity baseActivity;
    private final List<String> siblingIds;

    public SiblingPictureAdapter(BaseActivity baseActivity, List<String> siblingIds) {
        this.baseActivity = baseActivity;
        this.siblingIds = siblingIds;
    }

    @Override
    public SiblingPicture onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(baseActivity).inflate(R.layout.view_sibling_picture, parent, false);
        return new SiblingPicture(view);
    }

    @Override
    public void onBindViewHolder(SiblingPicture siblingPicture, int position) {
        if (siblingIds.size() > position) {
            siblingPicture.setChildBaseEntityId(baseActivity, siblingIds.get(position));
        }
    }

    @Override
    public long getItemId(int position) {
        return 4223 + position;
    }

    @Override
    public int getItemCount() {
        return siblingIds.size();
    }

}
