/****************************************************************************
 * CMAB Testing Example for Optimizely Java SDK
 * 
 * This file contains comprehensive test scenarios for CMAB functionality
 * 
 * To run: 
 *   1. Save this file to: java-sdk/java-quickstart/src/main/java/com/optimizely/CmabBugBash.java
 *   2. From java-sdk directory, run: ./gradlew :java-quickstart:run -Dmain.class=com.optimizely.CmabBugBash
 *   Or with specific test:
 *      ./gradlew :java-quickstart:run -Dmain.class=com.optimizely.CmabBugBash -Dtest=basic
 * 
 * Alternatively, if you have gradle wrapper issues:
 *   1. cd java-sdk/java-quickstart
 *   2. javac -cp "../../build/libs/*:." src/main/java/com/optimizely/CmabBugBash.java
 *   3. java -cp "../../build/libs/*:." com.optimizely.CmabBugBash
 ***************************************************************************/
package com.optimizely;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.OptimizelyFactory;
import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.cmab.DefaultCmabClient;
import com.optimizely.ab.cmab.service.DefaultCmabService;
import com.optimizely.ab.config.HttpProjectConfigManager;
import com.optimizely.ab.event.AsyncEventHandler;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption;
import com.optimizely.ab.optimizelydecision.OptimizelyDecision;
import com.optimizely.ab.optimizelyjson.OptimizelyJSON;

public class CmabBugBash {
    
    // Configuration constants
    private static final String SDK_KEY = "YOUR_SDK_KEY"; // prod
    private static final String FLAG_KEY = "cmab_flag";
    
    // Test user IDs
    private static final String USER_QUALIFIED = "test_user_99";      // Will be bucketed into CMAB
    private static final String USER_NOT_BUCKETED = "test_user_1";    // Won't be bucketed (traffic allocation)
    private static final String USER_CACHE_TEST = "cache_user_123";
    
    private static Optimizely optimizelyClient;
    
    public static void main(String[] args) throws InterruptedException {
        String testToRun = System.getProperty("test", "all");
        
        System.out.println("=== CMAB Testing Suite for Java SDK ===");
        System.out.println("Testing CMAB with production environment");
        System.out.println("SDK Key: " + SDK_KEY);
        System.out.println("Flag Key: " + FLAG_KEY + "\n");
        
        // Initialize Optimizely client
//        CmabClientConfig cmabClientConfig = CmabClientConfig.withCmabEndpoint("https://inte.prediction.cmab.optimizely.com/%s")
//        DefaultCmabClient defaultCmabClient = new DefaultCmabClient(cmabClientConfig);
//        DefaultCmabService cmabService = DefaultCmabService.builder()
//            .withClient(defaultCmabClient)
//            .build();

//        HttpProjectConfigManager configManager = HttpProjectConfigManager.builder()
//            .withSdkKey(SDK_KEY)
//            .build();
//
//        EventHandler eventHandler = AsyncEventHandler.builder()
//            .withOptimizelyHttpClient(null)
//            .build();
//        // Create Optimizely client with custom CMAB service
//        optimizelyClient = OptimizelyFactory.newDefaultInstance(
//            configManager,
//            null,  // notification center
//            eventHandler,  // event handler
//            null,  // odp api manager
//            cmabService
//        );
        optimizelyClient = OptimizelyFactory.newDefaultInstance(SDK_KEY);
        
        // Wait for datafile to load
        System.out.println("Waiting for datafile to load...");
        Thread.sleep(2000);
        
        // Validate client
        if (!optimizelyClient.isValid()) {
            System.err.println("ERROR: Optimizely client invalid. Verify SDK key.");
            return;
        }
        
        System.out.println("Optimizely client initialized successfully.\n");
        
        // Run tests
        try {
            runTests(testToRun);
        } finally {
            // Cleanup
            optimizelyClient.close();
        }
    }
    
