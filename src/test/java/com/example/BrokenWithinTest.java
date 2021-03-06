package com.example;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.impl.Point;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;
import org.junit.BeforeClass;
import org.junit.Test;

public class BrokenWithinTest {
    private static final Logger logger = LogManager.getLogger();
    private static EmbeddedCacheManager cacheManager;
    private static Cache<String, CacheEntity> cache;

    @BeforeClass
    public static void setup() {
        Configuration dc = new ConfigurationBuilder()
                .indexing()
                    .enable()
                    .addProperty("default.directory_provider", "ram")
                    .addProperty("hibernate.search.lucene_version", "LUCENE_CURRENT")
                .build();
        cacheManager = new DefaultCacheManager(dc);
        cache = cacheManager.getCache();
    }

    private static List<Object> doSpatialQuery(double radius, Unit unit, double latitude, double longitude) {
        SearchManager searchManager = Search.getSearchManager(cache);
        Query query = searchManager.buildQueryBuilderForClass(CacheEntity.class).get().spatial()
                .onDefaultCoordinates() // Comment out on JDG 7.
                .within(radius, unit)
                .ofLatitude(latitude)
                .andLongitude(longitude)
                .createQuery();
        CacheQuery cacheQuery = searchManager.getQuery(query, CacheEntity.class);
        return cacheQuery.list();
    }

    private static double calcDistanceByHS(Coordinates from, double latitude, double longitude) {
        return Point.fromCoordinates(from).getDistanceTo(latitude, longitude);
    }
    
    private static void putRandomDataAround(Coordinates center, double deltaLatitude, double deltaLongitude, int num) {
        Random random = new Random();
        for (int i = 0; i < num; i++) {
            double dlat = (random.nextDouble() - 0.5) * deltaLatitude;
            double dlon = (random.nextDouble() - 0.5) * deltaLongitude;
            cache.put(String.valueOf(i),
                    new CacheEntity("taxi-" + i, center.getLatitude() + dlat, center.getLongitude() + dlon));
        }
    }
    
    @Test
    public void testConcurrentModfication() {
        CacheEntity center = new CacheEntity("ce00", 1.300, 104.000);

        CacheEntity p1 = new CacheEntity("ce01", 1.310, 104.010);
        assertEquals(1.572329, calcDistanceByHS(center, p1.getLatitude(), p1.getLongitude()), 0.000001);

        CacheEntity p2 = new CacheEntity("ce02", 1.302, 104.021);
        assertEquals(2.345060, calcDistanceByHS(center, p2.getLatitude(), p2.getLongitude()), 0.000001);

        CacheEntity p3 = new CacheEntity("ce03", 1.400, 104.100);
        assertEquals(15.723154, calcDistanceByHS(center, p3.getLatitude(), p3.getLongitude()), 0.000001);

        cache.put("taxi-1", p1);
        cache.put("taxi-2", p2);
        List<Object> result = doSpatialQuery(2.5, Unit.KM, center.getLatitude(), center.getLongitude());
        assertEquals(2, result.size());

        cache.put("taxi-2", p3);
        for (Object o : result) {
            CacheEntity c = (CacheEntity) o;
            double d = calcDistanceByHS(center, c.getLatitude(), c.getLongitude());
            assertTrue(c + " is " + d + " distant", d <= 2.5);
        }
    }

    @Test
    public void putPeriodicallyAndCheckResults() {
        CacheEntity center = new CacheEntity("ce00", 1.288724, 103.842672);
        ArrayBlockingQueue<String> q = new ArrayBlockingQueue<>(1);
        ScheduledExecutorService es = Executors.newScheduledThreadPool(4);
        
        es.scheduleAtFixedRate(() -> {
            logger.info("Putting data");
            try {
                putRandomDataAround(center, 0.20, 0.40, 5000);
            } catch (Throwable e) {
                logger.error("Error in writer", e);;
            } finally {
                logger.info("Have put data");
            }
        }, 0, 30, TimeUnit.SECONDS);
        
        es.scheduleAtFixedRate(() -> {
            logger.info("Querying");
            try {
                List<Object> results = doSpatialQuery(2.5, Unit.KM, center.getLatitude(), center.getLongitude());
                for (Object o : results) {
                    CacheEntity c = (CacheEntity) o;
                    double d = calcDistanceByHS(center, c.getLatitude(), c.getLongitude());
                    if (d > 2.5) {
                        q.add(c + " is " + d + " distant, more than 2.5 (km)");
                    }
                }                          
            } catch (Throwable e) {
                logger.error("Error in reader", e);
            } finally {
                logger.info("Queried");                
            }
        }, 0, 5, TimeUnit.SECONDS);
        
        try {
            fail(q.take());
        } catch (InterruptedException ignore) {
        }
        es.shutdown();
    }
}
