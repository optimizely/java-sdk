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
package com.optimizely.ab.common.lifecycle;

import java.util.concurrent.TimeUnit;

public interface LifecycleAware {
    /**
     * Called once when component starts.
     */
    void onStart();

    /**
     * Called once when component stops.
     *
     * Blocks execution until all tasks have completed execution after a shutdown request, or the
     * timeout occurs, or the current thread is interrupted, whichever happens first.
     *
     * TODO remove timeout arguments and return value
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return true if the receiver was stopped cleanly and normally, false otherwise.
     */
    boolean onStop(long timeout, TimeUnit unit);

    /**
     * Helper to call {@link #onStop(long, TimeUnit)} on input when it is lifecycle aware.
     *
     * @param component object to try lifecycle method invocation
     */
    static void start(Object component) {
        if (component instanceof LifecycleAware) {
            ((LifecycleAware) component).onStart();
        }
    }

    /**
     * Helper to call {@link #onStop(long, TimeUnit)} on input when it is lifecycle aware.
     *
     * @param component object to try lifecycle method invocation
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return true if object is lifecycle aware and was stopped, otherwise false
     */
    static boolean stop(Object component, long timeout, TimeUnit unit) {
        if (component instanceof LifecycleAware) {
            return ((LifecycleAware) component).onStop(timeout, unit);
        }
        return true;
    }
}
