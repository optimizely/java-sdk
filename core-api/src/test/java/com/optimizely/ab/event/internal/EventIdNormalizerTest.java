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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link EventIdNormalizer}.
 *
 * <p>Covers FSSDK-12813 normalization rules:
 * <ul>
 *   <li>A "numeric string" is non-empty and consists entirely of decimal digits {@code [0-9]}.</li>
 *   <li>Leading zeros are allowed.</li>
 *   <li>Whitespace, negatives, decimals, and exponents are INVALID.</li>
 *   <li>{@code campaign_id} (and impression {@code entity_id}) → falls back to {@code experiment_id}
 *       when invalid.</li>
 *   <li>{@code variation_id} → falls back to {@code null} when invalid.</li>
 * </ul>
 */
public class EventIdNormalizerTest {

    // ---------------------------------------------------------------------
    // isNumericString
    // ---------------------------------------------------------------------

    @Test
    public void isNumericString_validDecimalDigits_returnsTrue() {
        assertTrue(EventIdNormalizer.isNumericString("0"));
        assertTrue(EventIdNormalizer.isNumericString("1"));
        assertTrue(EventIdNormalizer.isNumericString("12345"));
        assertTrue(EventIdNormalizer.isNumericString("9999999999999"));
    }

    @Test
    public void isNumericString_leadingZerosAllowed_returnsTrue() {
        assertTrue(EventIdNormalizer.isNumericString("0123"));
        assertTrue(EventIdNormalizer.isNumericString("00"));
        assertTrue(EventIdNormalizer.isNumericString("000000001"));
    }

    @Test
    public void isNumericString_null_returnsFalse() {
        assertFalse(EventIdNormalizer.isNumericString(null));
    }

    @Test
    public void isNumericString_empty_returnsFalse() {
        assertFalse(EventIdNormalizer.isNumericString(""));
    }

    @Test
    public void isNumericString_whitespace_returnsFalse() {
        assertFalse(EventIdNormalizer.isNumericString(" "));
        assertFalse(EventIdNormalizer.isNumericString("  "));
        assertFalse(EventIdNormalizer.isNumericString("\t"));
        assertFalse(EventIdNormalizer.isNumericString("\n"));
        // surrounding whitespace is also invalid
        assertFalse(EventIdNormalizer.isNumericString(" 123"));
        assertFalse(EventIdNormalizer.isNumericString("123 "));
        assertFalse(EventIdNormalizer.isNumericString(" 123 "));
    }

    @Test
    public void isNumericString_nonDigits_returnsFalse() {
        assertFalse(EventIdNormalizer.isNumericString("abc"));
        assertFalse(EventIdNormalizer.isNumericString("variation_a"));
        assertFalse(EventIdNormalizer.isNumericString("exp_42"));
        assertFalse(EventIdNormalizer.isNumericString("layerId"));
        assertFalse(EventIdNormalizer.isNumericString("12a"));
        assertFalse(EventIdNormalizer.isNumericString("a12"));
    }

    @Test
    public void isNumericString_negativesAreInvalid_returnsFalse() {
        assertFalse(EventIdNormalizer.isNumericString("-1"));
        assertFalse(EventIdNormalizer.isNumericString("-123"));
    }

    @Test
    public void isNumericString_decimalsAreInvalid_returnsFalse() {
        assertFalse(EventIdNormalizer.isNumericString("1.0"));
        assertFalse(EventIdNormalizer.isNumericString("123.456"));
        assertFalse(EventIdNormalizer.isNumericString("."));
    }

    @Test
    public void isNumericString_exponentsAreInvalid_returnsFalse() {
        assertFalse(EventIdNormalizer.isNumericString("1e5"));
        assertFalse(EventIdNormalizer.isNumericString("1E5"));
        assertFalse(EventIdNormalizer.isNumericString("1.0e3"));
    }

    @Test
    public void isNumericString_unicodeDigitsAreInvalid_returnsFalse() {
        // Unicode digit U+0660 (Arabic-Indic 0) — not ASCII [0-9], must be rejected.
        assertFalse(EventIdNormalizer.isNumericString("٠١٢"));
        // Fullwidth digits U+FF10..U+FF19 — must be rejected.
        assertFalse(EventIdNormalizer.isNumericString("１２３"));
    }

    // ---------------------------------------------------------------------
    // normalizeCampaignId
    // ---------------------------------------------------------------------

