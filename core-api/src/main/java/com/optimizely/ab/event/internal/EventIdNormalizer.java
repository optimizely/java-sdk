/**
 *
 *    Copyright 2025, Optimizely and contributors
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
package com.optimizely.ab.event.internal;

/**
 * EventIdNormalizer normalizes decision-event identifier fields prior to wire serialization.
 *
 * <p>Implements FSSDK-12813:
 * <ul>
 *   <li>{@code campaign_id} and impression {@code entity_id} must be a non-empty
 *       decimal-digit string. If empty / null / non-numeric / whitespace, substitute
 *       {@code experiment_id}.</li>
 *   <li>{@code variation_id} must be a non-empty decimal-digit string OR {@code null}.
 *       If empty / non-numeric / non-string / whitespace, substitute {@code null}.</li>
 * </ul>
 *
 * <p>A "numeric string" is a non-empty string consisting entirely of decimal digits
 * {@code [0-9]}. Leading zeros are allowed. Whitespace, negatives, decimals, and
 * exponents are INVALID.
 *
 * <p>Normalization applies uniformly to all decision types (experiment, feature test,
 * rollout, holdout). It must not drop, defer, or fail event dispatch, and it must not
 * emit any log or warning on the normalization path.
 */
final class EventIdNormalizer {

    private EventIdNormalizer() {
        // Utility class — not instantiable.
    }

    /**
     * @return {@code true} iff {@code value} is non-null and consists entirely of decimal digits.
     *         Empty strings, whitespace, negatives, decimals, and exponents are all invalid.
     */
    static boolean isNumericString(String value) {
        if (value == null) {
            return false;
        }
        int length = value.length();
        if (length == 0) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * Normalize a {@code campaign_id} or impression {@code entity_id}.
     *
     * @param campaignId the candidate campaign_id (may be null, empty, or non-numeric)
     * @param experimentId fallback experiment_id (returned as-is; not re-validated)
     * @return {@code campaignId} when it is a non-empty numeric string,
     *         otherwise {@code experimentId} (which may itself be {@code null}).
     */
    static String normalizeCampaignId(String campaignId, String experimentId) {
        if (isNumericString(campaignId)) {
            return campaignId;
        }
        return experimentId;
    }

    /**
     * Normalize a {@code variation_id}.
     *
     * @param variationId the candidate variation_id (may be null, empty, or non-numeric)
     * @return {@code variationId} when it is a non-empty numeric string, otherwise {@code null}.
     */
    static String normalizeVariationId(String variationId) {
        if (isNumericString(variationId)) {
            return variationId;
        }
        return null;
    }
}
