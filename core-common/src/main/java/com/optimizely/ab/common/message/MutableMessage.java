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
package com.optimizely.ab.common.message;

import com.optimizely.ab.common.Callback;

import javax.annotation.Nonnull;

/**
 * Basic mutable implementation for the {@link Message} interface.
 */
public class MutableMessage<T> implements Message<T> {
    private T value;
    private Callback<T> callback;

    public MutableMessage() {
        this(null, null);
    }

    public MutableMessage(T value, Callback<T> callback) {
        this.value = value;
        this.callback = callback;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public void markSuccess() {
        if (callback != null) {
            callback.success(getValue());
        }
    }

    @Override
    public void markFailure(@Nonnull Throwable ex) {
        if (callback != null) {
            callback.failure(getValue(), ex);
        }
    }

    public void set(T value, Callback<T> callback) {
        setValue(value);
        setCallback(callback);
    }

    public void clear() {
        set(null, null);
    }

    protected void setValue(T value) {
        this.value = value;
    }

    protected void setCallback(Callback<T> callback) {
        this.callback = callback;
    }
}
