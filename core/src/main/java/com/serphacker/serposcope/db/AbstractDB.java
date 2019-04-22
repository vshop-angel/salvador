/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.db;

import com.querydsl.sql.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.sql.DataSource;


public abstract class AbstractDB {
    
    protected final static Logger LOG = LoggerFactory.getLogger(AbstractDB.class);
    
    @Inject
    protected DataSource ds;
    
    @Inject
    protected Configuration dbTplConf;

    protected boolean isMySQL(){
        return !isH2();
    }
    
    protected boolean isH2(){
        return dbTplConf.getTemplates().isNativeMerge();
    }
}
