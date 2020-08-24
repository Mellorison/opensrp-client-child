package org.smartregister.child.adapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.smartregister.child.BaseUnitTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ViewPagerAdapterTest extends BaseUnitTest {

    private ViewPagerAdapter adapter;

    @Mock
    private FragmentManager fragmentManager;

    @Mock
    private Fragment fragment1;

    @Mock
    private Fragment fragment2;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        adapter = new ViewPagerAdapter(fragmentManager);

        adapter.addFragment(fragment1, "Title1");
        adapter.addFragment(fragment2, "Title2");
    }

    @Test
    public void testAdapterInstantiatesCorrectly() {
        assertNotNull(adapter);
    }

    @Test
    public void testGetItemReturnsFragmentAtPosition() {
        assertEquals(fragment2, adapter.getItem(1));
    }

    @Test
    public void testGetCountReturnsTotalFragmentsInList() {
        assertEquals(2, adapter.getCount());
    }

    @Test
    public void testGetPageTitleReturnsFragmentTitleAtPosition() {
        assertEquals("Title1", adapter.getPageTitle(0));
    }
}
