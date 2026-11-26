package org.smartregister.chw.hf.activity;

import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.views.JsonFormFragmentView;
import com.vijay.jsonwizard.widgets.NativeRadioButtonFactory;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KvpEnrollmentRadioValidationTest {
    @Test
    public void soleMhrPassesRequiredRadioValidationAfterRelevanceRefresh() {
        assertSoleGroupPassesValidation("msm");
    }

    @Test
    public void soleWhrPassesRequiredRadioValidationAfterRelevanceRefresh() {
        assertSoleGroupPassesValidation("fsw");
    }

    @Test
    public void soleOtherGroupPassesRequiredRadioValidationAfterRelevanceRefresh() {
        assertSoleGroupPassesValidation("other_key_and_vulnerable_populations");
    }

    @Test
    public void soleDrugChecklistOrMaternityGroupPassesRequiredRadioValidation() {
        for (String key : new String[]{"pwid", "pwud", "agyw", "serodiscordant_couple",
                "pregnant_breastfeeding_high_risk"}) {
            assertSoleGroupPassesValidation(key);
        }
    }

    @Test
    public void doesNotReplaceAnExplicitChoiceWhenMultipleGroupsAreAvailable() {
        LinearLayout root = mock(LinearLayout.class);
        RadioButton mhr = radio("msm");
        RadioButton pwud = radio("pwud");
        when(pwud.isChecked()).thenReturn(true);
        when(root.getChildCount()).thenReturn(2);
        when(root.getChildAt(0)).thenReturn(mhr);
        when(root.getChildAt(1)).thenReturn(pwud);

        KvpJsonWizardFormActivity.selectSoleEnrollmentOption(root);

        verify(mhr, never()).setChecked(true);
        verify(pwud, never()).setChecked(false);
    }

    private static void assertSoleGroupPassesValidation(String key) {
        LinearLayout root = mock(LinearLayout.class);
        RadioGroup group = mock(RadioGroup.class);
        RelativeLayout outer = mock(RelativeLayout.class);
        ViewGroup row = mock(ViewGroup.class);
        ViewGroup content = mock(ViewGroup.class);
        RadioButton option = radio(key);

        // Native Forms registers a required group and validates its nested Android RadioButton.
        when(group.getTag(R.id.error)).thenReturn("Please select an answer");
        when(group.isEnabled()).thenReturn(true);
        when(group.getParent()).thenReturn(root);
        when(root.isShown()).thenReturn(true);
        when(root.getChildCount()).thenReturn(1);
        when(root.getChildAt(0)).thenReturn(group);
        when(group.getChildCount()).thenReturn(1);
        when(group.getChildAt(0)).thenReturn(outer);
        when(outer.getChildCount()).thenReturn(1);
        when(outer.getChildAt(0)).thenReturn(row);
        when(row.getChildCount()).thenReturn(1);
        when(row.getChildAt(0)).thenReturn(content);
        when(content.getChildCount()).thenReturn(1);
        when(content.getChildAt(0)).thenReturn(option);

        KvpJsonWizardFormActivity.selectSoleEnrollmentOption(root);

        assertTrue(option.isChecked());
        assertTrue(NativeRadioButtonFactory.validate(mock(JsonFormFragmentView.class), group).isValid());
    }

    private static RadioButton radio(String key) {
        RadioButton option = mock(RadioButton.class);
        AtomicBoolean checked = new AtomicBoolean();
        when(option.getTag(R.id.childKey)).thenReturn(key);
        when(option.isChecked()).thenAnswer(call -> checked.get());
        when(option.isEnabled()).thenReturn(true);
        doAnswer(call -> {
            checked.set(call.getArgument(0));
            return null;
        }).when(option).setChecked(true);
        return option;
    }
}
