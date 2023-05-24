package org.smartregister.chw.hf.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.presenter.BaseAncMedicalHistoryPresenter;
import org.smartregister.chw.core.activity.CoreAncMedicalHistoryActivity;
import org.smartregister.chw.core.activity.DefaultAncMedicalHistoryActivityFlv;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.interactor.VmmcMedicalHistoryInteractor;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class VmmcMedicalHistoryActivity extends CoreAncMedicalHistoryActivity {
    private static org.smartregister.chw.vmmc.domain.MemberObject vmmcMemberObject;
    private final Flavor flavor = new VmmcMedicalHistoryActivityFlv();
    private ProgressBar progressBar;

//    public static void startMe(Activity activity, MemberObject memberObject) {
//        Intent intent = new Intent(activity, VmmcMedicalHistoryActivity.class);
//        vmmcMemberObject = memberObject;
//        activity.startActivity(intent);
//    }

    public static void startMe(VmmcProfileActivity vmmcProfileActivity, org.smartregister.chw.vmmc.domain.MemberObject memberObject) {
        Intent intent = new Intent(vmmcProfileActivity, VmmcMedicalHistoryActivity.class);
        vmmcMemberObject = memberObject;
        vmmcProfileActivity.startActivity(intent);
    }

    @Override
    public void initializePresenter() {
        presenter = new BaseAncMedicalHistoryPresenter(new VmmcMedicalHistoryInteractor(), this, vmmcMemberObject.getBaseEntityId());
    }

    @Override
    public void setUpView() {
        linearLayout = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.linearLayoutMedicalHistory);
        progressBar = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.progressBarMedicalHistory);

        TextView tvTitle = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.tvTitle);
        tvTitle.setText(getString(org.smartregister.chw.opensrp_chw_anc.R.string.back_to, vmmcMemberObject.getFullName()));

        ((TextView) findViewById(R.id.medical_history)).setText(getString(R.string.visits_history));
    }

    @Override
    public View renderView(List<Visit> visits) {
        super.renderView(visits);
        View view = flavor.bindViews(this);
        displayLoadingState(true);
        flavor.processViewData(visits, this);
        displayLoadingState(false);
        TextView agywVisitTitle = view.findViewById(org.smartregister.chw.core.R.id.customFontTextViewHealthFacilityVisitTitle);
        agywVisitTitle.setText(R.string.visits_history);
        return view;
    }

    @Override
    public void displayLoadingState(boolean state) {
        progressBar.setVisibility(state ? View.VISIBLE : View.GONE);
    }

    private static class VmmcMedicalHistoryActivityFlv extends DefaultAncMedicalHistoryActivityFlv {
        private final StyleSpan boldSpan = new StyleSpan(android.graphics.Typeface.BOLD);

        @Override
        protected void processAncCard(String has_card, Context context) {
            // super.processAncCard(has_card, context);
            linearLayoutAncCard.setVisibility(View.GONE);
        }

        @Override
        protected void processHealthFacilityVisit(List<Map<String, String>> hf_visits, Context context) {
            //super.processHealthFacilityVisit(hf_visits, context);
        }

        @Override
        public void processViewData(List<Visit> visits, Context context) {

            if (visits.size() > 0) {
                int days = 0;
                List<LinkedHashMap<String, String>> hf_visits = new ArrayList<>();

                int x = 0;
                while (x < visits.size()) {
                    LinkedHashMap<String, String> visitDetails = new LinkedHashMap<>();

                    // the first object in this list is the days difference
                    if (x == 0) {
                        days = Days.daysBetween(new DateTime(visits.get(visits.size() - 1).getDate()), new DateTime()).getDays();
                    }

                    String[] medicalHistoryParams = {"has_client_had_any_sti", "any_complaints","client_diagnosed","diabetes_treatment","surgical_procedure","type_complication","hematological_disease","known_allergies","tetanus_vaccination"};
                    extractVisitDetails(visits, medicalHistoryParams, visitDetails, x, context);

                    String[] physicalExamParams = {"physical_abnormality", "client_weight","pulse_rate","systolic","diastolic","temperature","respiration_rate","genital_examination","preferred_client","penile_size"};
                    extractVisitDetails(visits, physicalExamParams, visitDetails, x, context);

                    String[] htsParams = {"tested_hiv", "hiv_result","hiv_reasons","hiv_viral_load_text","self_test_kits","client_referred_to","client_medically_cleared","mc_reasons","uds_managed","smegma_managed","gud_managed","phimosis_managed","paraphimosis_managed","underscended_managed","condylomata_managed","adhesion_managed","balanitis_managed","urethral_managed","chordae_managed","hydrocele_managed"};
                    extractVisitDetails(visits, htsParams, visitDetails, x, context);

                    String[] consentForParams = {"client_consent_for_mc_procedure", "consent_form","health_care_provider"};
                    extractVisitDetails(visits, consentForParams, visitDetails, x, context);

                    String[] mcProcedureParams = {"is_male_procedure_circumcision_conducted", "start_time","end_time","aneathesia_administered","other_aneathesia_administered","lignocaine_dosage","bupivacaine_dosage","dosage","male_circumcision_method","device_name","lot_number","select_size_place","surgeon_name","surgeons_cadre","assistant_name","assistant_cadre","intraoperative_adverse_event_occured","type_of_adverse_event","type_of_adverse_event_others","desc_intraoperative_ae_bleed","desc_intraoperative_ae_skin_removal","desc_intraoperative_ae_injury_to_penis","desc_intraoperative_ae_anaesthetic_related_event","nature_of_ae","what_done","treatment_outcome"};
                    extractVisitDetails(visits, mcProcedureParams, visitDetails, x, context);

                    String[] postOpParams = {"dressing_condition"};
                    extractVisitDetails(visits, postOpParams, visitDetails, x, context);

                    String[] dischargeParams = {"discharge_condition","discharged_reasons","analgesics_given","analgenics_type","analgenics_dosage","analgenics_reasons","discharge_time","discharging_name","provider_cadre"};
                    extractVisitDetails(visits, dischargeParams, visitDetails, x, context);

//                    String[] postOP = {"has_client_had_any_sti", "any_complaints","client_diagnosed","diabetes_treatment","surgical_procedure","type_complication","hematological_disease","known_allergies","tetanus_vaccination"};
//                    extractVisitDetails(visits, medicalHistoryParams, visitDetails, x, context);


                    hf_visits.add(visitDetails);

                    x++;
                }

                processLastVisit(days, context);
                processVisit(hf_visits, context, visits);
            }
        }

        private void extractVisitDetails(List<Visit> sourceVisits, String[] hf_params, LinkedHashMap<String, String> visitDetailsMap, int iteration, Context context) {
            // get the hf details
            Map<String, String> map = new HashMap<>();
            for (String param : hf_params) {
                try {
                    List<VisitDetail> details = sourceVisits.get(iteration).getVisitDetails().get(param);
                    map.put(param, getTexts(context, details));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            visitDetailsMap.putAll(map);
        }


        private void processLastVisit(int days, Context context) {
            linearLayoutLastVisit.setVisibility(View.VISIBLE);
            if (days < 1) {
                customFontTextViewLastVisit.setText(org.smartregister.chw.core.R.string.less_than_twenty_four);
            } else {
                customFontTextViewLastVisit.setText(StringUtils.capitalize(MessageFormat.format(context.getString(org.smartregister.chw.core.R.string.days_ago), String.valueOf(days))));
            }
        }


        protected void processVisit(List<LinkedHashMap<String, String>> community_visits, Context context, List<Visit> visits) {
            if (community_visits != null && community_visits.size() > 0) {
                linearLayoutHealthFacilityVisit.setVisibility(View.VISIBLE);

                int x = 0;
                for (LinkedHashMap<String, String> vals : community_visits) {
                    View view = inflater.inflate(R.layout.medical_history_visit, null);
                    TextView tvTitle = view.findViewById(R.id.title);
                    LinearLayout visitDetailsLayout = view.findViewById(R.id.visit_details_layout);

                    tvTitle.setText(visits.get(x).getVisitType() + " " + visits.get(x).getDate());


                    for (LinkedHashMap.Entry<String, String> entry : vals.entrySet()) {
                        TextView visitDetailTv = new TextView(context);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams
                                ((int) LinearLayout.LayoutParams.MATCH_PARENT, (int) LinearLayout.LayoutParams.WRAP_CONTENT);

                        visitDetailTv.setLayoutParams(params);
                        float scale = context.getResources().getDisplayMetrics().density;
                        int dpAsPixels = (int) (10 * scale + 0.5f);
                        visitDetailTv.setPadding(dpAsPixels, 0, 0, 0);
                        visitDetailsLayout.addView(visitDetailTv);


                        try {
                            int resource = context.getResources().getIdentifier("vmmc_" + entry.getKey(), "string", context.getPackageName());
                            evaluateView(context, vals, visitDetailTv, entry.getKey(), resource, "");
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }
                    linearLayoutHealthFacilityVisitDetails.addView(view, 0);

                    x++;
                }
            }
        }

        private void evaluateView(Context context, Map<String, String> vals, TextView tv, String valueKey, int viewTitleStringResource, String valuePrefixInStringResources) {
            if (StringUtils.isNotBlank(getMapValue(vals, valueKey))) {
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                spannableStringBuilder.append(context.getString(viewTitleStringResource), boldSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE).append("\n");

                String stringValue = getMapValue(vals, valueKey);
                String[] stringValueArray;
                if (stringValue.contains(",")) {
                    stringValueArray = stringValue.split(",");
                    for (String value : stringValueArray) {
                        spannableStringBuilder.append(getStringResource(context, valuePrefixInStringResources, value.trim()) + "\n", new BulletSpan(10), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                } else {
                    spannableStringBuilder.append(getStringResource(context, valuePrefixInStringResources, stringValue)).append("\n");
                }
                tv.setText(spannableStringBuilder);
            } else {
                tv.setVisibility(View.GONE);
            }
        }


        private String getMapValue(Map<String, String> map, String key) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
            return "";
        }

        private String getStringResource(Context context, String prefix, String resourceName) {
            int resourceId = context.getResources().
                    getIdentifier(prefix + resourceName.trim(), "string", context.getPackageName());
            try {
                return context.getString(resourceId);
            } catch (Exception e) {
                Timber.e(e);
                return prefix + resourceName;
            }
        }
    }
}
