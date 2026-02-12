package org.smartregister.chw.hf.interactor;

import static org.smartregister.chw.anc.util.VisitUtils.getChildVisits;
import static org.smartregister.chw.anc.util.VisitUtils.getVisitDetailsOnly;
import static org.smartregister.chw.anc.util.VisitUtils.getVisitGroups;
import static org.smartregister.chw.anc.util.VisitUtils.getVisitsOnly;
import static org.smartregister.chw.hts.util.Constants.EVENT_TYPE.HTS_FIRST_HIV_TEST;
import static org.smartregister.chw.hts.util.Constants.EVENT_TYPE.HTS_SECOND_HIV_TEST;
import static org.smartregister.chw.hts.util.Constants.EVENT_TYPE.HTS_SERVICES;
import static org.smartregister.chw.hts.util.Constants.EVENT_TYPE.HTS_UNIGOLD_HIV_TEST;

import android.content.Context;

import com.google.gson.Gson;

import org.smartregister.chw.anc.contract.BaseAncMedicalHistoryContract;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.core.CoreBaseAncMedicalHistoryInteractor;
import org.smartregister.chw.hf.domain.SortableVisit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HtsMedicalHistoryInteractor extends CoreBaseAncMedicalHistoryInteractor {
    public static List<SortableVisit> getVisits(String memberID, String... eventTypes) {

        List<Visit> visits = new ArrayList<>();
        if (eventTypes != null) {
            for (String eventType : eventTypes) {
                List<Visit> visit = getVisitsOnly(memberID, eventType);
                visits.addAll(visit);
            }
        }

        int x = 0;
        while (visits.size() > x) {
            Visit visit = visits.get(x);
            List<VisitDetail> detailList = getVisitDetailsOnly(visit.getVisitId());
            visits.get(x).setVisitDetails(getVisitGroups(detailList));
            x++;
        }

        List<SortableVisit> sortableVisits = new ArrayList<>();
        for (Visit visit : visits) {
            Gson gson = new Gson();
            SortableVisit sortableVisit = gson.fromJson(gson.toJson(visit), SortableVisit.class);
            sortableVisits.add(sortableVisit);
        }

        Collections.sort(sortableVisits);

        return sortableVisits;
    }

    @Override
    public void getMemberHistory(final String memberID, final Context context, final BaseAncMedicalHistoryContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {

            String[] eventTypes = new String[]{HTS_SERVICES};
            List<SortableVisit> visits = getVisits(memberID, eventTypes);
            final List<Visit> all_visits = new ArrayList<>(visits);

            for (Visit visit : visits) {
                Map<String, List<VisitDetail>> visitDetails = visit.getVisitDetails() == null
                        ? new HashMap<>()
                        : new HashMap<>(visit.getVisitDetails());

                List<Visit> childVisits = getChildVisits(visit.getVisitId());
                for (Visit childVisit : childVisits) {
                    Map<String, List<VisitDetail>> mVisitDetails = childVisit.getVisitDetails();
                    if (mVisitDetails == null) {
                        continue;
                    }
                    Map<String, String> prefixedKeys = getPrefixedChildVisitDetailKeys(childVisit.getVisitType());
                    for (Map.Entry<String, List<VisitDetail>> entry : mVisitDetails.entrySet()) {
                        visitDetails.computeIfAbsent(entry.getKey(), key -> new ArrayList<>()).addAll(entry.getValue());
                        String prefixedKey = prefixedKeys.get(entry.getKey());
                        if (prefixedKey != null && !prefixedKey.equals(entry.getKey())) {
                            visitDetails.computeIfAbsent(prefixedKey, key -> new ArrayList<>()).addAll(entry.getValue());
                        }
                    }
                }

                visit.setVisitDetails(visitDetails);
            }

            appExecutors.mainThread().execute(() -> callBack.onDataFetched(all_visits));
        };

        appExecutors.diskIO().execute(runnable);
    }

    private Map<String, String> getPrefixedChildVisitDetailKeys(String visitType) {
        Map<String, String> prefixedKeys = new HashMap<>();
        if (visitType == null) {
            return prefixedKeys;
        }
        if (visitType.startsWith(HTS_FIRST_HIV_TEST)) {
            prefixedKeys.put("type_of_test_kit_used", "hts_type_of_test_used");
            prefixedKeys.put("test_kit_batch_number", "hts_kit_batch_number");
            prefixedKeys.put("test_kit_expire_date", "hts_kit_expire_date");
            prefixedKeys.put("test_result", "hts_first_hiv_test_result");
            prefixedKeys.put("syphilis_test_results", "syphilis_test_results");
        } else if (visitType.startsWith(HTS_SECOND_HIV_TEST)) {
            prefixedKeys.put("test_kit_batch_number", "hts_second_kit_batch_number");
            prefixedKeys.put("test_kit_expire_date", "hts_second_kit_expire_date");
            prefixedKeys.put("test_result", "hts_second_hiv_test_result");
        } else if (visitType.startsWith(HTS_UNIGOLD_HIV_TEST)) {
            prefixedKeys.put("test_kit_batch_number", "hts_unigold_kit_batch_number");
            prefixedKeys.put("test_kit_expire_date", "hts_unigold_kit_expire_date");
            prefixedKeys.put("test_result", "hts_unigold_hiv_test_result");
        }
        return prefixedKeys;
    }
}
