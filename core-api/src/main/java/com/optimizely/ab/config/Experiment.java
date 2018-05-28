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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.optimizely.ab.faultinjection.ExceptionSpot;
import com.optimizely.ab.faultinjection.FaultInjectionManager;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Represents the Optimizely Experiment configuration.
 *
 * @see <a href="http://developers.optimizely.com/server/reference/index.html#json">Project JSON</a>
 */
@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class Experiment implements IdKeyMapped {

    private String id;
    private String key;
    private String status;
    private String layerId;
    private String groupId;

    private List<String> audienceIds;
    private List<Variation> variations;
    private List<TrafficAllocation> trafficAllocation;

    private Map<String, Variation> variationKeyToVariationMap;
    private Map<String, Variation> variationIdToVariationMap;
    private Map<String, String> userIdToVariationKeyMap;

    private static void injectFault(ExceptionSpot spot) {
        FaultInjectionManager.getInstance().injectFault(spot);
    }

    public enum ExperimentStatus {
        RUNNING ("Running"),
        LAUNCHED ("Launched"),
        PAUSED ("Paused"),
        NOT_STARTED ("Not started"),
        ARCHIVED ("Archived");

        private final String experimentStatus;

        ExperimentStatus(String experimentStatus) {
            this.experimentStatus = experimentStatus;
        }

        public String toString() {
            return experimentStatus;
        }
    }

    @JsonCreator
    public Experiment(@JsonProperty("id") String id,
                      @JsonProperty("key") String key,
                      @JsonProperty("status") String status,
                      @JsonProperty("layerId") String layerId,
                      @JsonProperty("audienceIds") List<String> audienceIds,
                      @JsonProperty("variations") List<Variation> variations,
                      @JsonProperty("forcedVariations") Map<String, String> userIdToVariationKeyMap,
                      @JsonProperty("trafficAllocation") List<TrafficAllocation> trafficAllocation) {
            this(id, key, status, layerId, audienceIds, variations, userIdToVariationKeyMap, trafficAllocation, "");
    }

    public Experiment(@Nonnull String id,
                      @Nonnull String key,
                      @Nullable String status,
                      @Nullable String layerId,
                      @Nonnull List<String> audienceIds,
                      @Nonnull List<Variation> variations,
                      @Nonnull Map<String, String> userIdToVariationKeyMap,
                      @Nonnull List<TrafficAllocation> trafficAllocation,
                      @Nonnull String groupId) {
        try {
            injectFault(ExceptionSpot.Experiment_constructor_spot1);
            this.id = id;
            this.key = key;
            this.status = status == null ? ExperimentStatus.NOT_STARTED.toString() : status;
            this.layerId = layerId;
            this.audienceIds = Collections.unmodifiableList(audienceIds);
            this.variations = Collections.unmodifiableList(variations);
            this.trafficAllocation = Collections.unmodifiableList(trafficAllocation);
            this.groupId = groupId;
            this.userIdToVariationKeyMap = userIdToVariationKeyMap;
            this.variationKeyToVariationMap = ProjectConfigUtils.generateNameMapping(variations);
            this.variationIdToVariationMap = ProjectConfigUtils.generateIdMapping(variations);
        } catch (Exception e) {
            FaultInjectionManager.getInstance().throwExceptionIfTreatmentDisabled();
        }
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getStatus() {
        return status;
    }

    public String getLayerId() {
        return layerId;
    }

    public List<String> getAudienceIds() {
        return audienceIds;
    }

    public List<Variation> getVariations() {
        return variations;
    }

    public Map<String, Variation> getVariationKeyToVariationMap() {
        return variationKeyToVariationMap;
    }

    public Map<String, Variation> getVariationIdToVariationMap() {
        return variationIdToVariationMap;
    }

    public Map<String, String> getUserIdToVariationKeyMap() {
        return userIdToVariationKeyMap;
    }

    public List<TrafficAllocation> getTrafficAllocation() {
        return trafficAllocation;
    }

    public String getGroupId() {
        return groupId;
    }

    public boolean isActive() {
        try {
            injectFault(ExceptionSpot.Experiment_isActive_spot1);
            return status.equals(ExperimentStatus.RUNNING.toString()) ||
                    status.equals(ExperimentStatus.LAUNCHED.toString());
        } catch (Exception e) {
            FaultInjectionManager.getInstance().throwExceptionIfTreatmentDisabled();
            return false;
        }
    }

    public boolean isRunning() {
        try {
            injectFault(ExceptionSpot.Experiment_isRunning_spot1);
            return status.equals(ExperimentStatus.RUNNING.toString());
        } catch (Exception e) {
            FaultInjectionManager.getInstance().throwExceptionIfTreatmentDisabled();
            return false;
        }
    }

    public boolean isLaunched() {
        try {
            injectFault(ExceptionSpot.Experiment_isLaunched_spot1);
            return status.equals(ExperimentStatus.LAUNCHED.toString());
        } catch (Exception e) {
            FaultInjectionManager.getInstance().throwExceptionIfTreatmentDisabled();
            return false;
        }
    }

    @Override
    public String toString() {
        try {
            injectFault(ExceptionSpot.Experiment_toString_spot1);
            return "Experiment{" +
                    "id='" + id + '\'' +
                    ", key='" + key + '\'' +
                    ", groupId='" + groupId + '\'' +
                    ", status='" + status + '\'' +
                    ", audienceIds=" + audienceIds +
                    ", variations=" + variations +
                    ", variationKeyToVariationMap=" + variationKeyToVariationMap +
                    ", userIdToVariationKeyMap=" + userIdToVariationKeyMap +
                    ", trafficAllocation=" + trafficAllocation +
                    '}';
        } catch (Exception e) {
            FaultInjectionManager.getInstance().throwExceptionIfTreatmentDisabled();
            return null;
        }
    }
}
