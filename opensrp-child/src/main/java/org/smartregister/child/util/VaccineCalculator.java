package org.smartregister.child.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.smartregister.immunization.domain.VaccineCondition;
import org.smartregister.immunization.domain.VaccineTrigger;
import org.smartregister.immunization.domain.jsonmapping.Condition;
import org.smartregister.immunization.domain.jsonmapping.Expiry;
import org.smartregister.immunization.domain.jsonmapping.Schedule;
import org.smartregister.immunization.domain.jsonmapping.Vaccine;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 2019-09-02
 */

public class VaccineCalculator {

    /**
     * This method returns the vaccine due date once given the Vaccine mapping as declared in the vaccine configuration
     * in the vaccines.json. The issued vaccines need to be passed for the conditions to be checked. The method
     * uses vaccine configurations from {@link org.smartregister.immunization.db.VaccineRepo} to get configuration
     * information about other vaccines.
     *
     * @param vaccineMapping
     * @param dob
     * @param issuedVaccines
     * @return
     */
    @Nullable
    public static Date getVaccineDueDate(@NonNull Vaccine vaccineMapping, @NonNull Date dob
            , @NonNull List<org.smartregister.immunization.domain.Vaccine> issuedVaccines) {
        if (vaccineMapping.schedule != null) {
            if (!doesVaccinePassConditions(vaccineMapping.schedule.conditions, issuedVaccines)) {
                return null;
            }

            if (vaccineMapping.schedule.due != null && vaccineMapping.schedule.due.size() > 0) {
                return VaccineTrigger.init(Constants.CHILD_TYPE, vaccineMapping.schedule.due.get(0))
                        .getFireDate(issuedVaccines, dob);
            } else {
                return null;
            }
        } else if (vaccineMapping.schedules != null) {
            Collection<Schedule> schedules = vaccineMapping.schedules.values();
            int passedScheduleConditions = 0;

            for (Schedule schedule: schedules) {
                if (doesVaccinePassConditions(schedule.conditions, issuedVaccines)) {
                    passedScheduleConditions++;
                }
            }

            if (passedScheduleConditions != schedules.size()) {
                return null;
            }

            for (Schedule schedule: schedules) {
                if (schedule.due != null && schedule.due.size() > 0) {
                    return VaccineTrigger.init(Constants.CHILD_TYPE, schedule.due.get(0))
                            .getFireDate(issuedVaccines, dob);
                }
            }
        }

        return null;
    }

    @Nullable
    public static Date getVaccineExpiryDate(@NonNull Date dob, @Nullable Expiry data) {
        VaccineTrigger vaccineExpiryTrigger = VaccineTrigger.init(data);

        if (vaccineExpiryTrigger != null) {
            return vaccineExpiryTrigger.getFireDate(null, dob);
        }

        return null;
    }


    @Nullable
    public static Date getVaccineExpiryDate(@NonNull Date dob, @NonNull Vaccine vaccine) {
        if (vaccine.schedules != null) {
            for (Schedule schedule: vaccine.schedules.values()) {
                if (schedule.expiry != null && schedule.expiry.size() > 0) {
                    return getVaccineExpiryDate(dob, schedule.expiry.get(0));
                }
            }
        } else if (vaccine.schedule != null
                && vaccine.schedule.expiry != null
                && vaccine.schedule.expiry.size() > 0) {
                return getVaccineExpiryDate(dob, vaccine.schedule.expiry.get(0));
        }

        return null;
    }

    private static boolean doesVaccinePassConditions(@Nullable List<Condition> conditions, List<org.smartregister.immunization.domain.Vaccine> vaccineList) {
        if (conditions == null) {
            return true;

        } else {
            int passedConditions = 0;
            for (Condition condition: conditions) {
                VaccineCondition vaccineCondition = VaccineCondition.init(Constants.CHILD_TYPE, condition);

                if (vaccineCondition != null) {
                    if (vaccineCondition.passes(vaccineList)) {
                        passedConditions++;
                    }
                } else {
                    passedConditions++;
                }
            }

            return passedConditions == conditions.size();
        }
    }

}
