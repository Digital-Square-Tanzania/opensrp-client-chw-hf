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
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.chw.kvp.util.Constants;

import java.util.Calendar;
import java.util.Collections;

public class KvpServiceActivity extends BaseKvpServicesActivity {
    private static final String EXTRA_PROCESSING_ONLY = "marp_processing_only";

    private MarpVisitStatus marpVisitStatus;
    private Calendar serviceDay;

    public static void startMe(Activity activity, String baseEntityId) {
        start(activity, baseEntityId, false);
    }

    static void startProcessing(Activity activity, String baseEntityId) {
        start(activity, baseEntityId, true);
    }

    private static void start(Activity activity, String baseEntityId, boolean processingOnly) {
        Intent intent = new Intent(activity, KvpServiceActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(EXTRA_PROCESSING_ONLY, processingOnly);
        activity.startActivity(intent);
    }

    @Override
    public KvpServiceActionHandler getServiceHandler() {
        return new KvpServiceActionHandler();
    }

    @Override
    protected void initializeRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        KvpServiceActionHandler serviceHandler = getServiceHandler();
        serviceCardAdapter = new BaseServiceCardAdapter(this, Collections.emptyList(), serviceHandler, baseEntityId) {
            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                holder.processVisitBtn.setVisibility(View.GONE);
                holder.serviceStatusIcon.setVisibility(View.GONE);
                super.onBindViewHolder(holder, position);

                ServiceCard card = (ServiceCard) holder.cardLayout.getTag();
                String type = card.getServiceEventName();
                boolean submitted = marpVisitStatus.hasSubmittedCardToday(type);
                boolean processingOnly = isProcessingOnly();
                Calendar displayedDay = serviceDay;
                VisitRepository repository = KvpLibrary.getInstance().visitRepository();
                Visit target = MarpVisitStatus.findProcessableCard(repository, baseEntityId, type,
                        processingOnly ? displayedDay : null);
                boolean offerProcessing = serviceHandler.canOfferProcessVisit(target);
                holder.processVisitBtn.setVisibility(offerProcessing ? View.VISIBLE : View.GONE);
                holder.processVisitBtn.setOnClickListener(offerProcessing
                        ? view -> serviceHandler.confirmProcessVisit(KvpServiceActivity.this, baseEntityId,
                                target, displayedDay, processingOnly) : null);
                holder.servicesStatus.setVisibility(submitted ? View.VISIBLE : View.GONE);
                if (submitted) {
                    holder.servicesStatus.setText(R.string.marp_card_submitted);
                }
                if (submitted || processingOnly) {
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
        serviceDay = Calendar.getInstance();
        marpVisitStatus = readMarpVisitStatus();
        if (marpVisitStatus.isCompleteToday() != isProcessingOnly()) {
            finish();
            return;
        }
        super.initializeMainServiceContainers();
    }

    MarpVisitStatus readMarpVisitStatus() {
        return MarpVisitStatus.read(KvpLibrary.getInstance().visitRepository(), baseEntityId, serviceDay);
    }

    boolean isProcessingOnly() {
        return getIntent().getBooleanExtra(EXTRA_PROCESSING_ONLY, false);
    }
}
