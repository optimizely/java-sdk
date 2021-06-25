package com.optimizely.ab.optimizelyconfig;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static java.util.Arrays.asList;

public class OptimizelyEventTest {
    @Test
    public void testOptimizelyEvent() {
        OptimizelyEvent optimizelyEvent1 = new OptimizelyEvent(
            "5",
            "test_event",
            asList("123","234","345")
        );
        OptimizelyEvent optimizelyEvent2 = new OptimizelyEvent(
            "5",
            "test_event",
            asList("123","234","345")
        );
        assertEquals("5", optimizelyEvent1.getId());
        assertEquals("test_event", optimizelyEvent1.getKey());
        assertEquals(optimizelyEvent1, optimizelyEvent2);
        assertEquals(optimizelyEvent1.hashCode(), optimizelyEvent2.hashCode());
    }
}
