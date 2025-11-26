package org.smartregister.chw.hf.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.domain.dhis2_reports.DhisDataValues;

import java.util.ArrayList;
import java.util.List;

public class Dhis2DataValuesAdapter extends RecyclerView.Adapter<Dhis2DataValuesAdapter.VH> {

    private final List<DhisDataValues> items = new ArrayList<>();

    public void setItems(List<DhisDataValues> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dhis2_data_value, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DhisDataValues dv = items.get(position);
        h.dataElement.setText(dv.getDataElement());
        h.categoryOptionCombo.setText(dv.getCategoryOptionCombo());
        h.value.setText(String.valueOf(dv.getValue()));
        String comment = dv.getComment();
        if (comment == null || comment.trim().isEmpty()) {
            h.comment.setVisibility(View.GONE);
        } else {
            h.comment.setVisibility(View.VISIBLE);
            h.comment.setText(comment);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView dataElement, categoryOptionCombo, value, comment;
        VH(@NonNull View itemView) {
            super(itemView);
            dataElement = itemView.findViewById(R.id.tv_data_element);
            categoryOptionCombo = itemView.findViewById(R.id.tv_coc);
            value = itemView.findViewById(R.id.tv_value);
            comment = itemView.findViewById(R.id.tv_comment);
        }
    }
}

