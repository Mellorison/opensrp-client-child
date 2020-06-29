package org.smartregister.child.fragment;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.CoreLibrary;
import org.smartregister.child.ChildLibrary;
import org.smartregister.child.R;
import org.smartregister.child.activity.BaseChildRegisterActivity;
import org.smartregister.child.contract.ChildRegisterFragmentContract;
import org.smartregister.child.cursor.AdvancedMatrixCursor;
import org.smartregister.child.domain.RepositoryHolder;
import org.smartregister.child.presenter.BaseChildRegisterFragmentPresenter;
import org.smartregister.child.provider.ChildRegisterProvider;
import org.smartregister.child.util.ChildAppProperties;
import org.smartregister.child.util.Constants;
import org.smartregister.child.util.Utils;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.domain.FetchStatus;
import org.smartregister.growthmonitoring.GrowthMonitoringLibrary;
import org.smartregister.immunization.ImmunizationLibrary;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.view.LocationPickerView;
import org.smartregister.view.activity.BaseRegisterActivity;
import org.smartregister.view.customcontrols.CustomFontTextView;
import org.smartregister.view.fragment.BaseRegisterFragment;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import timber.log.Timber;

/**
 * Created by ndegwamartin on 25/02/2019.
 */
public abstract class BaseChildRegisterFragment extends BaseRegisterFragment
        implements ChildRegisterFragmentContract.View, SyncStatusBroadcastReceiver.SyncStatusListener, View.OnClickListener, LocationPickerView.OnLocationChangeListener {

    private View filterSection;
    protected LocationPickerView clinicSelection;
    private TextView overdueCountTV;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        this.getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        View view = inflater.inflate(R.layout.smart_register_activity_customized, container, false);
        mView = view;
        onInitialization();
        setupViews(view);
        onResumption();

        return view;
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
    }

    @Override
    public void setUniqueID(String s) {
        if (getSearchView() != null) {
            getSearchView().setText(s);
        }
    }

    @Override
    public void setAdvancedSearchFormData(HashMap<String, String> advancedSearchFormData) {
        //do nothing , overrode from ma
    }

    @Override
    public void setupViews(View view) {
        super.setupViews(view);

        CustomFontTextView buttonReportMonth = view.findViewById(R.id.btn_report_month);

        if (buttonReportMonth != null) {
            buttonReportMonth.setVisibility(View.INVISIBLE);
            view.findViewById(R.id.service_mode_selection).setVisibility(View.INVISIBLE);

            // Update top right icon
            setUpQRCodeScanButtonView(view);
            setUpScanCardButtonView(view);

            //OpenSRPLogo
            setUpOpenSRPLogoImageView(view, R.id.opensrp_logo_image_view);

            // Update title name
            setUpOpenSRPTitleView(view);

            filterSection = view.findViewById(R.id.filter_selection);
            filterSection.setOnClickListener(this);

            clinicSelection = view.findViewById(R.id.clinic_selection);
            clinicSelection.setOnLocationChangeListener(this);
            clinicSelection.init();

            clientsView.setVisibility(View.VISIBLE);
            clientsProgressView.setVisibility(View.INVISIBLE);
            setServiceModeViewDrawableRight(null);

            TextView nameInitials = view.findViewById(R.id.name_inits);
            nameInitials.setVisibility(View.GONE);
            LinearLayout btnBackToHome = view.findViewById(R.id.btn_back_to_home);
            btnBackToHome.setOnClickListener(this);
            syncButton = view.findViewById(R.id.back_button);
            syncButton.setVisibility(View.VISIBLE);
            syncButton.setImageResource(R.drawable.ic_action_menu);
            syncButton.setOnClickListener(this);


            View globalSearchButton = view.findViewById(R.id.global_search);
            globalSearchButton.setOnClickListener(this);

            overdueCountTV = view.findViewById(R.id.filter_count);
            overdueCountTV.setVisibility(View.GONE);
        }

        if (!(this instanceof BaseAdvancedSearchFragment)) {
            view.findViewById(R.id.child_next_appointment_header_wrapper).setVisibility(
                    ChildLibrary.getInstance().getProperties()
                            .getPropertyBoolean(ChildAppProperties.KEY.HOME_NEXT_VISIT_DATE_ENABLED) ? View.VISIBLE : View.GONE);


            if (ChildLibrary.getInstance().getProperties().hasProperty(ChildAppProperties.KEY.HOME_RECORD_WEIGHT_ENABLED)) {
                view.findViewById(R.id.child_weight_header_wrapper).setVisibility(ChildLibrary.getInstance().getProperties()
                        .getPropertyBoolean(ChildAppProperties.KEY.HOME_RECORD_WEIGHT_ENABLED) ? View.VISIBLE : View.GONE);
            }
        }
    }

    @Override
    protected void onResumption() {
        super.onResumption();

        if (filterMode()) {
            toggleFilterSelection();
        }

        updateSearchView();
        updateLocationText();
        if (getActivity() != null)
            Utils.refreshDataCaptureStrategyBanner(getActivity(), ((BaseChildRegisterActivity) this.getActivity()).getOpenSRPContext().allSharedPreferences().fetchCurrentLocality());
    }

    @Override
    protected abstract String getMainCondition();

    @Override
    protected abstract String getDefaultSortQuery();

    @Override
    protected void startRegistration() {
        ((BaseChildRegisterActivity) getActivity()).startRegistration();
    }

    @Override
    protected void onViewClicked(View view) {
        if (getActivity() == null) {
            return;
        }
    }

    @Override
    public void onSyncInProgress(FetchStatus fetchStatus) {
        // do we need to post progress?
    }

    @Override
    public void onSyncComplete(FetchStatus fetchStatus) {
        super.onSyncComplete(fetchStatus);
    }

    private void setUpQRCodeScanButtonView(View view) {
        FrameLayout qrCodeScanImageView = view.findViewById(R.id.scan_qr_code);
        if (qrCodeScanImageView != null) {

            if (ChildLibrary.getInstance().getProperties().getPropertyBoolean(ChildAppProperties.KEY.FEATURE_SCAN_QR_ENABLED) &&
                    ChildLibrary.getInstance().getProperties()
                            .getPropertyBoolean(ChildAppProperties.KEY.HOME_TOOLBAR_SCAN_QR_ENABLED)) {
                qrCodeScanImageView.setOnClickListener(this);
            } else {
                qrCodeScanImageView.setVisibility(View.GONE);
            }
        }
    }

    private void setUpScanCardButtonView(View view) {
        FrameLayout scanCardView = view.findViewById(R.id.scan_card);
        if (scanCardView != null &&
                ChildLibrary.getInstance().getProperties().getPropertyBoolean(ChildAppProperties.KEY.FEATURE_NFC_CARD_ENABLED) &&
                ChildLibrary.getInstance().getProperties()
                        .getPropertyBoolean(ChildAppProperties.KEY.HOME_TOOLBAR_SCAN_CARD_ENABLED)) {
            scanCardView.setOnClickListener(this);
            scanCardView.setVisibility(View.VISIBLE);

        }
    }

    private void setUpOpenSRPLogoImageView(View view, int p) {
        ImageView logo = view.findViewById(p);
        if (logo != null) {
            logo.setVisibility(View.GONE);
        }
    }

    private void setUpOpenSRPTitleView(View view) {
        CustomFontTextView titleView = view.findViewById(R.id.txt_title_label);
        if (titleView != null) {
            titleView.setVisibility(View.GONE);
        }
    }

    private boolean filterMode() {
        return filterSection != null && filterSection.getTag() != null;
    }

    protected void toggleFilterSelection() {
        if (filterSection != null) {
            String tagString = "PRESSED";
            if (filterSection.getTag() == null) {
                filter("", "", filterSelectionCondition(false), false);
                filterSection.setTag(tagString);
                filterSection.setBackgroundResource(R.drawable.transparent_clicked_background);
            } else if (filterSection.getTag().toString().equals(tagString)) {
                filter("", "", "", false);
                filterSection.setTag(null);
                filterSection.setBackgroundResource(R.drawable.transparent_gray_background);
            }
        }
    }

    protected void updateLocationText() {
        try {
            if (clinicSelection != null) {
                clinicSelection.setText(LocationHelper.getInstance().getOpenMrsReadableName(clinicSelection.getSelectedItem()));
                String locationId = LocationHelper.getInstance().getOpenMrsLocationId(clinicSelection.getSelectedItem());
                context().allSharedPreferences().savePreference(Constants.CURRENT_LOCATION_ID, locationId);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    protected abstract String filterSelectionCondition(boolean urgentOnly);

    public void updateDueOverdueCountText(int overDueCount) {
        if (overdueCountTV != null) {
            if (overDueCount > 0) {
                overdueCountTV.setText(String.valueOf(overDueCount));
                overdueCountTV.setVisibility(View.VISIBLE);
                overdueCountTV.setClickable(true);
            } else {
                overdueCountTV.setVisibility(View.GONE);
                overdueCountTV.setClickable(false);
            }
        } else {
            Timber.e("Over Due Count Text View (overdueCountTV) is NULL ...whyyy?");
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void showNotFoundPopup(String opensrpID) {
        NoMatchDialogFragment
                .launchDialog((BaseRegisterActivity) Objects.requireNonNull(getActivity()), DIALOG_TAG, opensrpID);
    }

    @Override
    public void initializeAdapter(Set<org.smartregister.configurableviews.model.View> visibleColumns) {
        RepositoryHolder repositoryHolder = new RepositoryHolder();
        repositoryHolder.setCommonRepository(commonRepository());
        repositoryHolder.setVaccineRepository(ImmunizationLibrary.getInstance().vaccineRepository());
        repositoryHolder.setWeightRepository(GrowthMonitoringLibrary.getInstance().weightRepository());

        if (CoreLibrary.getInstance().context().getAppProperties().isTrue(ChildAppProperties.KEY.MONITOR_HEIGHT)) {
            repositoryHolder.setHeightRepository(GrowthMonitoringLibrary.getInstance().heightRepository());
        }

        ChildRegisterProvider childRegisterProvider =
                new ChildRegisterProvider(getActivity(), repositoryHolder, visibleColumns, registerActionHandler,
                        paginationViewHandler, context().alertService());
        clientAdapter = new RecyclerViewPaginatedAdapter(null, childRegisterProvider, context()
                .commonrepository(this.tablename));
        clientAdapter.setCurrentlimit(20);
        clientsView.setAdapter(clientAdapter);
    }

    @Override
    public void recalculatePagination(AdvancedMatrixCursor matrixCursor) {
        clientAdapter.setTotalcount(matrixCursor.getCount());
        Timber.v("Total count here%s", clientAdapter.getTotalcount());
        clientAdapter.setCurrentlimit(20);
        if (clientAdapter.getTotalcount() > 0) {
            clientAdapter.setCurrentlimit(clientAdapter.getTotalcount());
        }
        clientAdapter.setCurrentoffset(0);
    }

    @Override
    public ChildRegisterFragmentContract.Presenter presenter() {
        return (ChildRegisterFragmentContract.Presenter) presenter;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final AdvancedMatrixCursor matrixCursor = ((BaseChildRegisterFragmentPresenter) presenter).getMatrixCursor();
        if (!globalQrSearch || matrixCursor == null) {
            if (id == LOADER_ID) {
                return new CursorLoader(getActivity()) {
                    @Override
                    public Cursor loadInBackground() {
                        String query = filterAndSortQuery();
                        return commonRepository().rawCustomQueryForAdapter(query);
                    }
                };
            } else {
                return new CursorLoader(getContext());
            }
        } else {
            globalQrSearch = false;
            if (id == LOADER_ID) {// Returns a new CursorLoader
                return new CursorLoader(getActivity()) {
                    @Override
                    public Cursor loadInBackground() {
                        return matrixCursor;
                    }
                };
            }// An invalid id was passed in
            return new CursorLoader(getContext());
        }
    }


    private String filterAndSortQuery() {
        SmartRegisterQueryBuilder sqb = new SmartRegisterQueryBuilder(mainSelect);
        String query = "";
        try {
            if (isValidFilterForFts(commonRepository())) {
                String sql = Utils.metadata().getRegisterQueryProvider().getObjectIdsQuery(mainCondition, filters) + (StringUtils.isBlank(getDefaultSortQuery()) ? "" : " order by " + getDefaultSortQuery());

                sql = sqb.addlimitandOffset(sql, clientAdapter.getCurrentlimit(), clientAdapter.getCurrentoffset());

                List<String> ids = commonRepository().findSearchIds(sql);
                query = Utils.metadata().getRegisterQueryProvider().mainRegisterQuery() + " where _id IN (%s)" + (StringUtils.isBlank(getDefaultSortQuery()) ? "" : " order by " + getDefaultSortQuery());

                String joinedIds = "'" + StringUtils.join(ids, "','") + "'";
                return query.replace("%s", joinedIds);
            } else {
                if (!TextUtils.isEmpty(filters) && TextUtils.isEmpty(Sortqueries)) {
                    sqb.addCondition(filters);
                    query = sqb.orderbyCondition(Sortqueries);
                    query = sqb.Endquery(sqb.addlimitandOffset(query
                            , clientAdapter.getCurrentlimit()
                            , clientAdapter.getCurrentoffset()));
                }
                return query;
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return query;
    }

    @Override
    public void countExecute() {
        try {
            String sql = Utils.metadata().getRegisterQueryProvider().getCountExecuteQuery(mainCondition, filters);
            Timber.i(sql);
            int totalCount = commonRepository().countSearchIds(sql);
            clientAdapter.setTotalcount(totalCount);
            Timber.i("Total Register Count %d", clientAdapter.getTotalcount());
            clientAdapter.setCurrentlimit(20);
            clientAdapter.setCurrentoffset(0);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void onLocationChange(final String newLocation) {
        Utils.refreshDataCaptureStrategyBanner(getActivity(), newLocation);
    }

}