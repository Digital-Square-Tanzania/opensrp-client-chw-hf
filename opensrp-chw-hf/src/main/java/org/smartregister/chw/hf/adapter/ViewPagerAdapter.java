package org.smartregister.chw.hf.adapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import org.smartregister.chw.hf.fragment.TabFragment;

public class ViewPagerAdapter extends FragmentPagerAdapter {

    private String title[] = {"All", "Static", "Outreach"};

    public ViewPagerAdapter(FragmentManager manager) {
        super(manager);
    }

    @Override
    public Fragment getItem(int position) {
        return TabFragment.getInstance(position);
    }

    @Override
    public int getCount() {
        return title.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return title[position];
    }
}