    private static void runTests(String testName) throws InterruptedException {
        Map<String, TestCase> testCases = new LinkedHashMap<>();
//        testCases.put("basic", CmabBugBash::testBasicCmab);
//        testCases.put("cache_hit", CmabBugBash::testCacheHit);
//        testCases.put("cache_miss", CmabBugBash::testCacheMissOnAttributeChange);
//        testCases.put("ignore_cache", CmabBugBash::testIgnoreCacheOption);
//        testCases.put("reset_cache", CmabBugBash::testResetCacheOption);
//        testCases.put("invalidate_user", CmabBugBash::testInvalidateUserCacheOption);
//        testCases.put("concurrent", CmabBugBash::testConcurrentRequests);
//        testCases.put("error", CmabBugBash::testErrorHandling);
//        testCases.put("fallback", CmabBugBash::testFallbackWhenNotQualified);
//        testCases.put("traffic", CmabBugBash::testTrafficAllocation);
        testCases.put("forced", CmabBugBash::testForcedVariationOverride);
//        testCases.put("event_tracking", CmabBugBash::testEventTracking);
//        testCases.put("attribute_types", CmabBugBash::testAttributeTypes);
//        testCases.put("performance", CmabBugBash::testPerformanceBenchmarks);
//        testCases.put("cache_expiry", CmabBugBash::testCacheExpiry);
        
        if ("all".equals(testName)) {
            for (Map.Entry<String, TestCase> entry : testCases.entrySet()) {
                entry.getValue().run();
            }
        } else if (testCases.containsKey(testName)) {
            testCases.get(testName).run();
        } else {
            System.err.println("Unknown test case: " + testName);
            System.out.println("Available test cases: " + String.join(", ", testCases.keySet()));
        }
    }
    
    @FunctionalInterface
    interface TestCase {
        void run() throws InterruptedException;
    }
    
    /**
     * Test 1: Basic CMAB functionality
     * 
     * EXPECTED BEHAVIOR:
     * - User qualifies for CMAB experiment (has required attribute)
     * - First decision triggers CMAB API call to fetch personalized variation
     * - Decision returns variation key, enabled=true, and flag variables
     * - CMAB UUID is generated and stored with decision for analytics tracking
     * - Subsequent decisions with same attributes hit cache (no new API call)
     * - Different attributes trigger cache miss and new CMAB API call
     * 
     * VALIDATION:
     * ✓ Verify CMAB API call appears in logs for first decision
     * ✓ Verify cache hit logs appear for repeated decisions with same attributes
     * ✓ Verify different attributes trigger new CMAB call (cache miss)
     * ✓ Check all decisions return valid variation keys and enabled=true
     */
    private static void testBasicCmab() {
        System.out.println("\n--- Test: Basic CMAB Functionality ---");
        
        for (int i = 1; i <= 1; i++) {
            System.out.println("=== Iteration " + i + " ===");
            
            // Test with user who qualifies for CMAB
            Map<String, Object> attributes1 = new HashMap<>();
            attributes1.put("cmab_attribute", "world");
            OptimizelyUserContext userContext = optimizelyClient.createUserContext(USER_QUALIFIED, attributes1);
            
            OptimizelyDecision decision = userContext.decide(FLAG_KEY);
            printDecision("CMAB Qualified User", decision);
            
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Cache miss - different attributes
            Map<String, Object> attributes2 = new HashMap<>();
            attributes2.put("cmab_attribute", "hello");
            OptimizelyUserContext userContext2 = optimizelyClient.createUserContext(USER_QUALIFIED, attributes2);
            
            OptimizelyDecision decision2 = userContext2.decide(FLAG_KEY);
            printDecision("CMAB Qualified User2 (Different Attributes)", decision2);
            
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Cache hit - same attributes as userContext (attributes1)
            Map<String, Object> attributes3 = new HashMap<>();
            attributes3.put("cmab_attribute", "world");
            OptimizelyUserContext userContext3 = optimizelyClient.createUserContext(USER_QUALIFIED, attributes3);
            
            OptimizelyDecision decision3 = userContext3.decide(FLAG_KEY);
            printDecision("CMAB Qualified User3 (Cache Hit Expected)", decision3);
            
            System.out.println("===============================");
        }
    }
    
    /**
     * Test 2: Cache hit scenario
     * 
     * EXPECTED BEHAVIOR:
     * - First decision with specific attributes triggers CMAB API call
     * - SDK stores decision in cache with key: hash(userId + ruleId + attributesHash)
     * - Second decision with SAME user and SAME attributes returns cached result
     * - No new CMAB API call made (check debug logs)
     * - Cache hit log message appears: "CMAB cache hit for user..."
     * - Changing attributes invalidates cache → triggers new CMAB call
     * 
     * VALIDATION:
     * ✓ First decision: CMAB API call in logs
     * ✓ Second decision: Cache hit log, no API call
     * ✓ After attribute change: Cache miss log + new CMAB API call
     * ✓ All decisions return consistent variation for same attribute set
     */
    private static void testCacheHit() {
        System.out.println("\n--- Test: Cache Hit Scenario ---");
        
        for (int i = 1; i <= 1; i++) {
            System.out.println("=== Iteration " + i + " ===");
            
            // Initial decision - user qualifies for CMAB
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("cmab_attribute", "hello");
            OptimizelyUserContext userContext = optimizelyClient.createUserContext(USER_QUALIFIED, attributes);
            
            List<OptimizelyDecideOption> options = Arrays.asList(
                OptimizelyDecideOption.INCLUDE_REASONS,
                OptimizelyDecideOption.DISABLE_DECISION_EVENT
            );
            
            OptimizelyDecision decision = userContext.decide(FLAG_KEY, options);
            printDecision("Initial Decision (Qualified)", decision);
            
            // Simulate cache hit by reusing user_context
            OptimizelyDecision decision2 = userContext.decide(FLAG_KEY, options);
            printDecision("Cache Hit Decision (Expected)", decision2);
            
            // Change attribute and test cache miss
            userContext.setAttribute("cmab_attribute", "world");
            OptimizelyDecision decision3 = userContext.decide(FLAG_KEY, options);
            printDecision("After Attribute Change (Cache Miss Expected)", decision3);
            
            System.out.println("===============================");
        }
    }
    
