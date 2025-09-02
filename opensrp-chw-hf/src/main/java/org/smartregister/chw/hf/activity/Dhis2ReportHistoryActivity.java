package org.smartregister.chw.hf.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.adapter.Dhis2ReportHistoryAdapter;
import org.smartregister.chw.hf.domain.dhis2_reports.Dhis2ReportHistory;
import org.smartregister.chw.hf.repository.Dhis2ReportHistoryRepository;

import java.util.List;

public class Dhis2ReportHistoryActivity extends AppCompatActivity {

    public static final String EXTRA_REPORT_TYPE = "EXTRA_REPORT_TYPE"; // hps_monthly | hps_annual
    public static final String EXTRA_PERIOD = "EXTRA_PERIOD";         // optional

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dhis2_history);
        setUpToolbar();

        RecyclerView rv = findViewById(R.id.rv_history);
        rv.setLayoutManager(new LinearLayoutManager(this));
        Dhis2ReportHistoryAdapter adapter = new Dhis2ReportHistoryAdapter(item -> {
            Intent i = new Intent(this, Dhis2SummaryViewActivity.class);
            i.putExtra(Dhis2SummaryViewActivity.EXTRA_EVENT_ID, item.eventId);
            startActivity(i);
        });
        rv.setAdapter(adapter);

        String reportType = getIntent().getStringExtra(EXTRA_REPORT_TYPE);
        String period = getIntent().getStringExtra(EXTRA_PERIOD);
        if (TextUtils.isEmpty(reportType)) reportType = "hps_monthly";

        Dhis2ReportHistoryRepository repo = new Dhis2ReportHistoryRepository();
        List<Dhis2ReportHistory> list = repo.list(reportType, period, 200, 0);
        adapter.setItems(list);
    }

    private void setUpToolbar() {
        Toolbar toolbar = findViewById(R.id.back_to_nav_toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(R.string.dhis2_history_title);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
}
