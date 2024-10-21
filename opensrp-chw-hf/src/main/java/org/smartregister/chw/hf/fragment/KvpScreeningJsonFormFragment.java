package org.smartregister.chw.hf.fragment;

import android.os.Bundle;

import com.vijay.jsonwizard.fragments.JsonWizardFormFragment;
import com.vijay.jsonwizard.interactors.JsonFormInteractor;
import com.vijay.jsonwizard.presenters.JsonFormFragmentPresenter;

import org.smartregister.chw.hf.presenter.KvpScreeningJsonFormFragmentPresenter;

public class KvpScreeningJsonFormFragment extends JsonWizardFormFragment {

    public static KvpScreeningJsonFormFragment getFormFragment(String stepName) {
        KvpScreeningJsonFormFragment jsonFormFragment = new KvpScreeningJsonFormFragment();
        Bundle bundle = new Bundle();
        bundle.putString("stepName", stepName);
        jsonFormFragment.setArguments(bundle);
        return jsonFormFragment;
    }

    @Override
    protected JsonFormFragmentPresenter createPresenter() {
        return new KvpScreeningJsonFormFragmentPresenter(this, JsonFormInteractor.getInstance());
    }
}