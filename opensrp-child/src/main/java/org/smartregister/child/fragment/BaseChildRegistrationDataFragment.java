package org.smartregister.child.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.utils.NativeFormLangUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.smartregister.child.ChildLibrary;
import org.smartregister.child.R;
import org.smartregister.child.activity.BaseChildDetailTabbedActivity;
import org.smartregister.child.adapter.ChildRegistrationDataAdapter;
import org.smartregister.child.contract.IChildDetails;
import org.smartregister.child.domain.Field;
import org.smartregister.child.domain.Form;
import org.smartregister.child.domain.KeyValueItem;
import org.smartregister.child.util.ChildAppProperties;
import org.smartregister.child.util.ChildJsonFormUtils;
import org.smartregister.child.util.Constants;
import org.smartregister.child.util.Utils;
import org.smartregister.cloudant.models.Client;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.util.AssetHandler;
import org.smartregister.util.FormUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by ndegwamartin on 06/03/2019.
 */
public abstract class BaseChildRegistrationDataFragment extends Fragment {
    protected Map<String, String> childDetails;
    protected View fragmentView;
    protected Map<String, String> fieldNameAliasMap;
    protected Map<String, Integer> fieldNameResourceMap = new HashMap<>();
    private ChildRegistrationDataAdapter mAdapter;
    private List<Field> fields;
    private Map<String, String> stringResourceIds;
    private List<String> unformattedNumberFields;
    private List<KeyValueItem> detailsList;

    public ChildRegistrationDataAdapter getmAdapter() {
        return mAdapter;
    }

    public void setmAdapter(ChildRegistrationDataAdapter mAdapter) {
        this.mAdapter = mAdapter;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Form form = getForm();
        setFields(form.getStep1().getFields());
        unformattedNumberFields = addUnFormattedNumberFields("");
        stringResourceIds = getDataRowLabelResourceIds();
        fieldNameAliasMap = new HashMap<>(); // some fields columns are named differently in database
    }

    /**
     * The map is such that key is the key defined in the registration form json while the value is the strings resource id
     * e.g. Key "First_Name" and Value "R.string.first_name"
     * <p>
     * At runtime, the correct language string will be loaded
     * <p>
     * Values will only show up if you add them here.
     * <p>
     * You can also replace the labels for instance when you want to use a different name for the label or
     * when you want to shorten the label in the json. To do this put the field key against the new String resource
     * id in the fieldNameResourceMap. This will be used instead (This is given precedence over the name of
     * the label as defined in the json)
     */

    protected Map<String, String> getDataRowLabelResourceIds() {

        Map<String, String> resourceIds = new HashMap<>();

        for (Field field : fields) {
            String fieldValue = fieldNameResourceMap.containsKey(field.getKey()) ?
                    getString(fieldNameResourceMap.get(field.getKey())) : field.getHint();
            if (fieldValue != null && !fieldValue.isEmpty()) {
                resourceIds.put(field.getKey(), fieldValue);
            }
        }
        return resourceIds;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() instanceof IChildDetails) {
            childDetails = ((IChildDetails) getActivity()).getChildDetails().getColumnmaps();
        }
        // Inflate the layout for this fragment
        fragmentView = inflater.inflate(R.layout.child_registration_data_fragment, container, false);

        Utils.refreshDataCaptureStrategyBanner(this.getActivity(), ((BaseChildDetailTabbedActivity) this.getActivity()).getOpenSRPContext().allSharedPreferences().fetchCurrentLocality());

