package org.smartregister.child.widgets;

import androidx.appcompat.app.AppCompatActivity;
import android.widget.EditText;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.Context;
import org.smartregister.child.ChildLibrary;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.AppProperties;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class AdvanceSearchDatePickerDialogTest {

    private AdvanceSearchDatePickerDialog datePickerDialog;

    @Before
    public void setUp() {
        AppCompatActivity startActivity = Robolectric.buildActivity(AppCompatActivity.class).create().get();
        ChildLibrary childLibrary = Mockito.mock(ChildLibrary.class);
        Context context = Mockito.mock(Context.class);
        AllSharedPreferences allSharedPreferences = Mockito.mock(AllSharedPreferences.class);
        AppProperties appProperties = Mockito.spy(AppProperties.class);
        Mockito.doReturn(context).when(childLibrary).context();
        Mockito.doReturn(allSharedPreferences).when(context).allSharedPreferences();
        Mockito.doReturn(appProperties).when(childLibrary).getProperties();
        ReflectionHelpers.setStaticField(ChildLibrary.class, "instance", childLibrary);
        EditText editText = Mockito.spy(new EditText(Mockito.spy(startActivity)));
        datePickerDialog = Mockito.spy(new AdvanceSearchDatePickerDialog(editText));
    }

    @Test
    public void testGetDatePickerDialog() {
        datePickerDialog.showDialog();
        Assert.assertNotNull(datePickerDialog.getDatePickerDialog());
    }
}