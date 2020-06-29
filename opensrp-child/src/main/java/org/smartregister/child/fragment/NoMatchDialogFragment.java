package org.smartregister.child.fragment;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.smartregister.child.R;
import org.smartregister.view.activity.BaseRegisterActivity;

@SuppressLint("ValidFragment")
public class NoMatchDialogFragment extends DialogFragment {

    public static final String TAG = NoMatchDialogFragment.class.getName();

    private final NoMatchDialogActionHandler noMatchDialogActionHandler = new NoMatchDialogActionHandler();
    private final BaseRegisterActivity baseRegisterActivity;
    private final String uniqueId;

    public NoMatchDialogFragment(BaseRegisterActivity baseRegisterActivity, String uniqueId) {
        this.uniqueId = uniqueId;
        this.baseRegisterActivity = baseRegisterActivity;
    }

    @Nullable
    public static NoMatchDialogFragment launchDialog(BaseRegisterActivity activity, String dialogTag, String opensrpID) {
        NoMatchDialogFragment noMatchDialogFragment = new NoMatchDialogFragment(activity, opensrpID);
        if (activity != null) {
            FragmentTransaction fragmentTransaction = activity.getFragmentManager().beginTransaction();
            Fragment prev = activity.getFragmentManager().findFragmentByTag(dialogTag);
            if (prev != null) {
                fragmentTransaction.remove(prev);
            }
            fragmentTransaction.addToBackStack(null);

            noMatchDialogFragment.show(fragmentTransaction, dialogTag);

            return noMatchDialogFragment;
        } else {
            return null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        super.onCancel(dialogInterface);
        baseRegisterActivity.setSearchTerm("");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup dialogView = (ViewGroup) inflater.inflate(R.layout.dialog_no_match, container, false);
        Button cancel = dialogView.findViewById(R.id.cancel_no_match_dialog);
        cancel.setOnClickListener(noMatchDialogActionHandler);
        Button advancedSearch = dialogView.findViewById(R.id.go_to_advanced_search);
        advancedSearch.setOnClickListener(noMatchDialogActionHandler);

        return dialogView;
    }

    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////

    private class NoMatchDialogActionHandler implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.cancel_no_match_dialog) {

                dismiss();
                baseRegisterActivity.setSearchTerm("");
            } else if (view.getId() == R.id.go_to_advanced_search) {
                baseRegisterActivity.setSearchTerm("");
                goToAdvancedSearch(uniqueId);
                baseRegisterActivity.setSelectedBottomBarMenuItem(R.id.action_search);
                dismiss();
            }
        }

        private void goToAdvancedSearch(String uniqueId) {
            Log.i(TAG, uniqueId);
            // TODO Implement Advanced Search Page
        }
    }
}
