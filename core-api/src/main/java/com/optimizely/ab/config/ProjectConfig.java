/**
 *
 *    Copyright 2016-2022, Optimizely and contributors
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

import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.error.ErrorHandler;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ProjectConfig is an interface capturing the experiment, variation and feature definitions.
 *
 * The default implementation of ProjectConfig can be found in {@link DatafileProjectConfig}.
 */
public interface ProjectConfig {
    String RESERVED_ATTRIBUTE_PREFIX = "$opt_";

    @CheckForNull
    Experiment getExperimentForKey(@Nonnull String experimentKey,
                                   @Nonnull ErrorHandler errorHandler);

    @CheckForNull
    EventType getEventTypeForName(String eventName, ErrorHandler errorHandler);

    @Nullable
    Experiment getExperimentForVariationId(String variationId);

    String getAttributeId(ProjectConfig projectConfig, String attributeKey);

    String getAccountId();

    String toDatafile();

    String getProjectId();

    String getVersion();

    String getRevision();

    String getSdkKey();

    String getEnvironmentKey();

    boolean getSendFlagDecisions();

    boolean getAnonymizeIP();

    Boolean getBotFiltering();

    List<Group> getGroups();

    List<Experiment> getExperiments();

    Set<String> getAllSegments();

    List<Experiment> getExperimentsForEventKey(String eventKey);

    List<FeatureFlag> getFeatureFlags();

    List<Rollout> getRollouts();

    List<Attribute> getAttributes();

    List<EventType> getEventTypes();

    List<Audience> getAudiences();

    List<Audience> getTypedAudiences();

    List<Integration> getIntegrations();

    Audience getAudience(String audienceId);

    Map<String, Experiment> getExperimentKeyMapping();

    Map<String, Attribute> getAttributeKeyMapping();

    Map<String, EventType> getEventNameMapping();

    Map<String, Audience> getAudienceIdMapping();

    Map<String, Experiment> getExperimentIdMapping();

    Map<String, Group> getGroupIdMapping();

    Map<String, Rollout> getRolloutIdMapping();

    Map<String, FeatureFlag> getFeatureKeyMapping();

    Map<String, List<String>> getExperimentFeatureKeyMapping();

    Map<String, List<Variation>> getFlagVariationsMap();

    Variation getFlagVariationByKey(String flagKey, String variationKey);

    String getHostForODP();

    String getPublicKeyForODP();

    @Override
    String toString();

    public enum Version {
        V2("2"),
        V3("3"),
        V4("4");

        private final String version;

        Version(String version) {
            this.version = version;
        }

        @Override
        public String toString() {
            return version;
        }
    }
}
