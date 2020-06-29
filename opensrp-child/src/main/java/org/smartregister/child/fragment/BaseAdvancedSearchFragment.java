package org.smartregister.child.fragment;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.appcompat.app.AlertDialog;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.vijay.jsonwizard.customviews.CheckBox;
import com.vijay.jsonwizard.customviews.RadioButton;
import com.vijay.jsonwizard.utils.FormUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.child.ChildLibrary;
import org.smartregister.child.R;
import org.smartregister.child.activity.BaseChildImmunizationActivity;
import org.smartregister.child.activity.BaseChildRegisterActivity;
import org.smartregister.child.contract.ChildAdvancedSearchContract;
import org.smartregister.child.contract.ChildRegisterFragmentContract;
import org.smartregister.child.cursor.AdvancedMatrixCursor;
import org.smartregister.child.domain.RegisterClickables;
import org.smartregister.child.domain.RepositoryHolder;
import org.smartregister.child.presenter.BaseChildAdvancedSearchPresenter;
import org.smartregister.child.provider.AdvancedSearchClientsProvider;
import org.smartregister.child.util.ChildAppProperties;
import org.smartregister.child.util.ChildJsonFormUtils;
import org.smartregister.child.util.Constants;
import org.smartregister.child.util.MoveToMyCatchmentUtils;
import org.smartregister.child.util.Utils;
import org.smartregister.child.widgets.AdvanceSearchDatePickerDialog;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.domain.FetchStatus;
import org.smartregister.event.Listener;
import org.smartregister.growthmonitoring.GrowthMonitoringLibrary;
import org.smartregister.immunization.ImmunizationLibrary;
import org.smartregister.view.activity.BaseRegisterActivity;
import org.smartregister.view.activity.SecuredNativeSmartRegisterActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

