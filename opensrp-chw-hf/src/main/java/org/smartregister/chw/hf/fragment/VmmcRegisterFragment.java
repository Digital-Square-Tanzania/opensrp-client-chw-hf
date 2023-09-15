package org.smartregister.chw.hf.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.google.android.material.tabs.TabLayout;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.core.fragment.CoreVmmcRegisterFragment;
import org.smartregister.chw.core.model.CoreVmmcRegisterFragmentModel;
import org.smartregister.chw.core.provider.ChwVmmcRegisterProvider;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.activity.VmmcProfileActivity;
import org.smartregister.chw.hf.presenter.VmmcRegisterFragmentPresenter;
import org.smartregister.chw.hf.provider.HfVmmcRegisterProvider;
import org.smartregister.configurableviews.model.View;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.family.util.DBConstants;
import org.smartregister.view.activity.BaseRegisterActivity;

import java.text.MessageFormat;
import java.util.Set;

import timber.log.Timber;

public class VmmcRegisterFragment extends CoreVmmcRegisterFragment {

    String customGroupFilter;

    @Override
    public void initializeAdapter(Set<View> visibleColumns) {
        ChwVmmcRegisterProvider vmmcRegisterProvider = new HfVmmcRegisterProvider(getActivity(), paginationViewHandler, registerActionHandler, visibleColumns);
        clientAdapter = new RecyclerViewPaginatedAdapter(null, vmmcRegisterProvider, context().commonrepository(this.tablename));
        clientAdapter.setCurrentlimit(20);
        clientsView.setAdapter(clientAdapter);
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        String viewConfigurationIdentifier = ((BaseRegisterActivity) getActivity()).getViewIdentifiers().get(0);
        presenter = new VmmcRegisterFragmentPresenter(this, new CoreVmmcRegisterFragmentModel(), viewConfigurationIdentifier);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, final Bundle args) {
        if (id == LOADER_ID) {
            return new CursorLoader(getActivity()) {
                @Override
                public Cursor loadInBackground() {
                    // Count query
                    final String COUNT = "count_execute";
                    if (args != null && args.getBoolean(COUNT)) {
                        countExecute();
                    }
                    String query = defaultFilterAndSortQuery();
                    return commonRepository().rawCustomQueryForAdapter(query);
                }
            };
        }
        return super.onCreateLoader(id, args);
    }

    private String defaultFilterAndSortQuery() {
        SmartRegisterQueryBuilder sqb = new SmartRegisterQueryBuilder(mainSelect);

        String query = "";
        StringBuilder customFilter = new StringBuilder();
        if (StringUtils.isNotBlank(filters)) {
            customFilter.append(MessageFormat.format(" and ( {0}.{1} like ''%{2}%'' ", CoreConstants.TABLE_NAME.FAMILY_MEMBER, DBConstants.KEY.FIRST_NAME, filters));
            customFilter.append(MessageFormat.format(" or {0}.{1} like ''%{2}%'' ", CoreConstants.TABLE_NAME.FAMILY_MEMBER, DBConstants.KEY.LAST_NAME, filters));
            customFilter.append(MessageFormat.format(" or {0}.{1} like ''%{2}%'' ", CoreConstants.TABLE_NAME.FAMILY_MEMBER, DBConstants.KEY.MIDDLE_NAME, filters));
            customFilter.append(MessageFormat.format(" or {0}.{1} like ''%{2}%'' ) ", CoreConstants.TABLE_NAME.FAMILY_MEMBER, DBConstants.KEY.UNIQUE_ID, filters));
        }


        if (StringUtils.isNotBlank(customGroupFilter)) {
            customFilter.append(MessageFormat.format((" and ( {0} ) "), customGroupFilter));
        }
        try {
            sqb.addCondition(customFilter.toString());
            query = sqb.orderbyCondition(Sortqueries);
            query = sqb.Endquery(sqb.addlimitandOffset(query, clientAdapter.getCurrentlimit(), clientAdapter.getCurrentoffset()));
        } catch (Exception e) {
            Log.e(getClass().getName(), e.toString(), e);
        }
        return query;
    }

