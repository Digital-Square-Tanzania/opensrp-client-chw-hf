package org.smartregister.chw.hf.activity;

import static org.smartregister.chw.cecap.util.Constants.EVENT_TYPE.CECAP_FOLLOW_UP_VISIT;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
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
import org.smartregister.chw.hf.interactor.HtsMedicalHistoryInteractor;
import org.smartregister.chw.hts.domain.MemberObject;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class HtsMedicalHistoryActivity extends CoreAncMedicalHistoryActivity {
    private static MemberObject htsMemberObject;

    private final Flavor flavor = new HtsMedicalHistoryActivityFlv();

    private ProgressBar progressBar;

    public static void startMe(Activity activity, MemberObject memberObject) {
        Intent intent = new Intent(activity, HtsMedicalHistoryActivity.class);
        htsMemberObject = memberObject;
        activity.startActivity(intent);
    }

    @Override
    public void initializePresenter() {
        presenter = new BaseAncMedicalHistoryPresenter(new HtsMedicalHistoryInteractor(), this, htsMemberObject.getBaseEntityId());
    }

    @Override
    public void setUpView() {
        linearLayout = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.linearLayoutMedicalHistory);
        progressBar = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.progressBarMedicalHistory);

        TextView tvTitle = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.tvTitle);
        tvTitle.setText(getString(org.smartregister.chw.opensrp_chw_anc.R.string.back_to, htsMemberObject.getFullName()));

        ((TextView) findViewById(R.id.medical_history)).setText(getString(R.string.visits_history));
    }

    @Override
    public View renderView(List<Visit> visits) {
        super.renderView(visits);
        View view = flavor.bindViews(this);
        displayLoadingState(true);
        flavor.processViewData(visits, this);
        displayLoadingState(false);
        TextView cecapVisitTitle = view.findViewById(org.smartregister.chw.core.R.id.customFontTextViewHealthFacilityVisitTitle);
        cecapVisitTitle.setText(R.string.hts_visit);
        return view;
    }

    @Override
    public void displayLoadingState(boolean state) {
        progressBar.setVisibility(state ? View.VISIBLE : View.GONE);
    }

    private static class HtsMedicalHistoryActivityFlv extends DefaultAncMedicalHistoryActivityFlv {
        private final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);

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

            if (!visits.isEmpty()) {
                int days = 0;
                List<LinkedHashMap<String, String>> hf_visits = new ArrayList<>();

                int x = 0;
                while (x < visits.size()) {
                    LinkedHashMap<String, String> visitDetails = new LinkedHashMap<>();
                    // the first object in this list is the days difference
                    if (x == 0) {
                        days = Days.daysBetween(new DateTime(visits.get(visits.size() - 1).getDate()), new DateTime()).getDays();
                    }

                    String[] visitTypeParams = {
                            "hts_visit_type", "hts_has_the_client_recently_tested_with_hivst", "hts_previous_hivst_client_type", "hts_previous_hivst_test_type", "hts_client_type",
                            "hts_testing_approach", "hts_pitc_testing_point", "hts_other_testing_point", "hts_has_pre_test_counselling_been_provided", "hts_type_of_counselling_provided", "hts_clients_tb_screening_outcome",
                            "hts_does_client_need_hiv_self_test_kits", "hts_final_hiv_status", "hts_repeat_first_test_kit_batch_number", "hts_repeat_first_test_kit_expire_date",
                            "hts_repeat_first_hiv_test_result", "hts_post_test_services", "hts_hiv_results_disclosure", "hts_preventive_services", "hts_were_condoms_distributed", "hts_condom_distribution", "hts_number_of_male_condoms_provided", "hts_number_of_female_condoms_provided",
                            "hts_sample_collection_for_dna_pcr_test", "hts_type_of_sample_collected", "hts_dbs_kit_batch_number", "hts_dbs_expire_date"
                    };
                    extractVisitDetails(visits, visitTypeParams, visitDetails, x, context);
                    String[] htsFirstTestParams = {
                            "hts_type_of_test_used", "hts_kit_batch_number", "hts_kit_expire_date", "hts_first_hiv_test_result", "syphilis_test_results"
                    };
                    extractVisitDetails(visits, htsFirstTestParams, visitDetails, x, context);
                    String[] htsSecondTestParams = {
                            "hts_second_kit_batch_number", "hts_second_kit_expire_date", "hts_second_hiv_test_result"
                    };
                    extractVisitDetails(visits, htsSecondTestParams, visitDetails, x, context);
                    String[] htsUnigoldTestParams = {
                            "hts_unigold_kit_batch_number", "hts_unigold_kit_expire_date", "hts_unigold_hiv_test_result"
                    };
                    extractVisitDetails(visits, htsUnigoldTestParams, visitDetails, x, context);

                    hf_visits.add(visitDetails);

                    x++;
                }

                processLastVisit(days, context);
                processVisit(hf_visits, context, visits);
            }
        }

        private void extractVisitDetails(List<Visit> sourceVisits, String[] hf_params, LinkedHashMap<String, String> visitDetailsMap, int iteration, Context context) {
            // get the hf details
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            for (String param : hf_params) {
                try {
                    List<VisitDetail> details = sourceVisits.get(iteration).getVisitDetails().get(param);
                    map.put(param, getTexts(context, details));
                } catch (Exception e) {
                    Timber.e(e);
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
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
            if (community_visits != null && !community_visits.isEmpty()) {
                linearLayoutHealthFacilityVisit.setVisibility(View.VISIBLE);

                int x = 0;
                for (LinkedHashMap<String, String> vals : community_visits) {
                    View view = inflater.inflate(R.layout.medical_history_visit, null);
                    TextView tvTypeOfService = view.findViewById(R.id.title);
                    LinearLayout visitDetailsLayout = view.findViewById(R.id.visit_details_layout);
                    TextView tvEdit = view.findViewById(R.id.textview_edit);

                    // Updating visibility of EDIT button if the visit is the last visit
                    if ((x == visits.size() - 1))
                        tvEdit.setVisibility(View.VISIBLE);
                    else
                        tvEdit.setVisibility(View.GONE);

                    tvEdit.setOnClickListener(view1 -> {
                        Visit visit = visits.get(0);

                        if (visit.getBaseEntityId() != null) {
                            ((Activity) context).finish();
                            HtsVisitActivity.startMe((Activity) context, visit.getBaseEntityId(), null, true);
                        }
                    });

                    String visitType;

                    if (CECAP_FOLLOW_UP_VISIT.equals(visits.get(x).getVisitType())) {
                        visitType = context.getString(R.string.cecap_visit);
                    } else {
                        visitType = visits.get(x).getVisitType();
                    }
                    tvTypeOfService.setText(visitType + " - " + simpleDateFormat.format(visits.get(x).getDate()));


                    for (LinkedHashMap.Entry<String, String> entry : vals.entrySet()) {
                        TextView visitDetailTv = new TextView(context);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams
                                (LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

                        visitDetailTv.setLayoutParams(params);
                        float scale = context.getResources().getDisplayMetrics().density;
                        int dpAsPixels = (int) (10 * scale + 0.5f);
                        visitDetailTv.setPadding(dpAsPixels, 0, 0, 0);
                        visitDetailsLayout.addView(visitDetailTv);


                        try {
                            int resource = context.getResources().getIdentifier("hts_" + entry.getKey(), "string", context.getPackageName());
                            evaluateView(context, vals, visitDetailTv, entry.getKey(), resource);
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }
                    linearLayoutHealthFacilityVisitDetails.addView(view, 0);

                    x++;
                }
            }
        }

        private void evaluateView(Context context, Map<String, String> vals, TextView tv, String valueKey, int viewTitleStringResource) {
            if (StringUtils.isNotBlank(getMapValue(vals, valueKey))) {
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                spannableStringBuilder.append(context.getString(viewTitleStringResource), boldSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE).append("\n");

                String stringValue = getMapValue(vals, valueKey);
                String[] stringValueArray;
                if (stringValue.contains(",")) {
                    stringValueArray = stringValue.split(",");
                    for (String value : stringValueArray) {
                        spannableStringBuilder.append(getStringResource(context, value.trim()) + "\n", new BulletSpan(10), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                } else {
                    spannableStringBuilder.append(getStringResource(context, stringValue)).append("\n");
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

        private String getStringResource(Context context, String resourceName) {
            int resourceId = context.getResources().
                    getIdentifier("hts_" + resourceName.trim(), "string", context.getPackageName());
            try {
                return context.getString(resourceId);
            } catch (Exception e) {
                Timber.e(e);
                return resourceName;
            }
        }
    }
}
