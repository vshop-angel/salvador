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
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FakeGScraper extends GoogleScraper {

    private static final Logger LOG = LoggerFactory.getLogger(FakeGScraper.class);

    ThreadLocalRandom random = ThreadLocalRandom.current();

    public FakeGScraper(ScrapClient client, CaptchaSolver solver) {
        super(client, solver);
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
                urls.add("http://www.site" + (position + 1) + ".com/" + options.getKeyword() + ".html");
            }

            long pauseMS = options.getRandomPagePauseMS();
            if (pauseMS > 0) {
                LOG.debug("KW {} page {} sleeping {} ms interrupt={}",
                    new Object[]{options.getKeyword(), page, pauseMS, Thread.currentThread().isInterrupted()}
                );
                Thread.sleep(pauseMS);
            }
        }
        return new GoogleScrapResult(GoogleScrapResult.Status.OK, urls);
    }

}
