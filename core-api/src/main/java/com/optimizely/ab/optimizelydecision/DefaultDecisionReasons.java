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
import java.util.List;

public class DefaultDecisionReasons extends DecisionReasons {

    public static DecisionReasons newInstance(@Nullable List<OptimizelyDecideOption> options) {
        if (options == null || options.contains(OptimizelyDecideOption.INCLUDE_REASONS)) return new DecisionReasons();
        else return new DefaultDecisionReasons();
    }

    public static DecisionReasons newInstance() {
        return newInstance(null);
    }

    @Override
    public String addInfo(String format, Object... args) {
        // skip tracking and pass-through reasons other than critical errors.
        return String.format(format, args);
    }

    @Override
    public void merge(DecisionReasons target) {
        // ignore infos
        errors.addAll(target.errors);
    }

}
