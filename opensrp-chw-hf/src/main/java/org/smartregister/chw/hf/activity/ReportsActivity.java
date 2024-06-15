package org.smartregister.chw.hf.activity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.appbar.AppBarLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.smartregister.chw.core.job.ChwIndicatorGeneratingJob;
import org.smartregister.chw.hf.HealthFacilityApplication;
import org.smartregister.chw.hf.R;
import org.smartregister.reporting.domain.TallyStatus;
import org.smartregister.reporting.event.IndicatorTallyEvent;
import org.smartregister.util.Utils;
import org.smartregister.view.activity.SecuredActivity;
import org.smartregister.view.customcontrols.CustomFontTextView;

public class ReportsActivity extends SecuredActivity implements View.OnClickListener {
    protected CustomFontTextView toolBarTextView;
    protected AppBarLayout appBarLayout;
    protected ConstraintLayout pmtctReportsLayout;
    protected ConstraintLayout ancReportsLayout;
    protected ConstraintLayout pncReportsLayout;
    protected ConstraintLayout cbhsReportsLayout;
    protected ConstraintLayout ltfuSummaryLayout;
    protected ConstraintLayout ldReportsLayout;
    protected ConstraintLayout motherChampionReportsLayout;
    protected ConstraintLayout selfTestingReports;
    protected ConstraintLayout condomDistributionReports;
    protected ConstraintLayout kvpReports;

    protected ConstraintLayout fpReportsLayout;

    protected ConstraintLayout asrhReportsLayout;

    protected ConstraintLayout cecapReportsLayout;

    protected ConstraintLayout vmcReports;

    protected TextView textViewLogs;

    @Override
    protected void onCreation() {
        ChwIndicatorGeneratingJob.scheduleJobImmediately(ChwIndicatorGeneratingJob.TAG);
//        GenerateMonthlyTalliesJob.scheduleJobImmediately(GenerateMonthlyTalliesJob.TAG);
        setContentView(R.layout.activity_reports);
        setUpToolbar();
        setUpViews();
    }

    public void setUpViews() {
        pmtctReportsLayout = findViewById(R.id.pmtct_reports);
        ancReportsLayout = findViewById(R.id.anc_reports);
        pncReportsLayout = findViewById(R.id.pnc_reports);
        cbhsReportsLayout = findViewById(R.id.cbhs_reports);
        ltfuSummaryLayout = findViewById(R.id.ltfu_summary);
        ldReportsLayout = findViewById(R.id.ld_reports);
        motherChampionReportsLayout = findViewById(R.id.mother_champion_reports);
        selfTestingReports = findViewById(R.id.self_testing_reports);
        condomDistributionReports = findViewById(R.id.cdp_reports);
        kvpReports = findViewById(R.id.kvp_reports);
        vmcReports = findViewById(R.id.vmmc_reports);
        fpReportsLayout = findViewById(R.id.fp_reports);
        asrhReportsLayout = findViewById(R.id.asrh_reports);
        cecapReportsLayout = findViewById(R.id.cecap_reports);
        textViewLogs = findViewById(R.id.textView_logs);

        if (HealthFacilityApplication.getApplicationFlavor().hasLD())
            ldReportsLayout.setVisibility(View.VISIBLE);

        if (HealthFacilityApplication.getApplicationFlavor().hasHivst())
            selfTestingReports.setVisibility(View.VISIBLE);

        if (HealthFacilityApplication.getApplicationFlavor().hasCdp())
            condomDistributionReports.setVisibility(View.VISIBLE);

        if (HealthFacilityApplication.getApplicationFlavor().hasKvpPrEP())
            kvpReports.setVisibility(View.VISIBLE);


        pmtctReportsLayout.setOnClickListener(this);
        ancReportsLayout.setOnClickListener(this);
        pncReportsLayout.setOnClickListener(this);
        cbhsReportsLayout.setOnClickListener(this);
        ltfuSummaryLayout.setOnClickListener(this);
        ldReportsLayout.setOnClickListener(this);
        motherChampionReportsLayout.setOnClickListener(this);
        selfTestingReports.setOnClickListener(this);
        condomDistributionReports.setOnClickListener(this);
        kvpReports.setOnClickListener(this);
        fpReportsLayout.setOnClickListener(this);
        vmcReports.setOnClickListener(this);
        asrhReportsLayout.setOnClickListener(this);
        cecapReportsLayout.setOnClickListener(this);
    }

