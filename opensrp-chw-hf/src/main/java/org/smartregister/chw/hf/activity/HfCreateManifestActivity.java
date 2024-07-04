package org.smartregister.chw.hf.activity;

import org.smartregister.chw.hf.repository.UniqueLabTestSampleTrackingIdRepository;
import org.smartregister.chw.lab.activity.CreateManifestActivity;
import org.smartregister.chw.lab.util.LabUtil;
import org.smartregister.domain.UniqueId;

public class HfCreateManifestActivity extends CreateManifestActivity {
    @Override
    public String generateManifestId() {
        UniqueLabTestSampleTrackingIdRepository repository = new UniqueLabTestSampleTrackingIdRepository();
        UniqueId uniqueId = repository.getNextUniqueId();
        String manifestId = uniqueId.getOpenmrsId();
        repository.close(manifestId);

        return "9" + LabUtil.getHfrCode() + manifestId;
    }
}
