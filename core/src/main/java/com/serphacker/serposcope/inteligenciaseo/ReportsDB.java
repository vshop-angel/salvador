package com.serphacker.serposcope.inteligenciaseo;

import com.google.inject.Singleton;
import com.querydsl.core.Tuple;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.dml.SQLDeleteClause;
import com.querydsl.sql.dml.SQLInsertClause;
import com.serphacker.serposcope.db.AbstractDB;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
public class ReportsDB extends AbstractDB {
    QReport t_report = QReport.t_report;

    public ReportsDB() {
    }

    public int insertReports(Collection<Report> reports) {
        int count = 0;
        try (Connection con = ds.getConnection()) {
            for (Report report : reports) {
                Integer key = new SQLInsertClause(con, dbTplConf, t_report)
                    .set(t_report.groupId, report.getGroupId())
                    .set(t_report.name, report.getName())
                    .set(t_report.iframe, report.getIframe())
                    .executeWithKey(t_report.id)
                ;
                count += 1;
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return count;
    }

    private Report fromTuple(Tuple tuple) {
        if (tuple == null)
            return null;
        Integer groupId = tuple.get(t_report.groupId);
        if (groupId == null)
            return null;
        Integer id = tuple.get(t_report.id);
        if (id == null)
            return null;
        return new Report(id, groupId, tuple.get(t_report.name), tuple.get(t_report.iframe));
    }

    public long delete(Integer[] ids) {
        try (Connection con = ds.getConnection()) {
            return new SQLDeleteClause(con, dbTplConf, t_report)
                    .where(t_report.id.in(ids))
                    .execute();
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error("SQLError ex", ex);
        }
        return 0;
    }

    public List<Report> listReports() {
        List<Report> reports = new ArrayList<>();
        try (Connection con = ds.getConnection()) {
            SQLQuery<Tuple> query = new SQLQuery<>(con, dbTplConf)
                .select(t_report.all())
                .from(t_report)
            ;
            List<Tuple> tuples = query.fetch();
            for (Tuple tuple : tuples) {
                reports.add(fromTuple(tuple));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error("SQLError ex", ex);
        }
        return reports;
    }
}