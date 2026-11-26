package org.smartregister.chw.hf.interactor;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.utils.MarpEnrollmentDate;
import org.smartregister.chw.hf.actionhelper.kvp.KvpPrepPepActionHelper;
import org.smartregister.chw.hf.utils.MarpPep;
import org.smartregister.chw.hf.utils.MarpVisitStatus;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.contract.BaseKvpVisitContract;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.interactor.BaseKvpVisitInteractor;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.VisitUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.util.JsonFormUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import timber.log.Timber;

/** Facility cards submit separately. Use the same-day Biomedical visit as the PEP record. */
public abstract class MarpPepVisitInteractor extends BaseKvpVisitInteractor {
    static final String BIOMEDICAL = Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT;
    static final String STRUCTURAL = Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT;

    private final String peerType;
    private Visit openedVisit;
    private Visit peerVisit;
    private Date visitDate;
    protected Event originalEvent;
    protected Event peerEvent;
    protected BaseKvpVisitAction pepAction;
    protected String reviewTrigger;
    protected String reviewedTrigger;
    protected boolean pepSaved;

    protected MarpPepVisitInteractor(String type, String peerType) {
        super(type);
        this.peerType = peerType;
    }

    @Override
    protected void getDetailsOnEdit(BaseKvpVisitContract.View view, MemberObject member) {
        String memberId = member.getBaseEntityId();
        openedVisit = view.getEditMode() ? visitRepository().getLatestVisit(memberId, getEncounterType()) : null;
        visitDate = openedVisit == null ? new Date() : openedVisit.getDate();
        if (openedVisit != null) {
            details = visitDetails(openedVisit);
            originalEvent = readEvent(openedVisit);
        }
        if (openedVisit == null || sameSnapshot(openedVisit, onlyVisit(sameDayVisits(memberId, getEncounterType())))) {
            peerVisit = onlyVisit(sameDayVisits(memberId, peerType));
            peerEvent = readEvent(peerVisit);
        }
    }

    static Map<String, List<VisitDetail>> visitDetails(Visit visit) {
        return visit == null ? null : VisitUtils.getVisitGroups(
                KvpLibrary.getInstance().visitDetailsRepository().getVisits(visit.getVisitId()));
    }

    protected Map<String, List<VisitDetail>> peerDetails() {
        return visitDetails(peerVisit);
    }

    static List<Visit> visitsOnDay(List<Visit> visits, Date day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(day);
        List<Visit> matches = new ArrayList<>();
        for (Visit visit : visits) {
            if (MarpVisitStatus.isOnDate(visit.getDate(), calendar)) {
                matches.add(visit);
            }
        }
        return matches;
    }

    private List<Visit> sameDayVisits(String memberId, String encounterType) {
        return visitsOnDay(visitRepository().getVisits(memberId, encounterType), visitDate);
    }

    private static Visit onlyVisit(List<Visit> visits) {
        return visits.size() == 1 ? visits.get(0) : null;
    }

    static boolean sameSnapshot(Visit first, Visit second) {
        return first == second || (first != null && second != null
                && Objects.equals(first.getVisitId(), second.getVisitId())
                && Objects.equals(first.getFormSubmissionId(), second.getFormSubmissionId())
                && Objects.equals(first.getJson(), second.getJson()));
    }

    static Event readEvent(Visit visit) {
        if (visit == null) {
            return null;
        }
        try {
            return new Gson().fromJson(visit.getJson(), Event.class);
        } catch (JsonParseException e) {
            Timber.e(e, "Unable to read a MARP visit");
            return null;
        }
    }

    BaseKvpVisitAction buildPepAction(Map<String, List<VisitDetail>> biomedicalDetails)
            throws BaseKvpVisitAction.ValidationException, JSONException {
        String formName = Constants.KVP_BIO_MEDICAL_SERVICE_FORMS.KVP_PrEP_PEP;
        JSONObject form = FormUtils.getFormUtils().getFormJson(formName);
        JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
        JsonFormUtils.getFieldJSONObject(fields, MarpPep.PREP_ENROLMENT).put(JsonFormUtils.VALUE, false);
        pepAction = getBuilder(context.getString(R.string.kvp_pep_assesment))
                .withOptional(true)
                .withDetails(biomedicalDetails)
                .withHelper(new KvpPrepPepActionHelper())
                .withJsonPayload(form.toString())
                .withFormName(formName)
                .build();
        refreshPepReview();
        return pepAction;
    }

