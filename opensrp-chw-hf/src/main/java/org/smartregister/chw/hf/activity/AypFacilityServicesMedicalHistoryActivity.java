package org.smartregister.chw.hf.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.presenter.BaseAncMedicalHistoryPresenter;
import org.smartregister.chw.ayp.domain.MemberObject;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.chw.core.activity.CoreAncMedicalHistoryActivity;
import org.smartregister.chw.core.activity.DefaultAncMedicalHistoryActivityFlv;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.interactor.AypFacilityServicesMedicalHistoryInteractor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class AypFacilityServicesMedicalHistoryActivity extends CoreAncMedicalHistoryActivity {

    private static MemberObject memberProfile;

    private final Flavor flavor = new AypFacilityServicesMedicalHistoryActivityFlv();

    private ProgressBar progressBar;

    public static void startMe(Activity activity, MemberObject memberObject) {
        Intent intent = new Intent(activity, AypFacilityServicesMedicalHistoryActivity.class);
        memberProfile = memberObject;
        activity.startActivity(intent);
    }

    @Override
    public void initializePresenter() {
        if (memberProfile == null) {
            Timber.w("AypFacilityServicesMedicalHistoryActivity launched without a member profile");
            return;
        }
        presenter = new BaseAncMedicalHistoryPresenter(new AypFacilityServicesMedicalHistoryInteractor(), this, memberProfile.getBaseEntityId());
    }

    @Override
    public void setUpView() {
        if (memberProfile == null) {
            Timber.w("AypFacilityServicesMedicalHistoryActivity launched without a member profile");
            finish();
            return;
        }
        linearLayout = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.linearLayoutMedicalHistory);
        progressBar = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.progressBarMedicalHistory);

        TextView tvTitle = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.tvTitle);
        String displayName = StringUtils.isNotBlank(memberProfile.getFullName()) ? memberProfile.getFullName() : getString(R.string.ayp_facility_services_register_title);
        tvTitle.setText(getString(org.smartregister.chw.opensrp_chw_anc.R.string.back_to, displayName));

        ((TextView) findViewById(R.id.medical_history)).setText(R.string.ayp_facility_visit_history);
    }

    @Override
    public View renderView(List<Visit> visits) {
        super.renderView(visits);
        View view = flavor.bindViews(this);
        displayLoadingState(true);
        flavor.processViewData(visits, this);
        displayLoadingState(false);
        TextView visitTitle = view.findViewById(org.smartregister.chw.core.R.id.customFontTextViewHealthFacilityVisitTitle);
        visitTitle.setText(R.string.ayp_facility_visit);
        return view;
    }

    @Override
    public void displayLoadingState(boolean state) {
        progressBar.setVisibility(state ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        memberProfile = null;
    }

    private static class AypFacilityServicesMedicalHistoryActivityFlv extends DefaultAncMedicalHistoryActivityFlv {

        private static final String[] FIELD_ORDER = new String[]{
                "ayp_eligibility", "enrollment_status", "enrollment_date", "uic_number",
                "enrollment_education_level", "ayp_beneficiary", "program_name", "program_name_other_specify",
                "linkage_economic_strengthening", "economic_strengthening_options", "hts_result",
                "hivst_result", "condom_distribution_male", "condom_distribution_female", "gbv_screening",
                "post_gbv_services_provided", "sti_screening_result", "sti_treatment_client",
                "sti_treatment_partner", "tb_screening_result", "pep_service_provided", "hepatitis_screening_result",
                "family_planning_method", "referral_to_other_services", "next_appointment_date", "visit_notes"
        };

        private final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());

        @Override
        protected void processAncCard(String has_card, Context context) {
            linearLayoutAncCard.setVisibility(View.GONE);
        }

        @Override
        protected void processHealthFacilityVisit(List<Map<String, String>> hf_visits, Context context) {
            // Hide default ANC-specific section
        }

        @Override
        public void processViewData(List<Visit> visits, Context context) {
            if (visits == null || visits.isEmpty()) {
                return;
            }

            int days = 0;
            List<LinkedHashMap<String, List<VisitDetail>>> visitDetailsList = new ArrayList<>();

            for (int index = 0; index < visits.size(); index++) {
                Visit visit = visits.get(index);
                if (index == 0 && visits.get(visits.size() - 1).getDate() != null) {
                    days = Days.daysBetween(new DateTime(visits.get(visits.size() - 1).getDate()), new DateTime()).getDays();
                }

                LinkedHashMap<String, List<VisitDetail>> fieldMap = new LinkedHashMap<>();
                extractVisitDetails(visit, fieldMap);
                if (!fieldMap.isEmpty()) {
                    visitDetailsList.add(fieldMap);
                }
            }

            // processLastVisit(days, context); // optional
            processVisit(visitDetailsList, context, visits);
        }

        private void extractVisitDetails(Visit visit, LinkedHashMap<String, List<VisitDetail>> destination) {
            if (visit == null || visit.getVisitDetails() == null) {
                return;
            }

            Map<String, List<VisitDetail>> groupedDetails = visit.getVisitDetails();
            for (String key : FIELD_ORDER) {
                List<VisitDetail> details = groupedDetails.get(key);
                if (details != null && !details.isEmpty()) {
                    destination.put(key, new ArrayList<>(details));
                }
            }

            for (Map.Entry<String, List<VisitDetail>> entry : groupedDetails.entrySet()) {
                if (!destination.containsKey(entry.getKey()) && entry.getValue() != null && !entry.getValue().isEmpty()) {
                    destination.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                }
            }
        }

        protected void processVisit(List<LinkedHashMap<String, List<VisitDetail>>> visitsData, Context context, List<Visit> visits) {
            if (visitsData == null || visitsData.isEmpty()) {
                return;
            }

            linearLayoutHealthFacilityVisit.setVisibility(View.VISIBLE);

            for (int index = 0; index < visitsData.size(); index++) {
                LinkedHashMap<String, List<VisitDetail>> values = visitsData.get(index);
                View view = inflater.inflate(R.layout.medical_history_visit, null);
                view.findViewById(R.id.title).setVisibility(View.GONE);

                TextView typeOfService = view.findViewById(R.id.type_of_service);
                LinearLayout detailsLayout = view.findViewById(R.id.visit_details_layout);
                TextView editView = view.findViewById(R.id.textview_edit);
                editView.setVisibility(View.GONE);

                Visit visit = visits.get(index);
                typeOfService.setText(buildVisitHeader(context, visit));

                populateVisitDetails(context, detailsLayout, values);

                linearLayoutHealthFacilityVisitDetails.addView(view, 0);
            }
        }

        private String buildVisitHeader(Context context, Visit visit) {
            String visitType = visit != null ? visit.getVisitType() : null;
            String visitDate = context.getString(R.string.ayp_visit_history_unknown_date);
            if (visit != null) {
                if (visit.getDate() != null) {
                    visitDate = dateFormat.format(visit.getDate());
                } else if (visit.getUpdatedAt() != null) {
                    visitDate = dateFormat.format(visit.getUpdatedAt());
                }
            }

            String label = resolveVisitTypeLabel(context, visitType);
            return String.format(Locale.getDefault(), "%s - %s", label, visitDate);
        }

        private String resolveVisitTypeLabel(Context context, String visitType) {
            if (StringUtils.equalsIgnoreCase(visitType, Constants.EVENT_TYPE.AYP_SERVICES)) {
                return context.getString(R.string.ayp_services_visit_type);
            }
            if (StringUtils.equalsIgnoreCase(visitType, Constants.EVENT_TYPE.AYP_FACILITY_SCREENING)) {
                return context.getString(R.string.ayp_facility_screening_visit_type);
            }
            return StringUtils.isNotBlank(visitType) ? visitType : context.getString(R.string.ayp_facility_visit);
        }

        private void populateVisitDetails(Context context, LinearLayout detailsLayout, LinkedHashMap<String, List<VisitDetail>> values) {
            detailsLayout.removeAllViews();
            for (Map.Entry<String, List<VisitDetail>> entry : values.entrySet()) {
                String key = entry.getKey();
                List<VisitDetail> visitDetails = entry.getValue();

                if (visitDetails == null || visitDetails.isEmpty()) {
                    continue;
                }

                String translatedKey = translateKey(context, key);
                String formattedValue = formatValues(visitDetails);

                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                spannableStringBuilder.append(translatedKey).append(": ");
                spannableStringBuilder.setSpan(boldSpan, 0, spannableStringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableStringBuilder.append(formattedValue);

                TextView value = new TextView(context);
                value.setText(spannableStringBuilder);
                value.setTextColor(ContextCompat.getColor(context, org.smartregister.chw.opensrp_chw_anc.R.color.black));
                value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                value.setPadding(0, context.getResources().getDimensionPixelSize(R.dimen.ayp_medical_history_item_padding), 0, 0);

                detailsLayout.addView(value);
            }
        }

        private String formatValues(List<VisitDetail> visitDetails) {
            if (visitDetails == null || visitDetails.isEmpty()) {
                return "";
            }

            List<String> values = new ArrayList<>();
            for (VisitDetail detail : visitDetails) {
                String humanReadable = detail.getHumanReadable();
                String details = detail.getDetails();
                String value = StringUtils.isNotBlank(humanReadable) ? humanReadable : details;
                if (StringUtils.isNotBlank(value)) {
                    values.add(value);
                }
            }

            if (values.isEmpty()) {
                return "";
            }

            return TextUtils.join("\n", values);
        }

        private String translateKey(Context context, String key) {
            int resourceId = context.getResources().getIdentifier(key, "string", context.getPackageName());
            if (resourceId != 0) {
                return context.getString(resourceId);
            }
            return key.replace("_", " ");
        }
    }
}
