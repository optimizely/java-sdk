/**
 *
 *    Copyright 2016-2019, 2021, Optimizely and contributors
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

import com.fasterxml.jackson.annotation.*;
import com.optimizely.ab.annotations.VisibleForTesting;
import com.optimizely.ab.config.audience.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents the Optimizely Experiment configuration.
 *
 * @see <a href="http://developers.optimizely.com/server/reference/index.html#json">Project JSON</a>
 */
@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class Experiment implements IdKeyMapped {

    private final String id;
    private final String key;
    private final String status;
    private final String layerId;
    private final String groupId;

    private final List<String> audienceIds;
    private final Condition<AudienceIdCondition> audienceConditions;
    private final List<Variation> variations;
    private final List<TrafficAllocation> trafficAllocation;

    private final Map<String, Variation> variationKeyToVariationMap;
    private final Map<String, Variation> variationIdToVariationMap;
    private final Map<String, String> userIdToVariationKeyMap;

    public enum ExperimentStatus {
        RUNNING("Running"),
        LAUNCHED("Launched"),
        PAUSED("Paused"),
        NOT_STARTED("Not started"),
        ARCHIVED("Archived");

        private final String experimentStatus;

        ExperimentStatus(String experimentStatus) {
            this.experimentStatus = experimentStatus;
        }

        public String toString() {
            return experimentStatus;
        }
    }

    @VisibleForTesting
    public Experiment(String id, String key, String layerId) {
        this(id, key, null, layerId, Collections.emptyList(), null, Collections.emptyList(), Collections.emptyMap(), Collections.emptyList(), "");
    }

    @JsonCreator
    public Experiment(@JsonProperty("id") String id,
                      @JsonProperty("key") String key,
                      @JsonProperty("status") String status,
                      @JsonProperty("layerId") String layerId,
                      @JsonProperty("audienceIds") List<String> audienceIds,
                      @JsonProperty("audienceConditions") Condition audienceConditions,
                      @JsonProperty("variations") List<Variation> variations,
                      @JsonProperty("forcedVariations") Map<String, String> userIdToVariationKeyMap,
                      @JsonProperty("trafficAllocation") List<TrafficAllocation> trafficAllocation) {
        this(id, key, status, layerId, audienceIds, audienceConditions, variations, userIdToVariationKeyMap, trafficAllocation, "");
    }

    public Experiment(@Nonnull String id,
                      @Nonnull String key,
                      @Nullable String status,
                      @Nullable String layerId,
                      @Nonnull List<String> audienceIds,
                      @Nullable Condition audienceConditions,
                      @Nonnull List<Variation> variations,
                      @Nonnull Map<String, String> userIdToVariationKeyMap,
                      @Nonnull List<TrafficAllocation> trafficAllocation,
                      @Nonnull String groupId) {
        this.id = id;
        this.key = key;
        this.status = status == null ? ExperimentStatus.NOT_STARTED.toString() : status;
        this.layerId = layerId;
        this.audienceIds = Collections.unmodifiableList(audienceIds);
        this.audienceConditions = audienceConditions;
        this.variations = Collections.unmodifiableList(variations);
        this.trafficAllocation = Collections.unmodifiableList(trafficAllocation);
        this.groupId = groupId;
        this.userIdToVariationKeyMap = userIdToVariationKeyMap;
        this.variationKeyToVariationMap = ProjectConfigUtils.generateNameMapping(variations);
        this.variationIdToVariationMap = ProjectConfigUtils.generateIdMapping(variations);
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

    public Condition getAudienceConditions() {
        return audienceConditions;
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
        return status.equals(ExperimentStatus.RUNNING.toString()) ||
            status.equals(ExperimentStatus.LAUNCHED.toString());
    }

    public boolean isRunning() {
        return status.equals(ExperimentStatus.RUNNING.toString());
    }

    public boolean isLaunched() {
        return status.equals(ExperimentStatus.LAUNCHED.toString());
    }

    public String serializeConditions(Map<String, String> audiencesMap) {

        Condition condition = this.audienceConditions;

        return condition instanceof EmptyCondition ? "" : this.serialize(condition, audiencesMap);
    }

    private String getNameFromAudienceId(String audienceId, Map<String, String> audiencesMap) {
        String audienceName = audiencesMap.get(audienceId);
        return audienceName != null ? "\"" + audienceName + "\"" : "\"" + audienceId + "\"";
    }

    private String getOperandOrAudienceId(Condition condition, Map<String, String> audiencesMap){
        String operand = "";
        if (condition != null) {
            if (condition instanceof AndCondition) {
                operand = "AND";
            } else if (condition instanceof NotCondition) {
                operand = "NOT";
            } else if (condition instanceof OrCondition) {
                operand = "OR";
            } else if (condition instanceof AudienceIdCondition) {
                String audienceName = this.getNameFromAudienceId(((AudienceIdCondition<?>) condition).getAudienceId(),
                    audiencesMap);
                operand = audienceName;
            }
        }
        return operand;
    }

    public String serialize(Condition condition, Map<String, String> audiencesMap) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("");
        List<Condition> conditions;

        String operand = this.getOperandOrAudienceId(condition, audiencesMap);
        switch (operand){
            case ("AND"):
                conditions = ((AndCondition<?>) condition).getConditions();
                stringBuilder.append(this.getNameOrNextCondition(operand, conditions, audiencesMap));
                break;
            case ("OR"):
                conditions = ((OrCondition<?>) condition).getConditions();
                stringBuilder.append(this.getNameOrNextCondition(operand, conditions, audiencesMap));
                break;
            case ("NOT"):
                stringBuilder.append(operand + " ");
                Condition notCondition = ((NotCondition<?>) condition).getCondition();
                if (notCondition instanceof AudienceIdCondition) {
                    stringBuilder.append(serialize(notCondition, audiencesMap));
                } else {
                    stringBuilder.append("(" + serialize(notCondition, audiencesMap)+ ")");
                }
                break;
            default:
                stringBuilder.append(operand);
                break;
        }

        return stringBuilder.toString();
    }

    public String getNameOrNextCondition(String operand, List<Condition> conditions, Map<String, String> audiencesMap) {
        StringBuilder stringBuilder = new StringBuilder();
        int ctr = 0;
        if (conditions.isEmpty()) {
            return "";
        } else if(conditions.size() == 1) {
            return serialize(conditions.get(0), audiencesMap);
        } else {
            for (Condition con : conditions) {
                ctr++;
                if (ctr + 1 <= conditions.size()) {
                    if (con instanceof AudienceIdCondition) {
                        String audienceName = this.getNameFromAudienceId(((AudienceIdCondition<?>) con).getAudienceId(),
                            audiencesMap);
                        stringBuilder.append( audienceName + " ");
                    } else {
                        stringBuilder.append("(" + serialize(con, audiencesMap) + ") ");
                    }
                    stringBuilder.append(operand);
                    stringBuilder.append(" ");
                } else {
                    if (con instanceof AudienceIdCondition) {
                        String audienceName = this.getNameFromAudienceId(((AudienceIdCondition<?>) con).getAudienceId(),
                            audiencesMap);
                        stringBuilder.append(audienceName);
                    } else {
                        stringBuilder.append("(" + serialize(con, audiencesMap) + ")");
                    }
                }
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        return "Experiment{" +
            "id='" + id + '\'' +
            ", key='" + key + '\'' +
            ", groupId='" + groupId + '\'' +
            ", status='" + status + '\'' +
            ", audienceIds=" + audienceIds +
            ", audienceConditions=" + audienceConditions +
            ", variations=" + variations +
            ", variationKeyToVariationMap=" + variationKeyToVariationMap +
            ", userIdToVariationKeyMap=" + userIdToVariationKeyMap +
            ", trafficAllocation=" + trafficAllocation +
            '}';
    }
}