    /**
     * Test 3: Cache miss on attribute change
     * 
     * EXPECTED BEHAVIOR:
     * - Initial decision caches result with attributesHash = hash({"cmab_attribute": "hello"})
     * - When attribute changes to "world", attributesHash changes
     * - Cache lookup finds entry but attributesHash doesn't match → cache miss
     * - Old cache entry is removed
     * - New CMAB API call fetches decision for new attributes
     * - New result cached with updated attributesHash
     * 
     * VALIDATION:
     * ✓ Logs show "CMAB cache attributes mismatch"
     * ✓ New CMAB API call made after attribute change
     * ✓ Decision variations may differ for different attributes (personalization)
     */
    private static void testCacheMissOnAttributeChange() {
        System.out.println("\n--- Test: Cache Miss on Attribute Change ---");
        
        for (int i = 1; i <= 1; i++) {
            System.out.println("=== Iteration " + i + " ===");
            
            // Initial decision - user qualifies for CMAB
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("cmab_attribute", "hello");
            OptimizelyUserContext userContext = optimizelyClient.createUserContext(USER_QUALIFIED, attributes);
            
            OptimizelyDecision decision = userContext.decide(FLAG_KEY);
            printDecision("Initial Decision (Qualified)", decision);
            
            // Change attribute - expect cache miss
            userContext.setAttribute("cmab_attribute", "world");
            OptimizelyDecision decision2 = userContext.decide(FLAG_KEY);
            printDecision("Decision After Attribute Change (Cache Miss Expected)", decision2);
            
            System.out.println("===============================");
        }
    }
    
    /**
     * Test 4: Ignore cache option
     * 
     * EXPECTED BEHAVIOR:
     * - OptimizelyDecideOption.IGNORE_CMAB_CACHE bypasses cache for this decision only
     * - Even if cached result exists, new CMAB API call is made
     * - New result is NOT stored in cache (cache remains unchanged)
     * - Subsequent decisions without this option can still use old cache
     * - Useful for testing or forcing fresh decisions
     * 
     * VALIDATION:
     * ✓ First decision: CMAB API call + result cached
     * ✓ Second decision with IGNORE_CMAB_CACHE: New CMAB API call despite cache
     * ✓ Third decision without option: Uses original cached result (no new call)
     * ✓ Check logs for "Ignoring CMAB cache" message
     */
    private static void testIgnoreCacheOption() {
        System.out.println("\n--- Test: Ignore Cache Option ---");
        
        // User context with cache
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("cmab_attribute", "hello");
        OptimizelyUserContext userContext = optimizelyClient.createUserContext(USER_CACHE_TEST, attributes);
        
        OptimizelyDecision decision = userContext.decide(FLAG_KEY);
        printDecision("Initial Decision (With Cache)", decision);
        
        // Force ignore cache
        List<OptimizelyDecideOption> options = Arrays.asList(
            OptimizelyDecideOption.INCLUDE_REASONS,
            OptimizelyDecideOption.IGNORE_CMAB_CACHE
        );
        
        OptimizelyDecision decision2 = userContext.decide(FLAG_KEY, options);
        printDecision("Decision With Cache Ignored (New API Call Expected)", decision2);
        
        // Should return original cache
        OptimizelyDecision decision3 = userContext.decide(FLAG_KEY);
        printDecision("Decision With Cache (Original Cache Expected)", decision3);
        
        System.out.println("===============================");
    }
    
