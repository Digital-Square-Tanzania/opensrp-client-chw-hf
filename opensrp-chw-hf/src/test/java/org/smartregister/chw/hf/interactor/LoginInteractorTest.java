package org.smartregister.chw.hf.interactor;

import android.content.Context;

import org.junit.Test;
import org.mockito.Mockito;
import org.smartregister.view.contract.BaseLoginContract;

import static org.junit.Assert.assertSame;

public class LoginInteractorTest {

    @Test
    public void applicationContextRemainsAvailableAfterPresenterIsDestroyed() {
        BaseLoginContract.Presenter presenter = Mockito.mock(BaseLoginContract.Presenter.class);
        Context applicationContext = Mockito.mock(Context.class);
        LoginInteractor interactor = new LoginInteractor(presenter, applicationContext);

        interactor.onDestroy(false);

        assertSame(applicationContext, interactor.getApplicationContext());
    }
}
