package com.serphacker.serposcope.inteligenciaseo;

import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.dml.SQLUpdateClause;
import com.serphacker.serposcope.db.AbstractDB;
import com.serphacker.serposcope.models.base.User;

import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
public class SearchSettingsDB extends AbstractDB {
    QSearchSettings t_search_settings = QSearchSettings.searchSettings;

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
                        .executeWithKey(t_search_settings.id);
                count += 1;
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return count;
    }

    public List<String> getCategories(User user) {
        try (Connection con = ds.getConnection()) {
            SQLQuery<String> query = new SQLQuery<>(con, dbTplConf)
                    .select(t_search_settings.category)
                    .distinct()
                    .from(t_search_settings);
            if (!user.isAdmin()) {
                query = query.where(t_search_settings.adminsOnly.eq(false));
            }
            return query.fetch();
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
                    .where(t_search_settings.searchId.eq(id));
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
                    .where(t_search_settings.searchId.eq(id));
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
                    .where(t_search_settings.searchId.eq(id));
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

    private boolean exists(Integer search_id) {
        try (Connection con = ds.getConnection()) {
            SQLQuery<Integer> query = new SQLQuery<>(con, dbTplConf)
                    .select(t_search_settings.searchId)
                    .from(t_search_settings)
                    .where(t_search_settings.searchId.eq(search_id));
            return query.fetchCount() == 1;
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error("SQLError ex", ex);
        }
        return false;
    }

    public boolean update(Integer search_id, Integer group_id, String category, String volume, Boolean onlyAdmin) {
        if (exists(search_id)) {
            return doUpdate(search_id, group_id, category, volume, onlyAdmin);
        } else {
            return insertOne(search_id, group_id, category, volume, onlyAdmin);
        }
    }

    private boolean insertOne(Integer searchId, Integer groupId, String category, String volume, Boolean onlyAdmin) {
        try (Connection con = ds.getConnection()) {
            long result = new SQLInsertClause(con, dbTplConf, t_search_settings)
                    .set(t_search_settings.category, category)
                    .set(t_search_settings.volume, volume)
                    .set(t_search_settings.adminsOnly, onlyAdmin)
                    .set(t_search_settings.searchId, searchId)
                    .set(t_search_settings.groupId, groupId)
                    .execute();
            return result == 1L;
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error("SQLError ex", ex);
        }
        return false;
    }

    private boolean doUpdate(Integer searchId, Integer groupId, String category, String volume, Boolean onlyAdmin) {
        try (Connection con = ds.getConnection()) {
            long result = new SQLUpdateClause(con, dbTplConf, t_search_settings)
                    .set(t_search_settings.category, category)
                    .set(t_search_settings.volume, volume)
                    .set(t_search_settings.adminsOnly, onlyAdmin)
                    .where(t_search_settings.searchId.eq(searchId).and(t_search_settings.groupId.eq(groupId)))
                    .execute();
            return result == 1L;
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error("SQLError ex", ex);
        }
        return false;
    }

    public boolean setVisibleForAll(Integer searchId, Integer groupId, Boolean visible) {
        try (Connection con = ds.getConnection()) {
            long result;
            if (exists(searchId)) {
                result = new SQLUpdateClause(con, dbTplConf, t_search_settings)
                        .set(t_search_settings.adminsOnly, !visible)
                        .where(t_search_settings.searchId.eq(searchId).and(t_search_settings.groupId.eq(groupId)))
                        .execute();
            } else {
                result = new SQLInsertClause(con, dbTplConf, t_search_settings)
                        .set(t_search_settings.searchId, searchId)
                        .set(t_search_settings.groupId, groupId)
                        .set(t_search_settings.adminsOnly, true)
                        .execute();
            }
            return result == 1L;
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error("SQLError ex", ex);
        }
        return false;
    }

    public boolean setVolume(Integer searchId, int groupId, Integer volume) {
        try (Connection con = ds.getConnection()) {
            long result;
            if (exists(searchId)) {
                result = new SQLUpdateClause(con, dbTplConf, t_search_settings)
                        .set(t_search_settings.volume, volume.toString())
                        .where(t_search_settings.searchId.eq(searchId).and(t_search_settings.groupId.eq(groupId)))
                        .execute();
            } else {
                result = new SQLInsertClause(con, dbTplConf, t_search_settings)
                        .set(t_search_settings.searchId, searchId)
                        .set(t_search_settings.groupId, groupId)
                        .set(t_search_settings.volume, volume.toString())
                        .execute();
            }
            return result == 1L;
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error("SQLError ex", ex);
        }
        return false;
    }
}
