package org.smartregister.chw.hf.actionhelper.kvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;
import static org.smartregister.chw.kvp.model.BaseKvpVisitAction.Status.COMPLETED;
import static org.smartregister.chw.kvp.model.BaseKvpVisitAction.Status.PENDING;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;
import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.kvp.util.KvpJsonFormUtils;
import org.smartregister.chw.kvp.util.NCUtil;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.family.activity.FamilyWizardFormActivity;
import org.smartregister.util.JsonFormUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class MarpOtherServicesReferralsTest {

    private static final String FORM_NAME = "kvp_other_services_and_referrals";
    private static final String SERVICES = "other_services_provided";
    private static final String REFERRALS = "other_services_referrals_provided";
    private static final String OTHER_TEXT = "others_services_provided";
    private static final String COVID_SERVICE = "covid_vaccine";
    private static final String COVID_REFERRAL = "covid_vaccination";
    private static final String LEGACY_REFERRAL = "vaccine_type";
    private static final String LEGACY_VACCINE_TEXT = "vaccine";

    @Test
    public void displaysApprovedLabelsAndRegistrationSexOptions() throws Exception {
        JSONObject male = payload(open("Male", Locale.ENGLISH, Map.of()));
        assertEquals("Other Services and Referrals", male.getJSONObject("step1").getString("title"));
        assertEquals("Other services", field(male, SERVICES).getString("label"));
        assertEquals("Referrals", field(male, REFERRALS).getString("label"));
        assertEquals("Specify other service provided", field(male, OTHER_TEXT).getString("hint"));
        assertEquals(List.of("Mental health", "COVID-19 vaccine", "Legal services", "IGA", "Other (Specify)", "None"),
                labels(male, SERVICES));
        assertEquals(List.of("Psychosocial", "COVID vaccination", "Legal services", "IGA", "PEP", "VMMC",
                "Family Planning", "Hepatitis B & C", "STI", "MAT", "None"), labels(male, REFERRALS));

        JSONObject female = payload(open("Female", new Locale("sw"), Map.of()));
        assertEquals("Huduma nyingine na Rufaa", female.getJSONObject("step1").getString("title"));
        assertEquals("Huduma nyingine zilizotolewa", field(female, SERVICES).getString("label"));
        assertEquals("Rufaa zilizotolewa", field(female, REFERRALS).getString("label"));
        assertEquals("Taja huduma nyingine iliyotolewa", field(female, OTHER_TEXT).getString("hint"));
        assertEquals(List.of("Huduma za Mama na mtoto", "Afya ya akili na msaada wa kisaikolojia", "Chanjo ya UVIKO",
                "Huduma za msaada wa kisheria", "Huduma za kuongeza kipato", "Nyingine (taja)", "Hakuna"),
                labels(female, SERVICES));
        assertEquals(List.of("Huduma za Mama na mtoto", "Afya ya akili na msaada wa kisaikolojia", "Chanjo ya UVIKO",
                "Huduma za msaada wa kisheria", "Huduma za kuongeza kipato", "Huduma ya dawa kinga PEP",
                "Uzazi wa Mpango", "Homa ya Ini B & C", "Matibabu ya magonjwa ya ngono",
                "Saratani ya shingo ya kizazi", "Huduma za MAT", "Hakuna"), labels(female, REFERRALS));

        JSONObject unknown = payload(open(null, Locale.ENGLISH, Map.of()));
        assertNull(option(unknown, SERVICES, "rch"));
        assertNull(option(unknown, REFERRALS, "rch"));
        assertNull(option(unknown, REFERRALS, "cecap"));
        assertNull(option(unknown, REFERRALS, "vmmc"));
    }

    @Test
    public void filtersIneligibleHistoryChoicesFromTheFormAndStoredObservations() throws Exception {
        Map<String, List<VisitDetail>> history = Map.of(
                SERVICES, detail("rch, other"),
                REFERRALS, detail("rch, cecap, vmmc"),
                OTHER_TEXT, detail("Synthetic service, with follow-up"));
        BaseKvpVisitAction male = open("male", Locale.ENGLISH, history);
        assertEquals(COMPLETED, male.getActionStatus());
        assertSelections(payload(male), SERVICES, "other");
        assertSelections(payload(male), REFERRALS, "vmmc");
        Map<String, List<VisitDetail>> saved = savedDetails(male);
        assertEquals("other", saved.get(SERVICES).get(0).getDetails());
        assertEquals("vmmc", saved.get(REFERRALS).get(0).getDetails());
        assertEquals("Synthetic service, with follow-up", saved.get(OTHER_TEXT).get(0).getDetails());

        BaseKvpVisitAction female = open("female", new Locale("sw"), history);
        assertEquals(COMPLETED, female.getActionStatus());
        assertSelections(payload(female), SERVICES, "rch", "other");
        assertSelections(payload(female), REFERRALS, "rch", "cecap");
    }

    @Test
    public void staleOnlyOrConflictingHistoryRequiresAValidNewChoice() throws Exception {
        BaseKvpVisitAction action = open("male", Locale.ENGLISH, Map.of(
                SERVICES, detail("rch"),
                REFERRALS, detail("none, vmmc"),
                OTHER_TEXT, detail("Stale Other")));
        assertEquals(PENDING, action.getActionStatus());
        JSONObject form = payload(action);
        assertFalse(field(form, SERVICES).has("value"));
        assertFalse(field(form, REFERRALS).has("value"));
        assertFalse(field(form, OTHER_TEXT).has("value"));

        option(form, SERVICES, "other").put("value", true);
        option(form, REFERRALS, "none").put("value", true);
        field(form, OTHER_TEXT).put("value", "  ");
        action.setJsonPayload(form.toString());
        assertEquals(PENDING, action.getActionStatus());
    }

    @Test
    public void keepsLegacyVaccinationsAndTheirTextUntilTheAnswerIsReplaced() throws Exception {
        Map<String, List<VisitDetail>> history = Map.of(
                SERVICES, detail("covid_vaccine"),
                REFERRALS, detail("vaccine_type, fp"),
                COVID_SERVICE, detail("Influenza"),
                LEGACY_VACCINE_TEXT, detail("HPV, first dose"));
        BaseKvpVisitAction action = open("female", Locale.ENGLISH, history);
        JSONObject form = payload(action);
        assertEquals(COMPLETED, action.getActionStatus());
        assertEquals("Previous vaccination (Influenza)", option(form, SERVICES, COVID_SERVICE).getString("text"));
        assertEquals("Previous vaccination (HPV, first dose)",
                option(form, REFERRALS, LEGACY_REFERRAL).getString("text"));
        assertFalse(option(form, REFERRALS, COVID_REFERRAL).optBoolean("value"));
        assertEquals("hidden", field(form, COVID_SERVICE).getString("type"));
        assertEquals("hidden", field(form, LEGACY_VACCINE_TEXT).getString("type"));

        Map<String, List<VisitDetail>> savedLegacy = savedDetails(action);
        assertEquals("Influenza", savedLegacy.get(COVID_SERVICE).get(0).getDetails());
        assertEquals("HPV, first dose", savedLegacy.get(LEGACY_VACCINE_TEXT).get(0).getDetails());
        BaseKvpVisitAction readback = open("female", Locale.ENGLISH, savedLegacy);
        assertSelections(payload(readback), REFERRALS, "fp", LEGACY_REFERRAL);

        option(form, SERVICES, COVID_SERVICE).put("value", false);
        option(form, SERVICES, "none").put("value", true);
        option(form, REFERRALS, LEGACY_REFERRAL).put("value", false);
        option(form, REFERRALS, COVID_REFERRAL).put("value", true);
        action.setJsonPayload(form.toString());
        Map<String, List<VisitDetail>> saved = savedDetails(action);
        assertFalse(saved.containsKey(COVID_SERVICE));
        assertFalse(saved.containsKey(LEGACY_VACCINE_TEXT));
        JSONObject reopened = payload(open("female", Locale.ENGLISH, saved));
        assertNull(option(reopened, REFERRALS, LEGACY_REFERRAL));
        assertSelections(reopened, REFERRALS, COVID_REFERRAL, "fp");
    }

    @Test
    public void freshCovidDoesNotInheritOrphanedLegacyText() throws Exception {
        BaseKvpVisitAction action = open("male", Locale.ENGLISH, Map.of(
                SERVICES, detail("none"), REFERRALS, detail("none"),
                COVID_SERVICE, detail("Orphan service text"), LEGACY_VACCINE_TEXT, detail("Orphan referral text")));
        JSONObject form = payload(action);
        option(form, SERVICES, "none").put("value", false);
        option(form, SERVICES, COVID_SERVICE).put("value", true);
        option(form, REFERRALS, "none").put("value", false);
        option(form, REFERRALS, COVID_REFERRAL).put("value", true);
        action.setJsonPayload(form.toString());
        assertEquals(COMPLETED, action.getActionStatus());
        assertFalse(field(payload(action), COVID_SERVICE).has("key"));
        assertFalse(field(payload(action), LEGACY_VACCINE_TEXT).has("key"));
    }

    @Test
    public void nativeSaveRequiresBothGroupsAndOtherTextAndEnforcesNoneBothWays() throws Exception {
        BaseKvpVisitAction action = open("male", Locale.ENGLISH, Map.of());
        try (ActivityController<FamilyWizardFormActivity> controller = openNativeForm(action)) {
            FamilyWizardFormActivity activity = controller.get();
            JsonFormFragment fragment = fragment(activity);
            assertTrue(fragment.save(false));
            assertInvalid(fragment, SERVICES);
            assertInvalid(fragment, REFERRALS);

            CheckBox covidService = checkbox(activity, SERVICES, COVID_SERVICE);
            CheckBox noneService = checkbox(activity, SERVICES, "none");
            CheckBox other = checkbox(activity, SERVICES, "other");
            CheckBox covidReferral = checkbox(activity, REFERRALS, COVID_REFERRAL);
            CheckBox noneReferral = checkbox(activity, REFERRALS, "none");

            setChecked(covidService);
            setChecked(noneService);
            assertFalse(covidService.isChecked());
            setChecked(other);
            assertFalse(noneService.isChecked());

            setChecked(covidReferral);
            setChecked(noneReferral);
            assertFalse(covidReferral.isChecked());
            setChecked(covidReferral);
            assertFalse(noneReferral.isChecked());

            assertNull(activity.getFormDataView("step1:" + COVID_SERVICE));
            assertNull(activity.getFormDataView("step1:" + LEGACY_VACCINE_TEXT));
            EditText otherText = (EditText) activity.getFormDataView("step1:" + OTHER_TEXT);
            assertTrue(otherText.isEnabled());
            assertTrue(fragment.save(false));
            assertInvalid(fragment, OTHER_TEXT);

            otherText.setText("Synthetic other service");
            assertTrue(fragment.save(false));
            assertEquals(Activity.RESULT_OK, shadowOf(activity).getResultCode());
            action.setJsonPayload(shadowOf(activity).getResultIntent().getStringExtra("json"));
            assertEquals(COMPLETED, action.getActionStatus());
            assertSelections(payload(action), SERVICES, "other");
            assertSelections(payload(action), REFERRALS, COVID_REFERRAL);
        }
    }

    @Test
    public void nativeHistoryEditKeepsLegacyDetailWhileRemovingHiddenChoices() throws Exception {
        BaseKvpVisitAction action = open("male", new Locale("sw"), Map.of(
                SERVICES, detail("rch, other"),
                REFERRALS, detail("vaccine_type, cecap"),
                OTHER_TEXT, detail("Synthetic service"),
                LEGACY_VACCINE_TEXT, detail("HPV")));
        try (ActivityController<FamilyWizardFormActivity> controller = openNativeForm(action)) {
            FamilyWizardFormActivity activity = controller.get();
            CheckBox previousVaccination = checkbox(activity, REFERRALS, LEGACY_REFERRAL);
            assertTrue(previousVaccination.isChecked());
            assertEquals("Chanjo ya awali (HPV)", previousVaccination.getText().toString());
            View priorText = activity.getFormDataView("step1:" + LEGACY_VACCINE_TEXT);
            assertTrue(priorText == null || priorText.getVisibility() != View.VISIBLE);
            assertEquals("Synthetic service", ((EditText) activity.getFormDataView("step1:" + OTHER_TEXT)).getText().toString());

            setChecked(checkbox(activity, REFERRALS, "fp"));
            assertTrue(fragment(activity).save(false));
            assertEquals(Activity.RESULT_OK, shadowOf(activity).getResultCode());
            action.setJsonPayload(shadowOf(activity).getResultIntent().getStringExtra("json"));
            Map<String, List<VisitDetail>> stored = savedDetails(action);
            assertEquals("other", stored.get(SERVICES).get(0).getDetails());
            assertEquals("fp, vaccine_type", stored.get(REFERRALS).get(0).getDetails());
            assertEquals("HPV", stored.get(LEGACY_VACCINE_TEXT).get(0).getDetails());
        }
    }

    private static ActivityController<FamilyWizardFormActivity> openNativeForm(BaseKvpVisitAction action) {
        Form config = new Form();
        config.setWizard(false);
        config.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
        Intent intent = new Intent().putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, config)
                .putExtra("json", action.getJsonPayload());
        ActivityController<FamilyWizardFormActivity> controller =
                Robolectric.buildActivity(FamilyWizardFormActivity.class, intent).setup();
        shadowOf(Looper.getMainLooper()).idle();
        return controller;
    }

    private static JsonFormFragment fragment(FamilyWizardFormActivity activity) {
        JsonFormFragment fragment = (JsonFormFragment) activity.getSupportFragmentManager().findFragmentById(R.id.container);
        assertNotNull(fragment);
        return fragment;
    }

    private static BaseKvpVisitAction open(String sex, Locale locale, Map<String, List<VisitDetail>> details) throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        ResourceBundle strings = ResourceBundle.getBundle(FORM_NAME, locale);
        String json = new String(Files.readAllBytes(Paths.get("src/main/assets/json.form", FORM_NAME + ".json")),
                StandardCharsets.UTF_8);
        for (String key : strings.keySet()) {
            json = json.replace("{{" + key + "}}", strings.getString(key));
        }
        JSONObject form = new JSONObject(json);
        KvpJsonFormUtils.populateForm(form, details);
        return new BaseKvpVisitAction.Builder(context, "Other Services and Referrals")
                .withFormName(FORM_NAME).withJsonPayload(form.toString()).withOptional(false)
                .withHelper(new KvpOtherServicesActionHelper(sex)).withDetails(details).build();
    }

    private static List<VisitDetail> detail(String value) {
        VisitDetail detail = new VisitDetail();
        detail.setDetails(value);
        return List.of(detail);
    }

    private static Map<String, List<VisitDetail>> savedDetails(BaseKvpVisitAction action) throws Exception {
        assertEquals(COMPLETED, action.getActionStatus());
        JSONArray fields = payload(action).getJSONObject("step1").getJSONArray("fields");
        Event event = new Event();
        for (int i = 0; i < fields.length(); i++) {
            JsonFormUtils.addObservation(event, fields.getJSONObject(i));
        }
        return NCUtil.eventsObsToDetails(event.getObs(), "synthetic-visit", "synthetic-client");
    }

    private static JSONObject payload(BaseKvpVisitAction action) throws Exception {
        return new JSONObject(action.getJsonPayload());
    }

    private static JSONObject field(JSONObject form, String key) throws JSONException {
        return FormUtils.getFieldFromForm(form, key);
    }

    private static JSONObject option(JSONObject form, String group, String code) throws Exception {
        JSONArray options = field(form, group).getJSONArray("options");
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            if (code.equals(option.getString("key"))) {
                return option;
            }
        }
        return null;
    }

    private static List<String> labels(JSONObject form, String group) throws Exception {
        JSONArray options = field(form, group).getJSONArray("options");
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < options.length(); i++) {
            labels.add(options.getJSONObject(i).getString("text"));
        }
        return labels;
    }

    private static void assertSelections(JSONObject form, String group, String... codes) throws Exception {
        assertEquals(new JSONArray(List.of(codes)).toString(), field(form, group).getJSONArray("value").toString());
    }

    private static void assertInvalid(JsonFormFragment fragment, String key) {
        assertTrue(key, fragment.getPresenter().getInvalidFields().keySet().stream().anyMatch(field -> field.endsWith(key)));
    }

    private static CheckBox checkbox(FamilyWizardFormActivity activity, String field, String code) {
        LinearLayout group = (LinearLayout) activity.getFormDataView("step1:" + field);
        for (int i = 1; i < group.getChildCount(); i++) {
            CheckBox option = (CheckBox) ((LinearLayout) group.getChildAt(i)).getChildAt(0);
            if (code.equals(option.getTag(R.id.childKey))) {
                return option;
            }
        }
        throw new AssertionError(field + "/" + code);
    }

    private static void setChecked(CheckBox option) {
        option.setChecked(true);
        shadowOf(Looper.getMainLooper()).idle();
    }
}
