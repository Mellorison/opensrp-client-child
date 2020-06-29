package org.smartregister.child.activity;

import androidx.appcompat.view.menu.MenuBuilder;
import android.view.Menu;

import org.apache.commons.lang3.tuple.Triple;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.smartregister.child.BaseUnitTest;
import org.smartregister.child.R;
import org.smartregister.child.fragment.BaseChildRegistrationDataFragment;
import org.smartregister.growthmonitoring.domain.Weight;
import org.smartregister.immunization.domain.ServiceRecord;
import org.smartregister.immunization.domain.Vaccine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class BaseChildDetailTabbedActivityTest extends BaseUnitTest {

    @Test
    public void testUpdateOptionsMenuShouldDisableMenuItemIfLessThan3Months() {
        List<Vaccine> vaccineList = new ArrayList<>();
        Vaccine vaccine = new Vaccine();
        DateTime dateTime = new DateTime();
        vaccine.setCreatedAt(dateTime.toDate());
        vaccineList.add(vaccine);
        List<ServiceRecord> serviceRecordList = new ArrayList<>();
        ServiceRecord serviceRecord = new ServiceRecord();
        serviceRecord.setCreatedAt(dateTime.toDate());
        serviceRecordList.add(serviceRecord);
        List<Weight> weightList = new ArrayList<>();
        Weight weight = new Weight();
        weight.setCreatedAt(dateTime.toDate());
        Weight weight2 = new Weight();
        weight2.setCreatedAt(dateTime.toDate());
        weightList.add(weight);
        weightList.add(weight2);

        Menu menu = new MenuBuilder(RuntimeEnvironment.application);
        menu.add(0, R.id.immunization_data, 1, "title1");
        menu.add(0, R.id.recurring_services_data, 1, "title2");
        menu.add(0, R.id.weight_data, 1, "title3");

        TestBaseChildDetailTabbedActivity.setOverflow(menu);
        TestBaseChildDetailTabbedActivity.updateOptionsMenu(vaccineList, serviceRecordList, weightList, null);
        Assert.assertTrue(menu.findItem(R.id.immunization_data).isEnabled());
        Assert.assertTrue(menu.findItem(R.id.recurring_services_data).isEnabled());
        Assert.assertTrue(menu.findItem(R.id.weight_data).isEnabled());
    }


    private static class TestBaseChildDetailTabbedActivity extends BaseChildDetailTabbedActivity {

        @Override
        protected BaseChildRegistrationDataFragment getChildRegistrationDataFragment() {
            return null;
        }

        @Override
        protected void navigateToRegisterActivity() {
            Timber.e("navigateToRegisterActivity");
        }

        @Override
        public void onUniqueIdFetched(Triple<String, Map<String, String>, String> triple, String entityId) {
            Timber.e("onUniqueIdFetched");
        }

        @Override
        public void onNoUniqueId() {
            Timber.e("onNoUniqueId");
        }
    }
}