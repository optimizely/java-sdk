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
 * <p>Covers normalization rules:
 * <ul>
 *   <li>{@code campaign_id} (and impression {@code entity_id}) → falls back to
 *       {@code experiment_id} ONLY when {@code null} or empty string. Any other
 *       non-empty string passes through unchanged (IDs may be opaque).</li>
 *   <li>{@code variation_id} → falls back to {@code null} when null, empty, whitespace,
 *       or non-numeric. A "numeric string" is non-empty and consists entirely of
 *       decimal digits {@code [0-9]}; leading zeros allowed; whitespace, negatives,
 *       decimals, and exponents are INVALID.</li>
 * </ul>
 */
public class EventIdNormalizerTest {

    // ---------------------------------------------------------------------
    // isNonEmptyString — used for campaign_id / entity_id
    // ---------------------------------------------------------------------

    @Test
    public void isNonEmptyString_anyNonEmpty_returnsTrue() {
        assertTrue(EventIdNormalizer.isNonEmptyString("0"));
        assertTrue(EventIdNormalizer.isNonEmptyString("12345"));
        assertTrue(EventIdNormalizer.isNonEmptyString("default-12345"));
        assertTrue(EventIdNormalizer.isNonEmptyString("layer_abc"));
        // Whitespace-only strings are still non-empty by length, and any character
        // content is allowed under the relaxed campaign_id / entity_id contract.
        assertTrue(EventIdNormalizer.isNonEmptyString(" "));
        assertTrue(EventIdNormalizer.isNonEmptyString("\t"));
    }

    @Test
    public void isNonEmptyString_null_returnsFalse() {
        assertFalse(EventIdNormalizer.isNonEmptyString(null));
    }

    @Test
    public void isNonEmptyString_empty_returnsFalse() {
        assertFalse(EventIdNormalizer.isNonEmptyString(""));
    }

    // ---------------------------------------------------------------------
    // isNumericString — used for variation_id (strict)
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
    // normalizeCampaignId — relaxed: any non-empty string passes through
    // ---------------------------------------------------------------------

    @Test
    public void normalizeCampaignId_validNumeric_returnsCampaignId() {
        assertEquals("12345", EventIdNormalizer.normalizeCampaignId("12345", "67890"));
        assertEquals("0", EventIdNormalizer.normalizeCampaignId("0", "67890"));
        assertEquals("0123", EventIdNormalizer.normalizeCampaignId("0123", "67890"));
    }

    @Test
    public void normalizeCampaignId_opaqueString_passesThroughUnchanged() {
        // Any non-empty string is valid for campaign_id (IDs may be opaque),
        // so no fallback fires.
        assertEquals("default-12345", EventIdNormalizer.normalizeCampaignId("default-12345", "67890"));
        assertEquals("layer_abc", EventIdNormalizer.normalizeCampaignId("layer_abc", "67890"));
        assertEquals("abc", EventIdNormalizer.normalizeCampaignId("abc", "67890"));
        assertEquals("exp_42", EventIdNormalizer.normalizeCampaignId("exp_42", "67890"));
        assertEquals("12a", EventIdNormalizer.normalizeCampaignId("12a", "67890"));
        assertEquals("-1", EventIdNormalizer.normalizeCampaignId("-1", "67890"));
        assertEquals("1.0", EventIdNormalizer.normalizeCampaignId("1.0", "67890"));
    }

    @Test
    public void normalizeCampaignId_whitespace_passesThroughUnchanged() {
        // Whitespace-only strings have length >= 1, so under the relaxed contract
        // they are accepted as-is. The pipeline is responsible for further validation.
        assertEquals(" ", EventIdNormalizer.normalizeCampaignId(" ", "67890"));
        assertEquals("  ", EventIdNormalizer.normalizeCampaignId("  ", "67890"));
        assertEquals("\t", EventIdNormalizer.normalizeCampaignId("\t", "67890"));
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
    public void normalizeCampaignId_invalidCampaignAndNullExperiment_returnsNull() {
        // Spec is silent on this combination; library returns experiment_id as-is, which may be null.
        // This is intentional: no logging, no failure, just pass-through.
        assertNull(EventIdNormalizer.normalizeCampaignId(null, null));
        assertNull(EventIdNormalizer.normalizeCampaignId("", null));
    }

    @Test
    public void normalizeCampaignId_nullCampaignAndOpaqueExperiment_returnsExperimentAsIs() {
        // Fallback is verbatim — not re-validated. This matches the spec.
        assertEquals("expKey", EventIdNormalizer.normalizeCampaignId(null, "expKey"));
        assertEquals("default-99", EventIdNormalizer.normalizeCampaignId("", "default-99"));
    }

    // ---------------------------------------------------------------------
    // normalizeVariationId — strict numeric-string-only (UNCHANGED)
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
