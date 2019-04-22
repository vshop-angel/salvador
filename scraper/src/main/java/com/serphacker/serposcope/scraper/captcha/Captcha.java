/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.scraper.captcha;

import com.serphacker.serposcope.scraper.captcha.solver.CaptchaSolver;

import java.util.HashMap;
import java.util.Map;

import static com.serphacker.serposcope.scraper.captcha.Captcha.Error.SUCCESS;
import static com.serphacker.serposcope.scraper.captcha.Captcha.Status.CREATED;

/**
 *
 * @author admin
 */
public abstract class Captcha {
    
    public enum Status {
        CREATED,
        SUBMITTED,
        SOLVED,
        ERROR
    };
    
    public enum Error {
        SUCCESS,
        SERVICE_OVERLOADED,
        INVALID_CREDENTIALS,
        OUT_OF_CREDITS,
        NETWORK_ERROR,
        TIMEOUT,
        INTERRUPTED,
        EXCEPTION,
        UNSUPPORTED_TYPE
    };
    
    String id;
    Status status = CREATED;
    Error error = SUCCESS;
    long solveDuration;
    Map<String,String> context = new HashMap<>();
    CaptchaSolver lastSolver;
    
    
    
//    private int subType;
//    private String response;        // the response of the captcha solving

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        if(status == Status.SOLVED){
            this.error = SUCCESS;
        }
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
        if(error != SUCCESS){
            this.status = Status.ERROR;
        }
    }

    public long getSolveDuration() {
        return solveDuration;
    }

    public void setSolveDuration(long solveDuration) {
        this.solveDuration = solveDuration;
    }

    public CaptchaSolver getLastSolver() {
        return lastSolver;
    }

    public void setLastSolver(CaptchaSolver lastSolver) {
        this.lastSolver = lastSolver;
    }
}
