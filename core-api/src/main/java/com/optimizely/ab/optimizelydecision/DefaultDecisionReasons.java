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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DefaultDecisionReasons implements DecisionReasons {

    private final List<String> errors = new ArrayList<>();
    private final List<String> infos = new ArrayList<>();

    public static DecisionReasons newInstance(@Nullable List<OptimizelyDecideOption> options) {
        if (options != null && options.contains(OptimizelyDecideOption.INCLUDE_REASONS)) return new DefaultDecisionReasons();
        else return new ErrorsDecisionReasons();
    }

    public static DecisionReasons newInstance() {
        return newInstance(null);
    }

    public void addError(String format, Object... args) {
        String message = String.format(format, args);
        errors.add(message);
    }

    public String addInfo(String format, Object... args) {
        String message = String.format(format, args);
        infos.add(message);
        return message;
    }

    public List<String> toReport() {
        List<String> reasons = new ArrayList<>(errors);
        reasons.addAll(infos);
        return reasons;
    }

}
