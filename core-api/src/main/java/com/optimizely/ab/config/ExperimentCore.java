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

import com.optimizely.ab.config.audience.AndCondition;
import com.optimizely.ab.config.audience.AudienceIdCondition;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.EmptyCondition;
import com.optimizely.ab.config.audience.NotCondition;
import com.optimizely.ab.config.audience.OrCondition;

import java.util.List;
import java.util.Map;

public interface ExperimentCore extends IdKeyMapped {
    String AND = "AND";
    String OR = "OR";
    String NOT = "NOT";
    
    String getLayerId();
    String getGroupId();
    List<String> getAudienceIds();
    Condition<AudienceIdCondition> getAudienceConditions();
    List<Variation> getVariations();
    List<TrafficAllocation> getTrafficAllocation();
    Map<String, Variation> getVariationKeyToVariationMap();
    Map<String, Variation> getVariationIdToVariationMap();
    
    default String serializeConditions(Map<String, String> audiencesMap) {
        Condition condition = this.getAudienceConditions();
        return condition instanceof EmptyCondition ? "" : this.serialize(condition, audiencesMap);
    }
    
    default String getNameFromAudienceId(String audienceId, Map<String, String> audiencesMap) {
        StringBuilder audienceName = new StringBuilder();
        if (audiencesMap != null && audiencesMap.get(audienceId) != null) {
            audienceName.append("\"" + audiencesMap.get(audienceId) + "\"");
        } else {
            audienceName.append("\"" + audienceId + "\"");
        }
        return audienceName.toString();
    }
    
    default String getOperandOrAudienceId(Condition condition, Map<String, String> audiencesMap) {
        if (condition != null) {
            if (condition instanceof AudienceIdCondition) {
                return this.getNameFromAudienceId(condition.getOperandOrId(), audiencesMap);
            } else {
                return condition.getOperandOrId();
            }
        } else {
            return "";
        }
    }
    
    default String serialize(Condition condition, Map<String, String> audiencesMap) {
        StringBuilder stringBuilder = new StringBuilder();
        List<Condition> conditions;

        String operand = this.getOperandOrAudienceId(condition, audiencesMap);
        switch (operand){
            case (AND):
                conditions = ((AndCondition<?>) condition).getConditions();
                stringBuilder.append(this.getNameOrNextCondition(operand, conditions, audiencesMap));
                break;
            case (OR):
                conditions = ((OrCondition<?>) condition).getConditions();
                stringBuilder.append(this.getNameOrNextCondition(operand, conditions, audiencesMap));
                break;
            case (NOT):
                stringBuilder.append(operand + " ");
                Condition notCondition = ((NotCondition<?>) condition).getCondition();
                if (notCondition instanceof AudienceIdCondition) {
                    stringBuilder.append(serialize(notCondition, audiencesMap));
                } else {
                    stringBuilder.append("(" + serialize(notCondition, audiencesMap) + ")");
                }
                break;
            default:
                stringBuilder.append(operand);
                break;
        }

        return stringBuilder.toString();
    }
    
    default String getNameOrNextCondition(String operand, List<Condition> conditions, Map<String, String> audiencesMap) {
        StringBuilder stringBuilder = new StringBuilder();
        int index = 0;
        if (conditions.isEmpty()) {
            return "";
        } else if (conditions.size() == 1) {
            return serialize(conditions.get(0), audiencesMap);
        } else {
            for (Condition con : conditions) {
                index++;
                if (index + 1 <= conditions.size()) {
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
}