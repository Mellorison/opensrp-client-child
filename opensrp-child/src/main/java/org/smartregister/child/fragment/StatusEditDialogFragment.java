package org.smartregister.child.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.smartregister.child.R;
import org.smartregister.child.event.ClientStatusUpdateEvent;
import org.smartregister.child.listener.StatusChangeListener;
import org.smartregister.child.task.ArchiveRecordTask;
import org.smartregister.child.util.Constants;
import org.smartregister.util.Utils;

import java.util.Map;

@SuppressLint("ValidFragment")
public class StatusEditDialogFragment extends DialogFragment {
    private static final String inactive = Constants.CHILD_STATUS.INACTIVE;
    private static final String lostToFollowUp = Constants.CHILD_STATUS.LOST_TO_FOLLOW_UP;
    private static Map<String, String> details;
    private StatusChangeListener listener;

    private StatusEditDialogFragment(Map<String, String> details) {
        StatusEditDialogFragment.details = details;
    }

    public static StatusEditDialogFragment newInstance(Map<String, String> details) {
        return new StatusEditDialogFragment(details);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
    }

    @Override
    public void onStart() {
        super.onStart();
        // without a handler, the window sizes itself correctly
        // but the keyboard does not show up
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Window window = null;
                if (getDialog() != null) {
                    window = getDialog().getWindow();
                }

                if (window == null) {
                    return;
                }

                window.setLayout(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);

            }
        });

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = (StatusChangeListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString() + " must implement StatusChangeListener");
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {

        ViewGroup dialogView = (ViewGroup) inflater.inflate(R.layout.status_edit_dialog_view, container, false);
        LinearLayout activeLayout = dialogView.findViewById(R.id.activelayout);
        LinearLayout inactiveLayout = dialogView.findViewById(R.id.inactivelayout);
        LinearLayout lostToFollowUpLayout = dialogView.findViewById(R.id.losttofollowuplayout);
        LinearLayout childArchiveRecordLayout = dialogView.findViewById(R.id.child_archive_record_layout);
        final ImageView activeImageView = dialogView.findViewById(R.id.active_check);
        final ImageView inactiveImageView = dialogView.findViewById(R.id.inactive_check);
        final ImageView lostToFollowUpImageView = dialogView.findViewById(R.id.lost_to_followup_check);

        activeImageView.setVisibility(View.INVISIBLE);
        inactiveImageView.setVisibility(View.INVISIBLE);
        lostToFollowUpImageView.setVisibility(View.INVISIBLE);

        if (details.containsKey(inactive) && details.get(inactive) != null &&
                details.get(inactive).equalsIgnoreCase(Boolean.TRUE.toString())) {
            inactiveImageView.setVisibility(View.VISIBLE);
            activeImageView.setVisibility(View.INVISIBLE);
            lostToFollowUpImageView.setVisibility(View.INVISIBLE);

        } else if (details.containsKey(lostToFollowUp) && details.get(lostToFollowUp) != null &&
                details.get(lostToFollowUp).equalsIgnoreCase(Boolean.TRUE.toString())) {
            lostToFollowUpImageView.setVisibility(View.VISIBLE);
            inactiveImageView.setVisibility(View.INVISIBLE);
            activeImageView.setVisibility(View.INVISIBLE);

        } else {
            activeImageView.setVisibility(View.VISIBLE);
            inactiveImageView.setVisibility(View.INVISIBLE);
            lostToFollowUpImageView.setVisibility(View.INVISIBLE);
        }

        activeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateView updateView = new UpdateView(STATUS.ACTIVE, listener, details);
                updateView.setImageView(activeImageView, inactiveImageView, lostToFollowUpImageView);

                Utils.startAsyncTask(updateView, null);

            }
        });

        inactiveLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateView updateView = new UpdateView(STATUS.IN_ACTIVE, listener, details);
                updateView.setImageView(activeImageView, inactiveImageView, lostToFollowUpImageView);

                Utils.startAsyncTask(updateView, null);
            }
        });

        lostToFollowUpLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateView updateView = new UpdateView(STATUS.LOST_TO_FOLLOW_UP, listener, details);
                updateView.setImageView(activeImageView, inactiveImageView, lostToFollowUpImageView);

                Utils.startAsyncTask(updateView, null);
            }
        });

        childArchiveRecordLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog dialog = new AlertDialog.Builder(getActivity(), R.style.AppThemeAlertDialog)
                        .setTitle(R.string.child_archive_record_text)
                        .setCancelable(true)
                        .setMessage(R.string.child_archive_dialog_title)
                        .setNegativeButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                archiveChildRecord(details);
                            }
                        }).setPositiveButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create();

                dialog.show();
            }
        });


        return dialogView;
    }

    protected void archiveChildRecord(@NonNull Map<String, String> details) {
        new ArchiveRecordTask(getActivity(), details).execute();
    }

    private enum STATUS {
        ACTIVE, IN_ACTIVE, LOST_TO_FOLLOW_UP
    }

    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////
    private class UpdateView extends AsyncTask<Void, Void, Boolean> {
        private StatusChangeListener listener;
        private STATUS status;
        private Map<String, String> details;
        private ImageView activeImageView;
        private ImageView inactiveImageView;
        private ImageView lostToFollowUpImageView;
        private ProgressDialog progressDialog;

        private UpdateView(STATUS status, StatusChangeListener listener, Map<String, String> details) {
            this.status = status;
            this.listener = listener;
            this.details = details;

            this.progressDialog = new ProgressDialog(getActivity());
            this.progressDialog.setCancelable(false);
            this.progressDialog.setTitle(getString(R.string.updating_dialog_title));
            this.progressDialog.setMessage(getString(R.string.please_wait_message));
        }

        private void setImageView(ImageView activeImageView, ImageView inactiveImageView,
                                  ImageView lostToFollowUpImageView) {
            this.activeImageView = activeImageView;
            this.inactiveImageView = inactiveImageView;
            this.lostToFollowUpImageView = lostToFollowUpImageView;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean updateViews = false;
            switch (status) {
                case ACTIVE:
                    if (details.containsKey(inactive) && details.get(inactive) != null &&
                            details.get(inactive).equalsIgnoreCase(Boolean.TRUE.toString())) {
                        listener.updateClientAttribute(inactive, false);
                        updateViews = true;
                        details.put(inactive, "false");
                    }

                    if (details.containsKey(lostToFollowUp) && details.get(lostToFollowUp) != null &&
                            details.get(lostToFollowUp).equalsIgnoreCase(Boolean.TRUE.toString())) {
                        listener.updateClientAttribute(lostToFollowUp, false);
                        details.put(lostToFollowUp, "false");
                        updateViews = true;
                    }
                    break;

                case IN_ACTIVE:
                    if (!details.containsKey(inactive) || !(details.containsKey(inactive) && details.get(inactive) != null &&
                            details.get(inactive).equalsIgnoreCase(Boolean.TRUE.toString()))) {
                        listener.updateClientAttribute(inactive, true);
                        details.put(inactive, "true");
                        if (details.containsKey(lostToFollowUp) && details.get(lostToFollowUp) != null &&
                                details.get(lostToFollowUp).equalsIgnoreCase(Boolean.TRUE.toString())) {
                            listener.updateClientAttribute(lostToFollowUp, false);
                            details.put(lostToFollowUp, "false");
                        }
                        updateViews = true;
                    }
                    break;
                case LOST_TO_FOLLOW_UP:
                    if (!details.containsKey(lostToFollowUp) ||
                            !(details.containsKey(lostToFollowUp) && details.get(lostToFollowUp) != null &&
                                    details.get(lostToFollowUp).equalsIgnoreCase(Boolean.TRUE.toString()))) {
                        listener.updateClientAttribute(lostToFollowUp, true);
                        details.put(lostToFollowUp, "true");
                        if (details.containsKey(inactive) && details.get(inactive) != null &&
                                details.get(inactive).equalsIgnoreCase(Boolean.TRUE.toString())) {
                            listener.updateClientAttribute(inactive, false);
                            details.put(inactive, "false");
                        }
                        updateViews = true;
                    }
                    break;
                default:
                    break;

            }

            return updateViews;
        }

        @Override
        protected void onPreExecute() {
            this.progressDialog.show();
        }

        @Override
        protected void onPostExecute(Boolean updateViews) {
            this.progressDialog.dismiss();
            if (updateViews) {
                switch (status) {
                    case ACTIVE:
                        activeImageView.setVisibility(View.VISIBLE);
                        inactiveImageView.setVisibility(View.INVISIBLE);
                        lostToFollowUpImageView.setVisibility(View.INVISIBLE);
                        break;
                    case IN_ACTIVE:
                        activeImageView.setVisibility(View.INVISIBLE);
                        inactiveImageView.setVisibility(View.VISIBLE);
                        lostToFollowUpImageView.setVisibility(View.INVISIBLE);
                        break;
                    case LOST_TO_FOLLOW_UP:
                        activeImageView.setVisibility(View.INVISIBLE);
                        inactiveImageView.setVisibility(View.INVISIBLE);
                        lostToFollowUpImageView.setVisibility(View.VISIBLE);
                        break;
                    default:
                        break;
                }

                if (status != null) {
                    org.smartregister.child.util.Utils.postEvent(new ClientStatusUpdateEvent(status.toString()));
                }
            }
            listener.updateStatus(details);
            dismiss();

        }
    }

}
