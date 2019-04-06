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
package com.optimizely.ab.common.callback;

import org.junit.Test;

import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class CallbackHolderTest {
    @Test
    public void testDefaultsNoOp_success() {
        new CallbackHolder<>().success(new Object());
    }

    @Test
    public void testDefaultsNoOp_failure() {
        new CallbackHolder<>().failure(new Object(), new RuntimeException());
    }

    @Test
    public void testSuccess() {
        CallbackHolder<Integer> holder = new CallbackHolder<>();

        Callback<Number> cb = (Callback<Number>) spy(Callback.class);
        holder.set(cb);

        holder.success(1);
        verify(cb, only()).success(1);

        // can be cleared (defaults to no-op)
        holder.set(null);
        holder.success(2);
        holder.failure(-2, new RuntimeException());

        verifyNoMoreInteractions(cb);
    }

    @Test
    public void testFailure() {
        CallbackHolder<Integer> holder = new CallbackHolder<>();

        Callback<Number> cb = (Callback<Number>) spy(Callback.class);
        holder.set(cb);

        RuntimeException ex = new RuntimeException();
        holder.failure(1, ex);
        verify(cb, only()).failure(1, ex);

        // can be cleared (defaults to no-op)
        holder.set(null);
        holder.success(2);
        holder.failure(-2, new RuntimeException());

        verifyNoMoreInteractions(cb);
    }
}