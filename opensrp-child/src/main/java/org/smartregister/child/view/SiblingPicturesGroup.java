package org.smartregister.child.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import androidx.leanback.widget.HorizontalGridView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import org.smartregister.child.R;
import org.smartregister.child.activity.BaseActivity;
import org.smartregister.child.adapter.SiblingPictureAdapter;

import java.util.List;

/**
 * Created by Jason Rogena - jrogena@ona.io on 09/05/2017.
 */

public class SiblingPicturesGroup extends LinearLayout {
    private HorizontalGridView siblingsGV;

    public SiblingPicturesGroup(Context context) {
        super(context);
        init(context);
    }

    public SiblingPicturesGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SiblingPicturesGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SiblingPicturesGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.view_sibling_group, this, true);
        siblingsGV = findViewById(R.id.siblings_gv);
        siblingsGV.setRowHeight(context.getResources().getDimensionPixelSize(R.dimen.sibling_profile_pic_height));
    }

    public void setSiblingBaseEntityIds(BaseActivity baseActivity, List <String> baseEntityIds) {
        SiblingPictureAdapter siblingPictureAdapter = new SiblingPictureAdapter(baseActivity, baseEntityIds);
        siblingsGV.setAdapter(siblingPictureAdapter);
    }
}
