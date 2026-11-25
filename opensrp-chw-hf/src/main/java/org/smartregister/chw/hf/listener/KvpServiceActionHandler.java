package org.smartregister.chw.hf.listener;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import org.smartregister.chw.hf.activity.KvpBehavioralServiceActivity;
import org.smartregister.chw.hf.activity.KvpBioMedicalServiceActivity;
import org.smartregister.chw.hf.activity.KvpOtherServiceActivity;
import org.smartregister.chw.hf.activity.KvpStructuralServiceActivity;
import org.smartregister.chw.hf.utils.MarpVisitStatus;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.domain.ServiceCard;
import org.smartregister.chw.kvp.handlers.BaseServiceActionHandler;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.chw.kvp.util.Constants;

import java.util.Calendar;

public class KvpServiceActionHandler extends BaseServiceActionHandler {
    @Override
    protected void startVisitActivity(Context context, ServiceCard serviceCard, String baseEntityId) {
        String eventName = serviceCard.getServiceEventName();
        VisitRepository repository = KvpLibrary.getInstance().visitRepository();
        if (MarpVisitStatus.read(repository, baseEntityId, Calendar.getInstance()).hasSubmittedCardToday(eventName)) {
            return;
        }
        boolean isEditMode = MarpVisitStatus.isDraft(repository.getLatestVisit(baseEntityId, eventName));
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
