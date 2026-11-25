package org.smartregister.chw.hf.actionhelper.kvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationDeliveryTest.CHANNEL;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationDeliveryTest.MODES;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationTest.TOPICS;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationTest.form;

import android.app.Activity;
import android.app.Application;
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.smartregister.family.activity.FamilyWizardFormActivity;

import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class MarpHealthEducationDeliveryFormTest {
    private ActivityController<FamilyWizardFormActivity> controller;
    private FamilyWizardFormActivity activity;
    private JsonFormFragment fragment;

    @Before
    public void openNativeForm() throws Exception {
        Form config = new Form();
        config.setWizard(false);
        config.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
        Intent intent = new Intent()
                .putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, config)
                .putExtra("json", form(Locale.ENGLISH).toString());
        controller = Robolectric.buildActivity(FamilyWizardFormActivity.class, intent).setup();
        activity = controller.get();
        shadowOf(Looper.getMainLooper()).idle();
        fragment = (JsonFormFragment) activity.getSupportFragmentManager().findFragmentById(R.id.container);
        assertNotNull(fragment);
    }

    @After
    public void closeNativeForm() {
        if (controller != null) {
            controller.pause().stop().destroy();
        }
    }

    @Test
    public void nativeSaveRequiresEachVisibleChecklistAndOtherChannel() {
        assertFalse(view(MODES + "sti").isEnabled());
        assertFalse(view(MODES + "tb").isEnabled());
        select(TOPICS, "sti", true);
        select(TOPICS, "tb", true);
        assertTrue(view(MODES + "sti").isEnabled());
        assertTrue(view(MODES + "tb").isEnabled());

        assertTrue(fragment.save(false));
        assertInvalid(MODES + "sti");
        assertInvalid(MODES + "tb");
        select(MODES + "sti", "leaflets", true);
        assertTrue(fragment.save(false));
        assertInvalid(MODES + "tb");

        select(MODES + "tb", "other", true);
        assertFalse(view(CHANNEL + "sti").isEnabled());
        assertTrue(view(CHANNEL + "tb").isEnabled());
        assertTrue(fragment.save(false));
        assertInvalid(CHANNEL + "tb");

        ((EditText) view(CHANNEL + "tb")).setText("Synthetic channel");
        assertTrue(fragment.save(false));
        assertTrue(activity.isFinishing());
        assertEquals(Activity.RESULT_OK, shadowOf(activity).getResultCode());
        assertNotNull(shadowOf(activity).getResultIntent());
    }

    @Test
    public void nativeDeselectionClearsOnlyAffectedWidgetsAndNoneCanSave() {
        select(TOPICS, "sti", true);
        select(TOPICS, "tb", true);
        select(MODES + "sti", "leaflets", true);
        select(MODES + "tb", "other", true);
        ((EditText) view(CHANNEL + "tb")).setText("First synthetic channel");

        select(MODES + "tb", "other", false);
        assertFalse(view(CHANNEL + "tb").isEnabled());
        select(MODES + "tb", "other", true);
        assertEquals("", ((EditText) view(CHANNEL + "tb")).getText().toString());
        ((EditText) view(CHANNEL + "tb")).setText("Second synthetic channel");
        select(MODES + "tb", "television", true);

        select(TOPICS, "tb", false);
        assertFalse(view(MODES + "tb").isEnabled());
        assertFalse(view(CHANNEL + "tb").isEnabled());
        select(TOPICS, "tb", true);
        assertFalse(option(MODES + "tb", "other").isChecked());
        assertFalse(option(MODES + "tb", "television").isChecked());
        assertTrue(option(MODES + "sti", "leaflets").isChecked());
        select(MODES + "tb", "other", true);
        assertEquals("", ((EditText) view(CHANNEL + "tb")).getText().toString());

        select(TOPICS, "none", true);
        assertFalse(view(MODES + "sti").isEnabled());
        assertFalse(view(MODES + "tb").isEnabled());
        assertFalse(view(CHANNEL + "tb").isEnabled());
        assertTrue(fragment.save(false));
        assertEquals(Activity.RESULT_OK, shadowOf(activity).getResultCode());
    }

    private void assertInvalid(String key) {
        assertFalse(activity.isFinishing());
        assertTrue(key, fragment.getPresenter().getInvalidFields().keySet().stream().anyMatch(field -> field.endsWith(key)));
    }

    private View view(String key) {
        View view = activity.getFormDataView("step1:" + key);
        assertNotNull(key, view);
        return view;
    }

    private void select(String key, String code, boolean checked) {
        option(key, code).setChecked(checked);
        shadowOf(Looper.getMainLooper()).idle();
    }

    private CheckBox option(String key, String code) {
        LinearLayout group = (LinearLayout) view(key);
        for (int i = 1; i < group.getChildCount(); i++) {
            CheckBox option = (CheckBox) ((LinearLayout) group.getChildAt(i)).getChildAt(0);
            if (code.equals(option.getTag(R.id.childKey))) {
                return option;
            }
        }
        throw new AssertionError(key + "/" + code);
    }
}