    public void setUpToolbar() {
        Toolbar toolbar = findViewById(org.smartregister.chw.core.R.id.back_to_nav_toolbar);
        toolBarTextView = toolbar.findViewById(org.smartregister.chw.core.R.id.toolbar_title);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            final Drawable upArrow = getResources().getDrawable(org.smartregister.chw.core.R.drawable.ic_arrow_back_white_24dp);
            actionBar.setHomeAsUpIndicator(upArrow);
            actionBar.setElevation(0);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        toolBarTextView.setText(R.string.reports_title);
        toolBarTextView.setOnClickListener(v -> finish());
        appBarLayout = findViewById(org.smartregister.chw.core.R.id.app_bar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appBarLayout.setOutlineProvider(null);
        }
    }

    @Override
    protected void onResumption() {
        //overridden
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.anc_reports) {
            startActivity(new Intent(this, AncReportsActivity.class));
        } else if (id == R.id.pmtct_reports) {
            startActivity(new Intent(this, PmtctReportsActivity.class));
        } else if (id == R.id.pnc_reports) {
            startActivity(new Intent(this, PncReportsActivity.class));
        } else if (id == R.id.cbhs_reports) {
            startActivity(new Intent(this, CbhsReportsActivity.class));
        } else if (id == R.id.ltfu_summary) {
            startActivity(new Intent(this, LtfuSummaryActivity.class));
        } else if (id == R.id.ld_reports) {
            startActivity(new Intent(this, LdReportsActivity.class));
        } else if (id == R.id.mother_champion_reports) {
            startActivity(new Intent(this, MotherChampionReportsActivity.class));
        } else if (id == R.id.self_testing_reports) {
            Intent intent = new Intent(this, SelfTestingReportsActivity.class);
            startActivity(intent);
        } else if (id == R.id.cdp_reports) {
            Intent intent = new Intent(this, CdpReportsActivity.class);
            startActivity(intent);
        } else if (id == R.id.kvp_reports) {
            Intent intent = new Intent(this, KvpReportsActivity.class);
            startActivity(intent);
        } else if (id == R.id.fp_reports) {
            Intent intent = new Intent(this, FpReportsActivity.class);
            startActivity(intent);
        } else if (id == R.id.vmmc_reports) {
            Intent intent = new Intent(this, VmmcReportsActivity.class);
            startActivity(intent);
        } else if (id == R.id.asrh_reports) {
            Intent intent = new Intent(this, AsrhReportsActivity.class);
            startActivity(intent);
        } else if (id == R.id.cecap_reports) {
            Intent intent = new Intent(this, CecapReportsActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);

    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(IndicatorTallyEvent event) {
        if (event.getStatus().equals(TallyStatus.STARTED)) {
            textViewLogs.setVisibility(View.VISIBLE);
            textViewLogs.setText("Started Refreshing Reports");
            Utils.showToast(this, "Started Refreshing Reports");
        } else if (event.getStatus().equals(TallyStatus.INPROGRESS)) {
            textViewLogs.setVisibility(View.VISIBLE);
            if (event.getMessage() != null) {
                textViewLogs.setText(event.getMessage());
            } else {
                Utils.showToast(this, "Refreshing Reports is In-Progress");
            }
        } else if (event.getStatus().equals(TallyStatus.COMPLETE)) {
            textViewLogs.setVisibility(View.GONE);
            Utils.showToast(this, "Finished Refreshing Reports");
        }
    }
}