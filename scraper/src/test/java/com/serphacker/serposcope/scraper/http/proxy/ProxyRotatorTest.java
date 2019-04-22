/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.scraper.http.proxy;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 *
 * @author admin
 */
public class ProxyRotatorTest {
    
    private static final Logger LOG = LoggerFactory.getLogger(ProxyRotatorTest.class);

    @Test
    public void testCycle() {
        
        List<ScrapProxy> proxies = Arrays.asList(
            new HttpProxy("127.0.0.1", 0),
            new HttpProxy("127.0.0.2", 0),
            new HttpProxy("127.0.0.3", 0)
        );
        
        ProxyRotator rotator = new ProxyRotator(proxies);
        ScrapProxy proxy = null;
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < proxies.size(); j++) {
                proxy = rotator.rotate(proxy);
                assertEquals(((HttpProxy)proxies.get(j)).ip, ((HttpProxy)proxy).ip);
            }
        }
        rotator.add(proxy);
        
        assertEquals(proxies.size(), rotator.remaining());
    }
    
    public void testDrain(){
        List<ScrapProxy> proxies = Arrays.asList(
            new HttpProxy("127.0.0.1", 0),
            new HttpProxy("127.0.0.2", 0),
            new HttpProxy("127.0.0.3", 0)
        );
        ProxyRotator rotator = new ProxyRotator(proxies);
        for (int i = 0; i < proxies.size(); i++) {
            assertNotNull(rotator.poll());
        }
        assertNull(rotator.poll());
    }
    
}
