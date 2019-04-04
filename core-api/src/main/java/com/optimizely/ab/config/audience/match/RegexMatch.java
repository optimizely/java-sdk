/**
 *
 *    Copyright 2019, Optimizely and contributors
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

package com.optimizely.ab.config.audience.match;

import javax.annotation.Nullable;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexMatch extends AttributeMatch<String> {
    String regex;

    protected RegexMatch(String regex) {
        this.regex = regex;
    }
    /**
     * This verifies if provided value satisfies regex condition
     *
     * @param attributeValue
     * @return true/false if the user attribute string value satisfies the condition regex value
     */
    @Nullable
    public Boolean eval(Object attributeValue) {
        try {
            String input = castToValueType(attributeValue, regex);
            try {
                Pattern pattern = Pattern.compile(regex);
                return pattern.matcher(input).matches();
            } catch (PatternSyntaxException exception) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
