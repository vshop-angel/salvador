package com.serphacker.serposcope.inteligenciaseo;

import com.google.inject.Singleton;
import com.querydsl.core.Tuple;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.dml.SQLUpdateClause;
import com.serphacker.serposcope.db.AbstractDB;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
public class SearchSettingsDB extends AbstractDB {
    QSearchSettings t_search_settings = QSearchSettings.t_search_settings;
    SearchSettingsDB() {
    }

    public int insert(Collection<SearchSettings> list) {
        int count = 0;
        try (Connection con = ds.getConnection()) {
            for (SearchSettings item : list) {
                Integer key = new SQLInsertClause(con, dbTplConf, t_search_settings)
                        .set(t_search_settings.groupId, item.getGroupId())
                        .set(t_search_settings.searchId, item.getSearchId())
                        .set(t_search_settings.category, item.getCategory())
                        .set(t_search_settings.volume, item.getVolume())
                        .set(t_search_settings.adminsOnly, item.isAdminsOnly())
                        .executeWithKey(t_search_settings.id)
                ;
                count += 1;
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return count;
    }

    public List<String> getCategories() {
        try (Connection con = ds.getConnection()) {
            SQLQuery<String> query = new SQLQuery<>(con, dbTplConf)
                    .select(t_search_settings.category)
                    .distinct()
                    .from(t_search_settings)
            ;
            List<String> list = query.fetch();
            return list;
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error("SQLError ex", ex);
        }
        return new ArrayList<>();
    }

    public String getCategory(int id) {
        try (Connection con = ds.getConnection()) {
            SQLQuery<String> query = new SQLQuery<>(con, dbTplConf)
                    .select(t_search_settings.category)
                    .distinct()
                    .from(t_search_settings)
                    .where(t_search_settings.searchId.eq(id))
            ;
            List<String> list = query.fetch();
            if ((list == null) || (list.size() == 0))
                return null;
            // We just want the firs element
            return list.get(0);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error("SQLError ex", ex);
        }
        return "";
    }

    public String getVolume(int id) {
        try (Connection con = ds.getConnection()) {
            SQLQuery<String> query = new SQLQuery<>(con, dbTplConf)
                    .select(t_search_settings.volume)
                    .distinct()
                    .from(t_search_settings)
                    .where(t_search_settings.searchId.eq(id))
                    ;
            List<String> list = query.fetch();
            if ((list == null) || (list.size() == 0))
                return null;
            // We just want the firs element
            return list.get(0);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error("SQLError ex", ex);
        }
        return "";
    }

    public boolean getIsOnlyAdmins(int id) {
        try (Connection con = ds.getConnection()) {
            SQLQuery<Boolean> query = new SQLQuery<>(con, dbTplConf)
                    .select(t_search_settings.adminsOnly)
                    .from(t_search_settings)
                    .where(t_search_settings.searchId.eq(id))
            ;
            List<Boolean> list = query.fetch();
            if ((list == null) || (list.size() == 0))
                return false;
            // We just want the firs element
            return list.get(0);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error("SQLError ex", ex);
        }
        return false;
    }

    public boolean update(Integer id, String category, String volume, Boolean onlyAdmin) {
        try (Connection con = ds.getConnection()) {
            long result = new SQLUpdateClause(con, dbTplConf, t_search_settings)
                    .set(t_search_settings.category, category)
                    .set(t_search_settings.volume, volume)
                    .set(t_search_settings.adminsOnly, onlyAdmin)
                    .where(t_search_settings.id.eq(id))
                    .execute();
            return result == 1L;
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error("SQLError ex", ex);
        }
        return false;
    }
}
