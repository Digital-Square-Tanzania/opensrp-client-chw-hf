package org.smartregister.chw.hf.fragment;

import org.junit.Test;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.commonregistry.CommonPersonObjectClient;

import java.util.Collections;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.smartregister.chw.hf.utils.HfReferralUtils.REGISTER_TYPE;

public class AllClientsMarpRegisterRouteTest {

    @Test
    public void reopeningRowClassifiedAsMarpAfterEnrollmentUsesAllClientsProfileRoute() {
        AllClientsRegisterFragment register = mock(AllClientsRegisterFragment.class);
        CommonPersonObjectClient enrolledRow = new CommonPersonObjectClient("synthetic-client",
                Collections.singletonMap(REGISTER_TYPE, CoreConstants.REGISTER_TYPE.KVP), "");
        doCallRealMethod().when(register).goToClientDetailActivity(enrolledRow);

        register.goToClientDetailActivity(enrolledRow);

        verify(register).openGeneralProfileForMarpClient(enrolledRow);
    }
}
