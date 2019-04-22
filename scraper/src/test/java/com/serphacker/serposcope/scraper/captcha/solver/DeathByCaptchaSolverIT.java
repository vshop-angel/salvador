/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.scraper.captcha.solver;

import org.junit.Before;

import static org.junit.Assert.assertNotNull;

/**
 *
 * @author admin
 */
public class DeathByCaptchaSolverIT extends GenericSolverIT {

    public DeathByCaptchaSolverIT() {
    }

    String dbcLogin;
    String dbcPassword;

    @Before
    public void readCredentials() throws Exception {
        assertNotNull(dbcLogin = props.getProperty("dbclogin"));
        assertNotNull(dbcPassword = props.getProperty("dbcpassword"));
    }

    @Override
    protected CaptchaSolver getSolver() {
        return new DeathByCaptchaSolver(dbcLogin, dbcPassword);
    }

    @Override
    protected CaptchaSolver getSolverNoBalance() {
        return null;
    }

    @Override
    protected CaptchaSolver getSolverInvalidCredentials() {
        return new DeathByCaptchaSolver("wrong-login", "wrong-password");
    }

}
