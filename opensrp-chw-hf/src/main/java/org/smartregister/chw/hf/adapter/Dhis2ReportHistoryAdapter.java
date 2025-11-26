package org.smartregister.chw.hf.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.domain.dhis2_reports.Dhis2ReportHistory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Dhis2ReportHistoryAdapter extends RecyclerView.Adapter<Dhis2ReportHistoryAdapter.VH> {

    public interface OnClick {
        void onClick(Dhis2ReportHistory item);
    }

    private final List<Dhis2ReportHistory> items = new ArrayList<>();
    private final OnClick onClick;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    public Dhis2ReportHistoryAdapter(OnClick onClick) {
        this.onClick = onClick;
    }

    public void setItems(List<Dhis2ReportHistory> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dhis2_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Dhis2ReportHistory it = items.get(position);
        h.title.setText(h.itemView.getContext().getString(R.string.dhis2_history_title_line,
                it.period != null ? it.period : h.itemView.getContext().getString(R.string.na)));
        String typeLbl = "hps_annual".equalsIgnoreCase(it.reportType) ? h.itemView.getContext().getString(R.string.hps_annual_reports_title) : h.itemView.getContext().getString(R.string.hps_monthly_reports_title);
        String sub = h.itemView.getContext().getString(R.string.dhis2_history_sub_line_with_type,
                typeLbl,
                it.eventDate != null ? sdf.format(new Date(it.eventDate)) : h.itemView.getContext().getString(R.string.na));
        h.subtitle.setText(sub);
        h.itemView.setOnClickListener(v -> { if (onClick != null) onClick.onClick(it); });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_title);
            subtitle = itemView.findViewById(R.id.tv_subtitle);
        }
    }
}
