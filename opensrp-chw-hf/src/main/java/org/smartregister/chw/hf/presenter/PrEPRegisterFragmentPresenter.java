package org.smartregister.chw.hf.presenter;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.kvp.contract.KvpRegisterFragmentContract;
import org.smartregister.chw.kvp.presenter.BaseKvpRegisterFragmentPresenter;
import org.smartregister.chw.kvp.util.Constants;

import java.text.MessageFormat;

public class PrEPRegisterFragmentPresenter extends BaseKvpRegisterFragmentPresenter {
    public PrEPRegisterFragmentPresenter(KvpRegisterFragmentContract.View view, KvpRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainTable() {
        return Constants.TABLES.PrEP_REGISTER;
    }

    public String getDueFilterCondition(String nextAppointmentStartDate, String nextAppointmentEndDate, boolean isReferred, String prepClientStatus, Context context) {
        StringBuilder customFilter = new StringBuilder();

        // Append appointment date filter
        if (nextAppointmentStartDate != null && !nextAppointmentStartDate.equalsIgnoreCase(context.getString(R.string.none))) {
            customFilter.append(" AND date(substr(next_visit_date, 7, 4) || '-' || substr(next_visit_date, 4, 2) || '-' || substr(next_visit_date, 1, 2)) >= date(substr('" + nextAppointmentStartDate + "', 7, 4) || '-' || substr('" + nextAppointmentStartDate + "', 4, 2) || '-' || substr('"+nextAppointmentStartDate+"', 1, 2))");
        }
        // Append appointment date filter
        if (nextAppointmentEndDate != null && !nextAppointmentEndDate.equalsIgnoreCase(context.getString(R.string.none))) {
            customFilter.append(" AND date(substr(next_visit_date, 7, 4) || '-' || substr(next_visit_date, 4, 2) || '-' || substr(next_visit_date, 1, 2)) <= date(substr('" + nextAppointmentEndDate + "', 7, 4) || '-' || substr('" + nextAppointmentEndDate + "', 4, 2) || '-' || substr('"+nextAppointmentEndDate+"', 1, 2))");
        }

        // Append referral filter if necessary
        if (isReferred) {
            customFilter.append(MessageFormat.format(" and {0}.{1} IN (SELECT for FROM task WHERE business_status = ''Referred'') ", getMainTable(), "base_entity_id"));
        }

        // Translate prepClientStatus and append to filter
        if (StringUtils.isNotBlank(prepClientStatus)) {
            String prepStatus;
            switch (prepClientStatus) {
                case "Initiated":
                    prepStatus = "initiated";
                    break;
                case "Continuing":
                    prepStatus = "continuing";
                    break;
                case "Restarted":
                    prepStatus = "re_start";
                    break;
                case "Not initiated":
                    prepStatus = "not_initiated";
                    break;
                case "Discontinued/Quit":
                    prepStatus = "discontinued_quit";
                    break;
                default:
                    prepStatus = null;  // fallback to the original value if none matches
            }
            if (StringUtils.isNotBlank(prepStatus))
                customFilter.append(MessageFormat.format(" and {0} = ''{1}'' ", "prep_status", prepStatus));
        }

        return customFilter.toString();
    }

    @Override
    public String getMainCondition() {
        return this.getMainTable() + ".is_closed IS 0 AND " + this.getMainTable() + ".agreed_to_use_prep = 'yes'";
    }
}
