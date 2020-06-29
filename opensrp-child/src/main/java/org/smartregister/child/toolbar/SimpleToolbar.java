package org.smartregister.child.toolbar;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuItem;

import org.smartregister.child.R;
import org.smartregister.view.customcontrols.CustomFontTextView;

/**
 * Created by Jason Rogena - jrogena@ona.io on 12/06/2017.
 */

public class SimpleToolbar extends BaseToolbar {
    public static final int TOOLBAR_ID = R.id.simple_toolbar;
    private static final String TAG = SimpleToolbar.class.getName();
    private final Context context;
    private String title;

    public SimpleToolbar(Context context) {
        super(context);
        this.context = context;
    }

    public SimpleToolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public SimpleToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
    }

    public void setTitle(String title) {
        this.title = title;
        refreshTitleView();
    }

    private void refreshTitleView() {
        try {

            CustomFontTextView titleTV = findViewById(R.id.title);
            titleTV.setText(title);
        } catch (Exception e) {
            Log.e(TAG, "***************************************");
            Log.e(TAG, Log.getStackTraceString(e));
            Log.e(TAG, "***************************************");
        }
    }

    @Override
    public int getSupportedMenu() {
        return 0;
    }

    @Override
    public void prepareMenu() {
        refreshTitleView();
    }

    @Override
    public MenuItem onMenuItemSelected(MenuItem menuItem) {
        return null;
    }
}
