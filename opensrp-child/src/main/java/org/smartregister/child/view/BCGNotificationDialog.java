package org.smartregister.child.view;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.Button;

import org.smartregister.child.R;

/**
 * Created by ndegwamartin on 01/09/2020.
 */
public class BCGNotificationDialog {

    private final SparseIntArray selectedOption = new SparseIntArray();
    private final int YES = 0;
    private final int NO = 1;
    private final int SELECTED_OPTION = 2;
    private final int THEME = R.style.AppThemeAlertDialog;

    private AlertDialog alertDialog;
    private AlertDialog subDialogPositive;
    private AlertDialog subDialogNegative;

    private Context context;

    private final DialogInterface.OnClickListener backListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {

            if (alertDialog != null) {

                showDisablePositiveButton(alertDialog);
                increaseBtnTextSizeTabletDevice(alertDialog);
            }
        }
    };

    public BCGNotificationDialog(final Context context, final DialogInterface.OnClickListener subDialogPositiveListener,
                                 final DialogInterface.OnClickListener subDialogNegativeListener) {
        this.context = context;
        String[] singleChoiceItems = context.getResources().getStringArray(R.array.bcg_notification_options);
        alertDialog = new AlertDialog.Builder(context, THEME)
                .setCustomTitle(View.inflate(context, R.layout.dialog_view_title_bcg_scar, null))
                .setSingleChoiceItems(singleChoiceItems, -1, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int selectedIndex) {

                        ((AlertDialog) dialogInterface).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                        selectedOption.put(SELECTED_OPTION, selectedIndex);
                    }
                }).setPositiveButton(R.string.ok_button_label, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int selectedIndex) {

                        alertDialog =
                                new BCGNotificationDialog(context, subDialogPositiveListener, subDialogNegativeListener)
                                        .getAlertDialogInstance();

                        if (selectedOption.get(SELECTED_OPTION, NO) == YES) {
                            subDialogPositive.show();
                            increaseBtnTextSizeTabletDevice(subDialogPositive);
                        } else {
                            subDialogNegative.show();
                            increaseBtnTextSizeTabletDevice(subDialogNegative);
                        }
                    }
                }).setNegativeButton(R.string.dismiss_button_label, null).create();

        subDialogPositive = new AlertDialog.Builder(context, THEME).setCancelable(false)
                .setCustomTitle(View.inflate(context, R.layout.dialog_view_title_bcg_turn_off, null))
                .setPositiveButton(R.string.turn_off_reminder_button_label, subDialogPositiveListener)
                .setNegativeButton(R.string.go_back_button_label, backListener).create();

        subDialogNegative =
                new AlertDialog.Builder(context, THEME).setCancelable(false).setTitle(R.string.create_reminder_label)
                        .setCustomTitle(View.inflate(context, R.layout.dialog_view_title_bcg_create, null))
                        .setPositiveButton(R.string.create_reminder_button_label, subDialogNegativeListener)
                        .setNegativeButton(R.string.go_back_button_label, backListener).create();
    }

    private AlertDialog getAlertDialogInstance() {
        return alertDialog;
    }

    private void increaseBtnTextSizeTabletDevice(@NonNull AlertDialog alertDialog) {

        final float TEXT_SIZE = 20f;
        final int TABLET_WIDTH_DP = 600;
        final Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        final Button negativeButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        final float DEVICE_DP = displayMetrics.density;
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int DEVICE_WIDTH_DP = (int) (displayMetrics.widthPixels / DEVICE_DP);
        final int DIALOG_BUTTON_PADDING_TOP = (int) alertDialog.getContext().getResources().getDimension(R.dimen.bcg_popup_button_padding_top);
        final int DEFAULT_DIALOG_BUTTON_PADDING = (int) alertDialog.getContext().getResources().getDimension(R.dimen.bcg_popup_button_padding);


        if (DEVICE_WIDTH_DP >= TABLET_WIDTH_DP) {

            positiveButton.setTextSize(TEXT_SIZE);
            negativeButton.setTextSize(TEXT_SIZE);
            positiveButton
                    .setPadding(DEFAULT_DIALOG_BUTTON_PADDING, DIALOG_BUTTON_PADDING_TOP, DEFAULT_DIALOG_BUTTON_PADDING,
                            DEFAULT_DIALOG_BUTTON_PADDING);
            negativeButton
                    .setPadding(DEFAULT_DIALOG_BUTTON_PADDING, DIALOG_BUTTON_PADDING_TOP, DEFAULT_DIALOG_BUTTON_PADDING,
                            DEFAULT_DIALOG_BUTTON_PADDING);
        }
    }

    public void show() {
        showDisablePositiveButton(alertDialog);
        increaseBtnTextSizeTabletDevice(alertDialog);
    }

    public void showDisablePositiveButton(@NonNull AlertDialog alertDialog) {

        alertDialog.show();
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
    }
}
