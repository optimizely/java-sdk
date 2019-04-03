/**
 *    Copyright 2019, Optimizely Inc. and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab.processor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

public class BatchTaskTest {
    public static final Logger logger = LoggerFactory.getLogger(BatchTaskTest.class);

    private ExecutorService executor;
    private List<Collection<Object>> batches;

    @Before
    public void setUp() {
        executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("batching-processor-%d")
            .build());
        batches = Collections.synchronizedList(new ArrayList<>());
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void testMaxSize() throws Exception {
        BatchTask<Integer, ArrayList<Object>> task = new BatchTask<>(
            ArrayList::new,
            batches::add,
            2,
            Duration.ofMinutes(1));

        Future future = executor.submit(task);

        assertThat(task.isOpen(), is(true));
        assertThat(task.offer(1), is(true));
        assertThat(task.offer(2), is(true));
        assertThat(task.isOpen(), is(false));

        assertThat(task.offer(3), is(false));
        assertThat(task.isOpen(), is(false));

        future.get(1, TimeUnit.SECONDS);

        assertThat(batches, hasSize(1));
        assertThat(batches.get(0), contains(1, 2));
    }

    @Test
    public void testMaxAge() throws Exception {
        BatchTask<Integer, ArrayList<Object>> task = new BatchTask<>(
            ArrayList::new,
            batches::add,
            1000,
            Duration.ofMillis(100));

        assertThat(task.offer(1), is(true));

        Future future = executor.submit(task);
        future.get(1, TimeUnit.SECONDS);

        assertThat(batches, hasSize(1));
        assertThat(batches.get(0), contains(1));
        assertThat(task.offer(3), is(false));
        assertThat(task.isOpen(), is(false));
    }

    @Test
    public void testNoMaxSize() throws Exception {
        BatchTask<Integer, ArrayList<Object>> task = new BatchTask<>(
            ArrayList::new,
            batches::add,
            null,
            Duration.ofDays(1));

        for (int i = 0; i < 100; i++) {
            task.offer(i);
        }

        Future future = executor.submit(task);

        for (int i = 100; i < 200; i++) {
            task.offer(i);
        }

        task.close();
        future.get(1, TimeUnit.SECONDS);

        assertThat(batches, hasSize(1));
        assertThat(batches.get(0), hasSize(200));
    }

    @Test
    public void testClosedBeforeRun() throws Exception {
        BatchTask<Integer, ArrayList<Object>> task = new BatchTask<>(
            ArrayList::new,
            batches::add,
            2,
            null);

        assertThat(task.isOpen(), is(true));
        assertThat(task.offer(1), is(true));
        assertThat(task.isOpen(), is(true));
        assertThat(task.offer(2), is(true));
        assertThat(task.isOpen(), is(false));
        assertThat(task.offer(3), is(false));
        assertThat(task.isOpen(), is(false));

        assertThat(batches, empty());

        Future future = executor.submit(task);
        future.get(1, TimeUnit.SECONDS);

        assertThat(batches, hasSize(1));
        assertThat(batches.get(0), contains(1, 2));
    }

    @Test
    public void testTimedFromFirstElement() throws Exception {
        BatchTask<Integer, ArrayList<Object>> task = new BatchTask<>(
            ArrayList::new,
            batches::add,
            1000,
            Duration.ofMillis(10));

        Future future = executor.submit(task);

        assertThat(task.isOpen(), is(true));
        Thread.sleep(250L);
        assertThat(future.isDone(), is(false));
        assertThat(task.isOpen(), is(true));
        assertThat(task.offer(1), is(true));
        future.get(100L, TimeUnit.MILLISECONDS);
        assertThat(task.isOpen(), is(false));
    }

    @Test
    public void testFlushesWhenInterrupted() throws Exception {
        BatchTask<Integer, ArrayList<Object>> task = new BatchTask<>(
            ArrayList::new,
            batches::add,
            10,
            Duration.ofDays(1));

        Future future = executor.submit(task);

        task.offer(1);
        task.offer(2);

        future.cancel(true);
        await().atMost(500, TimeUnit.MILLISECONDS).until(future::isDone);

        assertThat(batches, hasSize(1));
        assertThat(batches.get(0), contains(1, 2));
    }

    @Test
    public void testThrowsIfRunAgain() throws Exception {
        BatchTask<Integer, ArrayList<Object>> task = new BatchTask<>(
            ArrayList::new,
            batches::add,
            1,
            Duration.ofSeconds(1));

        task.offer(1);
        executor.submit(task).get(1, TimeUnit.SECONDS);

        try {
            executor.submit(task).get(1, TimeUnit.SECONDS);
            fail("Should have thrown");
        } catch (ExecutionException e) {
            assertThat(e.getCause(), instanceOf(IllegalStateException.class));
        }
    }
}