package org.smartregister.chw.hf.interactor;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.actionhelper.vmmc.VmmcDischargeActionHelper;
import org.smartregister.chw.hf.actionhelper.vmmc.VmmcFirstVitalActionHelper;
import org.smartregister.chw.hf.actionhelper.vmmc.VmmcPostOpActionHelper;
import org.smartregister.chw.hf.actionhelper.vmmc.VmmcSecondVitalActionHelper;
import org.smartregister.chw.hf.repository.HfLocationRepository;
import org.smartregister.chw.referral.util.JsonFormConstants;
import org.smartregister.chw.vmmc.contract.BaseVmmcVisitContract;
import org.smartregister.chw.vmmc.domain.VisitDetail;
import org.smartregister.chw.vmmc.interactor.BaseVmmcVisitInteractor;
import org.smartregister.chw.vmmc.model.BaseVmmcVisitAction;
import org.smartregister.chw.vmmc.util.Constants;
import org.smartregister.domain.Location;
import org.smartregister.domain.LocationTag;

import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

public class VmmcVisitDischargeInteractor extends BaseVmmcVisitInteractor {

    String visitType;
    protected BaseVmmcVisitContract.InteractorCallBack callBack;

    public VmmcVisitDischargeInteractor(String visitType) {
        this.visitType = visitType;
    }

    @Override
    protected String getCurrentVisitType() {
        if (StringUtils.isNotBlank(visitType)) {
            return visitType;
        }
        return super.getCurrentVisitType();
    }

    @Override
    protected void populateActionList(BaseVmmcVisitContract.InteractorCallBack callBack) {
        this.callBack = callBack;
        final Runnable runnable = () -> {
            try {
                evaluatePostForm(details);
                evaluateFirstVitalProcedure(details);
                evaluateSecondVital(details);
                evaluateVmmcDischarge(details);

            } catch (BaseVmmcVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        };

        appExecutors.diskIO().execute(runnable);
    }

    private void evaluatePostForm(Map<String, List<VisitDetail>> details) throws BaseVmmcVisitAction.ValidationException {
//        JSONObject prepVisitType = FormUtils.getFormUtils().getFormJson(Constants.VMMC_FOLLOWUP_FORMS.MEDICAL_HISTORY);


        VmmcPostOpActionHelper actionHelper = new VmmcPostOpActionHelper();
        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.vmmc_post))
                .withOptional(false)
                .withDetails(details)
//                .withJsonPayload(prepVisitType.toString())
                .withHelper(actionHelper)
                .withFormName(Constants.VMMC_FOLLOWUP_FORMS.POST_OP)
                .build();
        actionList.put(context.getString(R.string.vmmc_post), action);

    }

    private void evaluateFirstVitalProcedure(Map<String, List<VisitDetail>> details) throws BaseVmmcVisitAction.ValidationException {

        VmmcFirstVitalActionHelper actionHelper = new VmmcFirstVitalActionHelper();
        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.vmmc_first_vital))
                .withOptional(false)
                .withDetails(details)
//                .withJsonPayload(prepVisitType.toString())
                .withHelper(actionHelper)
                .withFormName(Constants.VMMC_FOLLOWUP_FORMS.FIRST_VITAL_SIGN)
                .build();
        actionList.put(context.getString(R.string.vmmc_first_vital), action);
    }

    private void evaluateSecondVital(Map<String, List<VisitDetail>> details) throws BaseVmmcVisitAction.ValidationException {
//        JSONObject prepInitiation = FormUtils.getFormUtils().getFormJson(Constants.VMMC_FOLLOWUP_FORMS.HTS);

        VmmcSecondVitalActionHelper actionHelper = new VmmcSecondVitalActionHelper();
        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.vmmc_second_vital))
                .withOptional(false)
                .withDetails(details)
