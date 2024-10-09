package org.smartregister.chw.hf.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.activity.VmmcReportsViewActivity;
import org.smartregister.chw.hf.utils.Constants;

import timber.log.Timber;

public class VmmcOutreachReportsFragment extends Fragment implements View.OnClickListener{

    protected ConstraintLayout vmmc_monthly_report;
    private AppCompatTextView vmmc_monthly_report_title;

    protected ConstraintLayout vmmc_register_report;
    private AppCompatTextView vmmc_register_title;

    protected ConstraintLayout vmmc_theatre_register_report;
    private AppCompatTextView vmmc_theatre_register_title;

    private String reportPeriod;
    private String startDate;
    private String endDate;

    public VmmcOutreachReportsFragment(){

    }

    public VmmcOutreachReportsFragment(String reportPeriod, String startDate, String endDate){
        this.reportPeriod = reportPeriod;
        this.startDate = startDate;
        this.endDate = endDate;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_vmmc_outreach_reports_tab, container, false);
        setupViews(view);
        return view;
    }

    public void setupViews(View view) {
        vmmc_monthly_report = view.findViewById(R.id.vmmc_report);
        vmmc_monthly_report_title = view.findViewById(R.id.vmmc_report_title);
        vmmc_register_report = view.findViewById(R.id.vmmc_register_report);
        vmmc_register_title = view.findViewById(R.id.vmmc_register_title);
        vmmc_theatre_register_report = view.findViewById(R.id.vmmc_theatre_register);
        vmmc_theatre_register_title = view.findViewById(R.id.vmmc_theatre_register_title);

        vmmc_monthly_report_title.setText("Vmmc Outreach Reports");
        vmmc_register_title.setText("Vmmc Outreach Register Repot");
        vmmc_theatre_register_title.setText("Vmmc Outreach Theatre Register Report");

        vmmc_monthly_report.setOnClickListener(this);
        vmmc_monthly_report.setOnClickListener(this);
        vmmc_register_report.setOnClickListener(this);
        vmmc_theatre_register_report.setOnClickListener(this);

    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.vmmc_report) {
            VmmcReportsViewActivity.startMe(getActivity(),  Constants.ReportConstants.ReportPaths.VMMC_OUTREACH_REPORT_PATH,R.string.vmmc_reports_subtitle, reportPeriod, startDate, endDate);
        }
        if (id == R.id.vmmc_register_report) {
            VmmcReportsViewActivity.startMe(getActivity(),  Constants.ReportConstants.ReportPaths.VMMC_OUTREACH_REGISTER_PATH,R.string.vmmc_register_subtitle, reportPeriod, startDate, endDate);
        }
        if (id == R.id.vmmc_theatre_register) {
            VmmcReportsViewActivity.startMe(getActivity(),  Constants.ReportConstants.ReportPaths.VMMC_OUTREACH_THEATRE_REGISTER_PATH,R.string.vmmc_theatre_register_subtitle, reportPeriod, startDate, endDate);
        }
    }

    public void updateReportPeriod(String newReportPeriod, String newStartDate, String newEndDate) {
        // Update any UI or perform necessary actions with the new report period
        Timber.e("Updated reportPeriod: " + newReportPeriod);
        Timber.e("Updated range date:: " + newStartDate+" to "+newEndDate);
        this.reportPeriod = newReportPeriod;
        this.startDate = newStartDate;
        this.endDate = newEndDate;
    }

}