    @Test
    public void normalizeCampaignId_validNumeric_returnsCampaignId() {
        assertEquals("12345", EventIdNormalizer.normalizeCampaignId("12345", "67890"));
        assertEquals("0", EventIdNormalizer.normalizeCampaignId("0", "67890"));
        assertEquals("0123", EventIdNormalizer.normalizeCampaignId("0123", "67890"));
    }

    @Test
    public void normalizeCampaignId_null_fallsBackToExperimentId() {
        assertEquals("67890", EventIdNormalizer.normalizeCampaignId(null, "67890"));
    }

    @Test
    public void normalizeCampaignId_empty_fallsBackToExperimentId() {
        assertEquals("67890", EventIdNormalizer.normalizeCampaignId("", "67890"));
    }

    @Test
    public void normalizeCampaignId_whitespace_fallsBackToExperimentId() {
        assertEquals("67890", EventIdNormalizer.normalizeCampaignId(" ", "67890"));
        assertEquals("67890", EventIdNormalizer.normalizeCampaignId("  ", "67890"));
        assertEquals("67890", EventIdNormalizer.normalizeCampaignId("\t", "67890"));
    }

    @Test
    public void normalizeCampaignId_nonNumeric_fallsBackToExperimentId() {
        assertEquals("67890", EventIdNormalizer.normalizeCampaignId("abc", "67890"));
        assertEquals("67890", EventIdNormalizer.normalizeCampaignId("layerId", "67890"));
        assertEquals("67890", EventIdNormalizer.normalizeCampaignId("exp_42", "67890"));
        assertEquals("67890", EventIdNormalizer.normalizeCampaignId("12a", "67890"));
        assertEquals("67890", EventIdNormalizer.normalizeCampaignId("-1", "67890"));
        assertEquals("67890", EventIdNormalizer.normalizeCampaignId("1.0", "67890"));
    }

    @Test
    public void normalizeCampaignId_invalidCampaignAndNullExperiment_returnsNull() {
        // Spec is silent on this combination; library returns experiment_id as-is, which may be null.
        // This is intentional: no logging, no failure, just pass-through.
        assertNull(EventIdNormalizer.normalizeCampaignId(null, null));
        assertNull(EventIdNormalizer.normalizeCampaignId("", null));
        assertNull(EventIdNormalizer.normalizeCampaignId("abc", null));
    }

    @Test
    public void normalizeCampaignId_invalidCampaignAndNonNumericExperiment_returnsExperimentAsIs() {
        // Fallback is verbatim — not re-validated. This matches the spec.
        assertEquals("expKey", EventIdNormalizer.normalizeCampaignId(null, "expKey"));
        assertEquals("", EventIdNormalizer.normalizeCampaignId("abc", ""));
    }

    // ---------------------------------------------------------------------
    // normalizeVariationId
    // ---------------------------------------------------------------------

    @Test
    public void normalizeVariationId_validNumeric_returnsVariationId() {
        assertEquals("12345", EventIdNormalizer.normalizeVariationId("12345"));
        assertEquals("0", EventIdNormalizer.normalizeVariationId("0"));
        assertEquals("0123", EventIdNormalizer.normalizeVariationId("0123"));
    }

    @Test
    public void normalizeVariationId_null_returnsNull() {
        assertNull(EventIdNormalizer.normalizeVariationId(null));
    }

    @Test
    public void normalizeVariationId_empty_returnsNull() {
        assertNull(EventIdNormalizer.normalizeVariationId(""));
    }

    @Test
    public void normalizeVariationId_whitespace_returnsNull() {
        assertNull(EventIdNormalizer.normalizeVariationId(" "));
        assertNull(EventIdNormalizer.normalizeVariationId("  "));
        assertNull(EventIdNormalizer.normalizeVariationId("\t"));
    }

    @Test
    public void normalizeVariationId_nonNumeric_returnsNull() {
        assertNull(EventIdNormalizer.normalizeVariationId("abc"));
        assertNull(EventIdNormalizer.normalizeVariationId("variation_a"));
        assertNull(EventIdNormalizer.normalizeVariationId("variationId"));
        assertNull(EventIdNormalizer.normalizeVariationId("12a"));
        assertNull(EventIdNormalizer.normalizeVariationId("-1"));
        assertNull(EventIdNormalizer.normalizeVariationId("1.0"));
    }
}
