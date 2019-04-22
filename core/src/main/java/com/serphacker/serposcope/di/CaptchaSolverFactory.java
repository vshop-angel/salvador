/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.di;

import com.google.inject.ImplementedBy;
import com.serphacker.serposcope.models.base.Config;
import com.serphacker.serposcope.scraper.captcha.solver.CaptchaSolver;

/**
 *
 * @author admin
 */
@ImplementedBy(CaptchaSolverFactoryImpl.class)
public interface CaptchaSolverFactory {
    public CaptchaSolver get(Config config);
}
