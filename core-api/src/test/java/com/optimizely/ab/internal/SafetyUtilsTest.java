/**
 *
 *    Copyright 2019, Optimizely
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
package com.optimizely.ab.internal;

import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SafetyUtilsTest {

    @Test
    public void tryCloseAutoCloseable() throws Exception {
        AutoCloseable autocloseable = mock(AutoCloseable.class);
        SafetyUtils.tryClose(autocloseable);

        verify(autocloseable).close();
    }

    @Test
    public void tryCloseCloseable() throws Exception {
        Closeable closeable = mock(Closeable.class);
        SafetyUtils.tryClose(closeable);

        verify(closeable).close();
    }

    @Test
    public void tryCloseNullDoesNotThrow() throws Exception {
        SafetyUtils.tryClose(null);
    }

    @Test
    public void tryCloseExceptionDoesNotThrow() throws Exception {
        AutoCloseable autocloseable = mock(AutoCloseable.class);
        doThrow(new RuntimeException()).when(autocloseable).close();
        SafetyUtils.tryClose(autocloseable);
    }
}
