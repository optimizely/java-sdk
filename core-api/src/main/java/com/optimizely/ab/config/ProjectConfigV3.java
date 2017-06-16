/**
 *
 *    Copyright 2016-2017, Optimizely and contributors
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
package com.optimizely.ab.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.Condition;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implements the Optimizely Project configuration for a v3 datafile.
 *
 * @see <a href="http://developers.optimizely.com/server/reference/index.html#json">Project JSON</a>
 */
@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectConfigV3 extends ProjectConfig {

    public ProjectConfigV3(String accountId, String projectId, String version, String revision, List<Group> groups,
                           List<Experiment> experiments, List<Attribute> attributes, List<EventType> eventType,
                           List<Audience> audiences, boolean anonymizeIP, List<LiveVariable> liveVariables) {
        super(accountId,
                projectId,
                version,
                revision,
                groups,
                experiments,
                attributes,
                eventType,
                audiences,
                anonymizeIP,
                liveVariables);
    }
}
