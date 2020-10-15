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

import java.util.ArrayList;
import java.util.List;

public class DecisionReasons {

    private boolean includeReasons;
    private final List<String> errors = new ArrayList<>();
    private final List<String> logs = new ArrayList<>();

    public DecisionReasons(boolean includeReasons) {
        this.includeReasons = includeReasons;
    }

    public DecisionReasons() {
        this(false);
    }

    public void addError(String message) {
        errors.add(message);
    }

    public void addInfo(String message) {
        logs.add(message);
    }

    public String addInfoF(String format, Object... args) {
        String message = String.format(format, args);
        if(includeReasons) addInfo(message);
        return message;
    }

    public List<String> toReport() {
        List<String> reasons = new ArrayList<>(errors);
        if(includeReasons) {
            reasons.addAll(logs);
        }
        return reasons;
    }

}
