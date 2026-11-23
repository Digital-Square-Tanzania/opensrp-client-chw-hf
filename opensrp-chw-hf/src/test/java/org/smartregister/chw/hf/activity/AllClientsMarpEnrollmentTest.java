package org.smartregister.chw.hf.activity;

import android.content.Intent;
import android.view.MenuItem;

import org.joda.time.DateTimeUtils;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.smartregister.chw.hf.R;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.DBConstants;

import java.util.Collections;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AllClientsMarpEnrollmentTest {
    private TestProfile profile;
    private MenuItem enrollmentItem;

    @Before
    public void setUp() {
        DateTimeUtils.setCurrentMillisFixed(new LocalDate(2026, 11, 23).toDateTimeAtStartOfDay().getMillis());
        profile = mock(TestProfile.class);
        doCallRealMethod().when(profile).onOptionsItemSelected(any(MenuItem.class));
        doCallRealMethod().when(profile).startKvpRegistration();
        doCallRealMethod().when(profile).setClient(any(CommonPersonObjectClient.class));
        enrollmentItem = mock(MenuItem.class);
        when(enrollmentItem.getItemId()).thenReturn(R.id.action_kvp_registration);
    }

    @After
    public void resetClock() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void dayBeforeFifteenthBirthdayShowsPromptAndStaysOnProfile() {
        setDateOfBirth("2011-11-24");

        assertTrue(profile.onOptionsItemSelected(enrollmentItem));

        verify(profile).showMarpEnrollmentMessage(R.string.marp_enrollment_minimum_age);
        verify(profile, never()).startMarpScreening(anyInt());
        verify(profile, never()).startActivity(any(Intent.class));
    }

    @Test
    public void fifteenthBirthdayStartsScreeningWithCompletedAge() {
        setDateOfBirth("2011-11-23");

        assertTrue(profile.onOptionsItemSelected(enrollmentItem));

        verify(profile).startMarpScreening(15);
        verify(profile, never()).showMarpEnrollmentMessage(anyInt());
    }

    @Test
    public void olderClientStartsScreeningWithCompletedAge() {
        setDateOfBirth("2004-12-23");

        assertTrue(profile.onOptionsItemSelected(enrollmentItem));

        verify(profile).startMarpScreening(21);
        verify(profile, never()).showMarpEnrollmentMessage(anyInt());
    }

    @Test
    public void enrolledClientOpensMarpProfileBeforeReadingDateOfBirth() {
        CommonPersonObjectClient client = mock(CommonPersonObjectClient.class);
        profile.setClient(client);
        when(profile.isMarpEnrolled()).thenReturn(true);

        assertTrue(profile.onOptionsItemSelected(enrollmentItem));

        InOrder actions = inOrder(profile);
        actions.verify(profile).showMarpEnrollmentMessage(R.string.marp_enrollment_already_enrolled);
        actions.verify(profile).startActivity(any(Intent.class));
        verify(profile, never()).startMarpScreening(anyInt());
        verify(client, never()).getColumnmaps();
    }

    private void setDateOfBirth(String dateOfBirth) {
        CommonPersonObjectClient client = mock(CommonPersonObjectClient.class);
        when(client.getColumnmaps()).thenReturn(Collections.singletonMap(DBConstants.KEY.DOB, dateOfBirth));
        profile.setClient(client);
    }

    public static class TestProfile extends AllClientsMemberProfileActivity {
        void setClient(CommonPersonObjectClient client) {
            commonPersonObject = client;
        }
    }
}
