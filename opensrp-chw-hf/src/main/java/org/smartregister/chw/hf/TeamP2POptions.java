package org.smartregister.chw.hf;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.P2POptions;
import org.smartregister.repository.AllSharedPreferences;

final class TeamP2POptions extends P2POptions {
    private final AllSharedPreferences preferences;

    TeamP2POptions(AllSharedPreferences preferences) {
        super(true);
        this.preferences = preferences;
    }

    @Override
    public boolean isEnableP2PLibrary() {
        String username = preferences.fetchRegisteredANM();
        // Core uses the default team ID to open the encrypted P2P database.
        return StringUtils.isNotBlank(username) && StringUtils.isNotBlank(preferences.fetchDefaultTeamId(username));
    }
}
