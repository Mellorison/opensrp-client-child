package org.smartregister.child.contract;

import org.smartregister.Context;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Alert;
import org.smartregister.growthmonitoring.domain.Height;
import org.smartregister.growthmonitoring.domain.Weight;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.domain.VaccineWrapper;
import org.smartregister.immunization.view.VaccineGroup;

import java.util.List;
import java.util.Map;

/**
 * Created by ndegwamartin on 01/09/2020.
 */
public interface ChildImmunizationContract {
    interface View {

        void updateViews();

        void showProgressDialog();

        void showProgressDialog(String title, String message);

        void hideProgressDialog();

        void showGrowthDialogFragment(Map<String, List> growthMonitoring);

        CommonPersonObjectClient getChildDetails();

        String getString(int updating_dialog_title);

        VaccineGroup getLastOpenedView();

        void updateVaccineGroupViews(android.view.View view, List<VaccineWrapper> wrappers, List<Vaccine> vaccineList, boolean undo);

        void updateVaccineGroupsUsingAlerts(List<String> affectedVaccines, List<Vaccine> vaccineList, List<Alert> alertList);

        void showVaccineNotifications(List<Vaccine> vaccineList, List<Alert> alertList);
    }

    interface Presenter {

        List<Weight> getAllWeights(CommonPersonObjectClient childDetails);

        List<Height> getAllHeights(CommonPersonObjectClient childDetails);

        void activateChildStatus(Context openSRPcontext, CommonPersonObjectClient childDetails);

        ChildImmunizationContract.View getView();
    }

}
