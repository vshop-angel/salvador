package com.serphacker.serposcope.inteligenciaseo;

public class SearchSettings {
    private final int id;
    private final int groupId;
    private final int searchId;
    private final String category;
    private final String volume;
    private final boolean adminsOnly;

    SearchSettings(int id, int groupId, int searchId, String category, String volume, boolean adminsOnly) {
        this.id = id;
        this.groupId = groupId;
        this.searchId = searchId;
        this.category = category;
        this.volume = volume;
        this.adminsOnly = adminsOnly;
    }

    public SearchSettings(Integer groupId, Integer searchId, String category, String volume, Boolean adminsOnly) {
        this.id = -1;
        this.groupId = groupId;
        this.searchId = searchId;
        this.category = category;
        this.volume = volume;
        this.adminsOnly = adminsOnly;
    }

    public int getId() {
        return id;
    }

    public int getGroupId() {
        return groupId;
    }

    public int getSearchId() {
        return searchId;
    }

    public String getCategory() {
        return category;
    }

    public String getVolume() {
        return volume;
    }

    public boolean isAdminsOnly() {
        return adminsOnly;
    }
}
