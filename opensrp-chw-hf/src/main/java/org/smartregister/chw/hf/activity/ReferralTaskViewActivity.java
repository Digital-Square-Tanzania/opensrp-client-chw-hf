package org.smartregister.chw.hf.activity;

import static org.smartregister.chw.core.utils.Utils.passToolbarTitle;
import static org.smartregister.chw.hf.utils.Constants.DIABETES_AND_HYPERTENSION_REFERRAL_FOCUS;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.smartregister.chw.core.activity.BaseReferralTaskViewActivity;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreReferralUtils;
import org.smartregister.chw.hf.BuildConfig;
import org.smartregister.chw.hf.HealthFacilityApplication;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.utils.CloseReferralDialog;
import org.smartregister.chw.hf.utils.AllClientsUtils;
import org.smartregister.chw.hf.utils.Constants;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Task;
import org.smartregister.family.FamilyLibrary;
import org.smartregister.family.util.Utils;
import org.smartregister.opd.utils.OpdDbConstants;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.BaseRepository;
import org.smartregister.sync.helper.ECSyncHelper;
import org.smartregister.util.JsonFormUtils;
import org.smartregister.view.customcontrols.CustomFontTextView;

import java.util.ArrayList;
import java.util.Date;

import timber.log.Timber;

public class ReferralTaskViewActivity extends BaseReferralTaskViewActivity implements View.OnClickListener {