        return fragmentView;
    }

    protected Form getForm() {
        try {
            if (getActivity() != null) {
                String jsonString = new FormUtils(getActivity()).getFormJson(getRegistrationForm()).toString();
                boolean useNewMLSApproach = Boolean.parseBoolean(ChildLibrary.getInstance().getProperties()
                        .getProperty(ChildAppProperties.KEY.MULTI_LANGUAGE_SUPPORT, "false"));
                if (useNewMLSApproach) {
                    jsonString = NativeFormLangUtils.getTranslatedString(jsonString, getContext());
                }
                return AssetHandler.jsonStringToJava(jsonString, Form.class);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        return null;
    }

    protected abstract String getRegistrationForm();

    public void updateChildDetails(Map<String, String> childDetails) {
        this.childDetails = childDetails;
    }

    public void loadData(Map<String, String> detailsMap) {
        RecyclerView mRecyclerView1 = getActivity().findViewById(R.id.recyclerView);
        resetAdapterData(detailsMap);
        mRecyclerView1.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView1.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView1.setAdapter(mAdapter);
    }

    public void resetAdapterData(Map<String, String> detailsMap) {
        detailsList = new ArrayList<>();
        String key;
        String value;

        for (int i = 0; i < getFields().size(); i++) {
            Field field = getFields().get(i);
            key = field.getKey();

            //Some fields have alias name on query
            if (fieldNameAliasMap.containsKey(key)) {
                String keyAlias = fieldNameAliasMap.get(key);
                value = getFieldValue(detailsMap, field, keyAlias);
            } else {
                value = getFieldValue(detailsMap, field, key);
            }

            //TODO Temporary fix for spinner setting value as hint when nothing is selected
            if (field.getType().equalsIgnoreCase(JsonFormConstants.SPINNER) && field.getHint().equalsIgnoreCase(value)) {
                value = null;
            }

            String label = getResourceLabel(key);

            if (!TextUtils.isEmpty(value) && !TextUtils.isEmpty(label) && !skipField(field.getKey(), value)) {
                detailsList.add(new KeyValueItem(label, cleanValue(field, value)));
            }
        }
        setmAdapter(new ChildRegistrationDataAdapter(detailsList));
    }

    /**
     * This method is used to determine whether a field should be skipped or not.
     * Useful in specifiy other fields e.g. father_nationality = value of father_nationality_other
     *
     * @param fieldKey field name
     * @param value    value of the field
     * @return true if field is skippable false otherwise
     */
    private boolean skipField(String fieldKey, String value) {
        List<String> suffixes = Collections.singletonList("_other");
        for (String suffix : suffixes) {
            if (fieldKey.endsWith(suffix)) {
                updateOtherField(fieldKey.substring(0, fieldKey.indexOf(suffix)), value);
                return true;
            }
        }
        return false;
    }

    private void updateOtherField(String actualField, String value) {
        String fieldLabel = getDataRowLabelResourceIds().get(actualField);
        for (KeyValueItem keyValueItem : detailsList) {
            if (keyValueItem.getKey().equalsIgnoreCase(fieldLabel)) {
                keyValueItem.setValue(value);
                return;
            }
        }
    }

    private String getFieldValue(Map<String, String> detailsMap, Field field, String key) {
        String value;
        value = detailsMap.get(field.getKey().toLowerCase(Locale.getDefault()));
        value = !TextUtils.isEmpty(value) ? value : detailsMap.get(getPrefix(field.getEntityId()) + key.toLowerCase(Locale.getDefault()));
        value = !TextUtils.isEmpty(value) ? value : detailsMap.get(getPrefix(field.getEntityId()) + cleanOpenMRSEntityId(field.getOpenmrsEntityId().toLowerCase(Locale.getDefault())));
        value = !TextUtils.isEmpty(value) ? value : detailsMap.get(key.toLowerCase(Locale.getDefault()));
        return value;
    }

    public String getPrefix(String entityId) {
        if (!TextUtils.isEmpty(entityId) && entityId.equalsIgnoreCase(Constants.KEY.MOTHER))
            return "mother_";
        else if (!TextUtils.isEmpty(entityId) && entityId.equalsIgnoreCase(Constants.KEY.FATHER))
            return "father_";
        else return "";
    }

    public String cleanOpenMRSEntityId(String rawEntityId) {
        return Client.birth_date_key.equals(rawEntityId) ? Constants.KEY.DOB : rawEntityId;
    }

    public String getResourceLabel(String raw) {
        String label = null;
        if (stringResourceIds != null && stringResourceIds.size() > 0) {
            label = stringResourceIds.get(raw);
        }
        return label;
    }

    public String cleanValue(Field field, String raw) {
        String result = raw;
        String type = field.getType();

        switch (type) {
            case JsonFormConstants.DATE_PICKER:
                Date date = ChildJsonFormUtils.formatDate(raw.contains("T") ? raw.substring(0, raw.indexOf('T')) : raw, false);
                if (date != null) {
                    result = new SimpleDateFormat(com.vijay.jsonwizard.utils.FormUtils.NATIIVE_FORM_DATE_FORMAT_PATTERN,
                            Locale.getDefault().toString().startsWith("ar") ? Locale.ENGLISH : Locale.getDefault()).format(date);
                }
                break;
            case JsonFormConstants.SPINNER:
                boolean useNewMLSApproach = Boolean.parseBoolean(ChildLibrary.getInstance().getProperties()
                        .getProperty(ChildAppProperties.KEY.MULTI_LANGUAGE_SUPPORT, "false"));
                if (useNewMLSApproach && !field.getOptions().isEmpty() && StringUtils.isNotBlank(raw)) {
                    for (Map<String, String> option : field.getOptions()){
                        if (option.containsKey(JsonFormConstants.KEY) && raw.equalsIgnoreCase(option.get(JsonFormConstants.KEY))) {
                            result = option.get(JsonFormConstants.TEXT);
                            break;
                        }
                    }
                }
                if (field.getKeys() != null && field.getKeys().size() > 0 && field.getKeys().contains(raw)) {
                    result = field.getValues().get(field.getKeys().indexOf(raw));
                } else if (field.getSubType() != null && field.getSubType().equalsIgnoreCase(Constants.JSON_FORM_KEY.LOCATION_SUB_TYPE)) {
                    result = ChildLibrary.getInstance().getLocationRepository().getLocationById(raw).getProperties().getName();
                }
                break;
            case JsonFormConstants.TREE:
                result = LocationHelper.getInstance()
                        .getOpenMrsReadableName(LocationHelper.getInstance().getOpenMrsLocationName(raw));
                break;
            default:
                break;
        }

        if (unformattedNumberFields.contains(field.getKey())) {
            result = result.trim();
        } else {
            result = cleanResult(result.trim());
        }

        return formatRenderValue(field, result);
    }

    protected String formatRenderValue(Field field, String value) {

        String renderType = StringUtils.isNotBlank(field.getRenderType()) ? field.getRenderType().toLowerCase() : "";
        String result;

        if ("id".equals(renderType)) {
            result = Utils.formatIdentifiers(value);
        } else {
            result = value;
        }

        return result;
    }

    private String cleanResult(String result) {
        if (NumberUtils.isDigits(result)) {
            return Utils.formatNumber(result);
        } else {
            return result;
        }
    }

    /**
     * Add number fields that should not be formatted by the number format e.g mothers phone number
     ***/
    protected List<String> addUnFormattedNumberFields(String... key) {
        unformattedNumberFields = new ArrayList<>();
        unformattedNumberFields.addAll(Arrays.asList(key));
        return unformattedNumberFields;
    }

    public void refreshRecyclerViewData(Map<String, String> detailsMap) {
        resetAdapterData(detailsMap);
        mAdapter.notifyDataSetChanged();
    }
}
