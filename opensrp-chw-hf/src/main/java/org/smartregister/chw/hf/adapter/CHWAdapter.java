package org.smartregister.chw.hf.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.domain.CHW;

import java.util.List;

public class CHWAdapter extends ArrayAdapter<CHW> {

    public CHWAdapter(Context context, List<CHW> chwList) {
        super(context, 0, chwList);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CHW chw = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_chw, parent, false);
        }

        TextView name = convertView.findViewById(R.id.chw_name);
        TextView date = convertView.findViewById(R.id.chw_synced_date);

        name.setText(chw.getName());
        date.setText("Last Synced: " + chw.getLastSyncedDate());

        return convertView;
    }
}