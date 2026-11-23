package org.smartregister.chw.hf.activity;

import android.content.Intent;
import android.view.MenuItem;

import org.joda.time.DateTimeUtils;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.smartregister.chw.hf.R;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.DBConstants;

import java.util.Collections;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doCallRealMethod;
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
        doCallRealMethod().when(profile).updateMarpEnrollmentVisibility(any(MenuItem.class), anyBoolean());
        doCallRealMethod().when(profile).setClient(any(CommonPersonObjectClient.class));
        enrollmentItem = mock(MenuItem.class);
        when(enrollmentItem.getItemId()).thenReturn(R.id.action_kvp_registration);
    }

    @After
    public void resetClock() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void dayBeforeFifteenthBirthdayHidesActionAndBlocksScreening() {
        setDateOfBirth("2011-11-24");
        profile.updateMarpEnrollmentVisibility(enrollmentItem, true);

        verify(enrollmentItem).setVisible(false);
        assertTrue(profile.onOptionsItemSelected(enrollmentItem));

        verify(profile).showMarpEnrollmentMessage(R.string.marp_enrollment_minimum_age);
        verify(profile, never()).isMarpEnrolled();
        verify(profile, never()).startMarpScreening(anyInt());
        verify(profile, never()).startActivity(any(Intent.class));
    }

    @Test
    public void fifteenthBirthdayStartsScreeningWithCompletedAge() {
        setDateOfBirth("2011-11-23");
        profile.updateMarpEnrollmentVisibility(enrollmentItem, true);

        verify(enrollmentItem).setVisible(true);
        assertTrue(profile.onOptionsItemSelected(enrollmentItem));

        verify(profile).startMarpScreening(15);
        verify(profile, never()).showMarpEnrollmentMessage(anyInt());
    }

    @Test
    public void olderClientStartsScreeningWithCompletedAge() {
        setDateOfBirth("2004-12-23");
        profile.updateMarpEnrollmentVisibility(enrollmentItem, true);

        verify(enrollmentItem).setVisible(true);
        assertTrue(profile.onOptionsItemSelected(enrollmentItem));

        verify(profile).startMarpScreening(21);
        verify(profile, never()).showMarpEnrollmentMessage(anyInt());
    }

    @Test
    public void eligibleEnrolledClientCanOpenSavedMarpProfile() {
        setDateOfBirth("2011-11-23");
        when(profile.isMarpEnrolled()).thenReturn(true);

        profile.updateMarpEnrollmentVisibility(enrollmentItem, true);

        verify(enrollmentItem).setVisible(true);
        assertTrue(profile.onOptionsItemSelected(enrollmentItem));
        verify(profile).startActivity(any(Intent.class));
        verify(profile, never()).startMarpScreening(anyInt());
    }

    @Test
    public void missingOrInvalidBirthDateHidesAction() {
        setDateOfBirth("");
        profile.updateMarpEnrollmentVisibility(enrollmentItem, true);

        MenuItem invalidDobItem = mock(MenuItem.class);
        setDateOfBirth("unknown");
        profile.updateMarpEnrollmentVisibility(invalidDobItem, true);

        verify(enrollmentItem).setVisible(false);
        verify(invalidDobItem).setVisible(false);
    }

    @Test
    public void disabledMarpFeatureHidesActionForEligibleClient() {
        setDateOfBirth("2011-11-23");

        profile.updateMarpEnrollmentVisibility(enrollmentItem, false);

        verify(enrollmentItem).setVisible(false);
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