public abstract class BaseAdvancedSearchFragment extends BaseChildRegisterFragment
        implements ChildAdvancedSearchContract.View, ChildRegisterFragmentContract.View, View.OnClickListener {
    public static final String START_DATE = "start_date";
    public static final String END_DATE = "end_date";
    private final Listener<JSONObject> moveToMyCatchmentListener = new Listener<JSONObject>() {
        public void onEvent(final JSONObject jsonObject) {
            if (jsonObject != null) {
                if (MoveToMyCatchmentUtils.processMoveToCatchment(getActivity(), context().allSharedPreferences(), jsonObject)) {
                    clientAdapter.notifyDataSetChanged();
                    ((BaseRegisterActivity) getActivity()).refreshList(FetchStatus.fetched);
                    ((BaseRegisterActivity) getActivity()).switchToBaseFragment();
                } else {
                    Utils.showShortToast(getActivity(), getActivity().getString(R.string.an_error_occured));
                }
            } else {
                Utils.showShortToast(getActivity(), getActivity().getString(R.string.unable_to_move_to_my_catchment));
            }
        }
    };
    protected AdvancedSearchTextWatcher advancedSearchTextwatcher = new AdvancedSearchTextWatcher();
    protected HashMap<String, String> searchFormData = new HashMap<>();
    protected CheckBox active;
    protected CheckBox inactive;
    protected CheckBox lostToFollowUp;
    protected EditText startDate;
    protected EditText endDate;
    protected Button qrCodeButton;
    protected Map<String, View> advancedFormSearchableFields = new HashMap<>();
    private View listViewLayout;
    private View advancedSearchForm;
    private ImageButton backButton;
    private Button searchButton;
    private Button advancedSearchToolbarSearchButton;
    private RadioButton outsideInside;
    private RadioButton myCatchment;
    private TextView searchCriteria;
    private TextView matchingResults;
    private boolean listMode = false;
    private boolean isLocal = false;
    private BroadcastReceiver connectionChangeReciever;
    private boolean registeredConnectionChangeReceiver = false;
    private ProgressDialog progressDialog;
    private AdvanceSearchDatePickerDialog startDateDatePicker;
    private AdvanceSearchDatePickerDialog endDateDatePicker;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat(FormUtils.NATIIVE_FORM_DATE_FORMAT_PATTERN,
            Locale.getDefault().toString().startsWith("ar") ? Locale.ENGLISH : Locale.getDefault());

    @Override
    protected void initializePresenter() {
        presenter = getPresenter();
        initProgressDialog();

    }

    private void initProgressDialog() {
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setCancelable(false);
    }

    protected abstract BaseChildAdvancedSearchPresenter getPresenter();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_advanced_search, container, false);
        rootView = view;//handle to the root
        setupViews(view);
        onResumption();
        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            switchViews(false);
            updateSearchLimits();
            resetForm();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (connectionChangeReciever != null && registeredConnectionChangeReceiver) {
            getActivity().unregisterReceiver(connectionChangeReciever);
            registeredConnectionChangeReceiver = false;
        }
    }

    @Override
    public boolean onBackPressed() {
        goBack();
        return true;
    }

    @Override
    protected void goBack() {
        if (listMode) {
            switchViews(false);
        } else {
            ((BaseRegisterActivity) getActivity()).switchToBaseFragment();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onViewClicked(View view) {
        if (view.getId() == R.id.record_growth && view.getTag() != null) {
            if (view.getTag() instanceof String) {
                recordService((String) view.getTag());
            } else if (view.getTag() instanceof CommonPersonObjectClient) {
                recordGrowth(view);
            }
        } else if (view.getId() == R.id.search) {
            search();
        } else if (view.getId() == R.id.advanced_form_search_btn) {
            search();
        } else if (view.getId() == R.id.back_button) {
            switchViews(false);
        } else if ((view.getId() == R.id.patient_column || view.getId() == R.id.child_profile_info_layout)
                && view.getTag() != null) {
            recordGrowth(view);
        } else if (view.getId() == R.id.move_to_catchment && view.getTag(R.id.move_to_catchment_ids)
                != null && view.getTag(R.id.move_to_catchment_ids) instanceof List) {
            List<String> ids = (List<String>) view.getTag(R.id.move_to_catchment_ids);
            moveToMyCatchmentArea(ids);
        }
    }

    private void recordGrowth(View view) {
        RegisterClickables registerClickables = new RegisterClickables();
        if (view.getTag(org.smartregister.child.R.id.record_action) != null) {
            registerClickables.setRecordWeight(Constants.RECORD_ACTION.GROWTH.equals(view.getTag(org.smartregister.child.R.id.record_action)));
            registerClickables.setRecordAll(Constants.RECORD_ACTION.VACCINATION.equals(view.getTag(org.smartregister.child.R.id.record_action)));
            registerClickables.setNextAppointmentDate(view.getTag(R.id.next_appointment_date) != null ? String.valueOf(view.getTag(R.id.next_appointment_date)) : "");
        }
        BaseChildImmunizationActivity.launchActivity(getActivity(), (CommonPersonObjectClient) view.getTag(), registerClickables);
    }

    protected void recordService(String openSrpId) {
        if (getActivity() != null) {
            try {
                ChildJsonFormUtils.startForm(getActivity(), ChildJsonFormUtils.REQUEST_CODE_GET_JSON, getOutOfCatchmentServiceFormName(), openSrpId,
                        ChildLibrary.getInstance().getLocationPickerView(getActivity()).getSelectedItem());
            } catch (Exception e) {
                Utils.showShortToast(getActivity(), getString(R.string.error_recording_out_of_catchment_service));
                Timber.e(e, "Error recording Out of Catchment Service");
            }
        } else {
            Utils.showShortToast(getActivity(), getString(R.string.error_recording_out_of_catchment_service));
        }
    }

    protected String getOutOfCatchmentServiceFormName() {
        return Constants.JsonForm.OUT_OF_CATCHMENT_SERVICE;
    }

    private void moveToMyCatchmentArea(final List<String> ids) {
        AlertDialog dialog = new AlertDialog.Builder(getActivity(), R.style.PathAlertDialog)
                .setMessage(R.string.move_to_catchment_confirm_dialog_message)
                .setTitle(R.string.move_to_catchment_confirm_dialog_title)
                .setCancelable(false)
                .setPositiveButton(R.string.no_button_label, null)
                .setNegativeButton(R.string.yes_button_label,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                progressDialog.setTitle(R.string.move_to_catchment_dialog_title);
                                progressDialog.setMessage(getString(R.string.move_to_catchment_dialog_message));
                                MoveToMyCatchmentUtils.moveToMyCatchment(ids, moveToMyCatchmentListener, progressDialog);
                            }
                        }).create();

        dialog.show();
    }

    @Override
    public void setupViews(View view) {
        super.setupViews(view);

        listViewLayout = view.findViewById(R.id.advanced_search_list);
        listViewLayout.setVisibility(View.GONE);

        advancedSearchForm = view.findViewById(R.id.advanced_search_form);
        backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(registerActionHandler);

        searchCriteria = view.findViewById(R.id.search_criteria);
        matchingResults = view.findViewById(R.id.matching_results);
        advancedSearchToolbarSearchButton = view.findViewById(R.id.search);
        searchButton = view.findViewById(R.id.advanced_form_search_btn);
        outsideInside = view.findViewById(R.id.out_and_inside);
        myCatchment = view.findViewById(R.id.my_catchment);

        populateFormViews(view);

        populateSearchableFields(view);

        resetForm();

    }

    public abstract void populateSearchableFields(View view);

    @Override
    public void initializeAdapter(Set<org.smartregister.configurableviews.model.View> visibleColumns) {

        RepositoryHolder repoHolder = new RepositoryHolder();

        repoHolder.setWeightRepository(GrowthMonitoringLibrary.getInstance().weightRepository());
        repoHolder.setHeightRepository(GrowthMonitoringLibrary.getInstance().heightRepository());
        repoHolder.setVaccineRepository(ImmunizationLibrary.getInstance().vaccineRepository());
        repoHolder.setCommonRepository(commonRepository());

        AdvancedSearchClientsProvider advancedSearchProvider = new AdvancedSearchClientsProvider(getActivity(), repoHolder, visibleColumns, registerActionHandler, paginationViewHandler, ChildLibrary.getInstance().context().alertService());

        clientAdapter = new RecyclerViewPaginatedAdapter(null, advancedSearchProvider, context().commonrepository(this.tablename));
        clientsView.setAdapter(clientAdapter);
    }

    protected void populateFormViews(View view) {

        setUpSearchButtons();

        setUpMyCatchmentControls(view, outsideInside, myCatchment, R.id.out_and_inside_layout);

        setUpMyCatchmentControls(view, myCatchment, outsideInside, R.id.my_catchment_layout);

        startDate = view.findViewById(R.id.start_date);
        startDate.setTag(R.id.type, START_DATE);
        startDate.setOnClickListener(this);
        startDate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //Overridden
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //Overridden
            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    if (StringUtils.isNoneBlank(editable))
                        endDateDatePicker.getDatePickerDialog().setMinDate(dateFormatter.parse(editable.toString()).getTime());
                } catch (ParseException e) {
                    Timber.e(e, "Error setting end date minimum to start date");
                }
            }
        });
        startDateDatePicker = new AdvanceSearchDatePickerDialog(startDate);
        startDateDatePicker.setDateFormatter(dateFormatter);

        endDate = view.findViewById(R.id.end_date);
        endDate.setTag(R.id.type, END_DATE);
        endDate.setOnClickListener(this);
        endDate.setEnabled(false);
        endDateDatePicker = new AdvanceSearchDatePickerDialog(endDate);
        endDateDatePicker.setDateFormatter(dateFormatter);

        setUpQRCodeButton(view);

        setUpScanCardButton(view);

        setUpMyCatchmentControls(view, myCatchment, outsideInside, R.id.my_catchment_layout);

        active = view.findViewById(R.id.active);
        active.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (!isChecked && !inactive.isChecked() && !lostToFollowUp.isChecked()) {
                    active.setChecked(true);
                }
            }
        });
        inactive = view.findViewById(R.id.inactive);
        inactive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (!isChecked && !active.isChecked() && !lostToFollowUp.isChecked()) {
                    inactive.setChecked(true);
                }
            }
        });
        lostToFollowUp = view.findViewById(R.id.lost_to_follow_up);
        lostToFollowUp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (!isChecked && !active.isChecked() && !inactive.isChecked()) {
                    lostToFollowUp.setChecked(true);
                }
            }
        });

        final View activeLayout = view.findViewById(R.id.active_layout);
        activeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                active.toggle();
            }
        });

        final View inactiveLayout = view.findViewById(R.id.inactive_layout);
        inactiveLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inactive.toggle();
            }
        });

        final View lostToFollowUpLayout = view.findViewById(R.id.lost_to_follow_up_layout);
        lostToFollowUpLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lostToFollowUp.toggle();
            }
        });
    }

    private void setUpMyCatchmentControls(View view, final RadioButton myCatchment,
                                          final RadioButton outsideInside, int p) {
        myCatchment.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!Utils.isConnectedToNetwork(getActivity())) {
                    myCatchment.setChecked(true);
                    outsideInside.setChecked(false);
                } else {
                    outsideInside.setChecked(!isChecked);
                }
            }
        });

        View myCatchmentLayout = view.findViewById(p);
        myCatchmentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myCatchment.toggle();
            }
        });
    }

    private void setUpSearchButtons() {
        advancedSearchToolbarSearchButton.setEnabled(false);
        advancedSearchToolbarSearchButton.setTextColor(getResources().getColor(R.color.contact_complete_grey_border));
        advancedSearchToolbarSearchButton.setOnClickListener(registerActionHandler);

        searchButton.setEnabled(false);
        searchButton.setTextColor(getResources().getColor(R.color.contact_complete_grey_border));
        searchButton.setOnClickListener(registerActionHandler);
    }

    private void setUpQRCodeButton(View view) {
        qrCodeButton = view.findViewById(R.id.qrCodeButton);
        if (!ChildLibrary.getInstance().getProperties().hasProperty(ChildAppProperties.KEY.FEATURE_SCAN_QR_ENABLED) || ChildLibrary.getInstance().getProperties().getPropertyBoolean(ChildAppProperties.KEY.FEATURE_SCAN_QR_ENABLED)) {
            qrCodeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (getActivity() == null) {
                        return;
                    }
                    BaseRegisterActivity baseRegisterActivity = (BaseRegisterActivity) getActivity();
                    baseRegisterActivity.startQrCodeScanner();

                    ((BaseChildRegisterActivity) getActivity()).setAdvancedSearch(true);
                    ((BaseChildRegisterActivity) getActivity()).setAdvancedSearchFormData(createSelectedFieldMap());
                }
            });
        } else {
            qrCodeButton.setVisibility(View.GONE);
        }
    }

    private void setUpScanCardButton(View view) {
        Button scanCardButton = view.findViewById(R.id.scanCardButton);

        if (ChildLibrary.getInstance().getProperties().getPropertyBoolean(ChildAppProperties.KEY.FEATURE_NFC_CARD_ENABLED)) {
            scanCardButton.setVisibility(View.VISIBLE);//should be visible

            ((View) view.findViewById(R.id.card_id).getParent()).setVisibility(View.VISIBLE);
        }
    }

    public abstract void assignedValuesBeforeBarcode();

    protected abstract HashMap<String, String> createSelectedFieldMap();

    private void checkTextFields() {
        if (anySearchableFieldHasValue()) {
            advancedSearchToolbarSearchButton.setEnabled(true);
            advancedSearchToolbarSearchButton.setTextColor(getResources().getColor(R.color.white));

            searchButton.setEnabled(true);
            searchButton.setTextColor(getResources().getColor(R.color.white));
        } else {
            advancedSearchToolbarSearchButton.setEnabled(false);
            advancedSearchToolbarSearchButton.setTextColor(getResources().getColor(R.color.contact_complete_grey_border));

            searchButton.setEnabled(false);
            searchButton.setTextColor(getResources().getColor(R.color.contact_complete_grey_border));
        }
    }

    private boolean anySearchableFieldHasValue() {
        for (Map.Entry<String, View> entry : advancedFormSearchableFields.entrySet()) {
            if (entry.getValue() instanceof TextView && !TextUtils.isEmpty(((TextView) entry.getValue()).getText())) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void switchViews(boolean showList) {
        if (showList) {
            Utils.hideKeyboard(getActivity());

            advancedSearchForm.setVisibility(View.GONE);
            listViewLayout.setVisibility(View.VISIBLE);
            clientsView.setVisibility(View.VISIBLE);
            backButton.setVisibility(View.VISIBLE);
            searchButton.setVisibility(View.GONE);
            advancedSearchToolbarSearchButton.setVisibility(View.GONE);

            if (titleLabelView != null) {
                titleLabelView.setText(getString(R.string.search_results));
            }

            // hide result count , should be dynamic
            if (matchingResults != null) {
                matchingResults.setVisibility(View.GONE);
            }

            showProgressView();
            listMode = true;
        } else {
            clearSearchCriteria();
            advancedSearchForm.setVisibility(View.VISIBLE);
            listViewLayout.setVisibility(View.GONE);
            clientsView.setVisibility(View.INVISIBLE);
            backButton.setVisibility(View.GONE);
            searchButton.setVisibility(View.VISIBLE);
            advancedSearchToolbarSearchButton.setVisibility(View.VISIBLE);

            if (titleLabelView != null) {
                titleLabelView.setText(getString(R.string.advanced_search));
            }

            listMode = false;
        }
    }

    private void updateSearchLimits() {
        if (Utils.isConnectedToNetwork(getActivity())) {
            outsideInside.setChecked(true);
            myCatchment.setChecked(false);
        } else {
            myCatchment.setChecked(true);
            outsideInside.setChecked(false);
        }

        if (connectionChangeReciever == null) {
            connectionChangeReciever = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!Utils.isConnectedToNetwork(getActivity())) {
                        myCatchment.setChecked(true);
                        outsideInside.setChecked(false);
                    }
                }
            };

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            getActivity().registerReceiver(connectionChangeReciever, intentFilter);
            registeredConnectionChangeReceiver = true;
        }

    }

    private void resetForm() {
        clearSearchCriteria();
        clearMatchingResults();
        clearFormFields();
    }


    protected void clearFormFields() {
        active.setChecked(true);
        inactive.setChecked(false);
        lostToFollowUp.setChecked(false);
        startDate.setText("");
        endDate.setText("");
    }

    private void clearSearchCriteria() {
        if (searchCriteria != null) {
            searchCriteria.setVisibility(View.GONE);
            searchCriteria.setText("");
        }
    }

    private void clearMatchingResults() {
        if (matchingResults != null) {
            matchingResults.setVisibility(View.GONE);
            matchingResults.setText("");
        }
    }

    @Override
    public void updateMatchingResults(int count) {
        if (matchingResults != null) {
            matchingResults.setText(getString(R.string.matching_results, String.valueOf(count)));
            matchingResults.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void updateSearchCriteria(String searchCriteriaString) {
        if (searchCriteria != null) {
            searchCriteria.setText(Html.fromHtml(searchCriteriaString));
            searchCriteria.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setupSearchView(View view) {
        // TODO implement this
    }

    @Override
    protected SecuredNativeSmartRegisterActivity.DefaultOptionsProvider getDefaultOptionsProvider
            () {
        return null;
    }

    @Override
    protected abstract String getMainCondition();

    private void search() {
        if (myCatchment.isChecked()) {
            isLocal = true;
        } else if (outsideInside.isChecked()) {
            isLocal = false;
        }

        Map<String, String> editMap = getSearchMap(!isLocal);

        ((ChildAdvancedSearchContract.Presenter) presenter).search(editMap, isLocal);
    }

    protected abstract Map<String, String> getSearchMap(boolean outOfarea);

    @Override
    public void recalculatePagination(AdvancedMatrixCursor matrixCursor) {
        super.recalculatePagination(matrixCursor);
        updateMatchingResults(clientAdapter.getTotalcount());
    }

    @Override
    public void showNotFoundPopup(String opensrpID) {
        //Todo implement this
    }

    @Override
    public void setAdvancedSearchFormData(HashMap<String, String> advancedSearchFormData) {
        this.searchFormData = advancedSearchFormData;
    }

    @Override
    public void countExecute() {
        try {
            String sql = ((BaseChildAdvancedSearchPresenter) presenter).getCountQuery();
            Timber.i(sql);
            int totalCount = commonRepository().countSearchIds(sql);
            clientAdapter.setTotalcount(totalCount);
            Timber.i("Total Register Count %d", clientAdapter.getTotalcount());
            clientAdapter.setCurrentlimit(20);
            clientAdapter.setCurrentoffset(0);
        } catch (Exception e) {
            Timber.e(e);
        }

        updateMatchingResults(clientAdapter.getTotalcount());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_ID) {
            return new AdvanceSearchCursorLoader(this);
        }
        return new CursorLoader(getContext());
    }


    @Override
    public String filterAndSortQuery() {
        SmartRegisterQueryBuilder sqb = new SmartRegisterQueryBuilder(mainSelect);

        String query = "";
        try {
            sqb.addCondition(filters);
            query = sqb.orderbyCondition(Sortqueries);
            query = sqb.Endquery(
                    sqb.addlimitandOffset(query, clientAdapter.getCurrentlimit(), clientAdapter.getCurrentoffset()));
        } catch (Exception e) {
            Timber.e(e);
        }

        return query;
    }

    @Override
    public Cursor getRawCustomQueryForAdapter(String query) {
        return commonRepository().rawCustomQueryForAdapter(query);
    }

    @Override
    public void onResume() {
        super.onResume();
        assignedValuesBeforeBarcode();
    }

    private class AdvancedSearchTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            //Todo later
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            checkTextFields();
        }

        @Override
        public void afterTextChanged(Editable s) {
            checkTextFields();
        }
    }

    static class AdvanceSearchCursorLoader extends CursorLoader {

        private final BaseAdvancedSearchFragment advancedSearchFragment;

        public AdvanceSearchCursorLoader(BaseAdvancedSearchFragment advancedSearchFragment) {
            super(advancedSearchFragment.getActivity());
            this.advancedSearchFragment = advancedSearchFragment;
        }

        @Override
        public Cursor loadInBackground() {
            AdvancedMatrixCursor matrixCursor = ((BaseChildAdvancedSearchPresenter) advancedSearchFragment.presenter).getMatrixCursor();
            if (advancedSearchFragment.getActivity() != null && (advancedSearchFragment.isLocal || matrixCursor == null)) {
                String query = advancedSearchFragment.filterAndSortQuery();
                Cursor cursor = advancedSearchFragment.commonRepository().rawCustomQueryForAdapter(query);
                advancedSearchFragment.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        advancedSearchFragment.hideProgressView();
                    }
                });
                return cursor;
            } else {
                return matrixCursor;
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view instanceof EditText) {
            Calendar currentDate = Calendar.getInstance();
            final EditText editText = (EditText) view;
            String previousDateString = editText.getText().toString();
            if (StringUtils.isNoneBlank(previousDateString)) {
                try {
                    currentDate.setTime(dateFormatter.parse(previousDateString));
                } catch (ParseException e) {
                    Timber.e(e, "Error parsing Advance Search Date: %s", e.getMessage());
                }
            }

            if (editText.getTag(R.id.type).equals(START_DATE)) {
                startDateDatePicker.setCurrentDate(currentDate);
                startDateDatePicker.showDialog();
                endDate.setEnabled(true);
            } else if (editText.getTag(R.id.type).equals(END_DATE)) {
                endDateDatePicker.setCurrentDate(currentDate);
                endDateDatePicker.showDialog();
            }
        }
    }

}