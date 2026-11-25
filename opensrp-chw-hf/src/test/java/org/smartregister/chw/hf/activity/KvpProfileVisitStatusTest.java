package org.smartregister.chw.hf.activity;

import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.utils.MarpVisitStatus;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.chw.kvp.util.Constants;

import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KvpProfileVisitStatusTest {
    private static final String CLIENT = "synthetic-marp-client";
    private static final String BIOMEDICAL = Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT;
    private static final String BEHAVIORAL = Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT;
    private static final String STRUCTURAL = Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT;
    private static final String OTHER = Constants.EVENT_TYPE.KVP_OTHER_SERVICE_VISIT;
    private static final String[] CARDS = {BIOMEDICAL, BEHAVIORAL, STRUCTURAL, OTHER};

    private final VisitRepository repository = mock(VisitRepository.class);
    private final TextView record = mock(TextView.class);
    private final RelativeLayout continueVisit = mock(RelativeLayout.class);
    private TestProfile profile;
    private Calendar now;

    @Before
    public void setUp() {
        now = localTime(23, 9, 0);
        profile = mock(TestProfile.class);
        doCallRealMethod().when(profile).bindViews(record, continueVisit);
        doCallRealMethod().when(profile).setupButtons();
        doCallRealMethod().when(profile).renderMarpVisitStatus(any(MarpVisitStatus.class));
        when(profile.findViewById(anyInt())).thenAnswer(invocation -> mock(TextView.class));
        when(profile.isVisitOnProgress(Constants.PROFILE_TYPES.KVP_PROFILE))
                .thenAnswer(invocation -> readStatus().isInProgress(repository, CLIENT));
        profile.bindViews(record, continueVisit);
    }

    @Test
    public void firstVisitKeepsProvideMarpServiceAvailable() {
        renderProfile();

        verify(record).setText(R.string.record_kvp);
        verify(record).setVisibility(View.VISIBLE);
    }

    @Test
    public void unsubmittedDraftKeepsContinueAndCanStillBeEdited() {
        Visit draft = visitAt(23, 9, 0, false, null);
        setVisits(BIOMEDICAL, draft);
        when(repository.getLatestVisit(CLIENT, BIOMEDICAL)).thenReturn(draft);

        renderProfile();

        assertTrue(MarpVisitStatus.isDraft(draft));
        assertFalse(readStatus().hasSubmittedCardToday(BIOMEDICAL));
        verify(record).setText(R.string.record_kvp);
        verify(continueVisit).setVisibility(View.VISIBLE);
    }

    @Test
    public void firstCardSubmissionKeepsContinueOnReturnAndRegisterReopen() {
        setVisits(BIOMEDICAL, submittedAt(23));

        renderProfile();
        MarpVisitStatus reopenedStatus = readStatus();

        assertTrue(reopenedStatus.isInProgress(repository, CLIENT));
        assertTrue(reopenedStatus.hasSubmittedCardToday(BIOMEDICAL));
        assertFalse(reopenedStatus.hasSubmittedCardToday(BEHAVIORAL));
        verify(record).setVisibility(View.GONE);
        verify(continueVisit).setVisibility(View.VISIBLE);
        verify(continueVisit, never()).setVisibility(View.GONE);
    }

    @Test
    public void threeSubmittedCardsAndAnUnsubmittedFourthKeepTheVisitOpen() {
        setVisits(BIOMEDICAL, submittedAt(23));
        setVisits(BEHAVIORAL, submittedAt(23));
        setVisits(STRUCTURAL, submittedAt(23));
        setVisits(OTHER, visitAt(23, 9, 0, false, null));

        MarpVisitStatus status = readStatus();

        assertTrue(status.isInProgress(repository, CLIENT));
        assertFalse(status.hasSubmittedCardToday(OTHER));
        assertFalse(status.isCompleteToday());
    }

    @Test
    public void finalSubmissionAfterSeparateReturnsClosesTheOverviewAndLocksTheProfileOffline() {
        KvpServiceActivity overview = mock(KvpServiceActivity.class);
        doCallRealMethod().when(overview).initializeMainServiceContainers();
        when(overview.readMarpVisitStatus()).thenAnswer(invocation -> readStatus());

        for (String card : Arrays.asList(BIOMEDICAL, BEHAVIORAL, STRUCTURAL)) {
            setVisits(card, submittedAt(23));
            assertTrue(readStatus().isInProgress(repository, CLIENT));
            assertFalse(readStatus().isCompleteToday());
            assertTrue(readStatus().hasSubmittedCardToday(BIOMEDICAL));
        }

        setVisits(OTHER, submittedAt(23));
        overview.initializeMainServiceContainers();
        renderProfile();

        verify(overview).finish();
        assertTrue(readStatus().isCompleteToday());
        assertFalse(readStatus().isInProgress(repository, CLIENT));
        verify(record).setVisibility(View.GONE);
        verify(continueVisit, atLeastOnce()).setVisibility(View.GONE);
        for (String card : CARDS) {
            assertTrue(readStatus().hasSubmittedCardToday(card));
        }
    }

    @Test
    public void repeatedSubmissionsFromOneCardAndAnEarlierCardCannotCompleteToday() {
        Visit yesterday = submittedAt(22);
        setVisits(BIOMEDICAL, submittedAt(23), submittedAt(23));
        setVisits(BEHAVIORAL, submittedAt(23));
        setVisits(STRUCTURAL, submittedAt(23));
        setVisits(OTHER, yesterday);

        MarpVisitStatus status = readStatus();

        assertFalse(status.isCompleteToday());
        assertTrue(status.isInProgress(repository, CLIENT));
        assertFalse(status.hasSubmittedCardToday(OTHER));
        assertFalse(MarpVisitStatus.isDraft(yesterday));
    }

    @Test
    public void legacyProcessedRowsCountAndLaterDraftDoesNotEraseASubmittedCard() {
        setVisits(BIOMEDICAL, visitAt(23, 8, 0, true, null), visitAt(23, 9, 0, false, null));
        for (String card : Arrays.asList(BEHAVIORAL, STRUCTURAL, OTHER)) {
            setVisits(card, submittedAt(23));
        }

        assertTrue(readStatus().isCompleteToday());
        renderProfile();
        verify(record).setVisibility(View.GONE);
        verify(continueVisit, atLeastOnce()).setVisibility(View.GONE);
    }

    @Test
    public void nextLocalDayRestoresFollowUpOnTheSameProfileBeforeProcessing() {
        for (String card : CARDS) {
            setVisits(card, visitAt(23, 23, 58, false, "late-submission"));
        }

        now = localTime(23, 23, 59);
        assertTrue(readStatus().isCompleteToday());
        renderProfile();
        now = localTime(24, 0, 0);
        assertFalse(readStatus().isCompleteToday());
        now = localTime(24, 0, 1);
        assertFalse(readStatus().isInProgress(repository, CLIENT));
        assertFalse(readStatus().hasSubmittedCardToday(BIOMEDICAL));
        renderProfile();

        InOrder order = inOrder(record);
        order.verify(record).setVisibility(View.GONE);
        order.verify(record).setVisibility(View.VISIBLE);
        order.verify(record).setText(R.string.record_kvp_followup_visit);
    }

    @Test
    public void priorDayPartialVisitLeavesFollowUpAvailableAndItsSubmissionIntact() {
        Visit previous = submittedAt(22);
        setVisits(BIOMEDICAL, previous);
        when(repository.getLatestVisit(CLIENT, BIOMEDICAL)).thenReturn(previous);

        renderProfile();

        assertFalse(MarpVisitStatus.isDraft(previous));
        assertFalse(readStatus().hasSubmittedCardToday(BIOMEDICAL));
        verify(record).setText(R.string.record_kvp_followup_visit);
        verify(record).setVisibility(View.VISIBLE);
    }

    @Test
    public void standalonePrepDoesNotCountTowardMarpCompletion() {
        for (String card : Arrays.asList(BIOMEDICAL, BEHAVIORAL, STRUCTURAL)) {
            setVisits(card, submittedAt(23));
        }
        setVisits(Constants.EVENT_TYPE.PrEP_FOLLOWUP_VISIT, submittedAt(23));

        assertFalse(readStatus().isCompleteToday());
        assertTrue(readStatus().isInProgress(repository, CLIENT));
    }

    private void renderProfile() {
        profile.setupButtons();
        profile.renderMarpVisitStatus(readStatus());
    }

    private void setVisits(String type, Visit... visits) {
        when(repository.getVisits(CLIENT, type)).thenReturn(Arrays.asList(visits));
    }

    private MarpVisitStatus readStatus() {
        return MarpVisitStatus.read(repository, CLIENT, now);
    }

    private static Visit submittedAt(int day) {
        return visitAt(day, 8, 0, false, "card-submission");
    }

    private static Visit visitAt(int day, int hour, int minute, boolean processed, String submissionId) {
        Visit visit = new Visit();
        visit.setDate(localTime(day, hour, minute).getTime());
        visit.setProcessed(processed);
        visit.setFormSubmissionId(submissionId);
        return visit;
    }

    private static Calendar localTime(int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Africa/Dar_es_Salaam"));
        calendar.clear();
        calendar.set(2026, Calendar.NOVEMBER, day, hour, minute);
        return calendar;
    }

    private static class TestProfile extends KvpProfileActivity {
        @Override
        protected void setupButtons() {
            super.setupButtons();
        }

        void bindViews(TextView record, RelativeLayout continueVisit) {
            profileType = Constants.PROFILE_TYPES.KVP_PROFILE;
            textViewRecordKvp = record;
            visitInProgress = continueVisit;
            rlLastVisit = mock(RelativeLayout.class);
        }
    }

}
