package com.serphacker.serposcope.inteligenciaseo;

import com.serphacker.serposcope.models.google.GoogleSearch;

public class SearchSettings {
    private final int id;
    private final GoogleSearch search;
    private final String category;
    private final Integer volume;
    private final Integer competition;
    private final boolean adminsOnly;
    private final Double cpc;
    private final String tag;

    public SearchSettings(GoogleSearch search, String category, Integer competition, Integer volume, Boolean adminsOnly, Double cpc, String tag) {
        this.id = -1;
        this.search = search;
        this.category = category;
        this.volume = volume;
        this.adminsOnly = adminsOnly;
        this.cpc = cpc;
        this.tag = tag;
        this.competition = competition;
    }

    public int getId() {
        return id;
    }

    public int getSearchId() {
        return search.getId();
    }

    public String getCategory() {
        return category;
    }

    public Integer getVolume() {
        return volume;
    }

    public boolean isAdminsOnly() {
        return adminsOnly;
    }

    public Integer getCompetition() {
        return competition;
    }

    public Double getCPC() {
        return cpc;
    }

    public String getTag() {
        return tag;
    }
}
