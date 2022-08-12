/**
 *
 *    Copyright 2016-2018, 2022, Optimizely and contributors
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
package com.optimizely.ab.config.audience;

import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.config.ProjectConfig;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Interface implemented by all conditions condition objects to aid in condition evaluation.
 */
public interface Condition<T> {

    @Nullable
    Boolean evaluate(ProjectConfig config, OptimizelyUserContext user);

    String toJson();

    String getOperandOrId();

    List<Condition> getConditions();
}
