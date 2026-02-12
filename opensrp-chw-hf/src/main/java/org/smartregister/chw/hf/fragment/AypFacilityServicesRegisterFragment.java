package org.smartregister.chw.hf.fragment;

import android.view.View;

import org.smartregister.chw.core.fragment.CoreAypRegisterFragment;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.activity.AypFacilityServicesProfileActivity;
import org.smartregister.chw.hf.model.AypFacilityServicesRegisterFragmentModel;
import org.smartregister.chw.hf.presenter.AypFacilityServicesRegisterPresenter;
import org.smartregister.view.customcontrols.CustomFontTextView;

public class AypFacilityServicesRegisterFragment extends CoreAypRegisterFragment {

    @Override
    protected void openProfile(String baseEntityId) {
        AypFacilityServicesProfileActivity.startProfileActivity(getActivity(), baseEntityId);
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new AypFacilityServicesRegisterPresenter(this, new AypFacilityServicesRegisterFragmentModel(), null);
    }

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        CustomFontTextView titleView = view.findViewById(org.smartregister.hivst.R.id.txt_title_label);
        if (titleView != null) {
            titleView.setText(getString(R.string.ayp_facility_services_register_title));
            titleView.setPadding(0, titleView.getTop(), titleView.getPaddingRight(), titleView.getPaddingBottom());
        }
    }
}
