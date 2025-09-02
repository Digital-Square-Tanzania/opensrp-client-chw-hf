package org.smartregister.chw.hf.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.adapter.Dhis2ReportHistoryAdapter;
import org.smartregister.chw.hf.domain.dhis2_reports.Dhis2ReportHistory;
import org.smartregister.chw.hf.repository.Dhis2ReportHistoryRepository;

import java.util.List;
import java.util.Locale;

public class Dhis2ReportHistoryActivity extends AppCompatActivity {

    public static final String EXTRA_REPORT_TYPE = "EXTRA_REPORT_TYPE"; // hps_monthly | hps_annual
    public static final String EXTRA_PERIOD = "EXTRA_PERIOD";         // optional

    private String selectedReportType = null; // null = all, or hps_monthly / hps_annual
    private String selectedPeriod = null;     // yyyyMM or yyyy
    private Dhis2ReportHistoryAdapter adapter;
    private LinearLayout chipContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dhis2_history);
        setUpToolbar();

        RecyclerView rv = findViewById(R.id.rv_history);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Dhis2ReportHistoryAdapter(item -> {
            Intent i = new Intent(this, Dhis2SummaryViewActivity.class);
            i.putExtra(Dhis2SummaryViewActivity.EXTRA_EVENT_ID, item.eventId);
            startActivity(i);
        });
        rv.setAdapter(adapter);

        chipContainer = findViewById(R.id.chip_container);

        selectedReportType = getIntent().getStringExtra(EXTRA_REPORT_TYPE);
        selectedPeriod = getIntent().getStringExtra(EXTRA_PERIOD);
        reload();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dhis2_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_filter) {
            showFilterSheet();
            return true;
        } else if (id == R.id.action_clear_filters) {
            selectedReportType = null;
            selectedPeriod = null;
            reload();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showFilterSheet() {
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.bottomsheet_dhis2_history_filters, null);
        dialog.setContentView(v);

        RadioGroup rg = v.findViewById(R.id.rg_report_type);
        RadioButton rbAll = v.findViewById(R.id.rb_all);
        RadioButton rbMonthly = v.findViewById(R.id.rb_monthly);
        RadioButton rbAnnual = v.findViewById(R.id.rb_annual);
        TextView tvPeriod = v.findViewById(R.id.tv_period);
        View btnSelectPeriod = v.findViewById(R.id.btn_select_period);
        View btnApply = v.findViewById(R.id.btn_apply);
        View btnCancel = v.findViewById(R.id.btn_cancel);

        // Initialize selections
        if (TextUtils.isEmpty(selectedReportType)) rbAll.setChecked(true);
        else if ("hps_monthly".equalsIgnoreCase(selectedReportType)) rbMonthly.setChecked(true);
        else if ("hps_annual".equalsIgnoreCase(selectedReportType)) rbAnnual.setChecked(true);

        tvPeriod.setText(formatPeriodForUi(selectedReportType, selectedPeriod));

        btnSelectPeriod.setOnClickListener(view -> {
            // Use MonthPickerDialog when Monthly; otherwise pick only year via DatePickerDialog year-only workaround
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int year = cal.get(java.util.Calendar.YEAR);
            int month = cal.get(java.util.Calendar.MONTH);
            if (rbMonthly.isChecked()) {
                try {
                    com.whiteelephant.monthpicker.MonthPickerDialog.Builder builder = new com.whiteelephant.monthpicker.MonthPickerDialog.Builder(
                            Dhis2ReportHistoryActivity.this,
                            (selectedMonth, selectedYear) -> {
                                selectedReportType = "hps_monthly";
                                selectedPeriod = String.format(Locale.getDefault(), "%04d%02d", selectedYear, (selectedMonth + 1));
                                tvPeriod.setText(formatPeriodForUi(selectedReportType, selectedPeriod));
                            }, year, month);
                    builder.setMinYear(year - 5).setMaxYear(year + 5).setTitle(getString(R.string.select_period)).build().show();
                } catch (Throwable t) {
                    // Fallback to DatePickerDialog
                    android.app.DatePickerDialog dp = new android.app.DatePickerDialog(Dhis2ReportHistoryActivity.this, (view1, y, m, d) -> {
                        selectedReportType = "hps_monthly";
                        selectedPeriod = String.format(Locale.getDefault(), "%04d%02d", y, (m + 1));
                        tvPeriod.setText(formatPeriodForUi(selectedReportType, selectedPeriod));
                    }, year, month, 1);
                    dp.show();
                }
            } else if (rbAnnual.isChecked()) {
                try {
                    com.whiteelephant.monthpicker.MonthPickerDialog.Builder builder = new com.whiteelephant.monthpicker.MonthPickerDialog.Builder(
                            Dhis2ReportHistoryActivity.this,
                            (selectedMonth, selectedYear) -> {
                                selectedReportType = "hps_annual";
                                selectedPeriod = String.format(Locale.getDefault(), "%04d", selectedYear);
                                tvPeriod.setText(formatPeriodForUi(selectedReportType, selectedPeriod));
                            }, year, month);
                    builder.setMinYear(year - 5).setMaxYear(year + 5).setTitle(getString(R.string.select_period)).showYearOnly().build().show();
                } catch (Throwable t) {
                    // Simple year picker fallback using DatePickerDialog (ignore month/day)
                    android.app.DatePickerDialog dp = new android.app.DatePickerDialog(Dhis2ReportHistoryActivity.this, (view12, y, m, d) -> {
                        selectedReportType = "hps_annual";
                        selectedPeriod = String.format(Locale.getDefault(), "%04d", y);
                        tvPeriod.setText(formatPeriodForUi(selectedReportType, selectedPeriod));
                    }, year, month, 1);
                    dp.show();
                }
            }
        });

        rbAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedReportType = null;
                selectedPeriod = null;
                tvPeriod.setText(formatPeriodForUi(selectedReportType, selectedPeriod));
            }
        });
        rbMonthly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedReportType = "hps_monthly";
                // keep or reset selectedPeriod depending on current format
                if (selectedPeriod != null && selectedPeriod.length() != 6) selectedPeriod = null;
                tvPeriod.setText(formatPeriodForUi(selectedReportType, selectedPeriod));
            }
        });
        rbAnnual.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedReportType = "hps_annual";
                if (selectedPeriod != null && selectedPeriod.length() != 4) selectedPeriod = null;
                tvPeriod.setText(formatPeriodForUi(selectedReportType, selectedPeriod));
            }
        });

        btnApply.setOnClickListener(v1 -> {
            reload();
            dialog.dismiss();
        });
        btnCancel.setOnClickListener(v12 -> dialog.dismiss());

        dialog.show();
    }

    private String formatPeriodForUi(String type, String period) {
        if (TextUtils.isEmpty(period)) return getString(R.string.period_not_set);
        if ("hps_monthly".equalsIgnoreCase(String.valueOf(type)) && period.length() == 6) {
            String yyyy = period.substring(0, 4);
            String mm = period.substring(4, 6);
            return mm + "-" + yyyy;
        }
        return period;
    }

    private void reload() {
        Dhis2ReportHistoryRepository repo = new Dhis2ReportHistoryRepository();
        List<Dhis2ReportHistory> list;
        if (TextUtils.isEmpty(selectedReportType) && TextUtils.isEmpty(selectedPeriod)) {
            list = repo.listAll(200, 0);
        } else {
            list = repo.list(selectedReportType, selectedPeriod, 200, 0);
        }
        adapter.setItems(list);
        renderChips();
    }

    private void renderChips() {
        if (chipContainer == null) return;
        chipContainer.removeAllViews();

        if (!TextUtils.isEmpty(selectedReportType)) {
            TextView chip = buildChip("hps_annual".equalsIgnoreCase(selectedReportType) ? getString(R.string.type_annual) : getString(R.string.type_monthly));
            chip.setOnClickListener(v -> { selectedReportType = null; reload();});
            chipContainer.addView(chip);
        } else {
            TextView chip = buildChip(getString(R.string.type_all));
            chip.setOnClickListener(v -> { /* no-op */ });
            chipContainer.addView(chip);
        }

        if (!TextUtils.isEmpty(selectedPeriod)) {
            TextView chip = buildChip(formatPeriodForUi(selectedReportType, selectedPeriod));
            chip.setOnClickListener(v -> { selectedPeriod = null; reload();});
            chipContainer.addView(chip);
        }
    }

    private TextView buildChip(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setBackgroundResource(R.drawable.chip_bg);
        tv.setPadding(24, 12, 24, 12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(8, 4, 8, 4);
        tv.setLayoutParams(lp);
        return tv;
    }
}
