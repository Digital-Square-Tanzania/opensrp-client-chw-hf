package org.smartregister.chw.hf;

import org.junit.Test;
import org.smartregister.repository.AllSharedPreferences;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TeamP2POptionsTest {
    @Test
    public void enablesP2PWhenTheRegisteredUserHasADefaultTeam() {
        AllSharedPreferences preferences = mock(AllSharedPreferences.class);
        TeamP2POptions options = new TeamP2POptions(preferences);

        when(preferences.fetchRegisteredANM()).thenReturn("");
        assertFalse(options.isEnableP2PLibrary());

        when(preferences.fetchRegisteredANM()).thenReturn("provider");
        assertFalse(options.isEnableP2PLibrary());

        when(preferences.fetchDefaultTeamId("provider")).thenReturn(" ");
        assertFalse(options.isEnableP2PLibrary());

        when(preferences.fetchDefaultTeamId("provider")).thenReturn("team-id");
        assertTrue(options.isEnableP2PLibrary());

        when(preferences.fetchRegisteredANM()).thenReturn("different-provider");
        assertFalse(options.isEnableP2PLibrary());
    }
}
