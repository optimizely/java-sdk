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
package com.optimizely.ab.optimizelyusercontext;

import java.util.ArrayList;
import java.util.List;

public class DecisionReasons {

    List<String> errors;
    List<String> logs;

    public DecisionReasons() {
        this.errors = new ArrayList<String>();
        this.logs = new ArrayList<String>();
    }

    public void addError(String message) {
        errors.add(message);
    }

    public void addInfo(String message) {
        logs.add(message);
    }


    public List<String> toReport(List<OptimizelyDecideOption> options) {
        List<String> reasons = new ArrayList<>(errors);
        if(options.contains(OptimizelyDecideOption.INCLUDE_REASONS)) {
            reasons.addAll(logs);
        }
        return reasons;
    }

}
