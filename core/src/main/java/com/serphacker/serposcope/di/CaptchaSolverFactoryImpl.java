/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.di;

import com.serphacker.serposcope.models.base.Config;
import com.serphacker.serposcope.scraper.captcha.solver.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CaptchaSolverFactoryImpl implements CaptchaSolverFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CaptchaSolverFactoryImpl.class);

    @Override
    public CaptchaSolver get(Config config) {
        if(config == null){
            return null;
        }
        
        List<CaptchaSolver> solvers = new ArrayList<>();
        
        if (!StringUtils.isEmpty(config.getDbcUser()) && !StringUtils.isEmpty(config.getDbcPass())) {
            DeathByCaptchaSolver solver = new DeathByCaptchaSolver(config.getDbcUser(), config.getDbcPass());
            if(init(solver)){
                solvers.add(solver);
            }
        }
        
        if(!StringUtils.isEmpty(config.getAnticaptchaKey())){
            AntiCaptchaSolver solver = new AntiCaptchaSolver(config.getAnticaptchaKey());
            if(init(solver)){
                solvers.add(solver);
            }                    
        }
        
        if(!StringUtils.isEmpty(config.getTwoCaptchaKey())){
            TwoCaptchaSolver solver = new TwoCaptchaSolver(config.getTwoCaptchaKey());
            if(init(solver)){
                solvers.add(solver);
            }                    
        }

        if(!StringUtils.isEmpty(config.getImageTyperzKey())){
            ImageTyperzSolver solver = new ImageTyperzSolver(config.getImageTyperzKey());
            if(init(solver)){
                solvers.add(solver);
            }                    
        }        
        
//        if (!StringUtils.isEmpty(config.getDecaptcherUser()) && !StringUtils.isEmpty(config.getDecaptcherPass())) {
//            DecaptcherSolver solver = new DecaptcherSolver(config.getDecaptcherUser(), config.getDecaptcherPass());
//            if(init(solver)){
//                solvers.add(solver);
//            }
//        }        
        
        if(solvers.isEmpty()){
            return null;
        }
        
        Collections.shuffle(solvers);
        return new RandomCaptchaSolver(solvers);
    }
    
    protected boolean init(CaptchaSolver solver){
        
        if(!solver.init()){
            LOG.warn("{} : failed to init()", solver.getFriendlyName());
            return false;                
        }            

        if(!solver.testLogin()){
            LOG.warn("{} : can't login in", solver.getFriendlyName());
            return false;
        }

        LOG.debug("{} : remaining credit {}", solver.getFriendlyName(), solver.getCredit());
        if(!solver.hasCredit()){
            LOG.warn("{} : not enough credit", solver.getFriendlyName());
            return false;
        }        
        
        return true;
    }
    
}
