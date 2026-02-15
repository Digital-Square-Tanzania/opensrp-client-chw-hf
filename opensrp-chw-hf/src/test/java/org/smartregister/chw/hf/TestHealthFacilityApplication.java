package org.smartregister.chw.hf;

/**
 * Test application used by Robolectric unit tests.
 *
 * The production application teardown touches the encrypted DB and requires a runtime password,
 * which is not initialized in these tests.
 */
public class TestHealthFacilityApplication extends HealthFacilityApplication {

    @Override
    public void onTerminate() {
        // No-op in unit tests to avoid teardown-time DB access.
    }
}
