package org.smartregister.child.fragment;

import android.os.Bundle;
import androidx.core.app.FragmentActivity;
import androidx.appcompat.widget.DefaultItemAnimator;
import androidx.appcompat.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.child.ChildLibrary;
import org.smartregister.child.R;
import org.smartregister.child.activity.BaseChildDetailTabbedActivity;
import org.smartregister.child.adapter.ChildRegistrationDataAdapter;
import org.smartregister.child.domain.Field;
import org.smartregister.child.domain.Form;
import org.smartregister.child.domain.Step;
import org.smartregister.child.util.ChildAppProperties;
import org.smartregister.child.util.Constants;
import org.smartregister.cloudant.models.Client;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Location;
import org.smartregister.domain.LocationProperty;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.LocationRepository;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ndegwamartin on 04/08/2020.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CoreLibrary.class, ChildLibrary.class})
public class BaseChildRegistrationDataFragmentTest {

    private BaseChildRegistrationDataFragment baseChildRegistrationDataFragment;

    @Mock
    private Bundle bundle;

    @Mock
    private Form form;

    @Mock
    private Step step;

    @Mock
    private LayoutInflater inflater;

    @Mock
    private ViewGroup container;

    @Mock
    private View fragmentView;

    @Mock
    private FragmentActivity activity;

    @Mock
    private RecyclerView recyclerView;

    @Mock
    private ChildRegistrationDataAdapter adapter;

    @Mock
    private Map<String, String> childDetais;

    @Mock
    private BaseChildDetailTabbedActivity baseChildDetailTabbedActivity;

    @Mock
    private Context opensrpContext;

    @Mock
    private AllSharedPreferences allSharedPreferences;

    @Mock
    private ChildLibrary childLibrary;

    @Mock
    private LocationHelper locationHelper;

    @Mock
    private LocationRepository locationRepository;

    private List<Field> fields;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        baseChildRegistrationDataFragment = Mockito.spy(BaseChildRegistrationDataFragment.class);
        Mockito.doReturn(form).when(baseChildRegistrationDataFragment).getForm();
        Mockito.doReturn(step).when(form).getStep1();
        fields = generateFormFieldsForTest();
        List<String> unformattedNumberFields = new ArrayList<>();

        Whitebox.setInternalState(baseChildRegistrationDataFragment, "fields", fields);
        Whitebox.setInternalState(baseChildRegistrationDataFragment, "mAdapter", adapter);
        Whitebox.setInternalState(baseChildRegistrationDataFragment, "unformattedNumberFields", unformattedNumberFields);
        Mockito.doReturn(fields).when(step).getFields();

