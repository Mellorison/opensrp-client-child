package org.smartregister.child.sample.presenter;

import org.smartregister.child.contract.ChildRegisterFragmentContract;
import org.smartregister.child.presenter.BaseChildRegisterFragmentPresenter;
import org.smartregister.child.sample.util.DBQueryHelper;
import org.smartregister.child.util.Constants;
import org.smartregister.child.util.Utils;

/**
 * Created by ndegwamartin on 01/03/2019.
 */
public class ChildRegisterFragmentPresenter extends BaseChildRegisterFragmentPresenter {

    public ChildRegisterFragmentPresenter(ChildRegisterFragmentContract.View view, ChildRegisterFragmentContract.Model model,
                                          String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainCondition() {
        return String.format(" %s is null AND %s == 0 ",
                Utils.metadata().getRegisterQueryProvider().getDemographicTable() + "." + Constants.KEY.DATE_REMOVED,
                Utils.metadata().getRegisterQueryProvider().getDemographicTable() + "." + Constants.KEY.IS_CLOSED);
    }

    @Override
    public String getDefaultSortQuery() {
        return DBQueryHelper.getSortQuery();
    }
}
