/**
 *
 *    Copyright 2016-2017, 2019, 2022, Optimizely and contributors
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.optimizely.ab.config.IdKeyMapped;

import javax.annotation.concurrent.Immutable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the Optimizely Audience configuration.
 *
 * @see <a href="http://developers.optimizely.com/server/reference/index.html#json">Project JSON</a>
 */
@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class Audience implements IdKeyMapped {

    private final String id;
    private final String name;
    private final Condition<UserAttribute> conditions;

    @JsonCreator
    public Audience(@JsonProperty("id") String id,
                    @JsonProperty("name") String name,
                    @JsonProperty("conditions") Condition conditions) {
        this.id = id;
        this.name = name;
        this.conditions = conditions;
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return name;
    }

    public String getName() {
        return name;
    }

    public Condition getConditions() {
        return conditions;
    }

    @Override
    public String toString() {
        return "Audience{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", conditions=" + conditions +
            '}';
    }

    public Set<String> getSegments() {
        return getSegments(conditions);
    }

    private static Set<String> getSegments(Condition conditions) {
        List<Condition> nestedConditions = conditions.getConditions();
        Set<String> segments = new HashSet<>();
        if (nestedConditions != null) {
            for (Condition nestedCondition : nestedConditions) {
                Set<String> nestedSegments = getSegments(nestedCondition);
                segments.addAll(nestedSegments);
            }
        } else {
            if (conditions.getClass() == UserAttribute.class) {
                UserAttribute userAttributeCondition = (UserAttribute) conditions;
                if (UserAttribute.QUALIFIED.equals(userAttributeCondition.getMatch())) {
                    segments.add((String)userAttributeCondition.getValue());
                }
            }
        }
        return segments;
    }
}
