package org.smartregister.child.fragment;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckBox;

import com.google.android.material.snackbar.Snackbar;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.vijay.jsonwizard.activities.JsonFormActivity;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.customviews.MaterialSpinner;
import com.vijay.jsonwizard.domain.Form;
import com.vijay.jsonwizard.fragments.JsonWizardFormFragment;
import com.vijay.jsonwizard.presenters.JsonFormFragmentPresenter;
import com.vijay.jsonwizard.utils.FormUtils;
import com.vijay.jsonwizard.utils.ValidationStatus;
import com.vijay.jsonwizard.viewstates.JsonFormFragmentViewState;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.smartregister.child.ChildLibrary;
import org.smartregister.child.R;
import org.smartregister.child.interactor.ChildFormInteractor;
import org.smartregister.child.presenter.ChildFormFragmentPresenter;
import org.smartregister.child.provider.MotherLookUpSmartClientsProvider;
import org.smartregister.child.util.Constants;
import org.smartregister.child.util.MotherLookUpUtils;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.event.Listener;
import org.smartregister.util.Utils;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

import static org.smartregister.util.Utils.getValue;

/**
 * Created by ndegwamartin on 01/03/2019.
 */
public class ChildFormFragment extends JsonWizardFormFragment {

    public static final String TAG = ChildFormFragment.class.getName();
    private static final int showResultsDuration = Integer.parseInt(ChildLibrary
            .getInstance()
            .getProperties()
            .getProperty(Constants.PROPERTY.MOTHER_LOOKUP_SHOW_RESULTS_DURATION, Constants.MOTHER_LOOKUP_SHOW_RESULTS_DEFAULT_DURATION));
    private static final int undoChoiceDuration = Integer.parseInt(ChildLibrary
            .getInstance()
            .getProperties()
            .getProperty(Constants.PROPERTY.MOTHER_LOOKUP_UNDO_DURATION, Constants.MOTHER_LOOKUP_UNDO_DEFAULT_DURATION));
    private Snackbar snackbar = null;
    private AlertDialog alertDialog = null;
    private boolean lookedUp = false;
    private final View.OnClickListener lookUpRecordOnClickLister = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (alertDialog != null && alertDialog.isShowing()) {
                alertDialog.dismiss();
                CommonPersonObjectClient client = null;
                if (view.getTag() != null && view.getTag() instanceof CommonPersonObjectClient) {
                    client = (CommonPersonObjectClient) view.getTag();
                }

                if (client != null) {
                    lookupDialogDismissed(client);
                }
            }
        }
    };

    private final Listener<HashMap<CommonPersonObject, List<CommonPersonObject>>> motherLookUpListener =
            new Listener<HashMap<CommonPersonObject, List<CommonPersonObject>>>() {
                @Override
                public void onEvent(HashMap<CommonPersonObject, List<CommonPersonObject>> data) {
                    if (!lookedUp) {
                        showMotherLookUp(data);
                    }
                }
            };

    public static ChildFormFragment getFormFragment(String stepName) {
        ChildFormFragment jsonFormFragment = new ChildFormFragment();
        Bundle bundle = new Bundle();
        bundle.putString(JsonFormConstants.JSON_FORM_KEY.STEPNAME, stepName);
        jsonFormFragment.setArguments(bundle);
        return jsonFormFragment;
    }

    @Override
    protected JsonFormFragmentViewState createViewState() {
        return new JsonFormFragmentViewState();
    }

    @Override
    protected ChildFormFragmentPresenter createPresenter() {
        return new ChildFormFragmentPresenter(this, ChildFormInteractor.getChildInteractorInstance());
    }

    @Override
    public void updateVisibilityOfNextAndSave(boolean next, boolean save) {
        super.updateVisibilityOfNextAndSave(next, save);
        Form form = getForm();
        if (form != null && form.isWizard() &&
                !ChildLibrary.getInstance().metadata().formWizardValidateRequiredFieldsBefore) {
            this.getMenu().findItem(com.vijay.jsonwizard.R.id.action_save).setVisible(save);
        }
    }

    private Form getForm() {
        return this.getActivity() != null && this.getActivity() instanceof JsonFormActivity ?
                ((JsonFormActivity) this.getActivity()).getForm() : null;
    }

    public void validateActivateNext() {
        if (!isVisible()) { //form fragment is initializing or not the last page
            return;
        }

        Form form = getForm();
        if (form == null || !form.isWizard()) {
            return;
        }

        ValidationStatus validationStatus = null;
        for (View dataView : getJsonApi().getFormDataViews()) {
            validationStatus = validateView(dataView);
            if (!validationStatus.isValid()) {
                break;
            }
        }

        if (validationStatus != null && validationStatus.isValid()) {
            if (!getPresenter().intermediatePage()) {
                getMenu().findItem(com.vijay.jsonwizard.R.id.action_save).setVisible(true);
            }
        } else {
            if (!getPresenter().intermediatePage()) {
                getMenu().findItem(com.vijay.jsonwizard.R.id.action_save).setVisible(false);
            }
        }
    }

    public ChildFormFragmentPresenter getPresenter() {
        return (ChildFormFragmentPresenter) presenter;
    }

    @VisibleForTesting
    protected ValidationStatus validateView(View dataView) {
        return JsonFormFragmentPresenter.validate(this, dataView, false);
    }

    //Mother Lookup
    public Listener<HashMap<CommonPersonObject, List<CommonPersonObject>>> motherLookUpListener() {
        return motherLookUpListener;
    }

    private void showMotherLookUp(final HashMap<CommonPersonObject, List<CommonPersonObject>> map) {
        if (!map.isEmpty()) {
            tapToView(map);
        } else {
            if (snackbar != null) {
                snackbar.dismiss();
            }
        }
    }

    private void tapToView(final HashMap<CommonPersonObject, List<CommonPersonObject>> map) {
        snackbar = Snackbar.make(getMainView(), getActivity().getString(R.string.mother_guardian_matches, String.valueOf(map.size())),
                Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.tap_to_view, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateResults(map);
                //updateResultTree(map);
            }
        });
        show(snackbar);

    }

    private void updateResults(final HashMap<CommonPersonObject, List<CommonPersonObject>> map) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.mother_lookup_results, null);

        ListView listView = view.findViewById(R.id.list_view);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.PathDialog);
        builder.setView(view).setNegativeButton(R.string.dismiss, null);
        builder.setCancelable(true);

        alertDialog = builder.create();

        final List<CommonPersonObject> mothers = new ArrayList<>();
        for (Map.Entry<CommonPersonObject, List<CommonPersonObject>> entry : map.entrySet()) {
            mothers.add(entry.getKey());
        }

        final MotherLookUpSmartClientsProvider motherLookUpSmartClientsProvider =
                new MotherLookUpSmartClientsProvider(getActivity());
        BaseAdapter baseAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mothers.size();
            }

            @Override
            public Object getItem(int position) {
                return mothers.get(position);
            }

            @Override
            public long getItemId(int position) {
                return new BigInteger(mothers.get(position).getCaseId().replaceAll("\\D+", "")).longValue();
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v;
                if (convertView == null) {
                    v = motherLookUpSmartClientsProvider.inflateLayoutForCursorAdapter();
                } else {
                    v = convertView;
                }

                CommonPersonObject commonPersonObject = mothers.get(position);
                List<CommonPersonObject> children = map.get(commonPersonObject);

                motherLookUpSmartClientsProvider.getView(commonPersonObject, children, v);

                v.setOnClickListener(lookUpRecordOnClickLister);
                v.setTag(Utils.convert(commonPersonObject));

                return v;
            }
        };

        listView.setAdapter(baseAdapter);
        alertDialog.show();

    }

    private void show(final Snackbar snackbar) {
        if (snackbar == null) {
            return;
        }

        float drawablePadding = getResources().getDimension(R.dimen.register_drawable_padding);
        int paddingInt = Float.valueOf(drawablePadding).intValue();

        float textSize = getActivity().getResources().getDimension(R.dimen.snack_bar_text_size);

        View snackbarView = getSnackBarView(snackbar);
        snackbarView.setMinimumHeight(Float.valueOf(textSize).intValue());
        snackbarView.setBackgroundResource(R.color.accent);

        final Button actionView = snackbarView.findViewById(R.id.snackbar_action);
        actionView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        actionView.setGravity(Gravity.CENTER);
        actionView.setTextColor(getResources().getColor(R.color.white));

        TextView textView = snackbarView.findViewById(R.id.snackbar_text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        textView.setGravity(Gravity.CENTER);
        textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_error, 0, 0, 0);
        textView.setCompoundDrawablePadding(paddingInt);
        textView.setPadding(paddingInt, 0, 0, 0);
        textView.setTextColor(getResources().getColor(R.color.white));

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionView.performClick();
            }
        });

        snackbarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionView.performClick();
            }
        });

        snackbar.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                snackbar.dismiss();
            }
        }, showResultsDuration);

    }

    private void showFinalActionSnackBar(final Snackbar snackbar) {
        if (snackbar == null) {
            return;
        }

        float drawablePadding = getResources().getDimension(R.dimen.register_drawable_padding);
        int paddingInt = Float.valueOf(drawablePadding).intValue();

        float textSize = getActivity().getResources().getDimension(R.dimen.snack_bar_text_size);

        View snackbarView = getSnackBarView(snackbar);
        snackbarView.setMinimumHeight(Float.valueOf(textSize).intValue());
        snackbarView.setBackgroundResource(R.color.accent);

        final Button actionView = snackbarView.findViewById(R.id.snackbar_action);
        actionView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        actionView.setGravity(Gravity.CENTER);
        actionView.setTextColor(getResources().getColor(R.color.white));

        TextView textView = snackbarView.findViewById(R.id.snackbar_text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        textView.setGravity(Gravity.CENTER);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearMotherLookUp();
            }
        });
        textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_error, 0, 0, 0);
        textView.setCompoundDrawablePadding(paddingInt);
        textView.setPadding(paddingInt, 0, 0, 0);
        textView.setTextColor(getResources().getColor(R.color.white));
        textView.setAllCaps(true);
        textView.setTypeface(Typeface.DEFAULT_BOLD);

        snackbarView.setOnClickListener(v -> {
            // actionView.performClick();
        });

        showSnackBar(snackbar);

        Handler handler = new Handler();
        handler.postDelayed(() -> dismissSnackBar(snackbar), showResultsDuration);

    }

    @VisibleForTesting
    @NotNull
    protected void showSnackBar(Snackbar snackbar) {
        snackbar.show();
    }

    @VisibleForTesting
    @NotNull
    protected void dismissSnackBar(Snackbar snackbar) {
        snackbar.dismiss();
    }

    @VisibleForTesting
    @NotNull
    protected View getSnackBarView(Snackbar snackbar) {
        return snackbar.getView();
    }

    protected void lookupDialogDismissed(CommonPersonObjectClient client) {
        final Map<String, String> keyAliasMap = getKeyAliasMap();
        if (client != null && getActivity() != null) {
            Map<String, List<View>> lookupMap = getLookUpMap();
            if (lookupMap.containsKey(Constants.KEY.MOTHER)) {
                List<View> lookUpViews = lookupMap.get(Constants.KEY.MOTHER);
                if (lookUpViews != null && !lookUpViews.isEmpty()) {
                    for (View view : lookUpViews) {
                        String key = (String) view.getTag(com.vijay.jsonwizard.R.id.key);
                        String fieldName = keyAliasMap.get(key) != null ? keyAliasMap.get(key) : key;
                        String value = getCurrentFieldValue(client.getColumnmaps(), fieldName);
                        setValueOnView(fieldName, value, view);
                    }
                    updateFormLookupField(client);
                }
            }
        }
    }

    /**
     * Map the name of field as in form json to the column name of the client object returned after mother lookup
     * For instance you may name your field as Mother_Guardian_First_Name whereas the client object returned
     * uses first_name. So this map will map the field key to the column name. E.g. Mother_Guardian_First_Name -> first_name
     *
     * @return a map of key against their column names.
     */
    @NotNull
    protected HashMap<String, String> getKeyAliasMap() {
        return new HashMap<>();
    }

    /**
     * Return a list of fields you do not want to format their field values
     *
     * @return non humanized (formatted) field values
     */
    @NotNull
    protected HashSet<String> getNonHumanizedFields() {
        return new HashSet<>();
    }

    private String getCurrentFieldValue(Map<String, String> columnMaps, String fieldName) {
        String value = getValue(columnMaps, fieldName, !getNonHumanizedFields().contains(fieldName));
        if (getActivity() != null) {
            Locale locale = getActivity().getResources().getConfiguration().locale;
            SimpleDateFormat mlsLookupDateFormatter = new SimpleDateFormat(FormUtils.NATIIVE_FORM_DATE_FORMAT_PATTERN,
                    locale.getLanguage().equals("ar") ? Locale.ENGLISH : locale);
            if (fieldName.equalsIgnoreCase(Constants.KEY.DOB)) {
                String dobString = getValue(columnMaps, Constants.KEY.DOB, false);
                Date motherDob = Utils.dobStringToDate(dobString);
                if (motherDob != null) {
                    try {
                        value = mlsLookupDateFormatter.format(motherDob);
                    } catch (Exception e) {
                        Timber.e(e, e.toString());
                    }
                }
            }
        }
        return value;
    }

    protected void setValueOnView(String fieldName, String value, View view) {
        if (StringUtils.isNotBlank(value)) {
            if (view instanceof MaterialEditText) {
                MaterialEditText materialEditText = (MaterialEditText) view;
                materialEditText.setEnabled(false);
                materialEditText.setTag(R.id.after_look_up, true);
                materialEditText.setText(value);
                materialEditText.setInputType(InputType.TYPE_NULL);
            } else if (view instanceof RelativeLayout) {
                setSpinnerValue(value, (ViewGroup) view);
            } else if (view instanceof LinearLayout) {
                setCheckboxValue(fieldName, value, (ViewGroup) view);
            }
        }
    }

    private void setCheckboxValue(String fieldName, String value, ViewGroup viewGroup) {
        if (viewGroup.getChildCount() == 2 && viewGroup.getChildAt(1) instanceof LinearLayout) {
            LinearLayout innerLayout = (LinearLayout) viewGroup.getChildAt(1);
            if (innerLayout.getChildAt(0) instanceof AppCompatCheckBox) {
                AppCompatCheckBox checkBox = (AppCompatCheckBox) innerLayout.getChildAt(0);
                if (value.contains(fieldName)) {
                    checkBox.setChecked(true);
                } else {
                    checkBox.setChecked(false);
                }
            }
        }
    }

    private void setSpinnerValue(String value, ViewGroup spinnerViewGroup) {
        if (spinnerViewGroup.getChildAt(0) instanceof MaterialSpinner) {
            MaterialSpinner spinner = (MaterialSpinner) spinnerViewGroup.getChildAt(0);
            for (int index = 0; index < spinner.getAdapter().getCount(); index++) {
                if (String.valueOf(spinner.getAdapter().getItem(index)).equalsIgnoreCase(value)) {
                    spinner.setSelection(index + 1);
                    break;
                }
            }
            spinner.setEnabled(false);
        }
    }

    protected void updateFormLookupField(CommonPersonObjectClient client) {
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put(Constants.KEY.ENTITY_ID, Constants.KEY.MOTHER);
        metadataMap.put(Constants.KEY.VALUE, getValue(client.getColumnmaps(), MotherLookUpUtils.baseEntityId, false));
        writeMetaDataValue(FormUtils.LOOK_UP_JAVAROSA_PROPERTY, metadataMap);
        lookedUp = true;
        clearView();
    }

    protected void clearView() {
        snackbar = Snackbar.make(getMainView(), R.string.undo_lookup, Snackbar.LENGTH_INDEFINITE);
        snackbar.setDuration(undoChoiceDuration);
        snackbar.setAction(R.string.dismiss_lookup, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackbar.dismiss();
            }
        });
        showFinalActionSnackBar(snackbar);
    }

    private void clearMotherLookUp() {
        Map<String, List<View>> lookupMap = getLookUpMap();
        if (lookupMap.containsKey(Constants.KEY.MOTHER)) {
            List<View> lookUpViews = lookupMap.get(Constants.KEY.MOTHER);
            if (lookUpViews != null && !lookUpViews.isEmpty()) {
                for (View view : lookUpViews) {
                    if (view instanceof MaterialEditText) {
                        MaterialEditText materialEditText = (MaterialEditText) view;
                        materialEditText.setEnabled(true);
                        enableEditText(materialEditText);
                        materialEditText.setTag(com.vijay.jsonwizard.R.id.after_look_up, false);
                        materialEditText.setText("");
                    } else if (view instanceof RelativeLayout) {
                        ViewGroup spinnerViewGroup = (ViewGroup) view;
                        if (spinnerViewGroup.getChildAt(0) instanceof MaterialSpinner) {
                            MaterialSpinner spinner = (MaterialSpinner) spinnerViewGroup.getChildAt(0);
                            spinner.setSelected(false);
                            spinner.setEnabled(true);
                        }
                    }
                }

                Map<String, String> metadataMap = new HashMap<>();
                metadataMap.put(Constants.KEY.ENTITY_ID, "");
                metadataMap.put(Constants.KEY.VALUE, "");
                writeMetaDataValue(FormUtils.LOOK_UP_JAVAROSA_PROPERTY, metadataMap);
                lookedUp = false;
            }
        }
    }

    private void enableEditText(MaterialEditText editText) {
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
    }

    public void getLabelViewFromTag(String labeltext, String todisplay) {
        updateRelevantTextView(getMainView(), todisplay, labeltext);
    }

    private void updateRelevantTextView(LinearLayout mMainView, String textstring, String currentKey) {
        if (mMainView != null) {
            int childCount = mMainView.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View view = mMainView.getChildAt(i);
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    String key = (String) textView.getTag(com.vijay.jsonwizard.R.id.key);
                    if (key.equals(currentKey)) {
                        textView.setText(textstring);
                    }
                }
            }
        }
    }

    public String getRelevantTextViewString(String currentKey) {
        String relevantText = "";
        if (getMainView() != null) {
            int childCount = getMainView().getChildCount();
            for (int i = 0; i < childCount; i++) {
                View view = getMainView().getChildAt(i);
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    String key = (String) textView.getTag(com.vijay.jsonwizard.R.id.key);
                    if (key.equals(currentKey)) {
                        relevantText = textView.getText().toString();
                    }
                }
            }
        }
        return relevantText;
    }
}
