package org.smartregister.child.fragment;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.snackbar.Snackbar;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.vijay.jsonwizard.activities.JsonFormActivity;
import com.vijay.jsonwizard.domain.Form;
import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.utils.ValidationStatus;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.child.BaseUnitTest;
import org.smartregister.child.ChildLibrary;
import org.smartregister.child.R;
import org.smartregister.child.presenter.ChildFormFragmentPresenter;
import org.smartregister.child.util.Constants;
import org.smartregister.child.util.MotherLookUpUtils;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.util.AppProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChildFormFragmentTest extends BaseUnitTest {
    private ChildFormFragment formFragment;

    @Mock
    private ChildLibrary childLibrary;

    @Mock
    private AppProperties appProperties;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ReflectionHelpers.setStaticField(ChildLibrary.class, "instance", childLibrary);
        Mockito.when(childLibrary.getProperties()).thenReturn(appProperties);
        Mockito.when(appProperties.getProperty(Constants.PROPERTY.MOTHER_LOOKUP_SHOW_RESULTS_DURATION, Constants.MOTHER_LOOKUP_SHOW_RESULTS_DEFAULT_DURATION)).thenReturn("300");
        Mockito.when(appProperties.getProperty(Constants.PROPERTY.MOTHER_LOOKUP_UNDO_DURATION, Constants.MOTHER_LOOKUP_UNDO_DEFAULT_DURATION)).thenReturn("300");
        formFragment = Mockito.spy(ChildFormFragment.class);
    }

    private Map<String, List<View>> getLookUpMap() {
        Map<String, List<View>> lookupMap = new HashMap<>();

        MaterialEditText edtFirstName = new MaterialEditText(RuntimeEnvironment.application);
        edtFirstName.setTag(com.vijay.jsonwizard.R.id.key, MotherLookUpUtils.firstName);
        edtFirstName.setText("Jane");

        MaterialEditText edtLastName = new MaterialEditText(RuntimeEnvironment.application);
        edtLastName.setTag(com.vijay.jsonwizard.R.id.key, MotherLookUpUtils.lastName);
        edtLastName.setText("Doe");

        MaterialEditText edtNationalId = new MaterialEditText(RuntimeEnvironment.application);
        edtNationalId.setTag(com.vijay.jsonwizard.R.id.key, MotherLookUpUtils.MOTHER_GUARDIAN_NRC);
        edtNationalId.setText("23322-23");

        MaterialEditText edtDob = new MaterialEditText(RuntimeEnvironment.application);
        edtDob.setTag(com.vijay.jsonwizard.R.id.key, MotherLookUpUtils.birthDate);
        edtDob.setText("2018-01-15");

        List<View> viewList = new ArrayList<>(Arrays.asList((View) edtFirstName, edtLastName, edtNationalId, edtDob));
        lookupMap.put(Constants.KEY.MOTHER, viewList);
        return lookupMap;
    }

    @Test
    public void testClearMotherLookUpShouldClearViews() throws Exception {
        Map<String, List<View>> lookupMap = getLookUpMap();

        ReflectionHelpers.setField(formFragment, "lookedUp", true);

        Mockito.doReturn(lookupMap).when(formFragment).getLookUpMap();

        Mockito.doNothing().when(formFragment).writeMetaDataValue(Mockito.anyString(), Mockito.any(Map.class));
        Whitebox.invokeMethod(formFragment, "clearMotherLookUp");

        List<View> lookUpViews = lookupMap.get(Constants.KEY.MOTHER);

        assert lookUpViews != null;
        for (View view : lookUpViews) {
            if (view instanceof MaterialEditText) {
                MaterialEditText materialEditText = (MaterialEditText) view;
                Assert.assertTrue(materialEditText.getText().toString().isEmpty());
                Assert.assertTrue(materialEditText.isEnabled());
                Assert.assertFalse((Boolean) materialEditText.getTag(com.vijay.jsonwizard.R.id.after_look_up));
            }
        }
    }

    @Test
    public void testGetRelevantTextViewStringShouldReturnTextViewValue() {
        LinearLayout linearLayout = new LinearLayout(RuntimeEnvironment.application);
        TextView textView = new TextView(RuntimeEnvironment.application);
        textView.setTag(com.vijay.jsonwizard.R.id.key, "test_key");
        textView.setText("text");
        linearLayout.addView(textView);
        Mockito.doReturn(linearLayout).when(formFragment).getMainView();
        Assert.assertEquals("text", formFragment.getRelevantTextViewString("test_key"));
    }

    @Test
    public void testGetRelevantTextViewStringShouldReturnEmptyOnKeyMismatch() {
        LinearLayout linearLayout = new LinearLayout(RuntimeEnvironment.application);
        TextView textView = new TextView(RuntimeEnvironment.application);
        textView.setTag(com.vijay.jsonwizard.R.id.key, "test_key2");
        textView.setText("text");
        linearLayout.addView(textView);
        Mockito.doReturn(linearLayout).when(formFragment).getMainView();
        Assert.assertTrue(formFragment.getRelevantTextViewString("test_key").isEmpty());
    }

    @Test
    public void testUpdateRelevantTextViewStringShouldReturnTextPassed() throws Exception {
        LinearLayout linearLayout = new LinearLayout(RuntimeEnvironment.application);
        TextView textView = new TextView(RuntimeEnvironment.application);
        textView.setTag(com.vijay.jsonwizard.R.id.key, "test_key");
        linearLayout.addView(textView);
        Mockito.doReturn(linearLayout).when(formFragment).getMainView();
        Whitebox.invokeMethod(formFragment, "updateRelevantTextView", linearLayout, "text", "test_key");
        Assert.assertEquals("text", textView.getText().toString());
    }

    @Test
    public void testLookupDialogDismissedShouldFillViewsWithDbValues() throws Exception {
        String caseId = "23-sd23";
        String firstName = "Jane";
        String lastName = "Doe";
        Map<String, String> details = new HashMap<>();
        details.put(MotherLookUpUtils.firstName, "Janet");
        details.put(MotherLookUpUtils.lastName, "Denice");
        details.put(MotherLookUpUtils.birthDate, "2010-01-15");
        details.put(MotherLookUpUtils.NRC_NUMBER, "1234");
        CommonPersonObjectClient personObjectClient = new CommonPersonObjectClient(caseId, details, firstName + " " + lastName);
        personObjectClient.setColumnmaps(details);
        formFragment = PowerMockito.spy(formFragment);
        ReflectionHelpers.setField(formFragment, "lookedUp", false);
        Mockito.doNothing().when(formFragment).clearView();
        Mockito.doNothing().when(formFragment).writeMetaDataValue(Mockito.anyString(), Mockito.any(Map.class));
        Mockito.doReturn(getLookUpMap()).when(formFragment).getLookUpMap();
        Activity activity = Robolectric.setupActivity(FragmentActivity.class);
        Mockito.doReturn(activity).when(formFragment).getActivity();
        Whitebox.invokeMethod(formFragment, "lookupDialogDismissed", personObjectClient);
        Assert.assertTrue((Boolean) ReflectionHelpers.getField(formFragment, "lookedUp"));
        Mockito.verify(formFragment, Mockito.times(1)).writeMetaDataValue(Mockito.anyString(), Mockito.<String, String>anyMap());
        List<View> viewList = formFragment.getLookUpMap().get(Constants.KEY.MOTHER);
        for (View view : viewList) {
            String key = (String) view.getTag(com.vijay.jsonwizard.R.id.key);
            if (view instanceof MaterialEditText) {
                MaterialEditText materialEditText = (MaterialEditText) view;
                if (StringUtils.containsIgnoreCase(key, MotherLookUpUtils.firstName)) {
                    Assert.assertEquals("Janet", materialEditText.getText().toString());
                } else if (StringUtils.containsIgnoreCase(key, MotherLookUpUtils.lastName)) {
                    Assert.assertEquals("Denice", materialEditText.getText().toString());
                } else if (StringUtils.containsIgnoreCase(key, MotherLookUpUtils.NRC_NUMBER)) {
                    Assert.assertEquals("1234", materialEditText.getText().toString());
                }
            }
        }
    }


    @Test
    public void testShowFinalActionSnackBarShouldShowSnackBar() throws Exception {

        formFragment = PowerMockito.spy(formFragment);

        Activity activity = Robolectric.setupActivity(FragmentActivity.class);

        CoordinatorLayout parentLayout = new CoordinatorLayout(activity);
        LinearLayout linearLayout = new LinearLayout(activity);

        Button actionView = new Button(activity);
        actionView.setId(com.google.android.material.R.id.snackbar_action);
        linearLayout.addView(actionView);

        TextView textView = new TextView(activity);
        textView.setId(com.google.android.material.R.id.snackbar_text);
        linearLayout.addView(textView);

        parentLayout.addView(linearLayout);

        Mockito.doReturn(activity).when(formFragment).getActivity();
        Mockito.doReturn(activity.getResources()).when(formFragment).getResources();
        Mockito.doReturn(linearLayout).when(formFragment).getSnackBarView(ArgumentMatchers.any(Snackbar.class));
        Snackbar snackbar = Snackbar.make(parentLayout, R.string.undo_lookup, Snackbar.LENGTH_INDEFINITE);

        ReflectionHelpers.setStaticField(ChildFormFragment.class, "showResultsDuration", 0);
        Whitebox.invokeMethod(formFragment, "showFinalActionSnackBar", snackbar);
        Mockito.verify(formFragment, Mockito.times(1)).showSnackBar(ArgumentMatchers.any(Snackbar.class));
        Mockito.verify(formFragment, Mockito.times(1)).dismissSnackBar(ArgumentMatchers.any(Snackbar.class));

    }

    @Test
    public void testValidateActivateNextShouldShowSaveIfNotIntermediatePageAndValid() {
        formFragment = PowerMockito.spy(formFragment);
        JsonFormActivity activity = Mockito.mock(JsonFormActivity.class);
        Mockito.doReturn(activity).when(formFragment).getActivity();
        Mockito.doReturn(activity.getResources()).when(formFragment).getResources();
        Mockito.when(formFragment.isVisible()).thenReturn(true);
        Form form = new Form();
        Mockito.doReturn(form).when(activity).getForm();
        Collection<View> viewCollection = new ArrayList<>();
        View view = new View(RuntimeEnvironment.application);
        viewCollection.add(view);
        JsonApi jsonApi = Mockito.mock(JsonApi.class);
        Mockito.doReturn(jsonApi).when(formFragment).getJsonApi();
        Mockito.doReturn(viewCollection).when(jsonApi).getFormDataViews();
        ChildFormFragmentPresenter childFormFragmentPresenter = Mockito.mock(ChildFormFragmentPresenter.class);
        Mockito.doReturn(childFormFragmentPresenter).when(formFragment).getPresenter();
        ValidationStatus validationStatus = new ValidationStatus(true, null, null, null);
        Mockito.doReturn(validationStatus).when(formFragment).validateView(view);
        Mockito.doReturn(false).when(childFormFragmentPresenter).intermediatePage();
        Menu menu = new MenuBuilder(RuntimeEnvironment.application);
        menu.add(0, com.vijay.jsonwizard.R.id.action_save, 0, "title1");
        Mockito.doReturn(menu).when(formFragment).getMenu();
        formFragment.validateActivateNext();
        Assert.assertTrue(menu.findItem(com.vijay.jsonwizard.R.id.action_save).isVisible());
    }

    @Test
    public void testValidateActivateNextShouldHideSaveIfNotIntermediatePageAndInvalid() {
        formFragment = PowerMockito.spy(formFragment);
        JsonFormActivity activity = Mockito.mock(JsonFormActivity.class);
        Mockito.doReturn(activity).when(formFragment).getActivity();
        Mockito.doReturn(activity.getResources()).when(formFragment).getResources();
        Mockito.when(formFragment.isVisible()).thenReturn(true);
        Form form = new Form();
        Mockito.doReturn(form).when(activity).getForm();
        Collection<View> viewCollection = new ArrayList<>();
        View view = new View(RuntimeEnvironment.application);
        viewCollection.add(view);
        JsonApi jsonApi = Mockito.mock(JsonApi.class);
        Mockito.doReturn(jsonApi).when(formFragment).getJsonApi();
        Mockito.doReturn(viewCollection).when(jsonApi).getFormDataViews();
        ChildFormFragmentPresenter childFormFragmentPresenter = Mockito.mock(ChildFormFragmentPresenter.class);
        Mockito.doReturn(childFormFragmentPresenter).when(formFragment).getPresenter();
        ValidationStatus validationStatus = new ValidationStatus(false, null, null, null);
        Mockito.doReturn(validationStatus).when(formFragment).validateView(view);
        Mockito.doReturn(false).when(childFormFragmentPresenter).intermediatePage();
        Menu menu = new MenuBuilder(RuntimeEnvironment.application);
        menu.add(0, com.vijay.jsonwizard.R.id.action_save, 0, "title1");
        Mockito.doReturn(menu).when(formFragment).getMenu();
        formFragment.validateActivateNext();
        Assert.assertFalse(menu.findItem(com.vijay.jsonwizard.R.id.action_save).isVisible());
    }

    @Test
    public void testShowShouldShowSnackbar() throws Exception {
        formFragment = PowerMockito.spy(formFragment);
        Activity activity = Robolectric.setupActivity(FragmentActivity.class);
        LinearLayout linearLayout = new LinearLayout(RuntimeEnvironment.application);
        Button actionView = new Button(RuntimeEnvironment.application);
        Button actionViewSpy = Mockito.spy(actionView);
        actionViewSpy.setId(com.google.android.material.R.id.snackbar_action);
        TextView textView = new TextView(RuntimeEnvironment.application);
        textView.setId(com.google.android.material.R.id.snackbar_text);
        linearLayout.addView(actionViewSpy);
        linearLayout.addView(textView);

        Mockito.doReturn(activity).when(formFragment).getActivity();
        Mockito.doReturn(activity.getResources()).when(formFragment).getResources();

        Snackbar snackbar = Mockito.mock(Snackbar.class);
        Mockito.doReturn(linearLayout).when(snackbar).getView();

        ReflectionHelpers.setStaticField(ChildFormFragment.class, "showResultsDuration", 0);
        Whitebox.invokeMethod(formFragment, "show", snackbar);
        Assert.assertTrue(linearLayout.hasOnClickListeners());
        Assert.assertTrue(textView.hasOnClickListeners());
        linearLayout.performClick();
        textView.performClick();
        Mockito.verify(actionViewSpy, Mockito.times(2)).performClick();
        Mockito.verify(snackbar, Mockito.times(1)).show();
        Mockito.verify(snackbar, Mockito.times(1)).dismiss();
    }

    @Test
    public void testUpdateResultsShouldShowDialogAfterLookUp() throws Exception {
        formFragment = PowerMockito.spy(formFragment);
        Activity activity = Robolectric.setupActivity(FragmentActivity.class);
        Activity activitySpy = Mockito.spy(activity);
        Mockito.doReturn(activitySpy).when(formFragment).getActivity();
        Mockito.doReturn(activitySpy.getResources()).when(formFragment).getResources();
        LayoutInflater layoutInflater = Mockito.mock(LayoutInflater.class);
        LinearLayout linearLayout = new LinearLayout(RuntimeEnvironment.application);
        ListView listView = new ListView(RuntimeEnvironment.application);
        listView.setId(R.id.list_view);
        linearLayout.addView(listView);
        Mockito.doReturn(linearLayout).when(layoutInflater).inflate(R.layout.mother_lookup_results, null);
        Mockito.doReturn(layoutInflater).when(activitySpy).getLayoutInflater();
        HashMap<CommonPersonObject, List<CommonPersonObject>> map = new HashMap<>();
        Whitebox.invokeMethod(formFragment, "updateResults", map);
        Assert.assertNotNull(listView.getAdapter());
        AlertDialog alertDialog = ReflectionHelpers.getField(formFragment, "alertDialog");
        Assert.assertTrue(alertDialog.isShowing());
    }

    @After
    public void tearDown() {
        ReflectionHelpers.setStaticField(ChildLibrary.class, "instance", null);
    }

}