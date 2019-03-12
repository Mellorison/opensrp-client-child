package org.smartregister.child.listener;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.MenuItem;

import org.smartregister.child.R;
import org.smartregister.child.activity.BaseChildRegisterActivity;
import org.smartregister.listener.BottomNavigationListener;
import org.smartregister.view.activity.BaseRegisterActivity;

public class ChildBottomNavigationListener extends BottomNavigationListener {
    private Activity context;

    public ChildBottomNavigationListener(Activity context) {
        super(context);
        this.context = context;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        super.onNavigationItemSelected(item);

        BaseRegisterActivity baseRegisterActivity = (BaseRegisterActivity) context;

        if (item.getItemId() == R.id.action_child) {
            baseRegisterActivity.switchToBaseFragment();
        } else if (item.getItemId() == R.id.action_scan_qr) {
            ((BaseChildRegisterActivity) baseRegisterActivity).startNearexScanner();
        }
        if (item.getItemId() == R.id.action_child) {
            baseRegisterActivity.switchToBaseFragment();
        } else if (item.getItemId() == R.id.action_search) {
            baseRegisterActivity.switchToFragment(1);
        }

        return true;
    }
}
