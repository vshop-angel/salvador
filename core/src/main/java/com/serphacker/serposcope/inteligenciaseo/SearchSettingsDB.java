package com.serphacker.serposcope.inteligenciaseo;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.dml.SQLDeleteClause;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.dml.SQLUpdateClause;
import com.serphacker.serposcope.db.AbstractDB;
import com.serphacker.serposcope.models.base.User;
import com.serphacker.serposcope.models.google.GoogleSearch;
import com.serphacker.serposcope.querybuilder.QIsSearchSettings;

import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
public class SearchSettingsDB extends AbstractDB {
    private QIsSearchSettings t_search_settings = QIsSearchSettings.isSearchSettings;

    SearchSettingsDB() {
    }

    public int insert(Collection<SearchSettings> list) {
        int count = 0;
        try (Connection con = ds.getConnection()) {
            for (SearchSettings item : list) {
                SQLInsertClause clause = new SQLInsertClause(con, dbTplConf, t_search_settings)
                        .set(t_search_settings.searchId, item.getSearchId())
                        .set(t_search_settings.category, item.getCategory())
                        .set(t_search_settings.volume, item.getVolume())
                        .set(t_search_settings.adminsOnly, item.isAdminsOnly())
                        .set(t_search_settings.competition, item.getCompetition())
                        .set(t_search_settings.cpc, item.getCPC())
                        .set(t_search_settings.tag, item.getTag());
                count += clause.execute();
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return count;
    }

    private List<String> listUniqueStringsFromColumn(User user, StringPath column) {
        try (Connection con = ds.getConnection()) {
            SQLQuery<String> query = new SQLQuery<>(con, dbTplConf)
                    .select(column)
                    .distinct()
                    .from(t_search_settings);
            if (!user.isAdmin()) {
                query = query.where(t_search_settings.adminsOnly.eq(false));
            }
            final ArrayList<String> finalList = new ArrayList<>();
            List<String> list = query.fetch();
            for (String each : list) {
                if (each == null)
                    continue;
                String trimmed = each.trim();
                if (trimmed.length() == 0)
                    continue;
                finalList.add(trimmed);
            }
            LOG.info(String.join(finalList.toString()));
            return finalList;
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error("SQLError ex", ex);
        }
        return new ArrayList<>();
    }

    public List<String> listTags(User user) {
        return listUniqueStringsFromColumn(user, t_search_settings.tag);
    }

    public List<String> listCategories(User user) {
        return listUniqueStringsFromColumn(user, t_search_settings.category);
    }

    private <T> T getValue(int id, Path<T> path) {
        try (Connection con = ds.getConnection()) {
            SQLQuery<T> query = new SQLQuery<>(con, dbTplConf)
                    .select(path)
                    .from(t_search_settings)
                    .where(t_search_settings.searchId.eq(id));
            List<T> list = query.fetch();
            if ((list == null) || (list.size() == 0))
                return null;
            // We just want the firs element
            return list.get(0);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error("SQLError ex", ex);
        }
        return null;
    }

    public String getCategory(int id) {
        return getValue(id, t_search_settings.category);
    }

    public Integer getCompetition(int id) {
        return getValue(id, t_search_settings.competition);
    }

    public String getTag(int id) {
        return getValue(id, t_search_settings.tag);
    }

    public Double getCPC(int id) {
        return getValue(id, t_search_settings.cpc);
    }

    public Integer getVolume(int id) {
        return getValue(id, t_search_settings.volume);
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
            SQLQuery<Long> query = new SQLQuery<>(con, dbTplConf)
                    .select(t_search_settings.count())
                    .from(t_search_settings)
                    .where(t_search_settings.searchId.eq(search_id));
            Long first = query.fetchFirst();
            if (first == 0) {
                LOG.warn("no search found with id (" + first.toString() + ")");
            } else if (first == 1) {
                return true;
            } else {
                LOG.warn("there's more than one row violating primary key constraint(" + first.toString() + ")");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error("SQLError ex", ex);
        }
        return false;
    }

    public boolean update(Integer search_id, String category, Integer volume, Boolean onlyAdmin) {
        if (exists(search_id)) {
            return doUpdate(search_id, category, volume, onlyAdmin);
        } else {
            return insertOne(search_id, category, volume, onlyAdmin);
        }
    }

    private boolean insertOne(Integer searchId, String category, Integer volume, Boolean onlyAdmin) {
        try (Connection con = ds.getConnection()) {
            long result = new SQLInsertClause(con, dbTplConf, t_search_settings)
                    .set(t_search_settings.category, category)
                    .set(t_search_settings.volume, volume)
                    .set(t_search_settings.adminsOnly, onlyAdmin)
                    .set(t_search_settings.searchId, searchId)
                    .execute();
            return result == 1L;
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error("SQLError ex", ex);
        }
        return false;
    }

    private boolean doUpdate(Integer searchId, String category, Integer volume, Boolean onlyAdmin) {
        try (Connection con = ds.getConnection()) {
            long result = new SQLUpdateClause(con, dbTplConf, t_search_settings)
                    .set(t_search_settings.category, category)
                    .set(t_search_settings.volume, volume)
                    .set(t_search_settings.adminsOnly, onlyAdmin)
                    .where(t_search_settings.searchId.eq(searchId))
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
                        .where(t_search_settings.searchId.eq(searchId))
                        .execute();
            } else {
                result = new SQLInsertClause(con, dbTplConf, t_search_settings)
                        .set(t_search_settings.searchId, searchId)
                        .set(t_search_settings.adminsOnly, true)
                        .execute();
            }
            return result == 1L;
        } catch (Exception ex) {
            LOG.error("SQLError ex", ex);
        }
        return false;
    }

    private <T> boolean setField(Integer searchId, int groupId, Path<T> field, T value) {
        if (value == null) {
            LOG.info("value is null");
            return false;
        }
        try (Connection con = ds.getConnection()) {
            long result;
            if (exists(searchId)) {
                result = new SQLUpdateClause(con, dbTplConf, t_search_settings)
                        .set(field, value)
                        .where(t_search_settings.searchId.eq(searchId))
                        .execute();
            } else {
                result = new SQLInsertClause(con, dbTplConf, t_search_settings)
                        .set(t_search_settings.searchId, searchId)
                        .set(field, value)
                        .execute();
            }
            return result == 1L;
        } catch (Exception ex) {
            LOG.error("SQLError ex", ex);
        }
        return false;
    }

    public boolean setCompetition(Integer searchId, int groupId, Integer competition) {
        return setField(searchId, groupId, t_search_settings.competition, competition);
    }

    public boolean setCPC(Integer searchId, int groupId, Double cpc) {
        return setField(searchId, groupId, t_search_settings.cpc, cpc);
    }

    public boolean setTag(Integer searchId, int groupId, String tag) {
        return setField(searchId, groupId, t_search_settings.tag, tag);
    }

    public boolean setCategory(Integer searchId, int groupId, String category) {
        return setField(searchId, groupId, t_search_settings.category, category);
    }

    public boolean setVolume(Integer searchId, int groupId, Integer volume) {
        return setField(searchId, groupId, t_search_settings.volume, volume);
    }

    public boolean deleteEntry(GoogleSearch search) {
        try (Connection con = ds.getConnection()) {
            long result;
            result = new SQLDeleteClause(con, dbTplConf, t_search_settings)
                    .where(t_search_settings.searchId.eq(search.getId()))
                    .execute();
            return result == 1L;
        } catch (Exception ex) {
            LOG.error("SQLError ex", ex);
        }
        return false;
    }
}
