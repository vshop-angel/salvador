/*
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */

package com.serphacker.serposcope.models.google;

import com.google.common.collect.MinMaxPriorityQueue;
import com.serphacker.serposcope.inteligenciaseo.SearchSettingsDB;
import com.serphacker.serposcope.models.base.User;

import java.util.*;


public class GoogleTargetSummary {
    
    public final static class RankComparator implements Comparator<GoogleRank> {
        @Override
        public int compare(GoogleRank o1, GoogleRank o2) {
            int x = Integer.compare(o1.rank, o2.rank);
            if(x == 0){
                return Integer.compare(o1.diff, o2.diff);
            }
            return x;
        }
    }
    
    public final static class ImprovementComparator implements Comparator<GoogleRank> {
        @Override
        public int compare(GoogleRank o1, GoogleRank o2) {
            return Integer.compare(o1.diff, o2.diff);
        }
    }    
    
    public final static class TopLostComparator implements Comparator<GoogleRank> {
        @Override
        public int compare(GoogleRank o1, GoogleRank o2) {
            int x = Integer.compare(o1.previousRank, o2.previousRank);
            if(x == 0){
                return -Integer.compare(o1.diff, o2.diff);
            }
            return x;
        }
    }
    
    public final static RankComparator RANK_COMPARATOR = new RankComparator();
    public final static ImprovementComparator IMPROVEMENT_COMPARATOR = new ImprovementComparator();
    public final static TopLostComparator TOP_LOST_COMPARATOR = new TopLostComparator();
    
    public final static int TOP_SIZE = 5;
    
    private int groupId;
    private int targetId;
    private int runId;
    
    private int totalTop3;
    private int totalTop10;
    private int totalTop100;
    private int totalOut;
    
    private int scoreRaw;
    private int scoreBP;

    private int previousScoreRaw;
    private int previousScoreBP;
    private int searchesCount;

    Queue<GoogleRank> topRanks = MinMaxPriorityQueue.orderedBy(RANK_COMPARATOR).maximumSize(TOP_SIZE).create();
    Queue<GoogleRank> topImprovements = MinMaxPriorityQueue.orderedBy(IMPROVEMENT_COMPARATOR).maximumSize(TOP_SIZE).create();
    Queue<GoogleRank> topLosts = MinMaxPriorityQueue.orderedBy(TOP_LOST_COMPARATOR).maximumSize(TOP_SIZE).create();

    public GoogleTargetSummary() {
    }

    public GoogleTargetSummary(int groupId, int targetId, int runId, int previousScoreBP) {
        this.groupId = groupId;
        this.targetId = targetId;
        this.runId = runId;
        this.previousScoreBP = previousScoreBP;
    }
    
    public synchronized void addRankCandidat(GoogleRank rank){
        searchesCount += 1;
        previousScoreRaw += getRankScore(rank.previousRank);
        scoreRaw += getRankScore(rank.rank);
        
        if(rank.rank <=3 ){
            ++totalTop3;
        } else if(rank.rank <= 10){
            ++totalTop10;
        } else if(rank.rank <= 100){
            ++totalTop100;
        } else {
            ++totalOut;
        }
        
        addRankToTop(rank);
    }
    
    public void addRankToTop(GoogleRank rank){
        if(rank.rank != GoogleRank.UNRANKED){
            topRanks.add(rank);
        }
        if(rank.diff < 0){
            topImprovements.add(rank);
        }
        if(rank.diff > 0){
            topLosts.add(rank);
        }        
    }
    
    public List<GoogleRank> getTopRanks(){
        List<GoogleRank> list = new ArrayList<>(topRanks);
        Collections.sort(list, RANK_COMPARATOR);
        return list;
    }
    
    public List<GoogleRank> getTopImprovements(){
        List<GoogleRank> list = new ArrayList<>(topImprovements);
        Collections.sort(list, IMPROVEMENT_COMPARATOR);
        return list;
    }
    
    public List<GoogleRank> getTopLosts(){
        List<GoogleRank> list = new ArrayList<>(topLosts);
        Collections.sort(list, TOP_LOST_COMPARATOR);
        return list;
    }    
    
