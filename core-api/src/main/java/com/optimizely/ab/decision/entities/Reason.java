/****************************************************************************
 * Copyright 2019, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/
package com.optimizely.ab.decision.entities;

public enum Reason {
    // BucketedVariationNotFound - the bucketed variation ID is not in the config
    BucketedVariationNotFound("Bucketed variation not found"),
    // BucketedIntoVariation - the user is bucketed into a variation for the given experiment
    BucketedIntoVariation("Bucketed into variation"),
    // FailedRolloutTargeting - the user does not meet the rollout targeting rules
    FailedRolloutTargeting("Does not meet rollout targeting rule"),
    // FailedAudienceTargeting - the user failed the audience targeting conditions
    FailedAudienceTargeting("Does not meet audience targeting conditions"),
    // NoRolloutForFeature - there is no rollout for the given feature
    NoRolloutForFeature("No rollout for feature"),
    // RolloutHasNoExperiments - the rollout has no assigned experiments
    RolloutHasNoExperiments("Rollout has no experiments"),
    // NotBucketedIntoVariation - the user is not bucketed into a variation for the given experiment
    NotBucketedIntoVariation("Not bucketed into a variation"),
    // NotInGroup - the user is not bucketed into the mutex group
    NotInGroup("Not bucketed into any experiment in mutex group");

    private String reason;

    Reason(String reason) {
        this.reason = reason;
    }

    public String toString() {
        return reason;
    }
}
