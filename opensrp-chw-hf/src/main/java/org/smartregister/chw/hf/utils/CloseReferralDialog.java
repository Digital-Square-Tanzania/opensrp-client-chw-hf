package org.smartregister.chw.hf.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.fragment.app.DialogFragment;

import org.smartregister.chw.hf.R;

public class CloseReferralDialog extends DialogFragment {
    public interface CloseReferralListener {
        void onSubmit(String hypertensionResult, String diabetesResult);
        void onCancel();
    }

    private CloseReferralListener listener;

    public void setListener(CloseReferralListener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_close_referral, null);

        final Spinner hypertensionResultSpinner = view.findViewById(R.id.sp_hypertension_result);
        final Spinner diabetesResultSpinner = view.findViewById(R.id.sp_diabetes_result);

        ArrayAdapter<String> resultAdapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_spinner_item,
                new String[]{"", "Positive", "Negative"}
        );
        resultAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        hypertensionResultSpinner.setAdapter(resultAdapter);
        diabetesResultSpinner.setAdapter(resultAdapter);

        builder.setView(view)
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String hypertensionResult = hypertensionResultSpinner.getSelectedItem().toString();
                        String diabetesResult = diabetesResultSpinner.getSelectedItem().toString();
                        if (listener != null) {
                            listener.onSubmit(hypertensionResult, diabetesResult);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (listener != null) {
                            listener.onCancel();
                        }
                        dialog.cancel();
                    }
                });
        return builder.create();
    }
}
