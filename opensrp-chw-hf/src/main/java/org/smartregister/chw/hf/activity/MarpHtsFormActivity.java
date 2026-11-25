package org.smartregister.chw.hf.activity;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.vijay.jsonwizard.R;

import org.smartregister.chw.hf.utils.MarpHts;
import org.smartregister.family.activity.FamilyWizardFormActivity;

public class MarpHtsFormActivity extends FamilyWizardFormActivity {
    @Override
    public void addFormDataView(View view) {
        super.addFormDataView(view);
        if (!(view instanceof EditText) || !MarpHts.CTC_NUMBER.equals(view.getTag(R.id.key))) {
            return;
        }

        // Keep the JSON field textual so leading zeros survive the native form result.
        EditText ctcNumber = (EditText) view;
        ctcNumber.setRawInputType(InputType.TYPE_CLASS_PHONE);
        ctcNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                String value = text.toString();
                String formatted = MarpHts.formatCtcId(value);
                if (!formatted.equals(value)) {
                    text.replace(0, text.length(), formatted);
                }
            }
        });
    }
}
