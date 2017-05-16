/**
 *
 *    Copyright 2017, Optimizely and contributors
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
package com.optimizely.ab.bucketing;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class Decision {
    @Nonnull
    public String variationId;

    public Decision(@Nonnull String variationId) {
        this.variationId = variationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Decision decision = (Decision) o;

        return variationId.equals(decision.variationId);
    }

    @Override
    public int hashCode() {
        return variationId.hashCode();
    }

    public Map<String, String> toMap() {
        Map<String, String> decisionMap = new HashMap<String, String>(1);
        decisionMap.put(UserProfileService.variationIdKey, variationId);
        return decisionMap;
    }
}