    @Override
    public void countExecute() {
        Cursor c = null;
        try {

            String query = "select count(*) from " + presenter().getMainTable() + " inner join " + CoreConstants.TABLE_NAME.FAMILY_MEMBER +
                    " on " + presenter().getMainTable() + "." + org.smartregister.chw.anc.util.DBConstants.KEY.BASE_ENTITY_ID + " = " +
                    CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + org.smartregister.chw.anc.util.DBConstants.KEY.BASE_ENTITY_ID +
                    " where " + presenter().getMainCondition();

            if (StringUtils.isNotBlank(filters)) {
                query = query + " and ( " + CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.FIRST_NAME + "like ''%" + filters + "%'' ";
                query = query + " or " + CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.LAST_NAME + "like ''%" + filters + "%'' ";
                query = query + " or " + CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.MIDDLE_NAME + "like ''%" + filters + "%'' ";
                query = query + " 0r " + CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.UNIQUE_ID + "like ''%" + filters + "%'' )";

            }


            if (StringUtils.isNotBlank(customGroupFilter)) {
                query = query + " and ( " + customGroupFilter + " ) ";
            }


            c = commonRepository().rawCustomQueryForAdapter(query);
            c.moveToFirst();
            clientAdapter.setTotalcount(c.getInt(0));
            Timber.v("total count here %s", clientAdapter.getTotalcount());

            clientAdapter.setCurrentlimit(20);
            clientAdapter.setCurrentoffset(0);

        } catch (Exception e) {
            Timber.e(e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    @Override
    public void setupViews(android.view.View view) {
        super.setupViews(view);
        setUpTabLayout(view, true);
    }

    @Override
    protected void openProfile(String baseEntityId) {
        VmmcProfileActivity.startVmmcActivity(getActivity(), baseEntityId);
    }

    protected void setUpTabLayout(android.view.View view, boolean enabled) {
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        if (enabled) {
            tabLayout.setVisibility(android.view.View.VISIBLE);
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    switch (tab.getPosition()) {
                        case 0:
                            customGroupFilter = "";
                            filterandSortExecute();
                            break;
                        case 1:
                            customGroupFilter = getDueTomorrow();
                            filterandSortExecute();
                            break;
                        case 2:
                            customGroupFilter = getMissed();
                            filterandSortExecute();
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    //do something
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    //do something
                }
            });
        }

    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_hei_register;
    }

    protected String getDueTomorrow() {
        return "ec_vmmc_enrollment.base_entity_id IN (SELECT ec_vmmc_follow_up_visit.entity_id  FROM ec_vmmc_follow_up_visit INNER JOIN ec_family_member efm ON efm.base_entity_id = ec_vmmc_follow_up_visit.entity_id and CASE WHEN ec_vmmc_follow_up_visit.next_followup_date is not null THEN date(substr(ec_vmmc_follow_up_visit.next_followup_date, 7, 4) || '-' || substr(ec_vmmc_follow_up_visit.next_followup_date, 4, 2) || '-' || substr(ec_vmmc_follow_up_visit.next_followup_date, 1, 2)) = date('now', '+1 day') END)";
    }

    protected String getMissed() {
        return "ec_vmmc_enrollment.base_entity_id IN (SELECT ec_vmmc_follow_up_visit.entity_id  FROM ec_vmmc_follow_up_visit INNER JOIN ec_family_member efm ON efm.base_entity_id = ec_vmmc_follow_up_visit.entity_id and CASE WHEN ec_vmmc_follow_up_visit.next_followup_date is not null THEN date(substr(ec_vmmc_follow_up_visit.next_followup_date, 7, 4) || '-' || substr(ec_vmmc_follow_up_visit.next_followup_date, 4, 2) || '-' || substr(ec_vmmc_follow_up_visit.next_followup_date, 1, 2)) < date('now') END)";
    }

    @Override
    protected String getDefaultSortQuery() {
        return "";
    }
}
