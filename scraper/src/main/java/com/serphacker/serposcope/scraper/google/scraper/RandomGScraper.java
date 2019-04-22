/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.scraper.google.scraper;

import com.serphacker.serposcope.scraper.captcha.solver.CaptchaSolver;
import com.serphacker.serposcope.scraper.google.GoogleScrapResult;
import com.serphacker.serposcope.scraper.google.GoogleScrapSearch;
import com.serphacker.serposcope.scraper.http.ScrapClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomGScraper extends GoogleScraper {

    private static final Logger LOG = LoggerFactory.getLogger(RandomGScraper.class);
    
    Random r;

    public RandomGScraper(ScrapClient client, CaptchaSolver solver) {
        this(client, solver, new Random());
    }
    
    public RandomGScraper(ScrapClient client, CaptchaSolver solver, Random r) {
        super(client, solver);
        this.r = r;
    }    
    
    @Override
    public GoogleScrapResult scrap(GoogleScrapSearch options) throws InterruptedException {
        
        List<String> urls = new ArrayList<>();
        for (int page = 0; page < options.getPages(); page++) {

            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            for (int result = 0; result < options.getResultPerPage(); result++) {
                int position = result + (page * options.getResultPerPage());
                String url = "http://www.site" + (position + 1) + ".com/" + options.getKeyword() + ".html";
                if(random.nextInt(options.getResultPerPage()*2) == 0){
//                    LOG.trace("skip ranking for {}", url);
                    continue;
                }                
                urls.add(url);
            }
            
//            long pauseMS = options.getRandomPagePauseMS();
//            if (pauseMS > 0) {
//                LOG.debug("KW {} page {} sleeping {} ms interrupt={}",
//                    new Object[]{options.getKeyword(), page, pauseMS, Thread.currentThread().isInterrupted()}
//                );
//                Thread.sleep(pauseMS);
//            }
        }
        
        Collections.shuffle(urls, random);
        return new GoogleScrapResult(GoogleScrapResult.Status.OK, urls, random.nextInt(5) + 5);
    }

}