    String knownHivStatus(String memberId, String recorded) {
        if (StringUtils.isNotBlank(recorded)) {
            return recorded;
        }
        // An earlier locally submitted Biomedical card may still be waiting for processing.
        Date date = visitDate == null ? new Date() : visitDate;
        Calendar visitDay = Calendar.getInstance();
        visitDay.setTime(date);
        return visitRepository().getVisits(memberId, BIOMEDICAL).stream()
                .filter(visit -> visit.getDate() != null && visit.getDate().before(date)
                        && !MarpVisitStatus.isOnDate(visit.getDate(), visitDay) && !MarpVisitStatus.isDraft(visit))
                .max(Comparator.comparing(Visit::getDate))
                .map(visit -> MarpEnrollmentDate.observation(readEvent(visit), "hiv_status"))
                .orElse("");
    }

    public JSONObject preparePepForm(JSONObject form) throws JSONException {
        return pepAction == null ? form
                : ((KvpPrepPepActionHelper) pepAction.getKvpVisitActionHelper()).forDisplay(form);
    }

    public void onPepFormSaved() {
        if (pepAction != null && MarpPep.hasExplicitAnswer(pepAction.getJsonPayload())) {
            pepSaved = true;
            reviewedTrigger = reviewTrigger;
        }
        refreshPepReview();
    }

    protected void refreshPepReview() {
        if (pepAction != null) {
            KvpPrepPepActionHelper helper = (KvpPrepPepActionHelper) pepAction.getKvpVisitActionHelper();
            helper.setReviewNeeded(MarpPep.needsReview(reviewTrigger, reviewedTrigger), pepAction);
        }
    }

    @Override
    protected void submitVisit(boolean editMode, String memberId, Map<String, BaseKvpVisitAction> actions,
                               String parentEventType) throws Exception {
        SQLiteDatabase database = CoreChwApplication.getInstance().getRepository().getWritableDatabase();
        database.beginTransaction();
        try {
            saveCurrentCard(editMode, memberId, actions, parentEventType);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    void saveCurrentCard(boolean editMode, String memberId, Map<String, BaseKvpVisitAction> actions,
                         String parentEventType) throws Exception {
        List<Visit> currentCards = sameDayVisits(memberId, getEncounterType());
        List<Visit> otherCards = sameDayVisits(memberId, peerType);
        Visit current = editMode ? visitRepository().getLatestVisit(memberId, getEncounterType())
                : onlyVisit(currentCards);
        if (currentCards.size() > 1 || otherCards.size() > 1
                || !sameSnapshot(openedVisit, current) || !sameSnapshot(peerVisit, onlyVisit(otherCards))) {
            throw new IllegalStateException("The MARP visit changed while its card was open");
        }
        saveCard(editMode, memberId, actions, parentEventType);
    }

    protected void saveCard(boolean editMode, String memberId, Map<String, BaseKvpVisitAction> actions,
                            String parentEventType) throws Exception {
        super.submitVisit(editMode, memberId, actions, parentEventType);
    }

    @Override
    protected Visit saveVisit(boolean editMode, String memberId, String encounterType, Map<String, String> payloads,
                              String parentEventType) throws Exception {
        Visit saved = super.saveVisit(editMode, memberId, encounterType, payloads, parentEventType);
        if (saved == null || !sameSnapshot(saved, visitRepository().getVisitByVisitId(saved.getVisitId()))) {
            throw new IllegalStateException("The MARP card could not be saved");
        }
        return saved;
    }

    @Override
    protected void prepareEvent(Event event) {
        super.prepareEvent(event);
        if (event != null && visitDate != null) {
            setVisitDate(event, visitDate);
        }
    }

    static void setVisitDate(Event event, Date date) {
        // The shared History save normally replaces the date. Keep this visit paired with its other card.
        event.setEventDate(date);
        if (event.getObs() != null) {
            event.getObs().removeIf(obs -> "kvp_visit_date".equals(obs.getFormSubmissionField()));
        }
        event.addObs(new Obs("concept", "text", "kvp_visit_date", "",
                Collections.singletonList(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(date)),
                Collections.emptyList(), null, "kvp_visit_date"));
    }

    protected Visit getPeerVisit() {
        return peerVisit;
    }

    protected Date getVisitDate() {
        return visitDate;
    }
}
