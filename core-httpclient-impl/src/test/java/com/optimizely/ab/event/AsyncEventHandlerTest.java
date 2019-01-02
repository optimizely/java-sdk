/**
 *
 *    Copyright 2016, 2019, Optimizely
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
package com.optimizely.ab.event;

import com.google.common.util.concurrent.MoreExecutors;

import com.optimizely.ab.event.internal.payload.EventBatch;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AsyncEventHandler}.
 */
public class AsyncEventHandlerTest {

    @Rule
    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    CloseableHttpClient mockHttpClient;
    @Mock
    ExecutorService mockExecutorService;

    @Test
    public void testQueueCapacityPreconditionCheck() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        new AsyncEventHandler(-1, 1);
    }

    @Test
    public void testDispatch() throws Exception {
        AsyncEventHandler eventHandler = new AsyncEventHandler(mockHttpClient, MoreExecutors.newDirectExecutorService());
        eventHandler.dispatchEvent(createLogEvent());
        verify(mockHttpClient).execute(any(HttpGet.class), any(ResponseHandler.class));
    }

    /**
     * Verify that {@link RejectedExecutionException}s are caught, rather than being propagated.
     */
    @Test
    public void testRejectedExecutionsAreHandled() throws Exception {
        AsyncEventHandler eventHandler = new AsyncEventHandler(mockHttpClient, mockExecutorService);
        doThrow(RejectedExecutionException.class).when(mockExecutorService).execute(any(Runnable.class));
        eventHandler.dispatchEvent(createLogEvent());
    }

    /**
     * Verify that {@link IOException}s are caught, rather than being propagated (which would cause a worker
     * thread to die).
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testIOExceptionsCaughtInDispatch() throws Exception {
        AsyncEventHandler eventHandler = new AsyncEventHandler(mockHttpClient, MoreExecutors.newDirectExecutorService());

        // have the http client throw an IOException on execute
        when(mockHttpClient.execute(any(HttpGet.class), any(ResponseHandler.class))).thenThrow(IOException.class);
        eventHandler.dispatchEvent(createLogEvent());
        verify(mockHttpClient).execute(any(HttpGet.class), any(ResponseHandler.class));
    }

    /**
     * Verifies the case where all queued events could be processed before the timeout is exceeded.
     */
    @Test
    public void testShutdownAndAwaitTermination() throws Exception {
        AsyncEventHandler eventHandler = new AsyncEventHandler(mockHttpClient, mockExecutorService);
        when(mockExecutorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);

        eventHandler.shutdownAndAwaitTermination(1, TimeUnit.SECONDS);
        verify(mockExecutorService).shutdown();
        verify(mockExecutorService, never()).shutdownNow();

        verify(mockHttpClient).close();
    }

    /**
     * Verify the case where all queued events count NOT be processed before the timeout was exceeded.
     * {@link ExecutorService#shutdownNow()} should be called to drop the queued events and attempt to interrupt
     * ongoing tasks.
     */
    @Test
    public void testShutdownAndForcedTermination() throws Exception {
        AsyncEventHandler eventHandler = new AsyncEventHandler(mockHttpClient, mockExecutorService);
        when(mockExecutorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(false);

        eventHandler.shutdownAndAwaitTermination(1, TimeUnit.SECONDS);
        verify(mockExecutorService).shutdown();
        verify(mockExecutorService).shutdownNow();
        verify(mockHttpClient).close();
    }

    //======== Helper methods ========//

    private LogEvent createLogEvent() {
        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("test", "params");
        return new LogEvent(LogEvent.RequestMethod.GET, "test_url", testParams, new EventBatch());
    }
}
