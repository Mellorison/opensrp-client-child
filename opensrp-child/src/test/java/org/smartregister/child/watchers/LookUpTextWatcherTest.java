package org.smartregister.child.watchers;

import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.view.View;

import com.vijay.jsonwizard.fragments.JsonFormFragment;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.child.BaseUnitTest;
import org.smartregister.child.ChildLibrary;
import org.smartregister.child.domain.EntityLookUp;
import org.smartregister.child.fragment.ChildFormFragment;
import org.smartregister.child.util.Constants;
import org.smartregister.event.Listener;
import org.smartregister.util.AppProperties;

import java.util.Map;


public class LookUpTextWatcherTest extends BaseUnitTest {

    private LookUpTextWatcher lookUpTextWatcher;

    private JsonFormFragment formFragment;

    @Mock
    private ChildLibrary childLibrary;

    @Mock
    private AppProperties appProperties;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Mockito.doReturn("20").when(appProperties)
                .getProperty(Constants.PROPERTY.MOTHER_LOOKUP_SHOW_RESULTS_DURATION,
                        Constants.MOTHER_LOOKUP_SHOW_RESULTS_DEFAULT_DURATION);

        Mockito.doReturn("20").when(appProperties)
                .getProperty(Constants.PROPERTY.MOTHER_LOOKUP_UNDO_DURATION,
                        Constants.MOTHER_LOOKUP_UNDO_DEFAULT_DURATION);

        Mockito.doReturn(appProperties).when(childLibrary).getProperties();

        ReflectionHelpers.setStaticField(ChildLibrary.class, "instance", childLibrary);

        formFragment = new ChildFormFragment();
    }

    @Test
    public void testAfterTextChangedShouldFillLookUpMapCorrectlyBeforeLookup() {
        View view = new View(RuntimeEnvironment.application);
        view.setTag(com.vijay.jsonwizard.R.id.raw_value, "test");
        view.setTag(com.vijay.jsonwizard.R.id.key, "first_name");
        view.setTag(com.vijay.jsonwizard.R.id.after_look_up, false);
        lookUpTextWatcher = new LookUpTextWatcher(formFragment, view, "mother");
        LookUpTextWatcher lookUpTextWatcherSpy = Mockito.spy(lookUpTextWatcher);

        Editable editable = new SpannableStringBuilder();
        lookUpTextWatcherSpy.afterTextChanged(editable);

        Mockito.doNothing().when(lookUpTextWatcherSpy)
                .initiateLookUp(Mockito.any(Listener.class));

        Map<String, EntityLookUp> lookUpMap = ReflectionHelpers.getField(lookUpTextWatcherSpy, "lookUpMap");
        Assert.assertFalse(lookUpMap.isEmpty());

        Mockito.verify(lookUpTextWatcherSpy, Mockito.times(1))
                .initiateLookUp(Mockito.any(Listener.class));
    }


    @Test
    public void testAfterTextChangedShouldNotLookUpAfterIfAfterLookUpIsTrue() {
        View view = new View(RuntimeEnvironment.application);
        view.setTag(com.vijay.jsonwizard.R.id.key, "first_name");
        view.setTag(com.vijay.jsonwizard.R.id.after_look_up, true);
        lookUpTextWatcher = new LookUpTextWatcher(formFragment, view, "mother");
        LookUpTextWatcher lookUpTextWatcherSpy = Mockito.spy(lookUpTextWatcher);

        Mockito.doNothing().when(lookUpTextWatcherSpy).initiateLookUp(Mockito.any(Listener.class));
        Editable editable = new SpannableStringBuilder().append("test");
        lookUpTextWatcherSpy.afterTextChanged(editable);

        Map<String, EntityLookUp> lookUpMap = ReflectionHelpers.getField(lookUpTextWatcherSpy, "lookUpMap");
        Assert.assertTrue(lookUpMap.isEmpty());

        Mockito.verify(lookUpTextWatcherSpy, Mockito.never())
                .initiateLookUp(Mockito.any(Listener.class));
    }

    @After
    public void tearDown() {
        ReflectionHelpers.setStaticField(ChildLibrary.class, "instance", null);
    }

}