package org.smartregister.chw.hf.interactor;

import androidx.sqlite.db.SupportSQLiteDatabase;

import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.actionhelper.kvp.KvpHealthEducationHelper;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.contract.BaseKvpVisitContract;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.interactor.BaseKvpVisitInteractor;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.kvp.repository.VisitDetailsRepository;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class KvpBehavioralServiceInteractor extends BaseKvpVisitInteractor {

    private static final String IEC_SBCC_COUNT = "iec_sbcc_materials";

    private final String historyVisitId;
    private Visit openedVisit;
    private Event originalEvent;

    public KvpBehavioralServiceInteractor(String historyVisitId) {
        super(Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT);
        this.historyVisitId = historyVisitId;
    }

    @Override
    protected void getDetailsOnEdit(BaseKvpVisitContract.View view, MemberObject member) {
        openedVisit = view.getEditMode() ? visitRepository().getLatestVisit(member.getBaseEntityId(), getEncounterType()) : null;
        if (historyVisitId != null && (openedVisit == null || !historyVisitId.equals(openedVisit.getVisitId()))) {
            openedVisit = null;
        }
        details = MarpPepVisitInteractor.visitDetails(openedVisit);
    }

    @Override
    protected void populateActionList(BaseKvpVisitContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {
            try {
                evaluateHealthEducation(details);
            } catch (BaseKvpVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        };

        appExecutors.diskIO().execute(runnable);
    }

    private void evaluateHealthEducation(Map<String, List<VisitDetail>> details) throws BaseKvpVisitAction.ValidationException {

        KvpHealthEducationHelper actionHelper = new KvpHealthEducationHelper();
        BaseKvpVisitAction action = getBuilder(context.getString(R.string.kvp_health_education))
                .withOptional(false)
                .withDetails(details)
                .withHelper(actionHelper)
                .withFormName(Constants.KVP_BEHAVIORAL_SERVICE_FORMS.KVP_HEALTH_EDUCATION)
                .build();

        actionList.put(context.getString(R.string.kvp_health_education), action);
    }

    @Override
    protected void submitVisit(boolean editMode, String memberId, Map<String, BaseKvpVisitAction> actions,
                               String parentEventType) throws Exception {
        if (!editMode) {
            super.submitVisit(false, memberId, actions, parentEventType);
            return;
        }

        SupportSQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            Visit latest = visitRepository().getLatestVisit(memberId, getEncounterType());
            originalEvent = MarpPepVisitInteractor.readEvent(openedVisit);
            if (openedVisit == null || !memberId.equals(openedVisit.getBaseEntityId())
                    || originalEvent == null || !memberId.equals(originalEvent.getBaseEntityId())
                    || !MarpPepVisitInteractor.sameSnapshot(openedVisit, latest)) {
                throw new IllegalStateException("The MARP Behavioral visit changed while its card was open");
            }
            super.submitVisit(true, memberId, actions, parentEventType);
            database.setTransactionSuccessful();
        } finally {
            originalEvent = null;
            database.endTransaction();
        }
    }

    protected SupportSQLiteDatabase getWritableDatabase() {
        return CoreChwApplication.getInstance().getRepository().getWritableDatabase();
    }

    @Override
    protected Visit saveVisit(boolean editMode, String memberId, String encounterType, Map<String, String> payloads,
                              String parentEventType) throws Exception {
        Visit saved = super.saveVisit(editMode, memberId, encounterType, payloads, parentEventType);
        if (editMode && (saved == null
                || !MarpPepVisitInteractor.sameSnapshot(saved, visitRepository().getVisitByVisitId(saved.getVisitId())))) {
            throw new IllegalStateException("The MARP Behavioral visit could not be saved");
        }
        return saved;
    }

    @Override
    protected void prepareEvent(Event event) {
        super.prepareEvent(event);
        if (event == null || originalEvent == null) {
            return;
        }

        MarpPepVisitInteractor.setVisitDate(event, openedVisit.getDate());
        // Shared History submit replaces the old event. Carry its retired observation into the replacement.
        if (originalEvent.getObs() != null) {
            for (Obs obs : originalEvent.getObs()) {
                if (IEC_SBCC_COUNT.equals(obs.getFormSubmissionField())) {
                    event.addObs(obs);
                }
            }
        }
    }

    @Override
    protected void saveVisitDetails(Visit visit, String payloadType, String payloadDetails) {
        List<VisitDetail> historicalCounts = originalEvent == null || details == null ? null : details.get(IEC_SBCC_COUNT);
        if (historicalCounts == null || historicalCounts.isEmpty()) {
            super.saveVisitDetails(visit, payloadType, payloadDetails);
            return;
        }

        // Restore the existing rows after the shared replacement deletes the visit's old details.
        visit.getVisitDetails().remove(IEC_SBCC_COUNT);
        super.saveVisitDetails(visit, payloadType, payloadDetails);
        VisitDetailsRepository repository = KvpLibrary.getInstance().visitDetailsRepository();
        historicalCounts.forEach(repository::addVisitDetails);
        visit.getVisitDetails().put(IEC_SBCC_COUNT, historicalCounts);
    }

    @Override
    protected String getEncounterType() {
        return Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT;
    }

    @Override
    protected String getTableName() {
        return Constants.TABLES.KVP_FOLLOW_UP;
    }

}
