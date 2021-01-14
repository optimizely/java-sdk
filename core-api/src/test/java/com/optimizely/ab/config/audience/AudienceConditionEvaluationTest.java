/**
 *
 *    Copyright 2016-2020, Optimizely and contributors
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
        testUserAttributes = new HashMap<>();
        testUserAttributes.put("browser_type", "chrome");
        testUserAttributes.put("device_type", "Android");

        testTypedUserAttributes = new HashMap<>();
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
    public void invalidMatchCondition() throws Exception {
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
            "Audience condition \"{name='browser_type', type='custom_attribute', match='blah', value='chrome'}\" uses an unknown match type. You may need to upgrade to a newer release of the Optimizely SDK");
    }

    /**
     * Verify that UserAttribute.evaluate returns null on invalid attribute type.
     */
    @Test
    public void unexpectedAttributeType() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "custom_attribute", "gt", 20);
        assertNull(testInstance.evaluate(null, testUserAttributes));
        logbackVerifier.expectMessage(Level.WARN,
            "Audience condition \"{name='browser_type', type='custom_attribute', match='gt', value=20}\" evaluated to UNKNOWN because a value of type \"java.lang.String\" was passed for user attribute \"browser_type\"");
    }

    /**
     * Verify that UserAttribute.evaluate returns null on invalid attribute type.
     */
    @Test
    public void unexpectedAttributeTypeNull() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "custom_attribute", "gt", 20);
        assertNull(testInstance.evaluate(null, Collections.singletonMap("browser_type", null)));
        logbackVerifier.expectMessage(Level.DEBUG,
            "Audience condition \"{name='browser_type', type='custom_attribute', match='gt', value=20}\" evaluated to UNKNOWN because a null value was passed for user attribute \"browser_type\"");
    }


    /**
     * Verify that UserAttribute.evaluate returns null on missing attribute value.
     */
    @Test
    public void missingAttribute() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "custom_attribute", "gt", 20);
        assertNull(testInstance.evaluate(null, Collections.EMPTY_MAP));
        logbackVerifier.expectMessage(Level.DEBUG,
            "Audience condition \"{name='browser_type', type='custom_attribute', match='gt', value=20}\" evaluated to UNKNOWN because no value was passed for user attribute \"browser_type\"");
    }

    /**
     * Verify that UserAttribute.evaluate returns null on passing null attribute object.
     */
    @Test
    public void nullAttribute() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "custom_attribute", "gt", 20);
        assertNull(testInstance.evaluate(null, null));
        logbackVerifier.expectMessage(Level.DEBUG,
            "Audience condition \"{name='browser_type', type='custom_attribute', match='gt', value=20}\" evaluated to UNKNOWN because no value was passed for user attribute \"browser_type\"");
    }

    /**
     * Verify that UserAttribute.evaluate returns null on unknown condition type.
     */
    @Test
    public void unknownConditionType() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "blah", "exists", "firefox");
        assertNull(testInstance.evaluate(null, testUserAttributes));
        logbackVerifier.expectMessage(Level.WARN,
            "Audience condition \"{name='browser_type', type='blah', match='exists', value='firefox'}\" uses an unknown condition type. You may need to upgrade to a newer release of the Optimizely SDK.");
    }

    /**
     * Verify that UserAttribute.evaluate for EXIST match type returns true for known visitor
     * attributes with non-null instances and empty string.
     */
    @Test
    public void existsMatchConditionEmptyStringEvaluatesTrue() throws Exception {
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
    public void existsMatchConditionEvaluatesTrue() throws Exception {
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
    public void existsMatchConditionEvaluatesFalse() throws Exception {
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
    public void exactMatchConditionEvaluatesTrue() {
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
    public void exactMatchConditionEvaluatesNullWithInvalidUserAttr() {
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
    public void invalidExactMatchConditionEvaluatesNull() {
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
    public void exactMatchConditionEvaluatesFalse() {
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
    public void exactMatchConditionEvaluatesNull() {
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
    public void gtMatchConditionEvaluatesTrue() {
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
    public void gtMatchConditionEvaluatesNullWithInvalidUserAttr() {
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
    public void gtMatchConditionEvaluatesNullWithInvalidAttr() {
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
    public void gtMatchConditionEvaluatesFalse()  {
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
    public void gtMatchConditionEvaluatesNull()  {
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
     * Verify that UserAttribute.evaluate for GE match type returns true for known visitor
     * attributes where the value's type is a number, and the UserAttribute's value is greater or equal than
     * the condition's value.
     */
    @Test
    public void geMatchConditionEvaluatesTrue() {
        UserAttribute testInstanceInteger = new UserAttribute("num_size", "custom_attribute", "ge", 2);
        UserAttribute testInstanceFloat = new UserAttribute("num_size", "custom_attribute", "ge", (float) 2);
        UserAttribute testInstanceDouble = new UserAttribute("num_counts", "custom_attribute", "ge", 2.55);

        assertTrue(testInstanceInteger.evaluate(null, testTypedUserAttributes));
        assertTrue(testInstanceFloat.evaluate(null, Collections.singletonMap("num_size", (float) 2)));
        assertTrue(testInstanceDouble.evaluate(null, testTypedUserAttributes));

        Map<String, Object> badAttributes = new HashMap<>();
        badAttributes.put("num_size", "bobs burgers");
        assertNull(testInstanceInteger.evaluate(null, badAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for GE match type returns null if the UserAttribute's
     * value type is invalid number.
     */
    @Test
    public void geMatchConditionEvaluatesNullWithInvalidUserAttr() {
        BigInteger bigInteger = new BigInteger("33221312312312312");
        Double infinitePositiveInfiniteDouble = Double.POSITIVE_INFINITY;
        Double infiniteNegativeInfiniteDouble = Double.NEGATIVE_INFINITY;
        Double infiniteNANDouble = Double.NaN;
        Double largeDouble = Math.pow(2, 53) + 2;
        float invalidFloatValue = (float) (Math.pow(2, 53) + 2000000000);

        UserAttribute testInstanceInteger = new UserAttribute(
            "num_size",
            "custom_attribute",
            "ge",
            5);
        UserAttribute testInstanceFloat = new UserAttribute(
            "num_size",
            "custom_attribute",
            "ge",
            (float) 5);
        UserAttribute testInstanceDouble = new UserAttribute(
            "num_counts",
            "custom_attribute",
            "ge",
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
     * Verify that UserAttribute.evaluate for GE match type returns null if the UserAttribute's
     * value type is invalid number.
     */
    @Test
    public void geMatchConditionEvaluatesNullWithInvalidAttr() {
        BigInteger bigInteger = new BigInteger("33221312312312312");
        Double infinitePositiveInfiniteDouble = Double.POSITIVE_INFINITY;
        Double infiniteNegativeInfiniteDouble = Double.NEGATIVE_INFINITY;
        Double infiniteNANDouble = Double.NaN;
        UserAttribute testInstanceInteger = new UserAttribute("num_size", "custom_attribute", "ge", bigInteger);
        UserAttribute testInstancePositiveInfinite = new UserAttribute("num_counts", "custom_attribute", "ge", infinitePositiveInfiniteDouble);
        UserAttribute testInstanceNegativeInfiniteDouble = new UserAttribute("num_counts", "custom_attribute", "ge", infiniteNegativeInfiniteDouble);
        UserAttribute testInstanceNANDouble = new UserAttribute("num_counts", "custom_attribute", "ge", infiniteNANDouble);

        assertNull(testInstanceInteger.evaluate(null, testTypedUserAttributes));
        assertNull(testInstancePositiveInfinite.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceNegativeInfiniteDouble.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceNANDouble.evaluate(null, testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for GE match type returns false for known visitor
     * attributes where the value's type is a number, and the UserAttribute's value is not greater or equal
     * than the condition's value.
     */
    @Test
    public void geMatchConditionEvaluatesFalse()  {
        UserAttribute testInstanceInteger = new UserAttribute("num_size", "custom_attribute", "ge", 5);
        UserAttribute testInstanceDouble = new UserAttribute("num_counts", "custom_attribute", "ge", 5.55);

        assertFalse(testInstanceInteger.evaluate(null, testTypedUserAttributes));
        assertFalse(testInstanceDouble.evaluate(null, testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for GE match type returns null if the UserAttribute's
     * value type is not a number.
     */
    @Test
    public void geMatchConditionEvaluatesNull()  {
        UserAttribute testInstanceString = new UserAttribute("browser_type", "custom_attribute", "ge", 3.5);
        UserAttribute testInstanceBoolean = new UserAttribute("is_firefox", "custom_attribute", "ge", 3.5);
        UserAttribute testInstanceObject = new UserAttribute("meta_data", "custom_attribute", "ge", 3.5);
        UserAttribute testInstanceNull = new UserAttribute("null_val", "custom_attribute", "ge", 3.5);

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
    public void ltMatchConditionEvaluatesTrue()  {
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
    public void ltMatchConditionEvaluatesFalse()  {
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
    public void ltMatchConditionEvaluatesNull()  {
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
    public void ltMatchConditionEvaluatesNullWithInvalidUserAttr() {
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
    public void ltMatchConditionEvaluatesNullWithInvalidAttributes() {
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
     * Verify that UserAttribute.evaluate for LE match type returns true for known visitor
     * attributes where the value's type is a number, and the UserAttribute's value is less or equal than
     * the condition's value.
     */
    @Test
    public void leMatchConditionEvaluatesTrue()  {
        UserAttribute testInstanceInteger = new UserAttribute("num_size", "custom_attribute", "le", 5);
        UserAttribute testInstanceDouble = new UserAttribute("num_counts", "custom_attribute", "le", 5.55);

        assertTrue(testInstanceInteger.evaluate(null, testTypedUserAttributes));
        assertTrue(testInstanceDouble.evaluate(null, Collections.singletonMap("num_counts", 5.55)));
    }

    /**
     * Verify that UserAttribute.evaluate for LE match type returns true for known visitor
     * attributes where the value's type is a number, and the UserAttribute's value is not less or equal
     * than the condition's value.
     */
    @Test
    public void leMatchConditionEvaluatesFalse()  {
        UserAttribute testInstanceInteger = new UserAttribute("num_size", "custom_attribute", "le", 2);
        UserAttribute testInstanceDouble = new UserAttribute("num_counts", "custom_attribute", "le", 2.55);

        assertFalse(testInstanceInteger.evaluate(null, testTypedUserAttributes));
        assertFalse(testInstanceDouble.evaluate(null, testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for LE match type returns null if the UserAttribute's
     * value type is not a number.
     */
    @Test
    public void leMatchConditionEvaluatesNull()  {
        UserAttribute testInstanceString = new UserAttribute("browser_type", "custom_attribute", "le", 3.5);
        UserAttribute testInstanceBoolean = new UserAttribute("is_firefox", "custom_attribute", "le", 3.5);
        UserAttribute testInstanceObject = new UserAttribute("meta_data", "custom_attribute", "le", 3.5);
        UserAttribute testInstanceNull = new UserAttribute("null_val", "custom_attribute", "le", 3.5);

        assertNull(testInstanceString.evaluate(null, testUserAttributes));
        assertNull(testInstanceBoolean.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceObject.evaluate(null, testTypedUserAttributes));
        assertNull(testInstanceNull.evaluate(null, testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for LE match type returns null if the UserAttribute's
     * value type is not a valid number.
     */
    @Test
    public void leMatchConditionEvaluatesNullWithInvalidUserAttr() {
        BigInteger bigInteger = new BigInteger("33221312312312312");
        Double infinitePositiveInfiniteDouble = Double.POSITIVE_INFINITY;
        Double infiniteNegativeInfiniteDouble = Double.NEGATIVE_INFINITY;
        Double infiniteNANDouble = Double.NaN;
        Double largeDouble = Math.pow(2,53) + 2;
        float invalidFloatValue = (float) (Math.pow(2, 53) + 2000000000);

        UserAttribute testInstanceInteger = new UserAttribute(
            "num_size",
            "custom_attribute",
            "le",
            5);
        UserAttribute testInstanceFloat = new UserAttribute(
            "num_size",
            "custom_attribute",
            "le",
            (float) 5);
        UserAttribute testInstanceDouble = new UserAttribute(
            "num_counts",
            "custom_attribute",
            "le",
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
     * Verify that UserAttribute.evaluate for LE match type returns null if the condition
     * value type is not a valid number.
     */
    @Test
    public void leMatchConditionEvaluatesNullWithInvalidAttributes() {
        BigInteger bigInteger = new BigInteger("33221312312312312");
        Double infinitePositiveInfiniteDouble = Double.POSITIVE_INFINITY;
        Double infiniteNegativeInfiniteDouble = Double.NEGATIVE_INFINITY;
        Double infiniteNANDouble = Double.NaN;
        UserAttribute testInstanceInteger = new UserAttribute("num_size", "custom_attribute", "le", bigInteger);
        UserAttribute testInstancePositiveInfinite = new UserAttribute("num_counts", "custom_attribute", "le", infinitePositiveInfiniteDouble);
        UserAttribute testInstanceNegativeInfiniteDouble = new UserAttribute("num_counts", "custom_attribute", "le", infiniteNegativeInfiniteDouble);
        UserAttribute testInstanceNANDouble = new UserAttribute("num_counts", "custom_attribute", "le", infiniteNANDouble);

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
    public void substringMatchConditionEvaluatesTrue()  {
        UserAttribute testInstanceString = new UserAttribute("browser_type", "custom_attribute", "substring", "chrome");
        assertTrue(testInstanceString.evaluate(null, testUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for SUBSTRING match type returns true if the
     * UserAttribute's value is a substring of the condition's value.
     */
    @Test
    public void substringMatchConditionPartialMatchEvaluatesTrue()  {
        UserAttribute testInstanceString = new UserAttribute("browser_type", "custom_attribute", "substring", "chro");
        assertTrue(testInstanceString.evaluate(null, testUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for SUBSTRING match type returns true if the
     * UserAttribute's value is NOT a substring of the condition's value.
     */
    @Test
    public void substringMatchConditionEvaluatesFalse()  {
        UserAttribute testInstanceString = new UserAttribute("browser_type", "custom_attribute", "substring", "chr0me");
        assertFalse(testInstanceString.evaluate(null, testUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for SUBSTRING match type returns null if the
     * UserAttribute's value type is not a string.
     */
    @Test
    public void substringMatchConditionEvaluatesNull()  {
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

    //======== Semantic version evaluation tests ========//

    // Test SemanticVersionEqualsMatch returns null if given invalid value type
    @Test
    public void testSemanticVersionEqualsMatchInvalidInput() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", 2.0);
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_eq", "2.0.0");
        assertNull(testInstanceString.evaluate(null, testAttributes));
    }

    @Test
    public void semanticVersionInvalidMajorShouldBeNumberOnly() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "a.1.2");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_eq", "2.0.0");
        assertNull(testInstanceString.evaluate(null, testAttributes));
    }

    @Test
    public void semanticVersionInvalidMinorShouldBeNumberOnly() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "1.b.2");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_eq", "2.0.0");
        assertNull(testInstanceString.evaluate(null, testAttributes));
    }

    @Test
    public void semanticVersionInvalidPatchShouldBeNumberOnly() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "1.2.c");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_eq", "2.0.0");
        assertNull(testInstanceString.evaluate(null, testAttributes));
    }

    // Test SemanticVersionEqualsMatch returns null if given invalid UserCondition Variable type
    @Test
    public void testSemanticVersionEqualsMatchInvalidUserConditionVariable() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "2.0");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_eq", 2.0);
        assertNull(testInstanceString.evaluate(null, testAttributes));
    }

    // Test SemanticVersionGTMatch returns null if given invalid value type
    @Test
    public void testSemanticVersionGTMatchInvalidInput() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", false);
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_gt", "2.0.0");
        assertNull(testInstanceString.evaluate(null, testAttributes));
    }

    // Test SemanticVersionGEMatch returns null if given invalid value type
    @Test
    public void testSemanticVersionGEMatchInvalidInput() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", 2);
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_ge", "2.0.0");
        assertNull(testInstanceString.evaluate(null, testAttributes));
    }

    // Test SemanticVersionLTMatch returns null if given invalid value type
    @Test
    public void testSemanticVersionLTMatchInvalidInput() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", 2);
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_lt", "2.0.0");
        assertNull(testInstanceString.evaluate(null, testAttributes));
    }

    // Test SemanticVersionLEMatch returns null if given invalid value type
    @Test
    public void testSemanticVersionLEMatchInvalidInput() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", 2);
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_le", "2.0.0");
        assertNull(testInstanceString.evaluate(null, testAttributes));
    }

    // Test if not same when targetVersion is only major.minor.patch and version is major.minor
    @Test
    public void testIsSemanticNotSameConditionValueMajorMinorPatch() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "1.2");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_eq", "1.2.0");
        assertFalse(testInstanceString.evaluate(null, testAttributes));
    }

    // Test if same when target is only major but user condition checks only major.minor,patch
    @Test
    public void testIsSemanticSameSingleDigit() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "3.0.0");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_eq", "3");
        assertTrue(testInstanceString.evaluate(null, testAttributes));
    }

    // Test if greater when User value patch is greater even when its beta
    @Test
    public void testIsSemanticGreaterWhenUserConditionComparesMajorMinorAndPatchVersion() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "3.1.1-beta");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_gt", "3.1.0");
        assertTrue(testInstanceString.evaluate(null, testAttributes));
    }

    // Test if greater when preRelease is greater alphabetically
    @Test
    public void testIsSemanticGreaterWhenMajorMinorPatchReleaseVersionCharacter() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "3.1.1-beta.y.1+1.1");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_gt", "3.1.1-beta.x.1+1.1");
        assertTrue(testInstanceString.evaluate(null, testAttributes));
    }

    // Test if greater when preRelease version number is greater
    @Test
    public void testIsSemanticGreaterWhenMajorMinorPatchPreReleaseVersionNum() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "3.1.1-beta.x.2+1.1");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_gt", "3.1.1-beta.x.1+1.1");
        assertTrue(testInstanceString.evaluate(null, testAttributes));
    }

    // Test if equals semantic version even when only same preRelease is passed in user attribute and no build meta
    @Test
    public void testIsSemanticEqualWhenMajorMinorPatchPreReleaseVersionNum() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "3.1.1-beta.x.1");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_eq", "3.1.1-beta.x.1");
        assertTrue(testInstanceString.evaluate(null, testAttributes));
    }

    // Test if not same
    @Test
    public void testIsSemanticNotSameReturnsFalse() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "2.1.2");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_eq", "2.1.1");
        assertFalse(testInstanceString.evaluate(null, testAttributes));
    }

    // Test when target is full semantic version major.minor.patch
    @Test
    public void testIsSemanticSameFull() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "3.0.1");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_eq", "3.0.1");
        assertTrue(testInstanceString.evaluate(null, testAttributes));
    }

    // Test compare less when user condition checks only major.minor
    @Test
    public void testIsSemanticLess() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "2.1.6");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_lt", "2.2");
        assertTrue(testInstanceString.evaluate(null, testAttributes));
    }

    // When user condition checks major.minor but target is major.minor.patch then its equals
    @Test
    public void testIsSemanticLessFalse() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "2.1.0");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_lt", "2.1");
        assertFalse(testInstanceString.evaluate(null, testAttributes));
    }

    // Test compare less when target is full major.minor.patch
    @Test
    public void testIsSemanticFullLess() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "2.1.6");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_lt", "2.1.9");
        assertTrue(testInstanceString.evaluate(null, testAttributes));
    }

    // Test compare greater when user condition checks only major.minor
    @Test
    public void testIsSemanticMore() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "2.3.6");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_gt", "2.2");
        assertTrue(testInstanceString.evaluate(null, testAttributes));
    }

    // Test compare greater when both are major.minor.patch-beta but target is greater than user condition
    @Test
    public void testIsSemanticMoreWhenBeta() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "2.3.6-beta");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_gt", "2.3.5-beta");
        assertTrue(testInstanceString.evaluate(null, testAttributes));
    }

    // Test compare greater when target is major.minor.patch
    @Test
    public void testIsSemanticFullMore() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "2.1.7");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_gt", "2.1.6");
        assertTrue(testInstanceString.evaluate(null, testAttributes));
    }

    // Test compare greater when target is major.minor.patch is smaller then it returns false
    @Test
    public void testSemanticVersionGTFullMoreReturnsFalse() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "2.1.9");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_gt", "2.1.10");
        assertFalse(testInstanceString.evaluate(null, testAttributes));
    }

    // Test compare equal when both are exactly same - major.minor.patch-beta
    @Test
    public void testIsSemanticFullEqual() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "2.1.9-beta");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_eq", "2.1.9-beta");
        assertTrue(testInstanceString.evaluate(null, testAttributes));
    }

    // Test compare equal when both major.minor.patch is same, but due to beta user condition is smaller
    @Test
    public void testIsSemanticLessWhenBeta() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "2.1.9");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_gt", "2.1.9-beta");
        assertTrue(testInstanceString.evaluate(null, testAttributes));
    }

    // Test compare greater when target is major.minor.patch-beta and user condition only compares major.minor.patch
    @Test
    public void testIsSemanticGreaterBeta() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "2.1.9");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_gt", "2.1.9-beta");
        assertTrue(testInstanceString.evaluate(null, testAttributes));
    }

    // Test compare equal when target is major.minor.patch
    @Test
    public void testIsSemanticLessEqualsWhenEqualsReturnsTrue() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "2.1.9");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_le", "2.1.9");
        assertTrue(testInstanceString.evaluate(null, testAttributes));
    }

    // Test compare less when target is major.minor.patch
    @Test
    public void testIsSemanticLessEqualsWhenLessReturnsTrue() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "2.132.9");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_le", "2.233.91");
        assertTrue(testInstanceString.evaluate(null, testAttributes));
    }

    // Test compare less when target is major.minor.patch
    @Test
    public void testIsSemanticLessEqualsWhenGreaterReturnsFalse() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "2.233.91");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_le", "2.132.009");
        assertFalse(testInstanceString.evaluate(null, testAttributes));
    }

    // Test compare equal when target is major.minor.patch
    @Test
    public void testIsSemanticGreaterEqualsWhenEqualsReturnsTrue() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "2.1.9");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_ge", "2.1.9");
        assertTrue(testInstanceString.evaluate(null, testAttributes));
    }

    // Test compare less when target is major.minor.patch
    @Test
    public void testIsSemanticGreaterEqualsWhenLessReturnsTrue() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "2.233.91");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_ge", "2.132.9");
        assertTrue(testInstanceString.evaluate(null, testAttributes));
    }

    // Test compare less when target is major.minor.patch
    @Test
    public void testIsSemanticGreaterEqualsWhenLessReturnsFalse() {
        Map testAttributes = new HashMap<String, String>();
        testAttributes.put("version", "2.132.009");
        UserAttribute testInstanceString = new UserAttribute("version", "custom_attribute", "semver_ge", "2.233.91");
        assertFalse(testInstanceString.evaluate(null, testAttributes));
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
