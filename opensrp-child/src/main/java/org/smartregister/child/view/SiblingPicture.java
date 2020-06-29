package org.smartregister.child.view;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import org.smartregister.child.activity.BaseActivity;
import org.smartregister.child.task.GetChildDetailsTask;
import org.smartregister.child.util.Utils;


/**
 * Created by ndegwamartin on 06/03/2019.
 */
public class SiblingPicture extends RecyclerView.ViewHolder {
    private final View itemView;

    public SiblingPicture(View itemView) {
        super(itemView);
        this.itemView = itemView;
    }

    public void setChildBaseEntityId(BaseActivity baseActivity, String baseEntityId) {
        Utils.startAsyncTask(
                new GetChildDetailsTask(baseActivity, baseEntityId, itemView), null);
    }


}
