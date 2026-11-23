package org.smartregister.chw.hf.utils;

import static com.vijay.jsonwizard.constants.JsonFormConstants.TEXT;
import static org.smartregister.util.JsonFormUtils.KEY;

import android.content.res.Resources;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.R;

public final class MarpGroupLabels {
    private MarpGroupLabels() {
    }

    public static void applyLongLabels(Resources resources, JSONArray options) throws JSONException {
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            switch (option.getString(KEY)) {
                case "msm":
                    option.put(TEXT, resources.getString(R.string.marp_mhr_option));
                    break;
                case "fsw":
                    option.put(TEXT, resources.getString(R.string.marp_whr_option));
                    break;
                default:
                    MarpOtherGroupsScreening.Group group =
                            MarpOtherGroupsScreening.Group.fromCode(option.getString(KEY));
                    if (group != null) {
                        option.put(TEXT, resources.getString(group.label));
                    }
                    break;
            }
        }
    }
}
