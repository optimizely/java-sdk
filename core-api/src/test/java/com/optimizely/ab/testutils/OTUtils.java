/**
 *
 *    Copyright 2022, Optimizely and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab.testutils;

import com.optimizely.ab.*;
import java.util.Collections;
import java.util.Map;

public class OTUtils {
    public static OptimizelyUserContext user(String userId, Map<String,?> attributes) {
        Optimizely optimizely = new Optimizely.Builder().build();
        return new OptimizelyUserContext(optimizely, userId, attributes);
    }

    public static OptimizelyUserContext user(Map<String,?> attributes) {
        return user("any-user", attributes);
    }

    public static OptimizelyUserContext user() {
        return user("any-user", Collections.emptyMap());
    }
}