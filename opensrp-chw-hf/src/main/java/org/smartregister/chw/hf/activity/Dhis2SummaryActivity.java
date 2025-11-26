package org.smartregister.chw.hf.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.adapter.Dhis2DataValuesAdapter;
import org.smartregister.chw.hf.dao.ReportDao;
import org.smartregister.chw.hf.domain.dhis2_reports.Dhis2ReportHistory;
import org.smartregister.chw.hf.domain.dhis2_reports.DhisDataValues;
import org.smartregister.chw.hf.repository.Dhis2ReportHistoryRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class Dhis2SummaryActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "EXTRA_EVENT_ID";
    public static final String EXTRA_FALLBACK_REPORT_DATA_JSON = "EXTRA_FALLBACK_REPORT_DATA_JSON";
    public static final String EXTRA_FALLBACK_DHIS2_PAYLOAD_JSON = "EXTRA_FALLBACK_DHIS2_PAYLOAD_JSON";

    private TextView tvDataset, tvPeriod, tvOrgUnit, tvCompleteDate, tvItems, tvEmpty;
    private Dhis2DataValuesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dhis2_summary);
        setUpToolbar();

        tvDataset = findViewById(R.id.tv_dataset);
        tvPeriod = findViewById(R.id.tv_period);
        tvOrgUnit = findViewById(R.id.tv_org_unit);
        tvCompleteDate = findViewById(R.id.tv_complete_date);
        tvItems = findViewById(R.id.tv_items);
        tvEmpty = findViewById(R.id.tv_empty);
        RecyclerView rv = findViewById(R.id.rv_data_values);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Dhis2DataValuesAdapter();
        rv.setAdapter(adapter);

        String eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        String fallbackReportDataJson = getIntent().getStringExtra(EXTRA_FALLBACK_REPORT_DATA_JSON);
        String fallbackDhis2PayloadJson = getIntent().getStringExtra(EXTRA_FALLBACK_DHIS2_PAYLOAD_JSON);

        Dhis2ReportHistoryRepository repo = new Dhis2ReportHistoryRepository();
        Dhis2ReportHistory h = !TextUtils.isEmpty(eventId) ? repo.getByEventId(eventId) : null;

        String reportDataJson = h != null ? h.reportDataJson : fallbackReportDataJson;
        String dataSet = h != null ? h.dataSet : null;
        String period = h != null ? h.period : null;
        String orgUnit = h != null ? h.orgUnit : null;
        String completeDate = h != null ? h.completeDate : null;
        if (TextUtils.isEmpty(completeDate) && !TextUtils.isEmpty(fallbackDhis2PayloadJson)) {
            try { completeDate = new JSONObject(fallbackDhis2PayloadJson).optString("completeDate", null);} catch (Exception ignore) {}
        }

        try {
            if (!TextUtils.isEmpty(reportDataJson)) {
                List<DhisDataValues> list = ReportDao.getDhisDataValues(new JSONObject(reportDataJson));
                adapter.setItems(list);
                tvItems.setText(getString(R.string.dhis2_items_count, list != null ? list.size() : 0));
                tvEmpty.setVisibility((list == null || list.isEmpty()) ? View.VISIBLE : View.GONE);
            } else {
                adapter.setItems(null);
                tvItems.setText(getString(R.string.dhis2_items_count, 0));
                tvEmpty.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Timber.e(e);
            adapter.setItems(null);
            tvItems.setText(getString(R.string.dhis2_items_count, 0));
            tvEmpty.setVisibility(View.VISIBLE);
        }

        tvDataset.setText(!TextUtils.isEmpty(dataSet) ? dataSet : getString(R.string.na));
        if (TextUtils.isEmpty(period) && h != null && h.eventDate != null) {
            period = new SimpleDateFormat("yyyyMM", Locale.getDefault()).format(new Date(h.eventDate));
        }
        tvPeriod.setText(!TextUtils.isEmpty(period) ? period : getString(R.string.na));
        tvOrgUnit.setText(!TextUtils.isEmpty(orgUnit) ? orgUnit : getString(R.string.na));
        tvCompleteDate.setText(!TextUtils.isEmpty(completeDate) ? completeDate : getString(R.string.na));
    }

    private void setUpToolbar() {
        Toolbar toolbar = findViewById(R.id.back_to_nav_toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(R.string.dhis2_summary_title);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
}

