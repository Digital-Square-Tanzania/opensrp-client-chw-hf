package org.smartregister.chw.hf.adapter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.smartregister.chw.hf.fragment.VmmcAllReportsFragment;
import org.smartregister.chw.hf.fragment.VmmcOutreachReportsFragment;
import org.smartregister.chw.hf.fragment.VmmcStaticReportsFragment;

public class VmmcViewPagerAdapter extends FragmentPagerAdapter {

    private Context context;
    private String reportPeriod;
    private String startDate;
    private String endDate;
    private int totalTabs;
    private FragmentManager fragmentManager;

    public VmmcViewPagerAdapter(Context context, FragmentManager fm, int totalTabs, String reportPeriod, String startDate, String endDate) {
        super(fm);
        fragmentManager = fm;
        this.context = context;
        this.totalTabs = totalTabs;
        this.reportPeriod = reportPeriod;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new VmmcAllReportsFragment(reportPeriod, startDate, endDate);
            case 1:
                return new VmmcStaticReportsFragment(reportPeriod, startDate, endDate);
            case 2:
                return new VmmcOutreachReportsFragment(reportPeriod, startDate, endDate);
            default:
                return null;
        }
    }

    public int getCount() {
        return totalTabs;
    }

    public void updateFragment(ViewPager viewPager, int position, String newReportPeriod, String newStartDate, String newEndDate) {
        String fragmentTag = "android:switcher:" + viewPager.getId() + ":" + position; // Create the tag for the fragment
        Fragment fragment = fragmentManager.findFragmentByTag(fragmentTag);
        if (fragment != null && fragment instanceof VmmcAllReportsFragment) {
            ((VmmcAllReportsFragment) fragment).updateReportPeriod(newReportPeriod, newStartDate, newEndDate);
        } else if (fragment != null && fragment instanceof VmmcStaticReportsFragment) {
            ((VmmcStaticReportsFragment) fragment).updateReportPeriod(newReportPeriod, newStartDate, newEndDate);
        } else if (fragment != null && fragment instanceof VmmcOutreachReportsFragment) {
            ((VmmcOutreachReportsFragment) fragment).updateReportPeriod(newReportPeriod, newStartDate, newEndDate);
        }
    }
}