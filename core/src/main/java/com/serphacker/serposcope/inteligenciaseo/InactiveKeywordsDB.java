package com.serphacker.serposcope.inteligenciaseo;

import com.google.inject.Inject;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.sql.SQLQuery;
import com.serphacker.serposcope.db.AbstractDB;
import com.serphacker.serposcope.querybuilder.QGoogleRank;
import com.serphacker.serposcope.querybuilder.QGoogleSearch;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class InactiveKeywordsDB extends AbstractDB {
    private static final int UNRANKED = 32767;

    private QGoogleSearch t_search = QGoogleSearch.googleSearch;
    private QGoogleRank t_rank = QGoogleRank.googleRank;

    public List<Integer> getInactiveSearchIds() {
        List<Integer> list = new ArrayList<>();
        BooleanExpression condition = t_rank.rank.avg().eq(t_rank.previousRank.avg())
                .and(t_rank.rank.avg().intValue().eq(UNRANKED));
        try (Connection con = ds.getConnection()) {
            SQLQuery<Integer> query = new SQLQuery<>(con, dbTplConf)
                    .select(t_search.id)
                    .from(t_search)
                    .join(t_rank)
                    .on(t_rank.googleSearchId.eq(t_search.id))
                    .groupBy(t_search.id)
                    .having(condition);
            list.addAll(query.fetch());
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return list;
    }
}
