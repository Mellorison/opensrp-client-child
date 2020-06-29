package org.smartregister.child.activity;

import androidx.core.app.Fragment;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.child.ChildLibrary;
import org.smartregister.child.domain.ChildMetadata;
import org.smartregister.child.shadows.ChildFormActivityShadow;
import org.smartregister.child.util.Constants;
import org.smartregister.util.AppProperties;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class BaseChildFormActivityTest {

    private ChildFormActivityShadow childFormActivity;

    @Mock
    private ChildLibrary childLibrary;

    @Spy
    private AppProperties appProperties;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        appProperties.setProperty(Constants.PROPERTY.MOTHER_LOOKUP_SHOW_RESULTS_DURATION, String.valueOf(3000));
        Mockito.doReturn(appProperties).when(childLibrary).getProperties();
        childFormActivity = Robolectric.buildActivity(ChildFormActivityShadow.class).get();
        ChildMetadata metadata = new ChildMetadata(BaseChildFormActivity.class, null, null,
                null, true);
        metadata.updateChildRegister("test", "test",
                "test", "ChildRegister",
                "test", "test",
                "test",
                "test", "test");
        ReflectionHelpers.setStaticField(ChildLibrary.class, "instance", childLibrary);
        Mockito.doReturn(metadata).when(childLibrary).metadata();
    }

    @After
    public void tearDown() {
        ReflectionHelpers.setStaticField(ChildLibrary.class, "instance", null);
    }

    @Test
    public void testThatActivityWasCreated() {
        Assert.assertNotNull(childFormActivity);
    }

    @Test
    @Ignore("Fix java.lang.IllegalStateException")
    public void testVisibleFragments() {

        childFormActivity.initializeFormFragment();
        Fragment visibleFragment = childFormActivity.getVisibleFragment();
        Assert.assertNotNull(visibleFragment);
    }

    @Test
    public void testLookupQuery() {
        Map<String, String> entityMap = new HashMap<String, String>() {
            {
                put("first_name", "sonia");
            }
        };
        String lookUpQuery = childFormActivity.lookUpQuery(entityMap, "table_test");
        Assert.assertEquals("Select table_test.id as _id , ec_client.relationalid , " +
                "ec_client.details , zeir_id , first_name , last_name , ec_client.gender , dob , nrc_number " +
                ", mother_guardian_phone_number , ec_mother_details.is_consented , ec_mother_details.preferred_language " +
                ", ec_client.residential_area , ec_client.residential_area_other , ec_client.residential_address " +
                ", ec_client.base_entity_id FROM table_test  join ec_child_details " +
                "on ec_child_details.relational_id=ec_mother_details.base_entity_id join ec_mother_details " +
                "on ec_mother_details.base_entity_id = ec_client.base_entity_id WHERE  first_name Like '%sonia%' ;", lookUpQuery);
    }
}