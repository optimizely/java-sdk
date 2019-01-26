/**
 *
 *    Copyright 2016-2019, Optimizely and contributors
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

import ch.qos.logback.classic.Level;
import com.optimizely.ab.internal.LogbackVerifier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the evaluation of different audience condition types (And, Or, Not, and UserAttribute)
 */
@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT",
    justification = "mockito verify calls do have a side-effect")
public class AudienceConditionEvaluationTest {

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    Map<String, String> testUserAttributes;
    Map<String, Object> testTypedUserAttributes;

    @Before
    public void initialize() {
        testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "chrome");
        testUserAttributes.put("device_type", "Android");

        testTypedUserAttributes = new HashMap<String, Object>();
        testTypedUserAttributes.put("is_firefox", true);
        testTypedUserAttributes.put("num_counts", 3.55);
        testTypedUserAttributes.put("num_size", 3);
        testTypedUserAttributes.put("meta_data", testUserAttributes);
        testTypedUserAttributes.put("null_val", null);
    }

    /**
     * Verify that UserAttribute.evaluate returns true on exact-matching visitor attribute data.
     */
    @Test
    public void userAttributeEvaluateTrue() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "custom_attribute", null, "chrome");
        assertTrue(testInstance.hashCode() != 0);
        assertNull(testInstance.getMatch());
        assertEquals(testInstance.getName(), "browser_type");
        assertEquals(testInstance.getType(), "custom_attribute");
        assertTrue(testInstance.evaluate(null, testUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate returns false on non-exact-matching visitor attribute data.
     */
    @Test
    public void userAttributeEvaluateFalse() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "custom_attribute", null, "firefox");
        assertFalse(testInstance.evaluate(null, testUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate returns false on unknown visitor attributes.
     */
    @Test
    public void userAttributeUnknownAttribute() throws Exception {
        UserAttribute testInstance = new UserAttribute("unknown_dim", "custom_attribute", null, "unknown");
        assertFalse(testInstance.evaluate(null, testUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate returns null on invalid match type.
     */
    @Test
    public void invalidAttributeCondition() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "unknown_dimension", null, "chrome");
        assertNull(testInstance.evaluate(null, testUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate returns null on invalid match type.
     */
    @Test
    public void invalidMatch() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "custom_attribute", "blah", "chrome");
        assertNull(testInstance.evaluate(null, testUserAttributes));
        logbackVerifier.expectMessage(Level.WARN,
            "Audience condition \"{name='browser_type', type='custom_attribute', match='blah', value='chrome'}\" is invalid: unknown match type. You may need to upgrade to a newer release of the Optimizely SDK");
    }

    /**
     * Verify that UserAttribute.evaluate returns null on invalid attribute type.
     */
    @Test
    public void unexpectedAttributeType() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "custom_attribute", "gt", 20);
        assertNull(testInstance.evaluate(null, testUserAttributes));
        logbackVerifier.expectMessage(Level.WARN,
            "Unable to evaluate condition for \"browser_type\" attribute: cannot convert from \"java.lang.String\" to \"java.lang.Number\"");
    }

    /**
     * Verify that UserAttribute.evaluate returns null on invalid attribute type.
     */
    @Test
    public void unexpectedAttributeTypeNull() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "custom_attribute", "gt", 20);
        assertNull(testInstance.evaluate(null, Collections.singletonMap("browser_type", null)));
        logbackVerifier.expectMessage(Level.DEBUG, "Skipping match condition on unset attribute: browser_type");
    }


    /**
     * Verify that UserAttribute.evaluate returns null on missing attribute value.
     */
    @Test
    public void missingAttribute() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "custom_attribute", "gt", 20);
        assertNull(testInstance.evaluate(null, Collections.EMPTY_MAP));
        logbackVerifier.expectMessage(Level.DEBUG, "Skipping match condition on unset attribute: browser_type");
    }

    /**
     * Verify that UserAttribute.evaluate returns null on passing null attribute object.
     */
    @Test
    public void nullAttribute() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "custom_attribute", "gt", 20);
        assertNull(testInstance.evaluate(null, null));
        logbackVerifier.expectMessage(Level.DEBUG, "Skipping match condition on unset attribute: browser_type");
    }

    /**
     * Verify that UserAttribute.evaluate returns null on unknown condition type.
     */
    @Test
    public void unknownConditionType() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "blah", "exists", "firefox");
        assertNull(testInstance.evaluate(null, testUserAttributes));
        logbackVerifier.expectMessage(Level.WARN,
            "Audience condition \"{name='browser_type', type='blah', match='exists', value='firefox'}\" is invalid: unknown type. You may need to upgrade to a newer release of the Optimizely SDK");
    }

    /**
     * Verify that UserAttribute.evaluate for EXIST match type returns true for known visitor
     * attributes with non-null instances and empty string.
     */
    @Test
    public void existsAttributeConditionEmptyStringEvaluatesTrue() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "custom_attribute", "exists", "firefox");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("browser_type", "");
        assertTrue(testInstance.evaluate(null, attributes));
        attributes.put("browser_type", null);
        assertFalse(testInstance.evaluate(null, attributes));
    }

    /**
     * Verify that UserAttribute.evaluate for EXIST match type returns true for known visitor
     * attributes with non-null instances
     */
    @Test
    public void existsAttributeConditionEvaluatesTrue() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "custom_attribute", "exists", "firefox");
        assertTrue(testInstance.evaluate(null, testUserAttributes));

        UserAttribute testInstanceBoolean = new UserAttribute("is_firefox", "custom_attribute", "exists", false);
        UserAttribute testInstanceInteger = new UserAttribute("num_size", "custom_attribute", "exists", 5);
        UserAttribute testInstanceDouble = new UserAttribute("num_counts", "custom_attribute", "exists", 4.55);
        UserAttribute testInstanceObject = new UserAttribute("meta_data", "custom_attribute", "exists", testUserAttributes);
        assertTrue(testInstanceBoolean.evaluate(null, testTypedUserAttributes));
        assertTrue(testInstanceInteger.evaluate(null, testTypedUserAttributes));
        assertTrue(testInstanceDouble.evaluate(null, testTypedUserAttributes));
        assertTrue(testInstanceObject.evaluate(null, testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for EXIST match type returns false for unknown visitor
     * attributes OR null visitor attributes.
     */
    @Test
    public void existsAttributeConditionEvaluatesFalse() throws Exception {
        UserAttribute testInstance = new UserAttribute("bad_var", "custom_attribute", "exists", "chrome");
        UserAttribute testInstanceNull = new UserAttribute("null_val", "custom_attribute", "exists", "chrome");
        assertFalse(testInstance.evaluate(null, testTypedUserAttributes));
        assertFalse(testInstanceNull.evaluate(null, testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for EXACT match type returns true for known visitor
     * attributes where the values and the value's type are the same
     */
    @Test
    public void exactAttributeConditionEvaluatesTrue() {
        UserAttribute testInstanceString = new UserAttribute("browser_type", "custom_attribute", "exact", "chrome");
        UserAttribute testInstanceBoolean = new UserAttribute("is_firefox", "custom_attribute", "exact", true);
        UserAttribute testInstanceInteger = new UserAttribute("num_size", "custom_attribute", "exact", 3);
        UserAttribute testInstanceFloat = new UserAttribute("num_size", "custom_attribute", "exact", (float) 3);
        UserAttribute testInstanceDouble = new UserAttribute("num_counts", "custom_attribute", "exact", 3.55);

        assertTrue(testInstanceString.evaluate(null, testUserAttributes));
        assertTrue(testInstanceBoolean.evaluate(null, testTypedUserAttributes));
        assertTrue(testInstanceInteger.evaluate(null, testTypedUserAttributes));
        assertTrue(testInstanceFloat.evaluate(null, Collections.singletonMap("num_size", (float) 3)));
        assertTrue(testInstanceDouble.evaluate(null, testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for EXACT match type returns null if the UserAttribute's
     * value type is not a valid number.
     */
    @Test
    public void exactAttributeConditionEvaluatesNullWithInvalidUserAttr() {
        BigInteger bigInteger = new BigInteger("33221312312312312");
        Double infinitePositiveInfiniteDouble = Double.POSITIVE_INFINITY;
        Double infiniteNegativeInfiniteDouble = Double.NEGATIVE_INFINITY;
        Double infiniteNANDouble = Double.NaN;
        Double largeDouble = Math.pow(2,53) + 2;
        float invalidFloatValue = (float) (Math.pow(2, 53) + 2000000000);
        UserAttribute testInstanceInteger = new UserAttribute(
            "num_size",
            "custom_attribute",
            "exact",
            5);
        UserAttribute testInstanceFloat = new UserAttribute(
            "num_size",
            "custom_attribute",
            "exact",
            (float) 5);
        UserAttribute testInstanceDouble = new UserAttribute(
            "num_counts",
            "custom_attribute",
            "exact",
            5.2);

        assertNull(testInstanceInteger.evaluate(
            null,
            Collections.singletonMap("num_size", bigInteger)));
        assertNull(testInstanceFloat.evaluate(
            null,
            Collections.singletonMap("num_size", invalidFloatValue)));
        assertNull(testInstanceDouble.evaluate(
            null,
            Collections.singletonMap("num_counts", infinitePositiveInfiniteDouble)));
        assertNull(testInstanceDouble.evaluate(
            null,
            Collections.singletonMap("num_counts", infiniteNegativeInfiniteDouble)));
        assertNull(testInstanceDouble.evaluate(
            null, Collections.singletonMap("num_counts",
                Collections.singletonMap("num_counts", infiniteNANDouble))));
        assertNull(testInstanceDouble.evaluate(
            null, Collections.singletonMap("num_counts",
                Collections.singletonMap("num_counts", largeDouble))));
    }

    /**
     * Verify that UserAttribute.evaluate for EXACT match type returns null if the UserAttribute's condition
     * value type is invalid number.
     */
    @Test
    public void invalidExactAttributeConditionEvaluatesNull() {
        BigInteger bigInteger = new BigInteger("33221312312312312");
        Double infinitePositiveInfiniteDouble = Double.POSITIVE_INFINITY;
        Double infiniteNegativeInfiniteDouble = Double.NEGATIVE_INFINITY;
        Double infiniteNANDouble = Double.NaN;
        UserAttribute testInstanceInteger = new UserAttribute("num_size", "custom_attribute", "exact", bigInteger);
        UserAttribute testInstancePositiveInfinite = new UserAttribute("num_counts", "custom_attribute", "exact", infinitePositiveInfiniteDouble);
        UserAttribute testInstanceNegativeInfiniteDouble = new UserAttribute("num_counts", "custom_attribute", "exact", infiniteNegativeInfiniteDouble);
        UserAttribute testInstanceNANDouble = new UserAttribute("num_counts", "custom_attribute", "exact", infiniteNANDouble);

        assertNull(testInstanceInteger.evaluate(null, testTypedUserAttributes));
        assertNull(testInstancePositiveInfinite.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceNegativeInfiniteDouble.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceNANDouble.evaluate(null, testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for EXACT match type returns false for known visitor
     * attributes where the value's type are the same, but the values are different
     */
    @Test
    public void exactAttributeConditionEvaluatesFalse() {
        UserAttribute testInstanceString = new UserAttribute("browser_type", "custom_attribute", "exact", "firefox");
        UserAttribute testInstanceBoolean = new UserAttribute("is_firefox", "custom_attribute", "exact", false);
        UserAttribute testInstanceInteger = new UserAttribute("num_size", "custom_attribute", "exact", 5);
        UserAttribute testInstanceDouble = new UserAttribute("num_counts", "custom_attribute", "exact", 5.55);

        assertFalse(testInstanceString.evaluate(null, testUserAttributes));
        assertFalse(testInstanceBoolean.evaluate(null, testTypedUserAttributes));
        assertFalse(testInstanceInteger.evaluate(null, testTypedUserAttributes));
        assertFalse(testInstanceDouble.evaluate(null, testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for EXACT match type returns null for known visitor
     * attributes where the value's type are different OR for values with null and object type.
     */
    @Test
    public void exactAttributeConditionEvaluatesNull() {
        UserAttribute testInstanceObject = new UserAttribute("meta_data", "custom_attribute", "exact", testUserAttributes);
        UserAttribute testInstanceString = new UserAttribute("browser_type", "custom_attribute", "exact", true);
        UserAttribute testInstanceBoolean = new UserAttribute("is_firefox", "custom_attribute", "exact", "true");
        UserAttribute testInstanceInteger = new UserAttribute("num_size", "custom_attribute", "exact", "3");
        UserAttribute testInstanceDouble = new UserAttribute("num_counts", "custom_attribute", "exact", "3.55");
        UserAttribute testInstanceNull = new UserAttribute("null_val", "custom_attribute", "exact", "null_val");

        assertNull(testInstanceObject.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceString.evaluate(null, testUserAttributes));
        assertNull(testInstanceBoolean.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceInteger.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceDouble.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceNull.evaluate(null, testTypedUserAttributes));
        Map<String, Object> attr = new HashMap<>();
        attr.put("browser_type", "true");
        assertNull(testInstanceString.evaluate(null, attr));
    }

    /**
     * Verify that UserAttribute.evaluate for GT match type returns true for known visitor
     * attributes where the value's type is a number, and the UserAttribute's value is greater than
     * the condition's value.
     */
    @Test
    public void gtAttributeConditionEvaluatesTrue() {
        UserAttribute testInstanceInteger = new UserAttribute("num_size", "custom_attribute", "gt", 2);
        UserAttribute testInstanceFloat = new UserAttribute("num_size", "custom_attribute", "gt", (float) 2);
        UserAttribute testInstanceDouble = new UserAttribute("num_counts", "custom_attribute", "gt", 2.55);

        assertTrue(testInstanceInteger.evaluate(null, testTypedUserAttributes));
        assertTrue(testInstanceFloat.evaluate(null, Collections.singletonMap("num_size", (float) 3)));
        assertTrue(testInstanceDouble.evaluate(null, testTypedUserAttributes));

        Map<String, Object> badAttributes = new HashMap<>();
        badAttributes.put("num_size", "bobs burgers");
        assertNull(testInstanceInteger.evaluate(null, badAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for GT match type returns null if the UserAttribute's
     * value type is invalid number.
     */
    @Test
    public void gtAttributeConditionEvaluatesNullWithInvalidUserAttr() {
        BigInteger bigInteger = new BigInteger("33221312312312312");
        Double infinitePositiveInfiniteDouble = Double.POSITIVE_INFINITY;
        Double infiniteNegativeInfiniteDouble = Double.NEGATIVE_INFINITY;
        Double infiniteNANDouble = Double.NaN;
        Double largeDouble = Math.pow(2, 53) + 2;
        float invalidFloatValue = (float) (Math.pow(2, 53) + 2000000000);

        UserAttribute testInstanceInteger = new UserAttribute(
            "num_size",
            "custom_attribute",
            "gt",
            5);
        UserAttribute testInstanceFloat = new UserAttribute(
            "num_size",
            "custom_attribute",
            "gt",
            (float) 5);
        UserAttribute testInstanceDouble = new UserAttribute(
            "num_counts",
            "custom_attribute",
            "gt",
            5.2);

        assertNull(testInstanceInteger.evaluate(
            null,
            Collections.singletonMap("num_size", bigInteger)));
        assertNull(testInstanceFloat.evaluate(
            null,
            Collections.singletonMap("num_size", invalidFloatValue)));
        assertNull(testInstanceDouble.evaluate(
            null,
            Collections.singletonMap("num_counts", infinitePositiveInfiniteDouble)));
        assertNull(testInstanceDouble.evaluate(
            null,
            Collections.singletonMap("num_counts", infiniteNegativeInfiniteDouble)));
        assertNull(testInstanceDouble.evaluate(
            null, Collections.singletonMap("num_counts",
                Collections.singletonMap("num_counts", infiniteNANDouble))));
        assertNull(testInstanceDouble.evaluate(
            null, Collections.singletonMap("num_counts",
                Collections.singletonMap("num_counts", largeDouble))));
    }

    /**
     * Verify that UserAttribute.evaluate for GT match type returns null if the UserAttribute's
     * value type is invalid number.
     */
    @Test
    public void gtAttributeConditionEvaluatesNullWithInvalidAttr() {
        BigInteger bigInteger = new BigInteger("33221312312312312");
        Double infinitePositiveInfiniteDouble = Double.POSITIVE_INFINITY;
        Double infiniteNegativeInfiniteDouble = Double.NEGATIVE_INFINITY;
        Double infiniteNANDouble = Double.NaN;
        UserAttribute testInstanceInteger = new UserAttribute("num_size", "custom_attribute", "gt", bigInteger);
        UserAttribute testInstancePositiveInfinite = new UserAttribute("num_counts", "custom_attribute", "gt", infinitePositiveInfiniteDouble);
        UserAttribute testInstanceNegativeInfiniteDouble = new UserAttribute("num_counts", "custom_attribute", "gt", infiniteNegativeInfiniteDouble);
        UserAttribute testInstanceNANDouble = new UserAttribute("num_counts", "custom_attribute", "gt", infiniteNANDouble);

        assertNull(testInstanceInteger.evaluate(null, testTypedUserAttributes));
        assertNull(testInstancePositiveInfinite.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceNegativeInfiniteDouble.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceNANDouble.evaluate(null, testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for GT match type returns false for known visitor
     * attributes where the value's type is a number, and the UserAttribute's value is not greater
     * than the condition's value.
     */
    @Test
    public void gtAttributeConditionEvaluatesFalse()  {
        UserAttribute testInstanceInteger = new UserAttribute("num_size", "custom_attribute", "gt", 5);
        UserAttribute testInstanceDouble = new UserAttribute("num_counts", "custom_attribute", "gt", 5.55);

        assertFalse(testInstanceInteger.evaluate(null, testTypedUserAttributes));
        assertFalse(testInstanceDouble.evaluate(null, testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for GT match type returns null if the UserAttribute's
     * value type is not a number.
     */
    @Test
    public void gtAttributeConditionEvaluatesNull()  {
        UserAttribute testInstanceString = new UserAttribute("browser_type", "custom_attribute", "gt", 3.5);
        UserAttribute testInstanceBoolean = new UserAttribute("is_firefox", "custom_attribute", "gt", 3.5);
        UserAttribute testInstanceObject = new UserAttribute("meta_data", "custom_attribute", "gt", 3.5);
        UserAttribute testInstanceNull = new UserAttribute("null_val", "custom_attribute", "gt", 3.5);

        assertNull(testInstanceString.evaluate(null, testUserAttributes));
        assertNull(testInstanceBoolean.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceObject.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceNull.evaluate(null, testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for GT match type returns true for known visitor
     * attributes where the value's type is a number, and the UserAttribute's value is less than
     * the condition's value.
     */
    @Test
    public void ltAttributeConditionEvaluatesTrue()  {
        UserAttribute testInstanceInteger = new UserAttribute("num_size", "custom_attribute", "lt", 5);
        UserAttribute testInstanceDouble = new UserAttribute("num_counts", "custom_attribute", "lt", 5.55);

        assertTrue(testInstanceInteger.evaluate(null, testTypedUserAttributes));
        assertTrue(testInstanceDouble.evaluate(null, testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for GT match type returns true for known visitor
     * attributes where the value's type is a number, and the UserAttribute's value is not less
     * than the condition's value.
     */
    @Test
    public void ltAttributeConditionEvaluatesFalse()  {
        UserAttribute testInstanceInteger = new UserAttribute("num_size", "custom_attribute", "lt", 2);
        UserAttribute testInstanceDouble = new UserAttribute("num_counts", "custom_attribute", "lt", 2.55);

        assertFalse(testInstanceInteger.evaluate(null, testTypedUserAttributes));
        assertFalse(testInstanceDouble.evaluate(null, testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for LT match type returns null if the UserAttribute's
     * value type is not a number.
     */
    @Test
    public void ltAttributeConditionEvaluatesNull()  {
        UserAttribute testInstanceString = new UserAttribute("browser_type", "custom_attribute", "lt", 3.5);
        UserAttribute testInstanceBoolean = new UserAttribute("is_firefox", "custom_attribute", "lt", 3.5);
        UserAttribute testInstanceObject = new UserAttribute("meta_data", "custom_attribute", "lt", 3.5);
        UserAttribute testInstanceNull = new UserAttribute("null_val", "custom_attribute", "lt", 3.5);

        assertNull(testInstanceString.evaluate(null, testUserAttributes));
        assertNull(testInstanceBoolean.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceObject.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceNull.evaluate(null, testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for LT match type returns null if the UserAttribute's
     * value type is not a valid number.
     */
    @Test
    public void ltAttributeConditionEvaluatesNullWithInvalidUserAttr() {
        BigInteger bigInteger = new BigInteger("33221312312312312");
        Double infinitePositiveInfiniteDouble = Double.POSITIVE_INFINITY;
        Double infiniteNegativeInfiniteDouble = Double.NEGATIVE_INFINITY;
        Double infiniteNANDouble = Double.NaN;
        Double largeDouble = Math.pow(2,53) + 2;
        float invalidFloatValue = (float) (Math.pow(2, 53) + 2000000000);

        UserAttribute testInstanceInteger = new UserAttribute(
            "num_size",
            "custom_attribute",
            "lt",
            5);
        UserAttribute testInstanceFloat = new UserAttribute(
            "num_size",
            "custom_attribute",
            "lt",
            (float) 5);
        UserAttribute testInstanceDouble = new UserAttribute(
            "num_counts",
            "custom_attribute",
            "lt",
            5.2);

        assertNull(testInstanceInteger.evaluate(
            null,
            Collections.singletonMap("num_size", bigInteger)));
        assertNull(testInstanceFloat.evaluate(
            null,
            Collections.singletonMap("num_size", invalidFloatValue)));
        assertNull(testInstanceDouble.evaluate(
            null,
            Collections.singletonMap("num_counts", infinitePositiveInfiniteDouble)));
        assertNull(testInstanceDouble.evaluate(
            null,
            Collections.singletonMap("num_counts", infiniteNegativeInfiniteDouble)));
        assertNull(testInstanceDouble.evaluate(
            null, Collections.singletonMap("num_counts",
                Collections.singletonMap("num_counts", infiniteNANDouble))));
        assertNull(testInstanceDouble.evaluate(
            null, Collections.singletonMap("num_counts",
                Collections.singletonMap("num_counts", largeDouble))));
    }

    /**
     * Verify that UserAttribute.evaluate for LT match type returns null if the condition
     * value type is not a valid number.
     */
    @Test
    public void ltAttributeConditionEvaluatesNullWithInvalidAttributes() {
        BigInteger bigInteger = new BigInteger("33221312312312312");
        Double infinitePositiveInfiniteDouble = Double.POSITIVE_INFINITY;
        Double infiniteNegativeInfiniteDouble = Double.NEGATIVE_INFINITY;
        Double infiniteNANDouble = Double.NaN;
        UserAttribute testInstanceInteger = new UserAttribute("num_size", "custom_attribute", "lt", bigInteger);
        UserAttribute testInstancePositiveInfinite = new UserAttribute("num_counts", "custom_attribute", "lt", infinitePositiveInfiniteDouble);
        UserAttribute testInstanceNegativeInfiniteDouble = new UserAttribute("num_counts", "custom_attribute", "lt", infiniteNegativeInfiniteDouble);
        UserAttribute testInstanceNANDouble = new UserAttribute("num_counts", "custom_attribute", "lt", infiniteNANDouble);

        assertNull(testInstanceInteger.evaluate(null, testTypedUserAttributes));
        assertNull(testInstancePositiveInfinite.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceNegativeInfiniteDouble.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceNANDouble.evaluate(null, testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for SUBSTRING match type returns true if the
     * UserAttribute's value is a substring of the condition's value.
     */
    @Test
    public void substringAttributeConditionEvaluatesTrue()  {
        UserAttribute testInstanceString = new UserAttribute("browser_type", "custom_attribute", "substring", "chrome");
        assertTrue(testInstanceString.evaluate(null, testUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for SUBSTRING match type returns true if the
     * UserAttribute's value is a substring of the condition's value.
     */
    @Test
    public void substringAttributeConditionPartialMatchEvaluatesTrue()  {
        UserAttribute testInstanceString = new UserAttribute("browser_type", "custom_attribute", "substring", "chro");
        assertTrue(testInstanceString.evaluate(null, testUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for SUBSTRING match type returns true if the
     * UserAttribute's value is NOT a substring of the condition's value.
     */
    @Test
    public void substringAttributeConditionEvaluatesFalse()  {
        UserAttribute testInstanceString = new UserAttribute("browser_type", "custom_attribute", "substring", "chr0me");
        assertFalse(testInstanceString.evaluate(null, testUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for SUBSTRING match type returns null if the
     * UserAttribute's value type is not a string.
     */
    @Test
    public void substringAttributeConditionEvaluatesNull()  {
        UserAttribute testInstanceBoolean = new UserAttribute("is_firefox", "custom_attribute", "substring", "chrome1");
        UserAttribute testInstanceInteger = new UserAttribute("num_size", "custom_attribute", "substring", "chrome1");
        UserAttribute testInstanceDouble = new UserAttribute("num_counts", "custom_attribute", "substring", "chrome1");
        UserAttribute testInstanceObject = new UserAttribute("meta_data", "custom_attribute", "substring", "chrome1");
        UserAttribute testInstanceNull = new UserAttribute("null_val", "custom_attribute", "substring", "chrome1");

        assertNull(testInstanceBoolean.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceInteger.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceDouble.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceObject.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceNull.evaluate(null, testTypedUserAttributes));
    }

    /**
     * Verify that NotCondition.evaluate returns null when its condition is null.
     */
    @Test
    public void notConditionEvaluateNull()  {
        NotCondition notCondition = new NotCondition(new NullCondition());
        assertNull(notCondition.evaluate(null, testUserAttributes));
    }

    /**
     * Verify that NotCondition.evaluate returns true when its condition operand evaluates to false.
     */
    @Test
    public void notConditionEvaluateTrue()  {
        UserAttribute userAttribute = mock(UserAttribute.class);
        when(userAttribute.evaluate(null, testUserAttributes)).thenReturn(false);

        NotCondition notCondition = new NotCondition(userAttribute);
        assertTrue(notCondition.evaluate(null, testUserAttributes));
        verify(userAttribute, times(1)).evaluate(null, testUserAttributes);
    }

    /**
     * Verify that NotCondition.evaluate returns false when its condition operand evaluates to true.
     */
    @Test
    public void notConditionEvaluateFalse()  {
        UserAttribute userAttribute = mock(UserAttribute.class);
        when(userAttribute.evaluate(null, testUserAttributes)).thenReturn(true);

        NotCondition notCondition = new NotCondition(userAttribute);
        assertFalse(notCondition.evaluate(null, testUserAttributes));
        verify(userAttribute, times(1)).evaluate(null, testUserAttributes);
    }

    /**
     * Verify that OrCondition.evaluate returns true when at least one of its operand conditions evaluate to true.
     */
    @Test
    public void orConditionEvaluateTrue()  {
        UserAttribute userAttribute1 = mock(UserAttribute.class);
        when(userAttribute1.evaluate(null, testUserAttributes)).thenReturn(true);

        UserAttribute userAttribute2 = mock(UserAttribute.class);
        when(userAttribute2.evaluate(null, testUserAttributes)).thenReturn(false);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(userAttribute1);
        conditions.add(userAttribute2);

        OrCondition orCondition = new OrCondition(conditions);
        assertTrue(orCondition.evaluate(null, testUserAttributes));
        verify(userAttribute1, times(1)).evaluate(null, testUserAttributes);
        // shouldn't be called due to short-circuiting in 'Or' evaluation
        verify(userAttribute2, times(0)).evaluate(null, testUserAttributes);
    }

    /**
     * Verify that OrCondition.evaluate returns true when at least one of its operand conditions evaluate to true.
     */
    @Test
    public void orConditionEvaluateTrueWithNullAndTrue()  {
        UserAttribute userAttribute1 = mock(UserAttribute.class);
        when(userAttribute1.evaluate(null, testUserAttributes)).thenReturn(null);

        UserAttribute userAttribute2 = mock(UserAttribute.class);
        when(userAttribute2.evaluate(null, testUserAttributes)).thenReturn(true);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(userAttribute1);
        conditions.add(userAttribute2);

        OrCondition orCondition = new OrCondition(conditions);
        assertTrue(orCondition.evaluate(null, testUserAttributes));
        verify(userAttribute1, times(1)).evaluate(null, testUserAttributes);
        // shouldn't be called due to short-circuiting in 'Or' evaluation
        verify(userAttribute2, times(1)).evaluate(null, testUserAttributes);
    }

    /**
     * Verify that OrCondition.evaluate returns true when at least one of its operand conditions evaluate to true.
     */
    @Test
    public void orConditionEvaluateNullWithNullAndFalse()  {
        UserAttribute userAttribute1 = mock(UserAttribute.class);
        when(userAttribute1.evaluate(null, testUserAttributes)).thenReturn(null);

        UserAttribute userAttribute2 = mock(UserAttribute.class);
        when(userAttribute2.evaluate(null, testUserAttributes)).thenReturn(false);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(userAttribute1);
        conditions.add(userAttribute2);

        OrCondition orCondition = new OrCondition(conditions);
        assertNull(orCondition.evaluate(null, testUserAttributes));
        verify(userAttribute1, times(1)).evaluate(null, testUserAttributes);
        // shouldn't be called due to short-circuiting in 'Or' evaluation
        verify(userAttribute2, times(1)).evaluate(null, testUserAttributes);
    }

    /**
     * Verify that OrCondition.evaluate returns true when at least one of its operand conditions evaluate to true.
     */
    @Test
    public void orConditionEvaluateFalseWithFalseAndFalse()  {
        UserAttribute userAttribute1 = mock(UserAttribute.class);
        when(userAttribute1.evaluate(null, testUserAttributes)).thenReturn(false);

        UserAttribute userAttribute2 = mock(UserAttribute.class);
        when(userAttribute2.evaluate(null, testUserAttributes)).thenReturn(false);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(userAttribute1);
        conditions.add(userAttribute2);

        OrCondition orCondition = new OrCondition(conditions);
        assertFalse(orCondition.evaluate(null, testUserAttributes));
        verify(userAttribute1, times(1)).evaluate(null, testUserAttributes);
        // shouldn't be called due to short-circuiting in 'Or' evaluation
        verify(userAttribute2, times(1)).evaluate(null, testUserAttributes);
    }

    /**
     * Verify that OrCondition.evaluate returns false when all of its operand conditions evaluate to false.
     */
    @Test
    public void orConditionEvaluateFalse()  {
        UserAttribute userAttribute1 = mock(UserAttribute.class);
        when(userAttribute1.evaluate(null, testUserAttributes)).thenReturn(false);

        UserAttribute userAttribute2 = mock(UserAttribute.class);
        when(userAttribute2.evaluate(null, testUserAttributes)).thenReturn(false);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(userAttribute1);
        conditions.add(userAttribute2);

        OrCondition orCondition = new OrCondition(conditions);
        assertFalse(orCondition.evaluate(null, testUserAttributes));
        verify(userAttribute1, times(1)).evaluate(null, testUserAttributes);
        verify(userAttribute2, times(1)).evaluate(null, testUserAttributes);
    }

    /**
     * Verify that AndCondition.evaluate returns true when all of its operand conditions evaluate to true.
     */
    @Test
    public void andConditionEvaluateTrue()  {
        OrCondition orCondition1 = mock(OrCondition.class);
        when(orCondition1.evaluate(null, testUserAttributes)).thenReturn(true);

        OrCondition orCondition2 = mock(OrCondition.class);
        when(orCondition2.evaluate(null, testUserAttributes)).thenReturn(true);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(orCondition1);
        conditions.add(orCondition2);

        AndCondition andCondition = new AndCondition(conditions);
        assertTrue(andCondition.evaluate(null, testUserAttributes));
        verify(orCondition1, times(1)).evaluate(null, testUserAttributes);
        verify(orCondition2, times(1)).evaluate(null, testUserAttributes);
    }

    /**
     * Verify that AndCondition.evaluate returns true when all of its operand conditions evaluate to true.
     */
    @Test
    public void andConditionEvaluateFalseWithNullAndFalse()  {
        OrCondition orCondition1 = mock(OrCondition.class);
        when(orCondition1.evaluate(null, testUserAttributes)).thenReturn(null);

        OrCondition orCondition2 = mock(OrCondition.class);
        when(orCondition2.evaluate(null, testUserAttributes)).thenReturn(false);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(orCondition1);
        conditions.add(orCondition2);

        AndCondition andCondition = new AndCondition(conditions);
        assertFalse(andCondition.evaluate(null, testUserAttributes));
        verify(orCondition1, times(1)).evaluate(null, testUserAttributes);
        verify(orCondition2, times(1)).evaluate(null, testUserAttributes);
    }

    /**
     * Verify that AndCondition.evaluate returns true when all of its operand conditions evaluate to true.
     */
    @Test
    public void andConditionEvaluateNullWithNullAndTrue()  {
        OrCondition orCondition1 = mock(OrCondition.class);
        when(orCondition1.evaluate(null, testUserAttributes)).thenReturn(null);

        OrCondition orCondition2 = mock(OrCondition.class);
        when(orCondition2.evaluate(null, testUserAttributes)).thenReturn(true);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(orCondition1);
        conditions.add(orCondition2);

        AndCondition andCondition = new AndCondition(conditions);
        assertNull(andCondition.evaluate(null, testUserAttributes));
        verify(orCondition1, times(1)).evaluate(null, testUserAttributes);
        verify(orCondition2, times(1)).evaluate(null, testUserAttributes);
    }

    /**
     * Verify that AndCondition.evaluate returns false when any one of its operand conditions evaluate to false.
     */
    @Test
    public void andConditionEvaluateFalse()  {
        OrCondition orCondition1 = mock(OrCondition.class);
        when(orCondition1.evaluate(null, testUserAttributes)).thenReturn(false);

        OrCondition orCondition2 = mock(OrCondition.class);
        when(orCondition2.evaluate(null, testUserAttributes)).thenReturn(true);

        // and[false, true]
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(orCondition1);
        conditions.add(orCondition2);

        AndCondition andCondition = new AndCondition(conditions);
        assertFalse(andCondition.evaluate(null, testUserAttributes));
        verify(orCondition1, times(1)).evaluate(null, testUserAttributes);
        // shouldn't be called due to short-circuiting in 'And' evaluation
        verify(orCondition2, times(0)).evaluate(null, testUserAttributes);

        OrCondition orCondition3 = mock(OrCondition.class);
        when(orCondition3.evaluate(null, testUserAttributes)).thenReturn(null);

        // and[null, false]
        List<Condition> conditions2 = new ArrayList<Condition>();
        conditions2.add(orCondition3);
        conditions2.add(orCondition1);

        AndCondition andCondition2 = new AndCondition(conditions2);
        assertFalse(andCondition2.evaluate(null, testUserAttributes));

        // and[true, false, null]
        List<Condition> conditions3 = new ArrayList<Condition>();
        conditions3.add(orCondition2);
        conditions3.add(orCondition3);
        conditions3.add(orCondition1);

        AndCondition andCondition3 = new AndCondition(conditions3);
        assertFalse(andCondition3.evaluate(null, testUserAttributes));
    }

    /**
     * Verify that AndCondition.evaluate returns null when any one of its operand conditions evaluate to false.
     */
    // @Test
    // public void andConditionEvaluateNull()  {

    // }

    /**
     * Verify that {@link Condition#evaluate(com.optimizely.ab.config.ProjectConfig, java.util.Map)}
     * called when its attribute value is null
     * returns True when the user's attribute value is also null
     * True when the attribute is not in the map
     * False when empty string is used.
     *
     * @
     */
    @Test
    public void nullValueEvaluate()  {
        String attributeName = "attribute_name";
        String attributeType = "attribute_type";
        String attributeValue = null;
        UserAttribute nullValueAttribute = new UserAttribute(
            attributeName,
            attributeType,
            "exact",
            attributeValue
        );

        assertNull(nullValueAttribute.evaluate(null, Collections.<String, String>emptyMap()));
        assertNull(nullValueAttribute.evaluate(null, Collections.singletonMap(attributeName, attributeValue)));
        assertNull(nullValueAttribute.evaluate(null, (Collections.singletonMap(attributeName, ""))));
    }
}
