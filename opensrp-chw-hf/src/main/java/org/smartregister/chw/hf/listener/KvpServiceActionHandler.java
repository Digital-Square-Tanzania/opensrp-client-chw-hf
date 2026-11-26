package org.smartregister.chw.hf.listener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

import org.smartregister.chw.hf.activity.KvpBehavioralServiceActivity;
import org.smartregister.chw.hf.activity.KvpBioMedicalServiceActivity;
import org.smartregister.chw.hf.activity.KvpOtherServiceActivity;
import org.smartregister.chw.hf.activity.KvpStructuralServiceActivity;
import org.smartregister.chw.hf.utils.MarpVisitStatus;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.domain.ServiceCard;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.handlers.BaseServiceActionHandler;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.KvpVisitsUtil;
import org.smartregister.chw.kvp.util.MarpVisitProcessing;

import java.util.Calendar;

import timber.log.Timber;

public class KvpServiceActionHandler extends BaseServiceActionHandler {
    public boolean canOfferProcessVisit(Visit candidate) {
        return candidate != null && canEnqueue(localOutcome(candidate));
    }

    static boolean canEnqueue(MarpVisitProcessing.Outcome outcome) {
        return outcome == MarpVisitProcessing.Outcome.NOT_ENQUEUED
                || outcome == MarpVisitProcessing.Outcome.ENQUEUE_INCOMPLETE;
    }

    public void confirmProcessVisit(Activity activity, String baseEntityId, Visit selected,
                                    Calendar displayedDay, boolean sameDayOnly) {
        new AlertDialog.Builder(activity)
                .setTitle(org.smartregister.chw.kvp.R.string.process_visit_title)
                .setMessage(org.smartregister.chw.kvp.R.string.process_visit_message)
                .setPositiveButton(org.smartregister.chw.kvp.R.string.yes, (dialog, which) -> {
                    if (activity.isFinishing() || activity.isDestroyed()) {
                        return;
                    }
                    try {
                        processSelectedCard(KvpLibrary.getInstance().visitRepository(), baseEntityId,
                                selected, displayedDay, Calendar.getInstance(), sameDayOnly);
                    } catch (Exception exception) {
                        Timber.e(exception, "Unable to process MARP service card");
                    }
                    activity.finish();
                })
                .setNegativeButton(org.smartregister.chw.kvp.R.string.cancel, null)
                .show();
    }

    void processSelectedCard(VisitRepository repository, String baseEntityId, Visit selected,
                             Calendar displayedDay, Calendar now, boolean sameDayOnly) throws Exception {
        if (!MarpVisitStatus.isOnDate(displayedDay.getTime(), now)) {
            return;
        }
        Visit current = MarpVisitStatus.findProcessableCard(repository, baseEntityId,
                selected.getVisitType(), sameDayOnly ? now : null);
        if (current == null || !current.getBaseEntityId().equals(selected.getBaseEntityId())
                || !current.getVisitId().equals(selected.getVisitId())
                || !current.getFormSubmissionId().equals(selected.getFormSubmissionId())
                || !current.getDate().equals(selected.getDate())
                || !canOfferProcessVisit(current)) {
            return;
        }
        processCard(current);
    }

    MarpVisitProcessing.Outcome localOutcome(Visit visit) {
        return MarpVisitProcessing.getLocalOutcome(visit);
    }

    void processCard(Visit visit) throws Exception {
        KvpVisitsUtil.manualProcessVisit(visit);
    }

    @Override
    protected void startVisitActivity(Context context, ServiceCard serviceCard, String baseEntityId) {
        String eventName = serviceCard.getServiceEventName();
        VisitRepository repository = KvpLibrary.getInstance().visitRepository();
        if (MarpVisitStatus.read(repository, baseEntityId, Calendar.getInstance()).hasSubmittedCardToday(eventName)) {
            return;
        }
        Visit latest = repository.getLatestVisit(baseEntityId, eventName);
        boolean isEditMode = MarpVisitStatus.isDraft(latest);
        if (isEditMode && (Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT.equals(eventName)
                || Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT.equals(eventName))) {
            isEditMode = MarpVisitStatus.isOnDate(latest.getDate(), Calendar.getInstance());
        }
        if (serviceCard.getServiceId().equals(Constants.SERVICES.KVP_BIO_MEDICAL)) {
            KvpBioMedicalServiceActivity.startKvpBioMedicalServiceActivity((Activity) context, baseEntityId, isEditMode);
            return;
        }
        else if (serviceCard.getServiceId().equals(Constants.SERVICES.KVP_BEHAVIORAL)) {
            KvpBehavioralServiceActivity.startKvpBehavioralServiceActivity((Activity) context, baseEntityId, isEditMode);
            return;
        }
        else if (serviceCard.getServiceId().equals(Constants.SERVICES.KVP_STRUCTURAL)) {
            KvpStructuralServiceActivity.startKvpStructuralServiceActivity((Activity) context, baseEntityId, isEditMode);
            return;
        }
        else if (serviceCard.getServiceId().equals(Constants.SERVICES.KVP_OTHERS)) {
            KvpOtherServiceActivity.startKvpOtherServiceActivity((Activity) context, baseEntityId, isEditMode);
            return;
        }
        Toast.makeText(context, serviceCard.getServiceName() + "Loading activity... ", Toast.LENGTH_SHORT).show();
    }
}