    protected int getRankScore(int rank){
        switch(rank){
            case 1:
                return 100;
            case 2:
                return 90;
            case 3:
                return 80;
            default:
                if(rank <= 5){
                    return 70;
                }
                if(rank <= 10){
                    return 60;
                }
                // end of top10
                
                if(rank <= 20){
                    return 40;
                }
                if(rank <= 30){
                    return 30;
                }
                if(rank <= 50){
                    return 20;
                }
                if(rank <= 100){
                    return 10;
                }
                if(rank <= 1000){
                    return 5;
                }
        }
        return 0;
    }    

    public int getGroupId() {
        return groupId;
    }

    public int getTargetId() {
        return targetId;
    }

    public int getRunId() {
        return runId;
    }

    public int getTotalTop3() {
        return totalTop3;
    }

    public int getTotalTop10() {
        return totalTop10;
    }

    public int getTotalTop100() {
        return totalTop100;
    }

    public int getTotalOut() {
        return totalOut;
    }
    
    public int getTotalKeywords(){
        return totalTop3 + totalTop10 + totalTop100 + totalOut;
    }
    
    public String getSerialized(Queue<GoogleRank> ranks){
        if(ranks == null || ranks.isEmpty()){
            return null;
        }
        
        StringBuilder builder = new StringBuilder();
        for (GoogleRank rank : ranks) {
            builder.append(rank.googleSearchId).append(",");
        }
        return builder.toString();
    }
    
    public String getTopRanksSerialized(){
        return getSerialized(topRanks);
    }
    
    public String getTopImprovementsSerialized(){
        return getSerialized(topImprovements);
    }

    public String getTopLostsSerialized(){
        return getSerialized(topLosts);
    }

    public void setTotalTop3(int totalTop3) {
        this.totalTop3 = totalTop3;
    }

    public void setTotalTop10(int totalTop10) {
        this.totalTop10 = totalTop10;
    }

    public void setTotalTop100(int totalTop100) {
        this.totalTop100 = totalTop100;
    }

    public void setTotalOut(int totalOut) {
        this.totalOut = totalOut;
    }

    public int getScoreRaw() {
        return scoreRaw;
    }

    public void setScoreRaw(int scoreRaw) {
        this.scoreRaw = scoreRaw;
    }
    
    public int computeScoreBP(int nSearches){
        if(nSearches == 0){
            return scoreBP = 0;
        }
        return scoreBP = scoreRaw*100/nSearches;
    }

    public int getScoreBP() {
        if (searchesCount == 0)
            return 0;
        return 100 * scoreRaw / searchesCount;
    }

    public void setScoreBP(int scoreBP) {
        this.scoreBP = scoreBP;
    }

    public int getPreviousScoreBP() {
        if (searchesCount == 0)
            return 0;
        return 100 * previousScoreRaw / searchesCount;
    }

    public void setPreviousScoreBP(int previousScoreBP) {
        this.previousScoreBP = previousScoreBP;
    }

    public int getDiffBP() {
        return getScoreBP() - getPreviousScoreBP();
    }

    private Queue<GoogleRank> scanAndFilterSearchIds(User user, SearchSettingsDB db, Set<Integer> searchIds, Queue<GoogleRank> ranks, Comparator<GoogleRank> comparator) {
        PriorityQueue<GoogleRank> copy = new PriorityQueue<>(comparator);
        for (GoogleRank rank : ranks) {
            boolean onlyAdmins = db.getIsOnlyAdmins(rank.googleSearchId);
            if (onlyAdmins && !user.isAdmin()) {
                continue;
            }
            searchIds.add(rank.googleSearchId);
            copy.add(rank);
        }
        return copy;
    }
    
    public void visitReferencedSearchId(User user, SearchSettingsDB db, Set<Integer> searchIds) {
        topRanks = scanAndFilterSearchIds(user, db, searchIds, topRanks, new RankComparator());
        topImprovements = scanAndFilterSearchIds(user, db, searchIds, topImprovements, new ImprovementComparator());
        topLosts = scanAndFilterSearchIds(user, db, searchIds, topLosts, new TopLostComparator());
    }
    
}
