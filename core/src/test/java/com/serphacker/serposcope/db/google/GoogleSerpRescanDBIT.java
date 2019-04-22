/*
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.db.google;

import com.google.inject.*;
import com.querydsl.sql.Configuration;
import com.serphacker.serposcope.DeepIntegrationTest;
import com.serphacker.serposcope.db.base.BaseDB;
import com.serphacker.serposcope.di.db.ConfigurationProvider;
import com.serphacker.serposcope.di.db.DataSourceProvider;
import com.serphacker.serposcope.models.google.GoogleSearch;
import com.serphacker.serposcope.models.google.GoogleTarget;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author admin
 */
public class GoogleSerpRescanDBIT extends DeepIntegrationTest {

    protected Injector injectorMySQL = null;

    @Inject
    BaseDB baseDB;
    
    @Inject
    GoogleDB googleDB;
    
    
    protected List<Module> getModule(String dburl) {
        List<Module> lists = new ArrayList<>();
        lists.add(new AbstractModule() {
            @Override
            protected void configure() {
                bind(DataSource.class).toProvider(new DataSourceProvider(dburl, false)).in(Singleton.class);
                bind(Configuration.class).toProvider(new ConfigurationProvider(dburl)).in(Singleton.class);
            }
        });
        return lists;
    }

    Properties props = new Properties();
    
    @Before
    public void before() throws Exception {
        super.before();
        props.load(ClassLoader.class.getResourceAsStream("/testconfig.properties"));
        injectorMySQL = Guice.createInjector(getModule(
            "jdbc:mysql://" + props.getProperty("mysql.rescan.host") + ":3306/" + 
            props.getProperty("mysql.rescan.database") + 
            "?user=" + props.getProperty("mysql.rescan.user") + 
            "&password=" + props.getProperty("mysql.rescan.password") + 
            "&allowMultiQueries=true"
        ));
        baseDB = injectorMySQL.getInstance(BaseDB.class);
        googleDB = injectorMySQL.getInstance(GoogleDB.class);        
    }

    @Test
    public void testSomeMethod() {
        
        int groupId = 1;
        GoogleTarget target = new GoogleTarget(groupId, "www.site45.com", GoogleTarget.PatternType.DOMAIN, "www.site45.com");
        List<GoogleSearch> searches = googleDB.search.listByGroup(Arrays.asList(groupId));
        System.out.println(searches.size());
        
        for (int i = 0; i < 2; i++) {
            
//            try{
//                googleDB.target.insert(Arrays.asList(target));
//                System.out.println("TARGET ID : " + target.getId());
//                googleDB.serpRescan.rescanNonBulk(null, Arrays.asList(target), searches, true);
//                googleDB.serpRescan.rescanNonBulk(null, Arrays.asList(target), searches, true);
//            } finally {
//                googleDB.targetSummary.deleteByTarget(target.getId());
//                googleDB.rank.deleteByTarget(groupId, target.getId());
//                googleDB.target.delete(target.getId());
//            }
            
            try{
                googleDB.target.insert(Arrays.asList(target));
                System.out.println("TARGET ID : " + target.getId());
                googleDB.serpRescan.rescan(null, Arrays.asList(target), searches, true);
                googleDB.serpRescan.rescan(null, Arrays.asList(target), searches, true);
            } finally {
                googleDB.targetSummary.deleteByTarget(target.getId());
                googleDB.rank.deleteByTarget(groupId, target.getId());
                googleDB.target.delete(target.getId());
            }
        }

    }
    
}
