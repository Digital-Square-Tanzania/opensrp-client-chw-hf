package org.smartregister.chw.hf.domain;

public class CHW {
    private String name;
    private String lastSyncedDate;

    public CHW(String name, String lastSyncedDate) {
        this.name = name;
        this.lastSyncedDate = lastSyncedDate;
    }

    public String getName() {
        return name;
    }

    public String getLastSyncedDate() {
        return lastSyncedDate;
    }
}