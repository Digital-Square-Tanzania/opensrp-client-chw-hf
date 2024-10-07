package org.smartregister.chw.hf.activity;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.whiteelephant.monthpicker.MonthPickerDialog;

import org.smartregister.chw.hf.R;

import org.smartregister.chw.hf.adapter.VmmcViewPagerAdapter;
import org.smartregister.chw.hf.utils.ReportUtils;
import org.smartregister.view.activity.SecuredActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import timber.log.Timber;

import android.app.DatePickerDialog;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

public class VmmcReportsActivity extends SecuredActivity {

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private Menu menu;
    private String reportPeriod = ReportUtils.getDefaultReportPeriod();
    String startDate;
    String endDate;
    VmmcViewPagerAdapter vmmcViewPagerAdapter;
    private Snackbar snackbar;

    @Override
    protected void onCreation() {
        setContentView(R.layout.activity_vmmc_reports);
        setupViews();
    }


    private void setupViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());


        viewPager = findViewById(R.id.viewpager);
        tabLayout = findViewById(R.id.tabs);
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Static"));
        tabLayout.addTab(tabLayout.newTab().setText("Outreach"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        vmmcViewPagerAdapter = new VmmcViewPagerAdapter(this, getSupportFragmentManager(), tabLayout.getTabCount(), reportPeriod, startDate, endDate);
        viewPager.setAdapter(vmmcViewPagerAdapter);
    }
    @Override
    protected void onResumption() {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.vmmc_reports_menu, menu);
        this.menu = menu;
        this.menu.findItem(R.id.action_select_month).setTitle(ReportUtils.displayMonthAndYear());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_select_month) {
            showMonthPicker(this, menu);
            return true;
        }
        if (itemId == R.id.action_select_custom_filter) {
            showDateRangePicker();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showMonthPicker(Context context, Menu menu) {
    MonthPickerDialog.Builder builder = new MonthPickerDialog.Builder(context, (selectedMonth, selectedYear) -> {
        int month = selectedMonth + 1;
        String monthString = (month < 10) ? "0" + month : String.valueOf(month);
        String yearString = String.valueOf(selectedYear);
        reportPeriod = monthString + "-" + yearString;

        // Update the menu item title to display the new report period
        menu.findItem(R.id.action_select_month).setTitle(ReportUtils.displayMonthAndYear(selectedMonth, selectedYear));

        // Update the fragments in the ViewPager
        vmmcViewPagerAdapter.updateFragment(viewPager, 0, reportPeriod, null, null);
        vmmcViewPagerAdapter.updateFragment(viewPager, 1, reportPeriod, null, null);
        vmmcViewPagerAdapter.updateFragment(viewPager, 2, reportPeriod, null, null);
        if(snackbar != null){
            snackbar.dismiss();
        }

    }, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH));

    try {
        Date reportDate = new SimpleDateFormat("MM-yyyy", Locale.getDefault()).parse(reportPeriod);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(reportDate);
        builder.setActivatedMonth(calendar.get(Calendar.MONTH));
        builder.setMinYear(2021);
        builder.setActivatedYear(calendar.get(Calendar.YEAR));
        builder.setMaxYear(Calendar.getInstance().get(Calendar.YEAR));
        builder.setMinMonth(Calendar.JANUARY);
        builder.setMaxMonth(Calendar.DECEMBER);
        builder.setTitle("Select Month 0");
        builder.build().show();
    } catch (ParseException e) {
        Timber.e(e);
    }
  }

    private void showDateRangePicker() {
        // Inflate the custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.custom_date_range_picker, null);

        final DatePicker startDatePicker = dialogView.findViewById(R.id.start_date_picker);
        final DatePicker endDatePicker = dialogView.findViewById(R.id.end_date_picker);

        new AlertDialog.Builder(this)
                .setTitle("Select Date Range")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    // Retrieve the selected dates
                    String startDate = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                            startDatePicker.getYear(), startDatePicker.getMonth() + 1, startDatePicker.getDayOfMonth());
                    String endDate = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                            endDatePicker.getYear(), endDatePicker.getMonth() + 1, endDatePicker.getDayOfMonth());

                    // Display the selected date range
                    snackbar = Snackbar.make(getWindow().getDecorView(), "Selected Date Range: " + startDate + " to " + endDate, Snackbar.LENGTH_INDEFINITE);

                    // Update the fragments in the ViewPager with the selected date range
                    vmmcViewPagerAdapter.updateFragment(viewPager, 0, reportPeriod, startDate, endDate);
                    vmmcViewPagerAdapter.updateFragment(viewPager, 1, reportPeriod, startDate, endDate);
                    vmmcViewPagerAdapter.updateFragment(viewPager, 2, reportPeriod, startDate, endDate);

                    if (snackbar != null) {
                        snackbar.show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

//    private void showDateRangePicker() {
//        final Calendar calendar = Calendar.getInstance();
//        DatePickerDialog startDatePickerDialog = new DatePickerDialog(this, (view, year, monthOfYear, dayOfMonth) -> {
//            String startDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth);
//
//            DatePickerDialog endDatePickerDialog = new DatePickerDialog(this, (view1, year1, monthOfYear1, dayOfMonth1) -> {
//                String endDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year1, monthOfYear1 + 1, dayOfMonth1);
//
//                // Display the selected date range
//                snackbar = Snackbar.make(getWindow().getDecorView(),"Selected Date Range: " + startDate + " to " + endDate, Snackbar.LENGTH_INDEFINITE);
//
//                // Update the fragments in the ViewPager with the selected date range
//                vmmcViewPagerAdapter.updateFragment(viewPager, 0, reportPeriod, startDate, endDate);
//                vmmcViewPagerAdapter.updateFragment(viewPager, 1, reportPeriod, startDate, endDate);
//                vmmcViewPagerAdapter.updateFragment(viewPager, 2, reportPeriod, startDate, endDate);
//
//                if (snackbar != null){
//                    snackbar.show();
//                }
//
//
//            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
//            endDatePickerDialog.setTitle("Select End Date");
//            endDatePickerDialog.show();
//
//        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
//        startDatePickerDialog.setTitle("Select Start Date");
//        startDatePickerDialog.show();
//    }
}