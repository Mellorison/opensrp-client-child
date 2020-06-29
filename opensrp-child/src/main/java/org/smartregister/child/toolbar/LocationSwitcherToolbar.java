package org.smartregister.child.toolbar;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import org.smartregister.AllConstants;
import org.smartregister.child.R;
import org.smartregister.child.activity.BaseActivity;
import org.smartregister.child.view.LocationActionView;
import org.smartregister.util.Utils;
import org.smartregister.view.LocationPickerView;
import org.smartregister.view.customcontrols.CustomFontTextView;

/**
 * To use this toolbar in your activity, include the following line as the first child in your activity's main {@link
 * CoordinatorLayout}
 * <p/>
 * <include layout="@layout/toolbar_location_switcher" />
 * <p/>
 * Created by Jason Rogena - jrogena@ona.io on 17/02/2017.
 */

public class LocationSwitcherToolbar extends BaseToolbar {
    public static final int TOOLBAR_ID = R.id.location_switching_toolbar;
    private BaseActivity baseActivity;
    private OnLocationChangeListener onLocationChangeListener;
    private String title;
    private int separatorResourceId;

    public LocationSwitcherToolbar(Context context) {
        super(context);
    }

    public LocationSwitcherToolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LocationSwitcherToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void init(BaseActivity baseActivity) {
        this.baseActivity = baseActivity;
    }

    public String getCurrentLocation() {
        if (baseActivity != null && baseActivity.getMenu() != null) {
            return ((LocationActionView) baseActivity.getMenu().findItem(R.id.location_switcher).getActionView())
                    .getSelectedItem();
        }

        return null;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public void setOnLocationChangeListener(OnLocationChangeListener onLocationChangeListener) {
        this.onLocationChangeListener = onLocationChangeListener;
    }

    @Override
    public int getSupportedMenu() {
        return R.menu.menu_location_switcher;
    }

    @Override
    public void prepareMenu() {
        if (baseActivity != null) {
            CustomFontTextView titleTV = baseActivity.findViewById(R.id.title);
            titleTV.setText(title);


            if (!Utils.getBooleanProperty(AllConstants.PROPERTY.DISABLE_LOCATION_PICKER_VIEW)) {
                LocationActionView locationActionView = new LocationActionView(baseActivity);
                locationActionView.getLocationPickerView()
                        .setOnLocationChangeListener(new LocationPickerView.OnLocationChangeListener() {
                            @Override
                            public void onLocationChange(String newLocation) {
                                if (onLocationChangeListener != null) {
                                    onLocationChangeListener.onLocationChanged(newLocation);
                                }
                            }
                        });
                View separatorV = baseActivity.findViewById(R.id.separator_v);
                separatorV.setVisibility(VISIBLE);
                baseActivity.getMenu().findItem(R.id.location_switcher).setActionView(locationActionView);
                separatorV.setBackgroundDrawable(baseActivity.getResources().getDrawable(separatorResourceId));
            }
        }
    }

    @Override
    public MenuItem onMenuItemSelected(MenuItem menuItem) {
        return menuItem;
    }

    public void updateSeparatorView(int newView) {
        separatorResourceId = newView;
    }

    public interface OnLocationChangeListener {
        void onLocationChanged(String newLocation);
    }
}