        PowerMockito.mockStatic(ChildLibrary.class);
        PowerMockito.when(ChildLibrary.getInstance()).thenReturn(childLibrary);
        ChildAppProperties appProperties = new ChildAppProperties();
        PowerMockito.doReturn(appProperties).when(childLibrary).getProperties();
    }

    @Test
    public void testAddUnformattedKeys() {
        Assert.assertEquals(baseChildRegistrationDataFragment.addUnFormattedNumberFields(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()).size(), 2);
    }

    @Test
    public void testCleanResultShouldReturnFormattedStringIfInputIsANumber() throws Exception {
        Method method = BaseChildRegistrationDataFragment.class.getDeclaredMethod("cleanResult", String.class);
        method.setAccessible(true);

        String inputString = "12345679";
        Assert.assertEquals("12345679", method.invoke(baseChildRegistrationDataFragment, inputString));
    }

    @Test
    public void testCleanResultShouldReturnSameStringIfInputIsNotANumber() throws Exception {
        Method method = BaseChildRegistrationDataFragment.class.getDeclaredMethod("cleanResult", String.class);
        method.setAccessible(true);

        String inputString = "samplestring";
        Assert.assertEquals("samplestring", method.invoke(baseChildRegistrationDataFragment, inputString));
    }

    @Test
    public void testOnCreateInitsCorrectly() {

        Mockito.doReturn(form).when(baseChildRegistrationDataFragment).getForm();
        baseChildRegistrationDataFragment.onCreate(bundle);

        Mockito.verify(baseChildRegistrationDataFragment).getForm();
        Mockito.verify(baseChildRegistrationDataFragment).setFields(fields);
        Mockito.verify(baseChildRegistrationDataFragment).addUnFormattedNumberFields("");
        Mockito.verify(baseChildRegistrationDataFragment).setFields(fields);
    }

    @Test
    public void testGetDataRowLabelResourceIdsReturnsCorrectValues() {
        Map<String, String> rowLabelResourceIds = baseChildRegistrationDataFragment.getDataRowLabelResourceIds();
        Assert.assertNotNull(rowLabelResourceIds);
        Assert.assertEquals("Hint A", rowLabelResourceIds.get("key1"));
        Assert.assertEquals("Hint B", rowLabelResourceIds.get("key2"));
    }

    @Test
    @Ignore("Fix powermock robolectric conflicts first")
    public void onCreateViewInflatesCorrectView() {

        Mockito.doReturn(fragmentView).when(inflater).inflate(R.layout.child_registration_data_fragment, container, false);
        Mockito.doReturn(baseChildDetailTabbedActivity).when(baseChildRegistrationDataFragment).getActivity();

        String baseEntityId = "6434-343-343434-3434";

        Map<String, String> details = new HashMap<>();
        details.put(Constants.KEY.FIRST_NAME, "John");
        details.put(Constants.KEY.LAST_NAME, "Doe");
        details.put(Constants.KEY.ZEIR_ID, "2120");
        details.put(Constants.KEY.DOB, "2020-09-09");

        details.put(Constants.KEY.MOTHER_FIRST_NAME, "Jane");
        details.put(Constants.KEY.MOTHER_LAST_NAME, "Doe");
        details.put(Constants.KEY.BASE_ENTITY_ID, baseEntityId);


        CommonPersonObjectClient client = new CommonPersonObjectClient(baseEntityId, details, "John Doe");
        client.setColumnmaps(details);

        Mockito.doReturn(client).when(baseChildDetailTabbedActivity).getChildDetails();

        Mockito.doReturn(opensrpContext).when(baseChildDetailTabbedActivity).getOpenSRPContext();
        Mockito.doReturn(allSharedPreferences).when(opensrpContext).allSharedPreferences();
        Mockito.doReturn(Constants.DATA_CAPTURE_STRATEGY.ADVANCED).when(allSharedPreferences).fetchCurrentLocality();

        baseChildRegistrationDataFragment.onCreateView(inflater, container, bundle);

        ArgumentCaptor<Boolean> attachToRootCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<ViewGroup> parentContainerCaptor = ArgumentCaptor.forClass(ViewGroup.class);
        ArgumentCaptor<Integer> layoutIdentifierCaptor = ArgumentCaptor.forClass(Integer.class);

        Mockito.verify(inflater).inflate(layoutIdentifierCaptor.capture(), parentContainerCaptor.capture(), attachToRootCaptor.capture());
        Assert.assertEquals((Integer) R.layout.child_registration_data_fragment, layoutIdentifierCaptor.getValue());
        Assert.assertEquals(false, attachToRootCaptor.getValue());
        Assert.assertEquals(container, parentContainerCaptor.getValue());
    }

    @Test
    @Ignore("Fix powermock robolectric conflicts first")
    public void testLoadDataSetsAdapterWithCorrectly() {

        Mockito.doReturn(activity).when(baseChildRegistrationDataFragment).getActivity();
        Mockito.doReturn(recyclerView).when(activity).findViewById(R.id.recyclerView);
        Mockito.doNothing().when(baseChildRegistrationDataFragment).resetAdapterData(childDetais);

        baseChildRegistrationDataFragment.loadData(childDetais);

        Mockito.verify(baseChildRegistrationDataFragment).resetAdapterData(childDetais);
    }

    @Test
    @Ignore("Fix powermock robolectric conflicts first")
    public void testLoadDataInitsRecyclerViewCorrectly() {

        Mockito.doNothing().when(baseChildRegistrationDataFragment).resetAdapterData(childDetais);
        Mockito.doReturn(recyclerView).when(activity).findViewById(R.id.recyclerView);
        Mockito.doReturn(activity).when(baseChildRegistrationDataFragment).getActivity();

        baseChildRegistrationDataFragment.loadData(childDetais);

        Mockito.verify(recyclerView).setLayoutManager(ArgumentMatchers.any(RecyclerView.LayoutManager.class));
        Mockito.verify(recyclerView).setItemAnimator(ArgumentMatchers.any(DefaultItemAnimator.class));
        Mockito.verify(recyclerView).setAdapter(adapter);
    }

    @Test
    public void testCleanOpenMRSEntityIdReturnsCorrectDOBValue() {

        String cleanId = baseChildRegistrationDataFragment.cleanOpenMRSEntityId(Client.birth_date_key);
        Assert.assertEquals("dob", cleanId);
    }

    @Test
    public void testCleanOpenMRSEntityIdReturnsCorrectNonDOBValue() {

        String openmrsId = "random_entity_id";
        String cleanId = baseChildRegistrationDataFragment.cleanOpenMRSEntityId(openmrsId);
        Assert.assertEquals(openmrsId, cleanId);
    }

    @Test
    public void testGetResourceLabelReturnsCorrectValuesForKey() {

        Whitebox.setInternalState(baseChildRegistrationDataFragment, "stringResourceIds", baseChildRegistrationDataFragment.getDataRowLabelResourceIds());

        Mockito.doReturn(fragmentView).when(inflater).inflate(R.layout.child_registration_data_fragment, container, false);

        String resourceLabel = baseChildRegistrationDataFragment.getResourceLabel("key1");
        Assert.assertNotNull(resourceLabel);
        Assert.assertEquals("Hint A", resourceLabel);

        resourceLabel = baseChildRegistrationDataFragment.getResourceLabel("key2");
        Assert.assertNotNull(resourceLabel);
        Assert.assertEquals("Hint B", resourceLabel);

    }

    @Test
    public void testAddUnFormattedNumberFieldsCreatesListWithCorrectValues() {

        List<String> fields = baseChildRegistrationDataFragment.addUnFormattedNumberFields("FieldA", "Field B");

        Assert.assertNotNull(fields);
        Assert.assertEquals(2, fields.size());
        Assert.assertEquals("FieldA", fields.get(0));
        Assert.assertEquals("Field B", fields.get(1));
    }

    @Test
    @Ignore("Fix powermock robolectric conflicts first")
    public void testRefreshRecyclerViewDataResetsAdapter() {
        Mockito.doNothing().when(baseChildRegistrationDataFragment).resetAdapterData(childDetais);

        baseChildRegistrationDataFragment.refreshRecyclerViewData(childDetais);

        Mockito.verify(baseChildRegistrationDataFragment).resetAdapterData(childDetais);
        Mockito.verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void testCleanValueReturnsFormattedDateForDatePicker() {
        String rawValue = "2020-08-28";

        String value = baseChildRegistrationDataFragment.cleanValue(fields.get(3), rawValue);
        Mockito.verify(baseChildRegistrationDataFragment).formatRenderValue(ArgumentMatchers.any(Field.class), ArgumentMatchers.any(String.class));
        Assert.assertEquals("28-08-2020", value);
    }

    @Test
    public void testCleanValueRetrievesValueFromKeysListForOtherSpinner() {
        String rawValue = "m";

        String value = baseChildRegistrationDataFragment.cleanValue(fields.get(4), rawValue);
        Mockito.verify(baseChildRegistrationDataFragment).formatRenderValue(ArgumentMatchers.any(Field.class), ArgumentMatchers.any(String.class));
        Assert.assertEquals("Male", value);
    }

    @Test
    public void testCleanValueRetrievesDatabaseValueForSpinnerSubtypeLocation() {
        String rawValue = "";

        Location location = new Location();
        location.setId("123");
        LocationProperty property = new LocationProperty();
        property.setName("Location1");
        location.setProperties(property);

        ReflectionHelpers.setStaticField(ChildLibrary.class, "instance", childLibrary);
        Mockito.when(childLibrary.getLocationRepository()).thenReturn(locationRepository);
        Mockito.when(locationRepository.getLocationById("")).thenReturn(location);

        String value = baseChildRegistrationDataFragment.cleanValue(fields.get(5), rawValue);
        Mockito.verify(baseChildRegistrationDataFragment).formatRenderValue(ArgumentMatchers.any(Field.class), ArgumentMatchers.any(String.class));
        Assert.assertEquals("Location1", value);
    }

    @Test
    public void testCleanValueRetrievesOpenMrsLocationNameForTree() {
        String rawValue = "";
        String locationName = "YAO";
        String readableName = "Yaounde";

        ReflectionHelpers.setStaticField(LocationHelper.class, "instance", locationHelper);
        Mockito.when(locationHelper.getOpenMrsLocationName(rawValue)).thenReturn(locationName);
        Mockito.when(locationHelper.getOpenMrsReadableName(locationName)).thenReturn(readableName);

        String value = baseChildRegistrationDataFragment.cleanValue(fields.get(6), rawValue);
        Mockito.verify(baseChildRegistrationDataFragment).formatRenderValue(ArgumentMatchers.any(Field.class), ArgumentMatchers.any(String.class));
        Assert.assertEquals("Yaounde", value);
    }

    @Test
    public void testUpdateChildDetailsPopulatesCorrectly() {
        Map<String, String> detailsMap = new HashMap<>();
        detailsMap.put("key1", "key1");
        detailsMap.put("key2", "key2");

        baseChildRegistrationDataFragment.updateChildDetails(detailsMap);
        Assert.assertEquals(2, baseChildRegistrationDataFragment.childDetails.size());
    }

    @Test
    public void testResetAdapterDataPopulatesAdapterWithMap() {
        List<Field> fieldsList = new ArrayList<>();
        List<Field> formFields = generateFormFieldsForTest();
        fieldsList.add(formFields.get(3));
        fieldsList.add(formFields.get(4));
        Map<String, String> fieldNameAliasMap = new HashMap<>();
        Whitebox.setInternalState(baseChildRegistrationDataFragment, "fields", fieldsList);
        Whitebox.setInternalState(baseChildRegistrationDataFragment, "stringResourceIds", baseChildRegistrationDataFragment.getDataRowLabelResourceIds());
        Whitebox.setInternalState(baseChildRegistrationDataFragment, "fieldNameAliasMap", fieldNameAliasMap);

        Map<String, String> detailsMap = new HashMap<>();
        detailsMap.put("key4", "key4");
        detailsMap.put("key5", "key5");

        baseChildRegistrationDataFragment.resetAdapterData(detailsMap);

        ArgumentCaptor<ChildRegistrationDataAdapter> adapterCaptor = ArgumentCaptor.forClass(ChildRegistrationDataAdapter.class);

        Mockito.verify(baseChildRegistrationDataFragment, Mockito.atLeast(detailsMap.size())).getResourceLabel(ArgumentMatchers.any(String.class));
        Mockito.verify(baseChildRegistrationDataFragment).setmAdapter(adapterCaptor.capture());
        Assert.assertEquals(detailsMap.size(), adapterCaptor.getAllValues().get(0).getItemCount());
    }

    private List<Field> generateFormFieldsForTest() {
        fields = new ArrayList<>();
        Field f = new Field();
        f.setHint("Hint A");
        Whitebox.setInternalState(f, "key", "key1");
        fields.add(f);

        f = new Field();
        f.setHint("Hint B");
        Whitebox.setInternalState(f, "key", "key2");
        fields.add(f);

        f = new Field();
        f.setHint("OpenSRP ID");
        Whitebox.setInternalState(f, "key", "key3");
        Whitebox.setInternalState(f, "renderType", "id");
        fields.add(f);

        f = new Field();
        f.setHint("Date Picker");
        Whitebox.setInternalState(f, "key", "key4");
        Whitebox.setInternalState(f, "type", JsonFormConstants.DATE_PICKER);
        fields.add(f);

        f = new Field();
        f.setHint("Spinner");
        Whitebox.setInternalState(f, "key", "key5");
        Whitebox.setInternalState(f, "type", JsonFormConstants.SPINNER);
        Whitebox.setInternalState(f, "keys", Arrays.asList(new String[]{"m", "f"}));
        Whitebox.setInternalState(f, "values", Arrays.asList(new String[]{"Male", "Female"}));
        fields.add(f);

        f = new Field();
        f.setHint("Location Spinner");
        Whitebox.setInternalState(f, "key", "key6");
        Whitebox.setInternalState(f, "type", JsonFormConstants.SPINNER);
        Whitebox.setInternalState(f, "subType", Constants.JSON_FORM_KEY.LOCATION_SUB_TYPE);
        fields.add(f);

        f = new Field();
        f.setHint("Tree");
        Whitebox.setInternalState(f, "key", "key7");
        Whitebox.setInternalState(f, "type", JsonFormConstants.TREE);
        fields.add(f);

        return fields;
    }
}