    /**
     * Test 5: Reset cache option
     * 
     * EXPECTED BEHAVIOR:
     * - OptimizelyDecideOption.RESET_CMAB_CACHE clears ENTIRE cache (all users, all experiments)
     * - This is a global operation affecting all subsequent decisions
     * - After reset, all users need fresh CMAB API calls
     * - Use cautiously - impacts all concurrent users
     * - Logs show "Resetting CMAB cache"
     * 
     * VALIDATION:
     * ✓ User 1 and User 2 both have cached decisions initially
     * ✓ User 1 decision with RESET_CMAB_CACHE clears ALL cache
     * ✓ User 1 makes new CMAB API call
     * ✓ User 2's next decision also makes new CMAB API call (cache was cleared)
     * ✓ Both users get fresh decisions from CMAB service
     */
    private static void testResetCacheOption() {
        System.out.println("\n--- Test: Reset Cache Option ---");
        
        // Setup two different users
        Map<String, Object> attributes1 = new HashMap<>();
        attributes1.put("cmab_attribute", "hello");
        OptimizelyUserContext userContext1 = optimizelyClient.createUserContext("reset_user_1", attributes1);
        
        Map<String, Object> attributes2 = new HashMap<>();
        attributes2.put("cmab_attribute", "hello");
        OptimizelyUserContext userContext2 = optimizelyClient.createUserContext("reset_user_2", attributes2);
        
        // Populate cache for both users
        OptimizelyDecision decision1 = userContext1.decide(FLAG_KEY);
        printDecision("User 1 Initial Decision", decision1);
        
        OptimizelyDecision decision2 = userContext2.decide(FLAG_KEY);
        printDecision("User 2 Initial Decision", decision2);
        
        // Reset cache for all users
        List<OptimizelyDecideOption> options = Arrays.asList(
            OptimizelyDecideOption.INCLUDE_REASONS,
            OptimizelyDecideOption.RESET_CMAB_CACHE
        );
        
        OptimizelyDecision decision3 = userContext1.decide(FLAG_KEY, options);
        printDecision("User 1 After RESET (New API Call Expected)", decision3);
        
        // Check if User 2's cache was also cleared and new API call made
        OptimizelyDecision decision4 = userContext2.decide(FLAG_KEY);
        printDecision("User 2 After Reset (New API Call Expected - Global Reset)", decision4);
        
        System.out.println("===============================");
    }
    
    /**
     * Test 6: Invalidate user cache option
     * 
     * EXPECTED BEHAVIOR:
     * - OptimizelyDecideOption.INVALIDATE_USER_CMAB_CACHE clears cache for SPECIFIC user only
     * - Other users' cached decisions remain intact
     * - After invalidation, that user gets fresh CMAB API call
     * - More surgical than RESET_CMAB_CACHE (doesn't affect other users)
     * - Logs show "Invalidating CMAB cache for user 'X'"
     * 
     * TEST FLOW:
     * 1. User 1: "hello" → CMAB API call → Cache stored for User 1
     * 2. User 2: "hello" → CMAB API call → Cache stored for User 2
     * 3. User 1 + INVALIDATE_USER_CMAB_CACHE → Only User 1's cache cleared → New CMAB API call
     * 4. User 2: Same "hello" → User 2's cache preserved → Cache hit (no API call)
     * 
     * VALIDATION:
     * ✓ User 1 invalidation doesn't affect User 2's cache
     * ✓ User 1 gets new CMAB decision after invalidation
     * ✓ User 2 still uses cached decision
     */
    private static void testInvalidateUserCacheOption() {
        System.out.println("\n--- Test: Invalidate User Cache Option ---");
        
        // Setup two different users
        Map<String, Object> attributes1 = new HashMap<>();
        attributes1.put("cmab_attribute", "hello");
        OptimizelyUserContext userContext1 = optimizelyClient.createUserContext("reset_user_1", attributes1);
        
        Map<String, Object> attributes2 = new HashMap<>();
        attributes2.put("cmab_attribute", "hello");
        OptimizelyUserContext userContext2 = optimizelyClient.createUserContext("reset_user_2", attributes2);
        
        // Populate cache for both users
        OptimizelyDecision decision1 = userContext1.decide(FLAG_KEY);
        printDecision("User 1 Initial Decision", decision1);
        
        OptimizelyDecision decision2 = userContext2.decide(FLAG_KEY);
        printDecision("User 2 Initial Decision", decision2);
        
        // Invalidate only user 1's cache
        List<OptimizelyDecideOption> options = Arrays.asList(
            OptimizelyDecideOption.INVALIDATE_USER_CMAB_CACHE
        );
        
        OptimizelyDecision decision3 = userContext1.decide(FLAG_KEY, options);
        printDecision("User 1 After INVALIDATE (New API Call Expected)", decision3);
        
        // Check if User 2's cache is still valid
        OptimizelyDecision decision4 = userContext2.decide(FLAG_KEY);
        printDecision("User 2 Still Cached (Cache Hit Expected)", decision4);
        
        System.out.println("===============================");
    }
    
