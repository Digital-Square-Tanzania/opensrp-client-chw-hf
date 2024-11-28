package org.smartregister.chw.hf.sync.intent;

/**
 * Created by cozeje on 18/06/2024.
 */

import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.CoreLibrary;
import org.smartregister.SyncConfiguration;
import org.smartregister.chw.hf.repository.UniqueLabTestSampleTrackingIdRepository;
import org.smartregister.domain.Response;
import org.smartregister.exception.NoHttpResponseException;
import org.smartregister.service.HTTPAgent;
import org.smartregister.sync.intent.PullUniqueIdsIntentService;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class PullUniqueLabTestSampleTrackingIdsIntentService extends PullUniqueIdsIntentService {
    public static final String ID_URL = "/uniqueids/get";
    public static final String IDENTIFIERS = "identifiers";
    private UniqueLabTestSampleTrackingIdRepository uniqueLabTestSampleTrackingIdRepository;

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            SyncConfiguration configs = CoreLibrary.getInstance().getSyncConfiguration();
            int numberToGenerate;
            if (uniqueLabTestSampleTrackingIdRepository.countUnUsedIds() == 0) { // first time pull no ids at all
                numberToGenerate = 50;
            } else if (uniqueLabTestSampleTrackingIdRepository.countUnUsedIds() <= 25) { //maintain a minimum of 25 else skip this pull
                numberToGenerate = 50;
            } else {
                return;
            }
            JSONObject ids = fetchOpenMRSIds(configs.getUniqueIdSource() + 1, numberToGenerate);
            if (ids.has(IDENTIFIERS)) {
                parseResponse(ids);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private JSONObject fetchOpenMRSIds(int source, int numberToGenerate) throws Exception {
        HTTPAgent httpAgent = CoreLibrary.getInstance().context().getHttpAgent();
        String baseUrl = CoreLibrary.getInstance().context().
                configuration().dristhiBaseURL();
        String endString = "/";
        if (baseUrl.endsWith(endString)) {
            baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf(endString));
        }

        String url = baseUrl + ID_URL + "?source=" + source + "&numberToGenerate=" + numberToGenerate;
        Timber.i("URL: %s", url);

        if (httpAgent == null) {
            throw new IllegalArgumentException(ID_URL + " http agent is null");
        }

        Response resp = httpAgent.fetch(url);
        if (resp.isFailure()) {
            throw new NoHttpResponseException(ID_URL + " not returned data");
        }

        return new JSONObject((String) resp.payload());
    }

    private void parseResponse(JSONObject idsFromOMRS) throws Exception {
        JSONArray jsonArray = idsFromOMRS.getJSONArray(IDENTIFIERS);
        if (jsonArray.length() > 0) {
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                ids.add(jsonArray.getString(i));
            }
            uniqueLabTestSampleTrackingIdRepository.bulkInsertOpenmrsIds(ids);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        uniqueLabTestSampleTrackingIdRepository = new UniqueLabTestSampleTrackingIdRepository();
        return super.onStartCommand(intent, flags, startId);
    }

}
