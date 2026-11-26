package org.smartregister.chw.hf.interactor;

import com.google.gson.Gson;

import org.json.JSONException;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.actionhelper.kvp.KvpGbvAnalysisActionHelper;
import org.smartregister.chw.hf.dao.HfKvpDao;
import org.smartregister.chw.hf.utils.MarpEnrollmentDate;
import org.smartregister.chw.hf.utils.MarpPep;
import org.smartregister.chw.hf.utils.MarpVisitStatus;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.contract.BaseKvpVisitContract;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.KvpJsonFormUtils;
import org.smartregister.chw.kvp.util.NCUtil;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.repository.AllSharedPreferences;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import timber.log.Timber;

public class KvpStructuralServiceInteractor extends MarpPepVisitInteractor {
    private final KvpGbvAnalysisActionHelper gbvHelper = new KvpGbvAnalysisActionHelper();
    private boolean sexualViolence;
    private boolean pepEligible;

    public KvpStructuralServiceInteractor() {
        super(STRUCTURAL, BIOMEDICAL);
    }

    @Override
    protected void populateActionList(BaseKvpVisitContract.InteractorCallBack callBack) {
        appExecutors.diskIO().execute(() -> {
            try {
                String memberId = memberObject.getBaseEntityId();
                pepEligible = MarpPep.eligible(knownHivStatus(memberId, HfKvpDao.getHivStatus(memberId)),
                        MarpEnrollmentDate.observation(peerEvent, "hiv_status"),
                        KvpDao.getPrEPMember(memberId) != null);
                reviewTrigger = MarpPep.eventDetail(originalEvent, MarpPep.REVIEW_TRIGGER);
                reviewedTrigger = MarpPep.eventDetail(peerEvent, MarpPep.REVIEWED_TRIGGER);

                String title = context.getString(R.string.kvp_gbv_analysis);
                actionList.put(title, getBuilder(title)
                        .withOptional(false)
                        .withDetails(details)
                        .withHelper(gbvHelper)
                        .withFormName(Constants.KVP_STRUCTURAL_AND_OTHER_SERVICES_FORMS.KVP_GBV_ANALYSIS)
                        .build());
                sexualViolence = gbvHelper.hasSexualViolence();
                if (pepEligible) {
                    buildPepAction(peerDetails());
                    updatePepVisibility();
                }
            } catch (BaseKvpVisitAction.ValidationException | JSONException e) {
                Timber.e(e, "Unable to load MARP Structural Services");
            }
            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        });
    }

    public LinkedHashMap<String, BaseKvpVisitAction> onGbvFormSaved() {
        boolean selected = gbvHelper.hasSexualViolence();
        String need = pepSaved ? MarpPep.payloadAnswer(pepAction.getJsonPayload(), MarpPep.NEED)
                : MarpEnrollmentDate.observation(peerEvent, MarpPep.NEED);
        reviewTrigger = MarpPep.reviewTriggerAfterScreening(sexualViolence, selected && pepEligible, need,
                reviewTrigger);
        sexualViolence = selected;
        updatePepVisibility();
        return actionList;
    }

    private void updatePepVisibility() {
        String title = context.getString(R.string.kvp_pep_assesment);
        if (sexualViolence && pepAction != null) {
            refreshPepReview();
            actionList.put(title, pepAction);
        } else {
            actionList.remove(title);
        }
    }

    @Override
    protected void saveCard(boolean editMode, String memberId, Map<String, BaseKvpVisitAction> actions,
                            String parentEventType) throws Exception {
        Map<String, BaseKvpVisitAction> structuralActions = new LinkedHashMap<>(actions);
        structuralActions.remove(context.getString(R.string.kvp_pep_assesment));
        super.saveCard(editMode, memberId, structuralActions, parentEventType);
        if (pepSaved) {
            saveBiomedicalAssessment(memberId);
        }
    }

    private void saveBiomedicalAssessment(String memberId) throws Exception {
        Visit existing = getPeerVisit();
        AllSharedPreferences preferences = KvpLibrary.getInstance().context().allSharedPreferences();
        Event assessment = KvpJsonFormUtils.processVisitJsonForm(preferences, memberId, BIOMEDICAL,
                Collections.singletonMap(pepAction.getTitle(), pepAction.getJsonPayload()), getTableName());
        Event biomedical = existing == null ? assessment : readEvent(existing);
        if (assessment == null || biomedical == null) {
            throw new IllegalStateException("The shared PEP assessment could not be saved");
        }

        // With no Biomedical card yet, store only a draft. Normal Biomedical Submit completes it.
        if (existing == null) {
            setVisitDate(biomedical, getVisitDate());
        } else {
            MarpPep.replaceAnswers(assessment, biomedical);
            biomedical.setEventDate(existing.getDate());
        }
        MarpPep.setEventDetail(biomedical, MarpPep.REVIEWED_TRIGGER, reviewedTrigger);

        if (existing == null || MarpVisitStatus.isDraft(existing)) {
            biomedical.setFormSubmissionId(null);
        } else {
            biomedical.setFormSubmissionId(KvpJsonFormUtils.generateRandomUUIDString());
            // Use the same replacement mechanism as a normal History edit after processing.
            if (Boolean.TRUE.equals(existing.getProcessed())) {
                deleteProcessedVisit(existing.getVisitId(), memberId);
            }
        }
        KvpJsonFormUtils.tagEvent(preferences, biomedical);

        String visitId = existing == null ? KvpJsonFormUtils.generateRandomUUIDString() : existing.getVisitId();
        Visit updated = NCUtil.eventToVisit(biomedical, visitId);
        updated.setPreProcessedJson(new Gson().toJson(biomedical));
        if (existing != null) {
            updated.setParentVisitID(existing.getParentVisitID());
            updated.setVisitGroup(existing.getVisitGroup());
            updated.setCreatedAt(existing.getCreatedAt());
            visitRepository().deleteVisit(visitId);
        }
        visitRepository().addVisit(updated);
        if (!sameSnapshot(updated, visitRepository().getVisitByVisitId(visitId))) {
            throw new IllegalStateException("The shared PEP assessment could not be stored");
        }
        saveVisitDetails(updated, BaseKvpVisitAction.PayloadType.JSON.name(), null);
    }

    @Override
    protected void prepareEvent(Event event) {
        super.prepareEvent(event);
        if (event != null) {
            MarpPep.setEventDetail(event, MarpPep.REVIEW_TRIGGER, reviewTrigger);
        }
    }

    @Override
    protected String getEncounterType() {
        return STRUCTURAL;
    }

    @Override
    protected String getTableName() {
        return Constants.TABLES.KVP_FOLLOW_UP;
    }
}