    public static void startReferralTaskViewActivity(Activity activity, CommonPersonObjectClient personObjectClient, Task task, String startingActivity) {
        ReferralTaskViewActivity.personObjectClient = personObjectClient;
        Intent intent = new Intent(activity, ReferralTaskViewActivity.class);
        intent.putExtra(CoreConstants.INTENT_KEY.USERS_TASKS, task);
        intent.putExtra(CoreConstants.INTENT_KEY.CHILD_COMMON_PERSON, personObjectClient);
        intent.putExtra(CoreConstants.INTENT_KEY.STARTING_ACTIVITY, startingActivity);
        passToolbarTitle(activity, intent);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.referrals_tasks_view_layout);
        if (getIntent().getExtras() != null) {
            extraClientTask();
            extraDetails();
            setStartingActivity((String) getIntent().getSerializableExtra(CoreConstants.INTENT_KEY.STARTING_ACTIVITY));
            inflateToolbar();
            setUpViews();
        }
    }

    @Override
    protected void onCreation() {
        //overridden
    }

    @Override
    protected void onResumption() {
        //Overridden
    }

    public void setUpViews() {
        clientName = findViewById(R.id.client_name);
        careGiverName = findViewById(R.id.care_giver_name);
        childName = findViewById(R.id.child_name);
        careGiverPhone = findViewById(R.id.care_giver_phone);
        clientReferralProblem = findViewById(R.id.client_referral_problem);
        chwDetailsNames = findViewById(R.id.chw_details_names);
        referralDate = findViewById(R.id.referral_date);

        womanGaLayout = findViewById(R.id.woman_ga_layout);
        careGiverLayout = findViewById(R.id.care_giver_name_layout);
        childNameLayout = findViewById(R.id.child_name_layout);

        womanGa = findViewById(R.id.woman_ga);
        CustomFontTextView viewProfile = findViewById(R.id.view_profile);

        CustomFontTextView markAskDone = findViewById(R.id.mark_ask_done);
        markAskDone.setOnClickListener(this);

        if (getStartingActivity().equals(CoreConstants.REGISTERED_ACTIVITIES.REFERRALS_REGISTER_ACTIVITY)) {
            viewProfile.setOnClickListener(this);
        } else {
            viewProfile.setVisibility(View.INVISIBLE);
        }
        getReferralDetails();
    }

    public void setStartingActivity(String startingActivity) {
        this.startingActivity = startingActivity;
    }

    public void closeReferral() {
        closeReferralDialog();
    }

    private String hypertensionResult = "";
    private String diabetesResult = "";

    private void closeReferralDialog() {
        boolean isDiabetesOrHypertensionReferral = getTask().getFocus().equals(DIABETES_AND_HYPERTENSION_REFERRAL_FOCUS);
        if (isDiabetesOrHypertensionReferral){
            closeWithTestResults();
        } else {
            closeReferralOnly();
        }
    }

    private void closeReferralOnly(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.mark_as_done_title));
        builder.setMessage(getString(R.string.mark_as_done_message));
        builder.setCancelable(true);

        builder.setPositiveButton(this.getString(R.string.mark_done), (dialog, id) -> {
            try {
                saveCloseReferralEvent();
                completeTask();
                finish();
            } catch (Exception e) {
                Timber.e(e, "ReferralTaskViewActivity --> closeReferralDialog");
            }
        });
        builder.setNegativeButton(this.getString(R.string.cancel), ((dialog, id) -> dialog.cancel()));

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void closeWithTestResults(){
        CloseReferralDialog dialog = new CloseReferralDialog();
        dialog.setListener(new CloseReferralDialog.CloseReferralListener() {
            @Override
            public void onSubmit(String hypertension, String diabetes) {
                hypertensionResult = hypertension;
                diabetesResult = diabetes;
                try {
                    completeTask();
                    saveCloseReferralWithResultsEvent();
                    finish();
                } catch (Exception e) {
                    Timber.e(e, "ReferralTaskViewActivity --> closeReferralDialog");
                }
            }

            @Override
            public void onCancel() {
                // No action needed, dialog will dismiss
            }
        });
        dialog.show(getSupportFragmentManager(), "CloseReferralDialog");
    }

    private void saveCloseReferralEvent() {
        try {
            AllSharedPreferences sharedPreferences = Utils.getAllSharedPreferences();
            ECSyncHelper syncHelper = FamilyLibrary.getInstance().getEcSyncHelper();
            Event baseEvent = (Event) new Event()
                    .withBaseEntityId(getBaseEntityId())
                    .withEventDate(new Date())
                    .withEventType(CoreConstants.EventType.CLOSE_REFERRAL)
                    .withFormSubmissionId(JsonFormUtils.generateRandomUUIDString())
                    .withEntityType(CoreConstants.TABLE_NAME.CLOSE_REFERRAL)
                    .withProviderId(sharedPreferences.fetchRegisteredANM())
                    .withLocationId(getTask().getLocation())
                    .withTeamId(sharedPreferences.fetchDefaultTeamId(sharedPreferences.fetchRegisteredANM()))
                    .withTeam(sharedPreferences.fetchDefaultTeam(sharedPreferences.fetchRegisteredANM()))
                    .withClientDatabaseVersion(BuildConfig.DATABASE_VERSION)
                    .withClientApplicationVersion(BuildConfig.VERSION_CODE)
                    .withDateCreated(new Date());

            baseEvent.addObs((new Obs()).withFormSubmissionField(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK).withValue(getTask().getIdentifier())
                    .withFieldCode(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK).withFieldType("formsubmissionField").withFieldDataType("text").withParentCode("").withHumanReadableValues(new ArrayList<>()));
            baseEvent.addObs((new Obs()).withFormSubmissionField(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK_PREVIOUS_STATUS).withValue(getTask().getStatus())
                    .withFieldCode(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK_PREVIOUS_STATUS).withFieldType("formsubmissionField").withFieldDataType("text").withParentCode("").withHumanReadableValues(new ArrayList<>()));
            baseEvent.addObs((new Obs()).withFormSubmissionField(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK_PREVIOUS_BUSINESS_STATUS).withValue(getTask().getBusinessStatus())
                    .withFieldCode(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK_PREVIOUS_BUSINESS_STATUS).withFieldType("formsubmissionField").withFieldDataType("text").withParentCode("").withHumanReadableValues(new ArrayList<>()));

            org.smartregister.chw.hf.utils.JsonFormUtils.tagSyncMetadata(Utils.context().allSharedPreferences(), baseEvent);// tag docs

            //setting the location uuid of the referral initiator so that to allow the event to sync back to the chw app since it sync data by location.
            baseEvent.setLocationId(getTask().getLocation());

            JSONObject eventJson = new JSONObject(JsonFormUtils.gson.toJson(baseEvent));
            syncHelper.addEvent(getBaseEntityId(), eventJson);
            long lastSyncTimeStamp = HealthFacilityApplication.getInstance().getContext().allSharedPreferences().fetchLastUpdatedAtDate(0);
            Date lastSyncDate = new Date(lastSyncTimeStamp);
            HealthFacilityApplication.getClientProcessor(HealthFacilityApplication.getInstance().getContext().applicationContext()).processClient(syncHelper.getEvents(lastSyncDate, BaseRepository.TYPE_Unprocessed));
            HealthFacilityApplication.getInstance().getContext().allSharedPreferences().saveLastUpdatedAtDate(lastSyncDate.getTime());
        } catch (Exception e) {
            Timber.e(e, "ReferralTaskViewActivity --> saveCloseReferralEvent");
        }

    }
    private void saveCloseReferralWithResultsEvent() {
        try {
            AllSharedPreferences sharedPreferences = Utils.getAllSharedPreferences();
            ECSyncHelper syncHelper = FamilyLibrary.getInstance().getEcSyncHelper();
            Event baseEvent = (Event) new Event()
                    .withBaseEntityId(getBaseEntityId())
                    .withEventDate(new Date())
                    .withEventType(Constants.Events.DIABETES_AND_HYPERTENSION_SCREENING_CONFIRMATION)
                    .withFormSubmissionId(JsonFormUtils.generateRandomUUIDString())
                    .withEntityType(CoreConstants.TABLE_NAME.CLOSE_REFERRAL)
                    .withProviderId(sharedPreferences.fetchRegisteredANM())
                    .withLocationId(getTask().getLocation())
                    .withTeamId(sharedPreferences.fetchDefaultTeamId(sharedPreferences.fetchRegisteredANM()))
                    .withTeam(sharedPreferences.fetchDefaultTeam(sharedPreferences.fetchRegisteredANM()))
                    .withClientDatabaseVersion(BuildConfig.DATABASE_VERSION)
                    .withClientApplicationVersion(BuildConfig.VERSION_CODE)
                    .withDateCreated(new Date());

            baseEvent.addObs((new Obs()).withFormSubmissionField(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK).withValue(getTask().getIdentifier())
                    .withFieldCode(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK).withFieldType("formsubmissionField").withFieldDataType("text").withParentCode("").withHumanReadableValues(new ArrayList<>()));
            baseEvent.addObs((new Obs()).withFormSubmissionField(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK_PREVIOUS_STATUS).withValue(getTask().getStatus())
                    .withFieldCode(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK_PREVIOUS_STATUS).withFieldType("formsubmissionField").withFieldDataType("text").withParentCode("").withHumanReadableValues(new ArrayList<>()));
            baseEvent.addObs((new Obs()).withFormSubmissionField(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK_PREVIOUS_BUSINESS_STATUS).withValue(getTask().getBusinessStatus())
                    .withFieldCode(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK_PREVIOUS_BUSINESS_STATUS).withFieldType("formsubmissionField").withFieldDataType("text").withParentCode("").withHumanReadableValues(new ArrayList<>()));

            // Add hypertension and diabetes results to event obs
            baseEvent.addObs((new Obs()).withFormSubmissionField("hypertension_result").withValue(hypertensionResult)
                    .withFieldCode("hypertension_result").withFieldType("formsubmissionField").withFieldDataType("text").withParentCode("").withHumanReadableValues(new ArrayList<>()));
            baseEvent.addObs((new Obs()).withFormSubmissionField("diabetes_result").withValue(diabetesResult)
                    .withFieldCode("diabetes_result").withFieldType("formsubmissionField").withFieldDataType("text").withParentCode("").withHumanReadableValues(new ArrayList<>()));

            org.smartregister.chw.hf.utils.JsonFormUtils.tagSyncMetadata(Utils.context().allSharedPreferences(), baseEvent);// tag docs

            //setting the location uuid of the referral initiator so that to allow the event to sync back to the chw app since it sync data by location.
            baseEvent.setLocationId(getTask().getLocation());

            JSONObject eventJson = new JSONObject(JsonFormUtils.gson.toJson(baseEvent));
            syncHelper.addEvent(getBaseEntityId(), eventJson);
            long lastSyncTimeStamp = HealthFacilityApplication.getInstance().getContext().allSharedPreferences().fetchLastUpdatedAtDate(0);
            Date lastSyncDate = new Date(lastSyncTimeStamp);
            HealthFacilityApplication.getClientProcessor(HealthFacilityApplication.getInstance().getContext().applicationContext()).processClient(syncHelper.getEvents(lastSyncDate, BaseRepository.TYPE_Unprocessed));
            HealthFacilityApplication.getInstance().getContext().allSharedPreferences().saveLastUpdatedAtDate(lastSyncDate.getTime());
        } catch (Exception e) {
            Timber.e(e, "ReferralTaskViewActivity --> saveCloseReferralEvent");
        }

    }

    private void completeTask() {
        Task currentTask = getTask();
        currentTask.setForEntity(getBaseEntityId());
        currentTask.setStatus(Task.TaskStatus.IN_PROGRESS);
        CoreReferralUtils.completeTask(currentTask, false);
    }

    public String getBaseEntityId() {
        return baseEntityId;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.view_profile) {
            personObjectClient.getDetails().put(OpdDbConstants.KEY.REGISTER_TYPE, mapTaskFocusToRegisterType());
            AllClientsUtils.goToClientProfile(this, personObjectClient);
        } else if (view.getId() == R.id.mark_ask_done) {
            closeReferral();
        }
    }

    @NonNull
    private String mapTaskFocusToRegisterType() {
        switch (task.getFocus()) {
            case CoreConstants.TASKS_FOCUS.SICK_CHILD:
                return CoreConstants.REGISTER_TYPE.CHILD;
            case CoreConstants.TASKS_FOCUS.SUSPECTED_MALARIA:
                return CoreConstants.REGISTER_TYPE.MALARIA;
            case CoreConstants.TASKS_FOCUS.ANC_DANGER_SIGNS:
                return CoreConstants.REGISTER_TYPE.ANC;
            case CoreConstants.TASKS_FOCUS.PNC_DANGER_SIGNS:
                return CoreConstants.REGISTER_TYPE.PNC;
            case CoreConstants.TASKS_FOCUS.FP_SIDE_EFFECTS:
                return CoreConstants.REGISTER_TYPE.FAMILY_PLANNING;
            default:
                return "";
        }
    }
}