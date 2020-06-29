package org.smartregister.child.util;

import android.content.ContentValues;
import androidx.annotation.NonNull;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.smartregister.CoreLibrary;
import org.smartregister.child.ChildLibrary;
import org.smartregister.commonregistry.CommonPersonObjectClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChildDbUtils {

    /**
     * Retrieves all child details needed for display and/or editing
     *
     * @param baseEntityId {@link String}
     * @return {@link HashMap}
     */
    public static HashMap<String, String> fetchChildDetails(@NonNull String baseEntityId) {
        ArrayList<HashMap<String, String>> childDetails = ChildLibrary.getInstance()
                .eventClientRepository()
                .rawQuery(ChildLibrary.getInstance().getRepository().getReadableDatabase(),
                        Utils.metadata().getRegisterQueryProvider().mainRegisterQuery() +
                                " WHERE " + Utils.metadata().getRegisterQueryProvider().getDemographicTable() + ".id = '" + baseEntityId + "' LIMIT 1");
        return childDetails != null && childDetails.size() > 0 ? childDetails.get(0) : null;
    }

    /**
     * Retrieves all child details and packages them into a CommonPersonObjectClient
     *
     * @param baseEntityId {@link String}
     * @return {@link CommonPersonObjectClient}
     */
    public static CommonPersonObjectClient fetchCommonPersonObjectClientByBaseEntityId(String baseEntityId) {

        CommonPersonObjectClient commonPersonObjectClient = null;
        Map<String, String> childDetails = fetchChildDetails(baseEntityId);

        if (childDetails != null) {

            commonPersonObjectClient = new CommonPersonObjectClient(baseEntityId, childDetails, Constants.KEY.CHILD);
            commonPersonObjectClient.setColumnmaps(childDetails);
            commonPersonObjectClient.setCaseId(baseEntityId);

        }

        return commonPersonObjectClient;
    }

    /**
     * Retrieves the initial GM values for child
     *
     * @param baseEntityId {@link String}
     * @return {@link HashMap}
     */
    public static HashMap<String, String> fetchChildFirstGrowthAndMonitoring(@NonNull String baseEntityId) {
        boolean heightMetricEnabled = CoreLibrary.getInstance().context().getAppProperties().getPropertyBoolean(ChildAppProperties.KEY.MONITOR_HEIGHT);
        HashMap<String, String> hashMap = new HashMap<>();
        SQLiteDatabase sqLiteDatabase = ChildLibrary.getInstance().getRepository().getReadableDatabase();
        Cursor weightCursor = sqLiteDatabase.query("weights", new String[]{"kg", "created_at"},
                "base_entity_id = ?",
                new String[]{baseEntityId}, null, null, "created_at asc", "1");
        String dateCreated = "";
        String weight = null;
        String height = null;
        if (weightCursor != null && weightCursor.getCount() > 0 && weightCursor.moveToNext()) {
            weight = weightCursor.getString(0);
            dateCreated = weightCursor.getString(1);
            hashMap.put("birth_weight", weight);
        }

        if (heightMetricEnabled) {
            Cursor heightCursor = sqLiteDatabase.query("heights", new String[]{"cm", "created_at"},
                    "base_entity_id = ? and created_at = ?",
                    new String[]{baseEntityId, dateCreated}, null, null, null, "1");
            if (heightCursor != null && heightCursor.getCount() > 0 && heightCursor.moveToNext()) {
                height = heightCursor.getString(0);
                hashMap.put("birth_height", height);

            }
        }
        return hashMap;
    }

    /**
     * Updates the ec_child_details table with values
     *
     * @param columnName   {@link String}
     * @param value        {@link String}
     * @param baseEntityId {@link String}
     * @return {@link boolean}
     */
    public static boolean updateChildDetailsValue(String columnName, String value, String baseEntityId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(columnName, value);
        int i = ChildLibrary.getInstance()
                .getRepository().getWritableDatabase()
                .update(ChildLibrary.getInstance().metadata().getRegisterQueryProvider().getChildDetailsTable(),
                        contentValues, Constants.KEY.BASE_ENTITY_ID + "= ?", new String[]{baseEntityId});
        return i != 0;
    }
}
