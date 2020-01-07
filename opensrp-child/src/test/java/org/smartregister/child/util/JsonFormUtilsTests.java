package org.smartregister.child.util;

import android.content.Context;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.CoreLibrary;
import org.smartregister.child.ChildLibrary;
import org.smartregister.clientandeventmodel.Client;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.sync.helper.ECSyncHelper;

import java.time.LocalDate;
import java.time.Period;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Utils.class, ChildLibrary.class, ECSyncHelper.class, CoreLibrary.class})
public class JsonFormUtilsTests {

    private JSONObject jsonObject;
    private static final String MY_KEY = "my_key";
    private static final String MY_LOCATION_ID = "mylo-cati-onid-endt-ifie-r00";

    @Mock
    private ChildLibrary childLibrary;

    @Mock
    private CoreLibrary coreLibrary;

    @Captor
    private ArgumentCaptor addClientCaptor;

    @Captor
    private ArgumentCaptor ecSyncHelperAddEventCaptor;

    @Mock
    private ECSyncHelper ecSyncHelper;

    @Mock
    private org.smartregister.Context opensrpContext;

    @Mock
    private AllSharedPreferences allSharedPreferences;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        jsonObject = new JSONObject();
    }

    @Test
    public void isDateApproxWithNonNumberTest() throws Exception {
        boolean isApproximate = Whitebox.invokeMethod(JsonFormUtils.class, "isDateApprox", "1500208620000L");
        Assert.assertFalse(isApproximate);
    }

    @Test
    public void isDateApproxWithNumberTest() throws Exception {
        boolean isApproximate = Whitebox.invokeMethod(JsonFormUtils.class, "isDateApprox", "1");
        Assert.assertTrue(isApproximate);
    }

    @Test(expected = IllegalArgumentException.class)
    public void processAgeWithWrongDateFormatTest() throws Exception {
        JSONObject result = Whitebox.invokeMethod(JsonFormUtils.class, "processAge", "7/19/19", jsonObject);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.has(JsonFormUtils.VALUE));
        Assert.assertNotNull(result.get(JsonFormUtils.VALUE));
    }

    @Test
    public void processAgeTest() throws Exception {
        String TEST_DOB = "2017-01-01";
        LocalDate date = LocalDate.parse(TEST_DOB);
        Integer expectedDifferenceInYears = Period.between(date, LocalDate.now()).getYears();

        Whitebox.invokeMethod(JsonFormUtils.class, "processAge", TEST_DOB, jsonObject);
        Assert.assertTrue(jsonObject.has(JsonFormUtils.VALUE));
        Assert.assertNotNull(jsonObject.get(JsonFormUtils.VALUE));
        Assert.assertEquals(expectedDifferenceInYears, jsonObject.get(JsonFormUtils.VALUE));
    }

    @Test
    public void testAddLocationDefault() throws Exception {
        jsonObject.put(JsonFormConstants.KEY, MY_KEY);
        JSONArray array = new JSONArray();
        array.put(MY_LOCATION_ID);
        Whitebox.invokeMethod(JsonFormUtils.class, "addLocationDefault", MY_KEY, jsonObject, array.toString());
        Assert.assertTrue(jsonObject.has(JsonFormConstants.DEFAULT));
        Assert.assertNotNull(jsonObject.get(JsonFormConstants.DEFAULT));
        Assert.assertEquals(array.get(0), jsonObject.getJSONArray(JsonFormConstants.DEFAULT).get(0));
    }

    @Test
    public void getChildLocationIdShouldReturnNullWhenCurrentLocalityIsNull() {
        AllSharedPreferences allSharedPreferences = Mockito.mock(AllSharedPreferences.class);

        Assert.assertNull(JsonFormUtils.getChildLocationId("98349797-489834", allSharedPreferences));
    }

    @Test
    public void getChildLocationIdShouldReturnCurrentLocalityIdWhenCurrentLocalityIsDifferentFromDefaultLocality() {
        AllSharedPreferences allSharedPreferences = Mockito.mock(AllSharedPreferences.class);
        String currentLocality = "Kilimani";
        String currentLocalityId = "9943-43534-2dsfs";

        Mockito.doReturn(currentLocality)
                .when(allSharedPreferences)
                .fetchCurrentLocality();

        LocationHelper locationHelper = Mockito.mock(LocationHelper.class);
        ReflectionHelpers.setStaticField(LocationHelper.class, "instance", locationHelper);

        Mockito.doReturn(currentLocalityId).when(locationHelper).getOpenMrsLocationId(Mockito.eq(currentLocality));

        Assert.assertEquals(currentLocalityId, JsonFormUtils.getChildLocationId("98349797-489834", allSharedPreferences));
    }

    @Test
    public void mergeAndSaveClient() throws Exception {
        PowerMockito.mockStatic(ChildLibrary.class);
        PowerMockito.when(ChildLibrary.getInstance()).thenReturn(childLibrary);
        ECSyncHelper ecSyncHelper = Mockito.mock(ECSyncHelper.class);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("first_name", "John");
        PowerMockito.when(ecSyncHelper.getClient("234")).thenReturn(jsonObject);
        PowerMockito.when(childLibrary.getEcSyncHelper()).thenReturn(ecSyncHelper);

        Client client = new Client("234");
        JsonFormUtils.mergeAndSaveClient(client);
        Mockito.verify(ecSyncHelper, Mockito.times(1))
                .addClient((String) addClientCaptor.capture(), (JSONObject) addClientCaptor.capture());

        JSONObject expected = new JSONObject();
        expected.put("baseEntityId", "234");
        expected.put("type", "Client");
        expected.put("first_name", "John");
        Assert.assertEquals("234", addClientCaptor.getAllValues().get(0));
        Assert.assertEquals(expected.toString(), addClientCaptor.getAllValues().get(1).toString());
    }

    @Test
    public void createBCGScarEvent() throws Exception {
        PowerMockito.mockStatic(ECSyncHelper.class);
        Context context = Mockito.mock(Context.class);
        PowerMockito.mockStatic(CoreLibrary.class);
        PowerMockito.mockStatic(ChildLibrary.class);
        PowerMockito.when(ChildLibrary.getInstance()).thenReturn(childLibrary);
        PowerMockito.when(CoreLibrary.getInstance()).thenReturn(coreLibrary);
        PowerMockito.when(coreLibrary.context()).thenReturn(opensrpContext);
        PowerMockito.when(opensrpContext.allSharedPreferences()).thenReturn(allSharedPreferences);
        String providerId = "providerId";
        String teamName = "teamA";
        String teamId = "24234-234";
        PowerMockito.when(allSharedPreferences.fetchRegisteredANM()).thenReturn(providerId);
        PowerMockito.when(allSharedPreferences.fetchDefaultTeam(providerId)).thenReturn(teamName);
        PowerMockito.when(allSharedPreferences.fetchDefaultTeamId(providerId)).thenReturn(teamId);
        PowerMockito.when(allSharedPreferences.fetchCurrentLocality()).thenReturn(null);

        PowerMockito.when(ECSyncHelper.getInstance(context)).thenReturn(ecSyncHelper);

        JsonFormUtils jsonFormUtils = new JsonFormUtils();
        Whitebox.invokeMethod(jsonFormUtils,
                "createBCGScarEvent", context,
                "3434-234", providerId, "locationId");
        Mockito.verify(ecSyncHelper).addEvent((String) ecSyncHelperAddEventCaptor.capture(),
                (JSONObject) ecSyncHelperAddEventCaptor.capture());

        String baseEntityId = (String) ecSyncHelperAddEventCaptor.getAllValues().get(0);
        JSONObject eventJson = (JSONObject) ecSyncHelperAddEventCaptor.getAllValues().get(1);
        JSONObject obsJsonObject = eventJson.optJSONArray("obs").optJSONObject(0);
        Assert.assertEquals("3434-234", baseEntityId);
        Assert.assertEquals("bcg_scar", obsJsonObject.optString("formSubmissionField"));
        Assert.assertEquals("concept", obsJsonObject.optString("fieldType"));

        Assert.assertEquals("160265AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", obsJsonObject.optString("fieldCode"));
        Assert.assertEquals("select one", obsJsonObject.optString("fieldDataType"));
        Assert.assertEquals("Yes", obsJsonObject.optJSONArray("humanReadableValues").optString(0));
        Assert.assertEquals("1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", obsJsonObject.optJSONArray("values").optString(0));

        Assert.assertEquals(JsonFormUtils.BCG_SCAR_EVENT, eventJson.optString("eventType"));
        Assert.assertEquals(teamName, eventJson.optString("team"));
        Assert.assertEquals("child", eventJson.optString("entityType"));
        Assert.assertEquals(teamId, eventJson.optString("teamId"));
    }
}