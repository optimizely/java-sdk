package com.optimizely.ab.processor.batch;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class BatchingTaskTest {
    public static final Logger logger = LoggerFactory.getLogger(BatchingTaskTest.class);

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
        BatchingTask<Integer, ArrayList<Object>> task = new BatchingTask<>(
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
        BatchingTask<Integer, ArrayList<Object>> task = new BatchingTask<>(
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
    public void testClosedBeforeRun() throws Exception {
        BatchingTask<Integer, ArrayList<Object>> task = new BatchingTask<>(
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
        BatchingTask<Integer, ArrayList<Object>> task = new BatchingTask<>(
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
}