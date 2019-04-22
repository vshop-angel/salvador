/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.models.google;

import com.serphacker.serposcope.scraper.google.GoogleCountryCode;
import com.serphacker.serposcope.scraper.google.GoogleDevice;


public class GoogleSettings {
    
    int resultPerPage = 100;
    int pages = 1;
    int minPauseBetweenPageSec = 5;
    int maxPauseBetweenPageSec = 5;
    int maxThreads = 1;
    int fetchRetry = 3;    
    
    GoogleCountryCode defaultCountry = GoogleCountryCode.__;
    String defaultDatacenter = null;
    GoogleDevice defaultDevice = GoogleDevice.DESKTOP;
    String defaultLocal = null;
    String defaultCustomParameters = null;

    public int getResultPerPage() {
        return resultPerPage;
    }

    public void setResultPerPage(int resultPerPage) {
        this.resultPerPage = resultPerPage;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public int getMinPauseBetweenPageSec() {
        return minPauseBetweenPageSec;
    }

    public void setMinPauseBetweenPageSec(int minPauseBetweenPageSec) {
        this.minPauseBetweenPageSec = minPauseBetweenPageSec;
    }

    public int getMaxPauseBetweenPageSec() {
        return maxPauseBetweenPageSec;
    }

    public void setMaxPauseBetweenPageSec(int maxPauseBetweenPageSec) {
        this.maxPauseBetweenPageSec = maxPauseBetweenPageSec;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public int getFetchRetry() {
        return fetchRetry;
    }

    public void setFetchRetry(int fetchRetry) {
        this.fetchRetry = fetchRetry;
    }
    
    // search

    public GoogleCountryCode getDefaultCountry() {
        return defaultCountry;
    }

    public void setDefaultCountry(GoogleCountryCode defaultCountry) {
        if(defaultCountry == null){
            defaultCountry = GoogleCountryCode.__;
        }
        this.defaultCountry = defaultCountry;
    }
    
    public void setDefaultCountry(String country){
        this.defaultCountry = GoogleCountryCode.__;
        
        if(country == null){
            return;
        }
        
        try {
            this.defaultCountry = GoogleCountryCode.valueOf(country.toUpperCase());
        } catch(Exception ex){
        }
    }    

    public String getDefaultDatacenter() {
        return defaultDatacenter;
    }

    public void setDefaultDatacenter(String defaultDatacenter) {
        this.defaultDatacenter = defaultDatacenter;
    }

    public GoogleDevice getDefaultDevice() {
        return defaultDevice;
    }

    public void setDefaultDevice(GoogleDevice defaultDevice) {
        this.defaultDevice = defaultDevice;
    }
    
    public void setDefaultDevice(String deviceId){
        this.defaultDevice = GoogleDevice.DESKTOP;
        
        if(deviceId == null){
            return;
        }
        
        try {
            this.defaultDevice = GoogleDevice.values()[Integer.parseInt(deviceId)];
        } catch(Exception ex){
        }
    }
    
    public String getDefaultLocal() {
        return defaultLocal;
    }

    public void setDefaultLocal(String defaultLocal) {
        this.defaultLocal = defaultLocal;
    }

    public String getDefaultCustomParameters() {
        return defaultCustomParameters;
    }

    public void setDefaultCustomParameters(String defaultCustomParameters) {
        this.defaultCustomParameters = defaultCustomParameters;
    }
    
}
