package org.smartregister.chw.hf.model;

import static org.smartregister.chw.lab.util.LabUtil.getHfrCode;
import static org.smartregister.client.utils.constants.JsonFormConstants.GLOBAL;

import org.json.JSONObject;
import org.smartregister.chw.hf.dao.HeiDao;
import org.smartregister.chw.hiv.dao.HivDao;
import org.smartregister.chw.hiv.domain.HivMemberObject;
import org.smartregister.chw.lab.model.BaseLabRegisterModel;
import org.smartregister.chw.lab.util.LabJsonFormUtils;
import org.smartregister.chw.pmtct.dao.PmtctDao;

public class LabRegisterModel extends BaseLabRegisterModel {
    @Override
    public JSONObject getFormAsJson(String formName, String entityId, String currentLocationId) throws Exception {
        JSONObject form = LabJsonFormUtils.getFormAsJson(formName);
        LabJsonFormUtils.getRegistrationForm(form, entityId, currentLocationId);

        String patientId;
        if (PmtctDao.isRegisteredForPmtct(entityId)) {
            HivMemberObject hivMemberObject = HivDao.getMember(entityId);
            if (hivMemberObject != null && hivMemberObject.getCtcNumber() != null)
                patientId = hivMemberObject.getCtcNumber();
            else return null;
        } else {
            patientId = HeiDao.getHeiNumber(entityId);
        }

        form.getJSONObject(GLOBAL).put("PatientId", patientId);
        form.getJSONObject(GLOBAL).put("HfrCode", getHfrCode());


        return form;
    }
}
