package com.serphacker.serposcope.inteligenciaseo;

import com.google.inject.Inject;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.SQLQuery;
import com.serphacker.serposcope.db.AbstractDB;
import com.serphacker.serposcope.db.google.GoogleRankDB;
import com.serphacker.serposcope.db.google.GoogleSearchDB;
import com.serphacker.serposcope.db.google.GoogleSerpDB;
import com.serphacker.serposcope.models.base.Group;
import com.serphacker.serposcope.models.google.GoogleSearch;
import com.serphacker.serposcope.querybuilder.QGoogleRank;
import com.serphacker.serposcope.querybuilder.QGoogleSearch;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class InactiveKeywordsDB extends AbstractDB {
    private static final short UNRANKED = 32767;

    private QGoogleSearch t_search = QGoogleSearch.googleSearch;
    private QGoogleRank t_rank = QGoogleRank.googleRank;

    @Inject
    private GoogleSearchDB searchDB;

    @Inject
    private GoogleSerpDB serpDB;

    @Inject
    private GoogleRankDB rankDB;

    @Inject
    private SearchSettingsDB searchSettingsDB;

    public interface Deleter<T> {
        T onError(int count);

        T onPartialSuccess(int actualCount, int expectedCount);

        T onSuccess(int count);

        T onAlreadyClean();
    }

    private List<Integer> listByCriterion(Predicate predicate) {
        List<Integer> list = new ArrayList<>();
        BooleanExpression condition = (new CaseBuilder())
                .when(t_rank.rank.eq(UNRANKED))
                .then(1)
                .otherwise(0)
                .sum()
                .eq(t_rank.rank.count().castToNum(Integer.class))
        ;
        try (Connection con = ds.getConnection()) {
            SQLQuery<Integer> query = new SQLQuery<>(con, dbTplConf)
                    .select(t_search.id)
                    .from(t_search)
                    .join(t_rank)
                    .on(predicate)
                    .groupBy(t_search.id)
                    .having(condition);
            list.addAll(query.fetch());
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return list;
    }

    public List<Integer> getInactiveSearchIdsForGroup(Group group) {
        return listByCriterion(t_rank.googleSearchId.eq(t_search.id).and(t_rank.groupId.eq(group.getId())));
    }

    public List<Integer> getInactiveSearchIds() {
        return listByCriterion(t_rank.googleSearchId.eq(t_search.id));
    }

    public <T> T removeFromList(List<Integer> list, Deleter<T> result) {
        if (list.size() == 0) {
            return result.onAlreadyClean();
        }
        int count = 0;
        for (Integer id : list) {
            GoogleSearch search = searchDB.find(id);
            if (search == null) {
                LOG.warn(String.format("Could not find search with id %d", id));
                continue;
            }
            // Delete the serp first
            serpDB.deleteBySearch(id);
            // Get all groups
            List<Integer> groups = searchDB.listGroups(search);
            for (Integer group : groups) {
                // Remove the ranks saved for this search
                rankDB.deleteBySearch(group, search.getId());
                // Remove the search from the group
                searchDB.deleteFromGroup(search, group);
            }
            searchSettingsDB.deleteEntry(search);
            // Now attempt to delete the search itself (it will probably fail)
            if (searchDB.delete(search)) {
                count += 1;
            }
        }
        LOG.info(String.format("Deleted %d keywords", count));
        if (count == 0) {
            return result.onError(count);
        } else if (count == list.size()) {
            return result.onSuccess(count);
        } else {
            return result.onPartialSuccess(count, list.size());
        }
    }
}
