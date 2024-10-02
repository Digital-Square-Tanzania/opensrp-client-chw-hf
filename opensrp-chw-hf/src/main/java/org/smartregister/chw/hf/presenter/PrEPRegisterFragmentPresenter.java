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

    public String getDueFilterCondition(String appointmentDate, boolean isReferred, String prepClientStatus, Context context) {
        StringBuilder customFilter = new StringBuilder();

        // Append appointment date filter
        if (appointmentDate != null && !appointmentDate.equalsIgnoreCase(context.getString(R.string.none))) {
            customFilter.append(MessageFormat.format(" and {0} like ''%{1}%'' ", "next_visit_date", appointmentDate));
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
                    prepStatus = prepClientStatus;  // fallback to the original value if none matches
            }
            customFilter.append(MessageFormat.format(" and {0} = ''{1}'' ", "prep_status", prepStatus));
        }

        return customFilter.toString();
    }

}
