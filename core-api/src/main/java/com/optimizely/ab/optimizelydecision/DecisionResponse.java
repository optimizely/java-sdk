/**
 *
 *    Copyright 2020, Optimizely and contributors
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
package com.optimizely.ab.optimizelydecision;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DecisionResponse<T> {
    private T result;
    private DecisionReasons reasons;
    private boolean error;
    private String cmabUuid;

    public DecisionResponse(@Nullable T result, @Nonnull DecisionReasons reasons, @Nonnull boolean error, @Nullable String cmabUuid) {
        this.result = result;
        this.reasons = reasons;
        this.error = error;
        this.cmabUuid = cmabUuid;
    }

    public DecisionResponse(@Nullable T result, @Nonnull DecisionReasons reasons) {
        this(result, reasons, false, null);
    }

    public static <E> DecisionResponse<E> responseNoReasons(@Nullable E result) {
        return new DecisionResponse<>(result, DefaultDecisionReasons.newInstance(), false, null);
    }

    public static <E> DecisionResponse<E> nullNoReasons() {
        return new DecisionResponse<>(null, DefaultDecisionReasons.newInstance(), false, null);
    }

    @Nullable
    public T getResult() {
        return result;
    }

    @Nonnull
    public DecisionReasons getReasons() {
        return reasons;
    }

    @Nonnull
    public boolean isError(){
        return error;
    }

    @Nullable
    public String getCmabUuid() {
        return cmabUuid;
    }
}