    /**
     * Test 7: Concurrent requests
     * 
     * EXPECTED BEHAVIOR (Java SDK uses ReentrantLock for thread-safety):
     * - Multiple threads request decision for same user + attributes simultaneously
     * - First thread acquires lock, checks cache (miss), makes CMAB API call
     * - Other threads wait on lock while first completes
     * - First thread stores result in cache and releases lock
     * - Subsequent threads acquire lock, find cached result, return it
     * - Result: 1 CMAB API call + 4 cache hits
     * 
     * KEY REQUIREMENT:
     * - All threads MUST return same variation for consistency
     * - Race conditions should be prevented by locking mechanism
     * 
     * VALIDATION:
     * ✓ Check logs for exactly 1 "Fetching CMAB decision" message
     * ✓ Check logs for 4 "CMAB cache hit" messages
     * ✓ All 5 threads get identical variation key
     * ✓ No duplicate CMAB API calls appear in logs
     */
    private static void testConcurrentRequests() throws InterruptedException {
        System.out.println("\n--- Test: Concurrent Requests ---");
        
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("cmab_attribute", "hello");
        OptimizelyUserContext userContext = optimizelyClient.createUserContext(USER_QUALIFIED, attributes);
        
        // Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);
        
        // Submit 5 concurrent tasks
        for (int i = 1; i <= 5; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    OptimizelyDecision decision = userContext.decide(FLAG_KEY);
                    printDecision("Concurrent Decision " + threadNum, decision);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        System.out.println("===============================");
    }
    
    /**
     * Test 8: Error handling
     * 
     * EXPECTED BEHAVIOR:
     * - User with invalid attribute type fails CMAB audience evaluation
     * - CMAB experiment requires string attribute "cmab_attribute": "hello" or "world"
     * - Using "error" triggers different code path or audience mismatch
     * - SDK logs warning about attribute issues during evaluation
     * - User fails CMAB audience check → falls through to default rollout
     * - Result: Gets rollout variation (typically 'off') instead of CMAB variation
     * 
     * This validates proper error handling and graceful fallback behavior
     * No CMAB API calls should occur if audience targeting fails
     * 
     * VALIDATION:
     * ✓ Decision reasons include audience evaluation failure
     * ✓ No CMAB API call in logs (failed audience check)
     * ✓ Fallback variation returned (usually from rollout rule)
     * ✓ No exceptions thrown - graceful degradation
     */
    private static void testErrorHandling() {
        System.out.println("\n--- Test: Error Handling ---");
        
        // Create user context with attributes that trigger error
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("cmab_attribute", "error");
        OptimizelyUserContext userContext = optimizelyClient.createUserContext(USER_QUALIFIED, attributes);
        
        List<OptimizelyDecideOption> options = Arrays.asList(
            OptimizelyDecideOption.INCLUDE_REASONS
        );
        
        OptimizelyDecision decision = userContext.decide(FLAG_KEY, options);
        printDecision("Decision with Error-Triggering Attributes", decision);
        
        System.out.println("===============================");
    }
    
    /**
     * Test 9: Fallback when not qualified
     * 
     * EXPECTED BEHAVIOR:
     * - User without required attributes fails CMAB audience targeting
     * - User has no attributes (empty map)
     * - CMAB experiment requires "cmab_attribute": "hello" OR "world"
     * - Both audience conditions evaluate to UNKNOWN (null attribute value)
     * - User fails CMAB audience check → falls through to default rollout
     * - Result: Gets rollout variation 'off' from "Everyone Else" rule
     * 
     * KEY VALIDATION:
     * - No CMAB API calls should appear in debug logs
     * - Decision source should be rollout, not CMAB experiment
     * - Variation key should be from rollout rule
     * 
     * This tests proper audience targeting and graceful fallback behavior
     */
    private static void testFallbackWhenNotQualified() {
        System.out.println("\n--- Test: Fallback When Not Qualified ---");
        
        // Test with user who does NOT qualify for CMAB
        Map<String, Object> attributes = new HashMap<>();
        OptimizelyUserContext userContext = optimizelyClient.createUserContext(USER_NOT_BUCKETED, attributes);
        
        OptimizelyDecision decision = userContext.decide(FLAG_KEY);
        printDecision("Not Qualified User Decision (Fallback Expected)", decision);
        
        System.out.println("===============================");
    }
    
    /**
     * Test 10: Traffic allocation
     * 
     * EXPECTED BEHAVIOR:
     * - CMAB experiment has traffic allocation % set in datafile
     * - Users are hashed (userId + experimentId) to determine traffic bucket
     * - USER_NOT_BUCKETED (test_user_1) falls outside traffic allocation range
     * - USER_QUALIFIED (test_user_99) falls inside traffic allocation range
     * 
     * TEST SCENARIOS:
     * 1. test_user_1 + qualifying attributes → NOT in traffic → rollout variation
     * 2. test_user_99 + qualifying attributes → IN traffic → CMAB variation
     * 
     * NOTE: According to requirements, only users IN traffic allocation should trigger CMAB API
     * Users outside traffic should get rollout variations without CMAB call
     * 
     * VALIDATION:
     * ✓ User 1: No CMAB API call, gets rollout variation
     * ✓ User 2: CMAB API call appears in logs, gets CMAB variation
     * ✓ Check decision reasons for traffic allocation messages
     */
    private static void testTrafficAllocation() {
        System.out.println("\n--- Test: Traffic Allocation ---");
        
        for (int i = 1; i <= 2; i++) {
            System.out.println("=== Iteration " + i + " ===");
            
            // User not in traffic allocation (test_user_1)
            Map<String, Object> attributes1 = new HashMap<>();
            attributes1.put("cmab_attribute", "hello");
            OptimizelyUserContext userContext1 = optimizelyClient.createUserContext(USER_NOT_BUCKETED, attributes1);
            
            OptimizelyDecision decision1 = userContext1.decide(FLAG_KEY);
            printDecision("User in Traffic (CMAB Expected)", decision1);

            
            // User in traffic allocation (test_user_99)
            Map<String, Object> attributes2 = new HashMap<>();
            attributes2.put("cmab_attribute", "hello");
            OptimizelyUserContext userContext2 = optimizelyClient.createUserContext(USER_QUALIFIED, attributes2);
            
            OptimizelyDecision decision2 = userContext2.decide(FLAG_KEY);
            printDecision("User Not in Traffic (Rollout Expected)", decision2);
            
            System.out.println("===============================");
        }
    }
    
    /**
     * Test 11: Forced variation override
     * 
     * EXPECTED BEHAVIOR:
     * - Forced variations (if configured) take precedence over CMAB decisions
     * - Forced variations are set via Optimizely UI or setForcedDecision API
     * - If user "forced_user" has forced variation: NO CMAB API call
     * - If no forced variation configured: normal CMAB flow with API call
     * - Current result shows CMAB API call → indicates no forced variation configured
     * 
     * USE CASES:
     * - QA testing specific variations
     * - VIP user experiences
     * - Emergency overrides
     * 
     * NOTE: To test forced variations, you must configure them in Optimizely UI first
     * This test validates forced variation precedence over CMAB decisions
     * 
     * VALIDATION:
     * ✓ If forced variation exists: No CMAB call, forced variation returned
     * ✓ If no forced variation: Normal CMAB flow with API call
     * ✓ Check decision source in reasons
     */
    private static void testForcedVariationOverride() {
        System.out.println("\n--- Test: Forced Variation Override ---");
        
        // User who qualifies for CMAB
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("cmab_attribute", "hello");
        OptimizelyUserContext userContext = optimizelyClient.createUserContext("forced_user", attributes);
        
        OptimizelyDecision decision = userContext.decide(FLAG_KEY);
        printDecision("Forced User Decision", decision);
        
        System.out.println("===============================");
    }
    
    /**
     * Test 12: Event tracking
     * 
     * EXPECTED BEHAVIOR:
     * - Impression events (from decide()) include CMAB UUID in metadata
     * - Conversion events (from trackEvent()) should NOT include CMAB UUID (FX requirement)
     * - CMAB UUID only appears in impression events for analytics correlation
     * - Current result: "cmab_event" should be configured in Optimizely project
     * - If event not configured, SDK logs warning
     * 
     * EVENT FLOW:
     * 1. decide() → Creates impression event with CMAB UUID
     * 2. trackEvent() → Creates conversion event WITHOUT CMAB UUID
     * 3. Both events sent to Optimizely analytics
     * 
     * VALIDATION:
     * ✓ Impression event contains CMAB UUID in metadata (check logs)
     * ✓ Conversion event does NOT contain CMAB UUID
     * ✓ Both events successfully dispatched
     * ✓ Event appears in Optimizely analytics dashboard
     * 
     * NOTE: For full validation, check network requests or event processor logs
     */
    private static void testEventTracking() {
        System.out.println("\n--- Test: Event Tracking ---");
        
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("cmab_attribute", "hello");
        OptimizelyUserContext userContext = optimizelyClient.createUserContext("event_user", attributes);
        
        // Properties for event
        Map<String, Object> properties = new HashMap<>();
        properties.put("Category", "value");
        properties.put("Subcategory", "value");
        properties.put("Text", "value");
        properties.put("URL", "value");
        properties.put("SKU", "value");
        
        Map<String, Object> tags = new HashMap<>();
        tags.put("$opt_event_properties", properties);
        
        // Make decision (creates impression event with CMAB UUID)
        OptimizelyDecision decision = userContext.decide(FLAG_KEY);
        printDecision("Decision for Events", decision);
        
        // Track conversion event (should NOT contain CMAB UUID)
        try {
            userContext.trackEvent("cmab_event", tags);
            System.out.println("\nConversion event tracked: 'cmab_event'");
            System.out.println("Expected: Impression events contain CMAB UUID, conversion events do NOT");
            System.out.println("Check event processor logs for CMAB UUID only in impression events");
        } catch (Exception e) {
            System.out.println("Event tracking error: " + e.getMessage());
            System.out.println("Ensure 'cmab_event' is configured in Optimizely project");
        }
        
        System.out.println("===============================");
    }
    
    /**
     * Test 13: Attribute types
     * 
     * EXPECTED BEHAVIOR:
     * - CMAB service accepts various attribute types: string, number, boolean
     * - SDK properly serializes all attribute types for CMAB API
     * - Audience conditions can use different attribute types for targeting
     * - Each attribute type is hashed correctly for cache key generation
     * 
     * TEST SCENARIOS:
     * 1. User with numeric attribute (age: 30)
     * 2. User with boolean attribute (is_premium: true)
     * 
     * VALIDATION:
     * ✓ Both users get CMAB decisions successfully
     * ✓ No type conversion errors in logs
     * ✓ Attributes properly passed to CMAB API
     * ✓ Cache works correctly with different attribute types
     */
    private static void testAttributeTypes() {
        System.out.println("\n--- Test: Attribute Types ---");
        
        for (int i = 1; i <= 1; i++) {
            System.out.println("=== Iteration " + i + " ===");
            
            // User with numeric attribute
            Map<String, Object> attributes1 = new HashMap<>();
            attributes1.put("country", "us");
            attributes1.put("age", 30);
            OptimizelyUserContext userContext1 = optimizelyClient.createUserContext("user_numeric", attributes1);
            
            OptimizelyDecision decision1 = userContext1.decide(FLAG_KEY);
            printDecision("User with Numeric Attribute", decision1);
            
            // User with boolean attribute
            Map<String, Object> attributes2 = new HashMap<>();
            attributes2.put("country", "us");
            attributes2.put("is_premium", true);
            OptimizelyUserContext userContext2 = optimizelyClient.createUserContext("user_boolean", attributes2);
            
            OptimizelyDecision decision2 = userContext2.decide(FLAG_KEY);
            printDecision("User with Boolean Attribute", decision2);
            
            System.out.println("===============================");
        }
    }
    
    /**
     * Test 14: Performance benchmarks
     * 
     * EXPECTED BEHAVIOR:
     * - First decision: CMAB API call (~100-500ms depending on network)
     * - Subsequent decisions: Cache hits (~1-5ms)
     * - 1000 cached decisions should complete in <10 seconds
     * - Lock contention should be minimal with proper cache usage
     * 
     * PERFORMANCE TARGETS (approximate):
     * - Uncached decision: 100-500ms (includes network latency)
     * - Cached decision: 1-10ms
     * - 1000 cached decisions: <10 seconds
     * 
     * VALIDATION:
     * ✓ Total execution time for 1000 decisions
     * ✓ Average time per decision
     * ✓ Cache hit rate should be ~99.9% (999/1000)
     */
    private static void testPerformanceBenchmarks() {
        System.out.println("\n--- Test: Performance Benchmarks ---");
        
        // Create user context
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("cmab_attribute", "hello");
        OptimizelyUserContext userContext = optimizelyClient.createUserContext(USER_QUALIFIED, attributes);
        
        // Warm up cache with first decision
        userContext.decide(FLAG_KEY);
        
        // Measure time for 1000 decisions
        long startTime = System.nanoTime();
        
        for (int i = 0; i < 1000; i++) {
            userContext.decide(FLAG_KEY);
        }
        
        long endTime = System.nanoTime();
        double durationSeconds = (endTime - startTime) / 1_000_000_000.0;
        
        System.out.println("Decision time for 1000 requests: " + durationSeconds + " seconds");
        System.out.println("Average time per decision: " + (durationSeconds / 1000 * 1000) + " ms");
        
        System.out.println("===============================");
    }
    
    /**
     * Test 15: Cache expiry
     * 
     * EXPECTED BEHAVIOR:
     * - Default CMAB cache TTL: 30 minutes (1800 seconds)
     * - After TTL expires, cached entry is automatically removed
     * - Next decision after expiry triggers new CMAB API call
     * - New result is cached with fresh timestamp
     * 
     * TEST FLOW:
     * 1. Initial decision → CMAB API call → Result cached
     * 2. Wait 31 seconds (> 30 sec for test, but < actual 30min TTL)
     * 3. Second decision → Check if cache expired
     * 
     * NOTE: Full 30-minute expiry test is impractical
     * This test demonstrates the mechanism, but won't wait full TTL
     * In production, cache expiry happens automatically via LRU cache implementation
     * 
     * VALIDATION:
     * ✓ Initial decision caches result
     * ✓ Decision within TTL uses cache
     * ✓ Decision after TTL makes new CMAB call
     * ✓ Check logs for cache miss after expiry
     */
    private static void testCacheExpiry() throws InterruptedException {
        System.out.println("\n--- Test: Cache Expiry ---");
        System.out.println("NOTE: Default TTL is 30 minutes. This test demonstrates the mechanism.");

        DefaultCmabService cmabService = DefaultCmabService.builder()
            .withClient(new DefaultCmabClient())
            .withCmabCacheSize(500)           // Cache up to 500 decisions
            .withCmabCacheTimeoutInSecs(30)  // Refresh cache every 30 seconds for testing
            .build();

        HttpProjectConfigManager configManager = HttpProjectConfigManager.builder()
            .withSdkKey(SDK_KEY)
            .build();

        EventHandler eventHandler = AsyncEventHandler.builder()
            .withOptimizelyHttpClient(null)
            .build();
        // Create Optimizely client with custom CMAB service
        Optimizely optimizelyClient2 = OptimizelyFactory.newDefaultInstance(
            configManager,
            null,  // notification center
            eventHandler,  // event handler
            null,  // odp api manager
            cmabService
        );

        // User context with cache
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("cmab_attribute", "hello");
        OptimizelyUserContext userContext = optimizelyClient2.createUserContext(USER_CACHE_TEST, attributes);
        
        OptimizelyDecision decision = userContext.decide(FLAG_KEY);
        printDecision("Initial Decision (With Cache)", decision);
        
        System.out.println("Waiting 31 seconds to simulate cache expiry...");
        System.out.println("(In production, actual TTL is 30 minutes)");
        Thread.sleep(31000); // Wait 31 seconds
        
        // Decision after simulated cache expiry
        OptimizelyDecision decision2 = userContext.decide(FLAG_KEY);
        printDecision("Decision After Wait (Check if Cache Expired)", decision2);
        
        System.out.println("===============================");
    }
    
    /**
     * Helper method to print decision details with comprehensive information
     */
    private static void printDecision(String context, OptimizelyDecision decision) {
        System.out.println("\n" + context + ":");
        
        if (decision == null) {
            System.out.println("    ❌ Decision: null (CMAB call failed or returned null)");
            System.out.println("    ❌ Check logs above for CMAB service errors (502, timeout, etc.)");
            return;
        }
        
        // Get decision properties
        String variationKey = decision.getVariationKey();
        boolean enabled = decision.getEnabled();
        String flagKey = decision.getFlagKey();
        String ruleKey = decision.getRuleKey();
        List<String> reasons = decision.getReasons();
        
        // Print core decision info
        System.out.println("    Variation Key: " + (variationKey != null ? variationKey : "null"));
        System.out.println("    Enabled: " + (enabled ? "true" : "false"));
        System.out.println("    Flag Key: " + flagKey);
        System.out.println("    Rule Key: " + ruleKey);
        
        // Print variables
        OptimizelyJSON variables = decision.getVariables();
        if (variables != null && variables.toMap() != null && !variables.toMap().isEmpty()) {
            System.out.println("    Variables: " + variables.toMap());
        } else {
            System.out.println("    Variables: {}");
        }
        
        // Print reasons with better formatting
        if (reasons != null && !reasons.isEmpty()) {
            System.out.println("    Reasons:");
            for (String reason : reasons) {
                System.out.println("      • " + reason);
            }
            
            // Check for CMAB-specific indicators
            String reasonsStr = String.join(" ", reasons).toLowerCase();
            if (reasonsStr.contains("cmab")) {
                System.out.println("    🎯 CMAB: Decision from CMAB service");
            } else if (reasonsStr.contains("cache")) {
                System.out.println("    💾 CACHE: Decision from cache");
            } else if (reasonsStr.contains("fallback") || reasonsStr.contains("rollout")) {
                System.out.println("    🔄 FALLBACK: Using fallback/rollout decision");
            }
            
            // Check for errors
            if (reasonsStr.contains("error") || reasonsStr.contains("fail")) {
                System.out.println("    ⚠️  WARNING: Error detected in decision reasons");
            }
        }
        
        // Validate decision
        if (variationKey == null || variationKey.isEmpty()) {
            System.out.println("    ❌ Invalid variation key - check flag configuration");
        }
        
        System.out.println("    📊 [Check debug logs above for detailed CMAB HTTP calls and timing]");
    }
}