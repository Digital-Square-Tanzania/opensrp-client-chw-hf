package org.smartregister.chw.hf.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.listener.KvpServiceActionHandler;
import org.smartregister.chw.hf.utils.MarpVisitStatus;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.activity.BaseKvpServicesActivity;
import org.smartregister.chw.kvp.adapter.BaseServiceCardAdapter;
import org.smartregister.chw.kvp.domain.ServiceCard;
import org.smartregister.chw.kvp.handlers.BaseServiceActionHandler;
import org.smartregister.chw.kvp.util.Constants;

import java.util.Calendar;
import java.util.Collections;

public class KvpServiceActivity extends BaseKvpServicesActivity {
    private MarpVisitStatus marpVisitStatus;

    public static void startMe(Activity activity, String baseEntityID) {
        Intent intent = new Intent(activity, KvpServiceActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        activity.startActivity(intent);
    }

    @Override
    public BaseServiceActionHandler getServiceHandler() {
        return new KvpServiceActionHandler();
    }

    @Override
    protected void initializeRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        BaseServiceActionHandler serviceHandler = getServiceHandler();
        serviceCardAdapter = new BaseServiceCardAdapter(this, Collections.emptyList(), serviceHandler, baseEntityId) {
            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                holder.processVisitBtn.setVisibility(View.GONE);
                holder.serviceStatusIcon.setVisibility(View.GONE);
                super.onBindViewHolder(holder, position);

                ServiceCard card = (ServiceCard) holder.cardLayout.getTag();
                boolean submitted = marpVisitStatus.hasSubmittedCardToday(card.getServiceEventName());
                holder.servicesStatus.setVisibility(submitted ? View.VISIBLE : View.GONE);
                if (submitted) {
                    holder.servicesStatus.setText(R.string.marp_card_submitted);
                    holder.cardLayout.setOnClickListener(null);
                    holder.cardLayout.setClickable(false);
                } else {
                    // Yesterday's processing status can otherwise block today's service card.
                    holder.cardLayout.setOnClickListener(serviceHandler);
                }
            }
        };
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        recyclerView.setAdapter(serviceCardAdapter);
    }

    @Override
    protected void initializeMainServiceContainers() {
        marpVisitStatus = readMarpVisitStatus();
        if (marpVisitStatus.isCompleteToday()) {
            finish();
            return;
        }
        super.initializeMainServiceContainers();
    }

    MarpVisitStatus readMarpVisitStatus() {
        return MarpVisitStatus.read(KvpLibrary.getInstance().visitRepository(), baseEntityId, Calendar.getInstance());
    }
}
