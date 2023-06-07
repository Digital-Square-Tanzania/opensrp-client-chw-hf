package org.smartregister.chw.hf.activity;

import static org.smartregister.chw.core.utils.Utils.passToolbarTitle;
import static org.smartregister.chw.vmmc.util.Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.activity.CoreVmmcProfileActivity;
import org.smartregister.chw.core.presenter.CoreFamilyOtherMemberActivityPresenter;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.adapter.ReferralCardViewAdapter;
import org.smartregister.chw.hf.contract.VmmcProfileContract;
import org.smartregister.chw.hf.presenter.FamilyOtherMemberActivityPresenter;
import org.smartregister.chw.hiv.dao.HivDao;
import org.smartregister.chw.hivst.dao.HivstDao;
import org.smartregister.chw.vmmc.domain.Visit;
import org.smartregister.chw.vmmc.util.Constants;
import org.smartregister.chw.vmmc.VmmcLibrary;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Task;
import org.smartregister.family.model.BaseFamilyOtherMemberProfileActivityModel;
import org.smartregister.family.util.JsonFormUtils;

import java.util.Set;

import timber.log.Timber;

public class VmmcProfileActivity extends CoreVmmcProfileActivity {

    private static String baseEntityId;


    public static void startVmmcActivity(Activity activity, String baseEntityId) {
        VmmcProfileActivity.baseEntityId = baseEntityId;
        Intent intent = new Intent(activity, VmmcProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.VMMC_PROFILE);
        activity.startActivity(intent);
    }

//    @Override
//    public void startVmmcNotifiableForm(String baseEntityId) {
//        JSONObject form = FormUtils.getFormUtils().getFormJson(Constants.FORMS.VMMC_NOTIFIABLE);
//
//        try {
//            JSONObject global = form.getJSONObject("global");
//            String method_used_notify = org.smartregister.chw.hf.actionhelper.vmmc.VmmcProcedureActionHelper.method_used;
//            global.put("method_used", method_used_notify);
//            Log.d("method_used_vmmc",method_used_notify);
//        } catch (JSONException e) {
//            e.printStackTrace();
//            Log.d("method_used_vmmc", e.toString());
//        }
//        startFormActivity(form);
//    }
//
//    @Override
//    public void startVmmcFollowUp(String baseEntityId) {
//        JSONObject form = FormUtils.getFormUtils().getFormJson(Constants.FORMS.VMMC_FOLLOW_UP_VISIT);
//
//        try {
//            JSONObject global = form.getJSONObject("global");
//            String method_used = org.smartregister.chw.hf.actionhelper.vmmc.VmmcProcedureActionHelper.method_used;
//            global.put("method_used", method_used);
//            Log.d("method_used_vmmc",method_used);
//        } catch (JSONException e) {
//            e.printStackTrace();
//            Log.d("method_used_vmmc", e.toString());
//        }
//
//        startFormActivity(form);
//    }

    @Override
    public void openFollowupVisit() {
        VmmcServiceActivity.startVmmcVisitActivity(this, baseEntityId, false);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
//        if (id == R.id.textview_record_vmmc) {
//            VmmcServiceActivity.startVmmcVisitActivity(this, baseEntityId, true);
//        }
        if (id == R.id.textview_procedure_vmmc) {
//            Snackbar.make(view.findViewById(R.id.textview_procedure_vmmc),"Hello Dev",Snackbar.LENGTH_LONG).show();
            VmmcProcedureActivity.startVmmcVisitProcedureActivity(this, baseEntityId, false);
        }
//        if (id == R.id.textview_notifiable_vmmc) {
////            Snackbar.make(view.findViewById(R.id.textview_procedure_vmmc),"Hello Dev",Snackbar.LENGTH_LONG).show();
//            startVmmcNotifiableForm( baseEntityId);
//        }
        if (id == R.id.textview_discharge_vmmc) {
//            Snackbar.make(view.findViewById(R.id.textview_procedure_vmmc),"Hello Dev",Snackbar.LENGTH_LONG).show();
            VmmcDischargeActivity.startVmmcVisitDischargeActivity(this, baseEntityId, false);
        }
        if (id == R.id.textview_notifiable_vmmc) {
//            Snackbar.make(view.findViewById(R.id.textview_procedure_vmmc),"Hello Dev",Snackbar.LENGTH_LONG).show();
//            startVmmcNotifiableForm( baseEntityId);
            VmmcNotifiableAdverseActivity.startVmmcVisitActivity(this, baseEntityId, false);
        }
        if (id == R.id.textview_followup_vmmc) {
//            Snackbar.make(view.findViewById(R.id.textview_procedure_vmmc),"Hello Dev",Snackbar.LENGTH_LONG).show();
//            startVmmcFollowUp( baseEntityId);
            VmmcFollowUpActivity.startVmmcVisitActivity(this, baseEntityId, false);

        }
        else {
            super.onClick(view);
        }
    }

    @Override
    protected Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivityClass() {
        return null;
    }

    @Override
    protected void removeMember() {

    }

    @NonNull
    @Override
    public CoreFamilyOtherMemberActivityPresenter presenter() {
        return null;
    }

    @Override
    public void setProfileImage(String s, String s1) {

    }

    @Override
    public void setProfileDetailThree(String s) {

    }

    @Override
    public void toggleFamilyHead(boolean b) {

    }

    @Override
    public void togglePrimaryCaregiver(boolean b) {

    }

    @Override
    public void refreshMedicalHistory(boolean hasHistory) {
        Visit vmmcServices = getVisit(Constants.EVENT_TYPE.VMMC_SERVICES);
        Visit vmmcProcedure = getVisit(Constants.EVENT_TYPE.VMMC_PROCEDURE);
        Visit vmmcDischarge = getVisit(Constants.EVENT_TYPE.VMMC_DISCHARGE);
        Visit vmmcFollowUp = getVisit(Constants.EVENT_TYPE.VMMC_FOLLOW_UP_VISIT);
        Visit vmmcNotifiableAdverse = getVisit(Constants.EVENT_TYPE.VMMC_NOTIFIABLE_EVENTS);


        if (vmmcServices != null || vmmcProcedure != null || vmmcDischarge != null || vmmcFollowUp != null || vmmcNotifiableAdverse != null) {
            rlLastVisit.setVisibility(View.VISIBLE);
            findViewById(R.id.view_notification_and_referral_row).setVisibility(View.VISIBLE);
//            ((TextView) findViewById(R.id.vViewHistory)).setText(R.string.visits_history);
//            ((TextView) findViewById(R.id.ivViewHistoryArrow)).setText(getString(R.string.view_visits_history));
        } else {
            rlLastVisit.setVisibility(View.GONE);
        }
    }

    @Override
    public void openMedicalHistory() {
        VmmcMedicalHistoryActivity.startMe(this, memberObject);
    }

    private Visit getVisit(String eventType) {
        return VmmcLibrary.getInstance().visitRepository().getLatestVisit(baseEntityId, eventType);
    }

    @Override
    public void refreshList() {

    }

    @Override
    public void updateHasPhone(boolean b) {

    }

    @Override
    public void setFamilyServiceStatus(String s) {

    }

    @Override
    public void verifyHasPhone() {

    }

    @Override
    public void notifyHasPhone(boolean b) {

    }
}