//                .withJsonPayload(prepVisitType.toString())
                .withHelper(actionHelper)
                .withFormName(Constants.VMMC_FOLLOWUP_FORMS.SECOND_VITAL_SIGN)
                .build();
        actionList.put(context.getString(R.string.vmmc_second_vital), action);
    }

    private void evaluateVmmcDischarge(Map<String, List<VisitDetail>> details) throws BaseVmmcVisitAction.ValidationException {
        JSONObject discharge = initializeHealthFacilitiesList(FormUtils.getFormUtils().getFormJson(Constants.VMMC_FOLLOWUP_FORMS.DISCHARGE));

        VmmcDischargeActionHelper actionHelper = new VmmcDischargeActionHelper();
        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.vmmc_post_discharge))
                .withOptional(false)
                .withDetails(details)
                .withJsonPayload(discharge.toString())
                .withHelper(actionHelper)
                .withFormName(Constants.VMMC_FOLLOWUP_FORMS.DISCHARGE)
                .build();

        actionList.put(context.getString(R.string.vmmc_post_discharge), action);
    }

    @Override
    protected String getEncounterType() {
        return Constants.EVENT_TYPE.VMMC_DISCHARGE;
    }

    @Override
    protected String getTableName() {
        return Constants.TABLES.VMMC_CONFIRMATION;
    }

//    private class VmmcMedicalHistoryTypeActionHelper extends org.smartregister.chw.hf.actionhelper.vmmc.VmmcMedicalHistoryTypeActionHelper {
//        @Override
//        public String postProcess(String s) {
//            if (StringUtils.isNotBlank(medical_history)) {
//                try {
//                    evaluatePrEPScreening(details);
//                } catch (BaseVmmcVisitAction.ValidationException e) {
//                    e.printStackTrace();
//                }
//            } else {
////                actionList.remove(context.getString(R.string.vmmc_medical_history));
////                actionList.remove(context.getString(R.string.vmmc_physical_examination));
////                actionList.remove(context.getString(R.string.vmmc_hts));
//            }
//            new AppExecutors().mainThread().execute(() -> callBack.preloadActions(actionList));
//            return super.postProcess(s);
//        }
//
//    }

//    private class VmmcPhysicalExamActionHelper extends org.smartregister.chw.hf.actionhelper.vmmc.VmmcPhysicalExamActionHelper {
//        public VmmcPhysicalExamActionHelper(String baseEntityId) {
//            super(baseEntityId);
//        }
//
//        @Override
//        public String postProcess(String s) {
//            if (should_initiate.equalsIgnoreCase("yes")) {
//                try {
//                    evaluatePrEPInitiation(details);
////                    evaluateOtherServices(details);
//                } catch (BaseVmmcVisitAction.ValidationException e) {
//                    e.printStackTrace();
//                }
//            } else {
//                actionList.remove(context.getString(R.string.prep_initiation));
//                actionList.remove(context.getString(R.string.other_services));
//            }
//            new AppExecutors().mainThread().execute(() -> callBack.preloadActions(actionList));
//            return super.postProcess(s);
//        }
//    }
public static JSONObject initializeHealthFacilitiesList(JSONObject form) {
    HfLocationRepository locationRepository = new HfLocationRepository();
    List<Location> locations = locationRepository.getAllLocationsWithTags();
    if (locations != null && form != null) {

        try {

            JSONArray fields = form.getJSONObject(org.smartregister.chw.hf.utils.Constants.JsonFormConstants.STEP1)
                    .getJSONArray(JsonFormConstants.FIELDS);

            JSONObject referralHealthFacilities = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, org.smartregister.chw.hf.utils.Constants.JsonFormConstants.NAME_OF_HF);

            JSONArray options = referralHealthFacilities.getJSONArray("options");
            String healthFacilityTagName = "Facility";
            for (Location location : locations) {
                Set<LocationTag> locationTags = location.getLocationTags();
                if (locationTags.iterator().next().getName().equalsIgnoreCase(healthFacilityTagName)) {
                    JSONObject optionNode = new JSONObject();
                    optionNode.put("text", StringUtils.capitalize(location.getProperties().getName()));
                    optionNode.put("key", StringUtils.capitalize(location.getProperties().getName()));
                    JSONObject propertyObject = new JSONObject();
                    propertyObject.put("presumed-id", location.getProperties().getUid());
                    propertyObject.put("confirmed-id", location.getProperties().getUid());
                    optionNode.put("property", propertyObject);

                    options.put(optionNode);
                }
            }
        } catch (JSONException e) {
            Timber.e(e);
        }
    }
    return form;
}
}
