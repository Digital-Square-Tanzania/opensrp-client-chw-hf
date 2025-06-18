package org.smartregister.chw.hf.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.adapter.CHWAdapter;
import org.smartregister.chw.hf.dao.ReportDao;
import org.smartregister.chw.hf.domain.CHW;
import org.smartregister.chw.hf.utils.Constants;

import java.util.List;


public class HpsReportsViewActivity extends HfReportsViewActivity {

    public static void startMe(Activity activity, String reportPath, int reportTitle, String reportDate) {
        Intent intent = new Intent(activity, HpsReportsViewActivity.class);
        intent.putExtra(ARG_REPORT_PATH, reportPath);
        intent.putExtra(ARG_REPORT_DATE, reportDate);
        intent.putExtra(ARG_REPORT_TITLE, reportTitle);
        intent.putExtra(ARG_REPORT_TYPE, Constants.ReportConstants.ReportTypes.HPS_REPORT);
        activity.startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reports_view_menu, menu);
        menu.findItem(R.id.action_view_chw_sync_status).setVisible(true);
        menu.findItem(R.id.action_upload_to_dhis2).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        int itemId = item.getItemId();
        if (itemId == R.id.action_view_chw_sync_status) {
            showCHWPopup(this, ReportDao.getChwsLastSyncDate());
            return true;
        } else if (itemId == R.id.action_upload_to_dhis2) {
            return true;
        }
        return true;
    }

    private void showCHWPopup(Context context, List<CHW> chwList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("CHWs Sync Status");

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(android.R.layout.list_content, null);

        ListView listView = new ListView(context);
        CHWAdapter adapter = new CHWAdapter(context, chwList);
        listView.setAdapter(adapter);

        builder.setView(listView);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }
}
