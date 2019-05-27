package org.smartregister.child.task;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.smartregister.child.R;
import org.smartregister.child.domain.RegisterActionParams;
import org.smartregister.child.util.Constants;
import org.smartregister.child.wrapper.WeightViewRecordUpdateWrapper;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.growthmonitoring.domain.Weight;
import org.smartregister.growthmonitoring.repository.WeightRepository;
import org.smartregister.util.Utils;
import org.smartregister.view.contract.SmartRegisterClient;

import static org.smartregister.util.Utils.getValue;

/**
 * Created by ndegwamartin on 05/03/2019.
 */
public class WeightAsyncTask extends AsyncTask<Void, Void, Void> {
    private final View convertView;
    private final String entityId;
    private final String lostToFollowUp;
    private final String inactive;
    private Weight weight;
    private SmartRegisterClient client;
    private WeightRepository weightRepository;
    private CommonRepository commonRepository;
    private Context context;
    private Boolean updateOutOfCatchment;
    private View.OnClickListener onClickListener;

    public WeightAsyncTask(RegisterActionParams recordActionParams, CommonRepository commonRepository, WeightRepository weightRepository, Context context) {
        this.convertView = recordActionParams.getConvertView();
        this.entityId = recordActionParams.getEntityId();
        this.lostToFollowUp = recordActionParams.getLostToFollowUp();
        this.inactive = recordActionParams.getInactive();
        this.client = recordActionParams.getSmartRegisterClient();
        this.updateOutOfCatchment = recordActionParams.getUpdateOutOfCatchment();
        this.onClickListener = recordActionParams.getOnClickListener();
        this.weightRepository = weightRepository;
        this.commonRepository = commonRepository;
        this.context = context;
    }


    @Override
    protected Void doInBackground(Void... params) {
        weight = weightRepository.findUnSyncedByEntityId(entityId);
        return null;
    }

    @Override
    protected void onPostExecute(Void param) {
        WeightViewRecordUpdateWrapper wrapper = new WeightViewRecordUpdateWrapper();
        wrapper.setWeight(weight);
        wrapper.setLostToFollowUp(lostToFollowUp);
        wrapper.setInactive(inactive);
        wrapper.setClient(client);
        wrapper.setConvertView(convertView);
        updateRecordWeight(wrapper, updateOutOfCatchment);

    }


    private void updateRecordWeight(WeightViewRecordUpdateWrapper updateWrapper, Boolean updateOutOfCatchment) {

        View recordWeight = updateWrapper.getConvertView().findViewById(R.id.record_weight);
        recordWeight.setVisibility(View.VISIBLE);

        if (updateWrapper.getWeight() != null) {
            TextView recordWeightText = updateWrapper.getConvertView().findViewById(R.id.record_weight_text);
            recordWeightText.setText(Utils.kgStringSuffix(updateWrapper.getWeight().getKg()));

            ImageView recordWeightCheck = updateWrapper.getConvertView().findViewById(R.id.record_weight_check);
            recordWeightCheck.setVisibility(View.VISIBLE);

            recordWeight.setClickable(false);
            recordWeight.setBackground(new ColorDrawable(context.getResources()
                    .getColor(android.R.color.transparent)));
        } else {
            TextView recordWeightText = updateWrapper.getConvertView().findViewById(R.id.record_weight_text);
            recordWeightText.setText(context.getString(R.string.record_weight_with_nl));

            ImageView recordWeightCheck = updateWrapper.getConvertView().findViewById(R.id.record_weight_check);
            recordWeightCheck.setVisibility(View.GONE);
            recordWeight.setClickable(true);
        }

        // Update active/inactive/lostToFollowup status
        if (updateWrapper.getLostToFollowUp().equals(Boolean.TRUE.toString()) || updateWrapper.getInactive().equals(Boolean.TRUE.toString())) {
            recordWeight.setVisibility(View.INVISIBLE);
        }

        //Update Out of Catchment
        if (updateOutOfCatchment) {
            updateViews(updateWrapper.getConvertView(), updateWrapper.getClient());
        }
    }

    protected void updateViews(View catchmentView, SmartRegisterClient client) {

        CommonPersonObjectClient pc = (CommonPersonObjectClient) client;

        if (commonRepository != null) {
            CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(pc.entityId());

            View recordVaccination = catchmentView.findViewById(R.id.record_vaccination);
            recordVaccination.setVisibility(View.VISIBLE);

            View moveToCatchment = catchmentView.findViewById(R.id.move_to_catchment);
            moveToCatchment.setVisibility(View.GONE);

            if (commonPersonObject == null) { //Out of area -- doesn't exist in local database

                catchmentView.findViewById(R.id.child_profile_info_layout).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Utils.showShortToast(context, context.getString(R.string.show_vaccine_card_disabled));
                    }
                });

                TextView recordWeightText = catchmentView.findViewById(R.id.record_weight_text);
                recordWeightText.setText(R.string.record_service);

                String zeirId = getValue(pc.getColumnmaps(), Constants.KEY.ZEIR_ID, false);

                View recordWeight = catchmentView.findViewById(R.id.record_weight);
                recordWeight.setBackground(context.getResources().getDrawable(R.drawable.record_weight_bg));
                recordWeight.setTag(zeirId);
                recordWeight.setClickable(true);
                recordWeight.setEnabled(true);
                recordWeight.setOnClickListener(onClickListener);

            }

        }
    }

}

