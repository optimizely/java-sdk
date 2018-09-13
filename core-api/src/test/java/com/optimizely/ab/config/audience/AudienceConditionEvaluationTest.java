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
package com.optimizely.ab.config.audience;

import com.optimizely.ab.config.Experiment;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the evaluation of different audience condition types (And, Or, Not, and UserAttribute)
 */
@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT",
    justification = "mockito verify calls do have a side-effect")
public class AudienceConditionEvaluationTest {

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
        UserAttribute testInstance = new UserAttribute("browser_type", "custom_attribute", null,"chrome");
        assertTrue(testInstance.hashCode() != 0);
        assertNull(testInstance.getMatch());
        assertEquals(testInstance.getName(), "browser_type");
        assertEquals(testInstance.getType(), "custom_attribute");
        assertTrue(testInstance.evaluate(testUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate returns false on non-exact-matching visitor attribute data.
     */
    @Test
    public void userAttributeEvaluateFalse() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "custom_attribute", null,"firefox");
        assertFalse(testInstance.evaluate(testUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate returns false on unknown visitor attributes.
     */
    @Test
    public void userAttributeUnknownAttribute() throws Exception {
        UserAttribute testInstance = new UserAttribute("unknown_dim", "custom_attribute", null,"unknown");
        assertFalse(testInstance.evaluate(testUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate returns null on invalid match type.
     */
    @Test
    public void invalidMatchCondition() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "unknown_dimension", null,"chrome");
        assertNull(testInstance.evaluate(testUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate returns null on invalid match type.
     */
    @Test
    public void invalidMatch() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "custom_attribute", "blah","chrome");
        assertNull(testInstance.evaluate(testUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for EXIST match type returns true for known visitor 
     * attributes with non-null instances
     */
    @Test
    public void existsMatchConditionEvaluatesTrue() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "custom_attribute","exists", "firefox");
        assertTrue(testInstance.evaluate(testUserAttributes));

        UserAttribute testInstanceBoolean = new UserAttribute("is_firefox",  "custom_attribute","exists", false);
        UserAttribute testInstanceInteger = new UserAttribute("num_size",  "custom_attribute","exists", 5);
        UserAttribute testInstanceDouble = new UserAttribute("num_counts",  "custom_attribute","exists", 4.55);
        UserAttribute testInstanceObject = new UserAttribute("meta_data",  "custom_attribute","exists", testUserAttributes);
        assertTrue(testInstanceBoolean.evaluate(testTypedUserAttributes));
        assertTrue(testInstanceInteger.evaluate(testTypedUserAttributes));
        assertTrue(testInstanceDouble.evaluate(testTypedUserAttributes));
        assertTrue(testInstanceObject.evaluate(testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for EXIST match type returns false for unknown visitor 
     * attributes OR null visitor attributes.
     */
    @Test
    public void existsMatchConditionEvaluatesFalse() throws Exception {
        UserAttribute testInstance = new UserAttribute("bad_var",  "custom_attribute","exists", "chrome");
        UserAttribute testInstanceNull = new UserAttribute("null_val",  "custom_attribute","exists", "chrome");
        assertFalse(testInstance.evaluate(testTypedUserAttributes));
        assertFalse(testInstanceNull.evaluate(testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for EXACT match type returns true for known visitor 
     * attributes where the values and the value's type are the same
     */
    @Test
    public void exactMatchConditionEvaluatesTrue() throws Exception {
        UserAttribute testInstanceString = new UserAttribute("browser_type",  "custom_attribute","exact", "chrome");
        UserAttribute testInstanceBoolean = new UserAttribute("is_firefox",  "custom_attribute","exact", true);
        UserAttribute testInstanceInteger = new UserAttribute("num_size",  "custom_attribute","exact", 3);
        UserAttribute testInstanceDouble = new UserAttribute("num_counts",  "custom_attribute","exact", 3.55);

        assertTrue(testInstanceString.evaluate(testUserAttributes));
        assertTrue(testInstanceBoolean.evaluate(testTypedUserAttributes));
        assertTrue(testInstanceInteger.evaluate(testTypedUserAttributes));
        assertTrue(testInstanceDouble.evaluate(testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for EXACT match type returns false for known visitor 
     * attributes where the value's type are the same, but the values are different
     */
    @Test
    public void exactMatchConditionEvaluatesFalse() throws Exception {
        UserAttribute testInstanceString = new UserAttribute("browser_type",  "custom_attribute","exact", "firefox");
        UserAttribute testInstanceBoolean = new UserAttribute("is_firefox",  "custom_attribute","exact", false);
        UserAttribute testInstanceInteger = new UserAttribute("num_size",  "custom_attribute","exact", 5);
        UserAttribute testInstanceDouble = new UserAttribute("num_counts",  "custom_attribute","exact", 5.55);

        assertFalse(testInstanceString.evaluate(testUserAttributes));
        assertFalse(testInstanceBoolean.evaluate(testTypedUserAttributes));
        assertFalse(testInstanceInteger.evaluate(testTypedUserAttributes));
        assertFalse(testInstanceDouble.evaluate(testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for EXACT match type returns null for known visitor 
     * attributes where the value's type are different OR for values with null and object type.
     */
    @Test
    public void exactMatchConditionEvaluatesNull() throws Exception {
        UserAttribute testInstanceObject = new UserAttribute("meta_data",  "custom_attribute","exact", testUserAttributes);
        UserAttribute testInstanceString = new UserAttribute("browser_type",  "custom_attribute","exact", true);
        UserAttribute testInstanceBoolean = new UserAttribute("is_firefox",  "custom_attribute","exact", "true");
        UserAttribute testInstanceInteger = new UserAttribute("num_size",  "custom_attribute","exact", "3");
        UserAttribute testInstanceDouble = new UserAttribute("num_counts", "custom_attribute", "exact", "3.55");
        UserAttribute testInstanceNull = new UserAttribute("null_val",  "custom_attribute","exact", "null_val");

        assertNull(testInstanceObject.evaluate(testTypedUserAttributes));
        assertNull(testInstanceString.evaluate(testUserAttributes));
        assertNull(testInstanceBoolean.evaluate(testTypedUserAttributes));
        assertNull(testInstanceInteger.evaluate(testTypedUserAttributes));
        assertNull(testInstanceDouble.evaluate(testTypedUserAttributes));
        assertNull(testInstanceNull.evaluate(testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for GT match type returns true for known visitor 
     * attributes where the value's type is a number, and the UserAttribute's value is greater than
     * the condition's value.
     */
    @Test
    public void gtMatchConditionEvaluatesTrue() throws Exception {
        UserAttribute testInstanceInteger = new UserAttribute("num_size",  "custom_attribute","gt", 2);
        UserAttribute testInstanceDouble = new UserAttribute("num_counts",  "custom_attribute","gt", 2.55);

        assertTrue(testInstanceInteger.evaluate(testTypedUserAttributes));
        assertTrue(testInstanceDouble.evaluate(testTypedUserAttributes));

        Map<String,Object> badAttributes = new HashMap<>();
        badAttributes.put("num_size", "bobs burgers");
        assertNull(testInstanceInteger.evaluate(badAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for GT match type returns false for known visitor 
     * attributes where the value's type is a number, and the UserAttribute's value is not greater
     * than the condition's value.
     */
    @Test
    public void gtMatchConditionEvaluatesFalse() throws Exception {
        UserAttribute testInstanceInteger = new UserAttribute("num_size",  "custom_attribute","gt", 5);
        UserAttribute testInstanceDouble = new UserAttribute("num_counts",  "custom_attribute","gt", 5.55);

        assertFalse(testInstanceInteger.evaluate(testTypedUserAttributes));
        assertFalse(testInstanceDouble.evaluate(testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for GT match type returns null if the UserAttribute's 
     * value type is not a number.
     */
    @Test
    public void gtMatchConditionEvaluatesNull() throws Exception {
        UserAttribute testInstanceString = new UserAttribute("browser_type",  "custom_attribute","gt", 3.5);
        UserAttribute testInstanceBoolean = new UserAttribute("is_firefox",  "custom_attribute","gt", 3.5);
        UserAttribute testInstanceObject = new UserAttribute("meta_data",  "custom_attribute","gt", 3.5);
        UserAttribute testInstanceNull = new UserAttribute("null_val",  "custom_attribute","gt", 3.5);

        assertNull(testInstanceString.evaluate(testUserAttributes));
        assertNull(testInstanceBoolean.evaluate(testTypedUserAttributes));
        assertNull(testInstanceObject.evaluate(testTypedUserAttributes));
        assertNull(testInstanceNull.evaluate(testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for GT match type returns true for known visitor 
     * attributes where the value's type is a number, and the UserAttribute's value is less than
     * the condition's value.
     */
    @Test
    public void ltMatchConditionEvaluatesTrue() throws Exception {
        UserAttribute testInstanceInteger = new UserAttribute("num_size",  "custom_attribute","lt", 5);
        UserAttribute testInstanceDouble = new UserAttribute("num_counts",  "custom_attribute","lt", 5.55);

        assertTrue(testInstanceInteger.evaluate(testTypedUserAttributes));
        assertTrue(testInstanceDouble.evaluate(testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for GT match type returns true for known visitor 
     * attributes where the value's type is a number, and the UserAttribute's value is not less
     * than the condition's value.
     */
    @Test
    public void ltMatchConditionEvaluatesFalse() throws Exception {
        UserAttribute testInstanceInteger = new UserAttribute("num_size",  "custom_attribute","lt", 2);
        UserAttribute testInstanceDouble = new UserAttribute("num_counts",  "custom_attribute","lt", 2.55);

        assertFalse(testInstanceInteger.evaluate(testTypedUserAttributes));
        assertFalse(testInstanceDouble.evaluate(testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for LT match type returns null if the UserAttribute's 
     * value type is not a number.
     */
    @Test
    public void ltMatchConditionEvaluatesNull() throws Exception {
        UserAttribute testInstanceString = new UserAttribute("browser_type",  "custom_attribute","lt", 3.5);
        UserAttribute testInstanceBoolean = new UserAttribute("is_firefox",  "custom_attribute","lt", 3.5);
        UserAttribute testInstanceObject = new UserAttribute("meta_data",  "custom_attribute","lt", 3.5);
        UserAttribute testInstanceNull = new UserAttribute("null_val",  "custom_attribute","lt", 3.5);

        assertNull(testInstanceString.evaluate(testUserAttributes));
        assertNull(testInstanceBoolean.evaluate(testTypedUserAttributes));
        assertNull(testInstanceObject.evaluate(testTypedUserAttributes));
        assertNull(testInstanceNull.evaluate(testTypedUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for SUBSTRING match type returns true if the 
     * UserAttribute's value is a substring of the condition's value.
     */
    @Test
    public void substringMatchConditionEvaluatesTrue() throws Exception {
        UserAttribute testInstanceString = new UserAttribute("browser_type",  "custom_attribute","substring", "chrome1");
        assertTrue(testInstanceString.evaluate(testUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for SUBSTRING match type returns true if the 
     * UserAttribute's value is NOT a substring of the condition's value.
     */
    @Test
    public void substringMatchConditionEvaluatesFalse() throws Exception {
        UserAttribute testInstanceString = new UserAttribute("browser_type",  "custom_attribute","substring", "chr");
        assertFalse(testInstanceString.evaluate(testUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate for SUBSTRING match type returns null if the 
     * UserAttribute's value type is not a string.
     */
    @Test
    public void substringMatchConditionEvaluatesNull() throws Exception {
        UserAttribute testInstanceBoolean = new UserAttribute("is_firefox",  "custom_attribute","substring", "chrome1");
        UserAttribute testInstanceInteger = new UserAttribute("num_size",  "custom_attribute","substring", "chrome1");
        UserAttribute testInstanceDouble = new UserAttribute("num_counts",  "custom_attribute","substring", "chrome1");
        UserAttribute testInstanceObject = new UserAttribute("meta_data",  "custom_attribute","substring", "chrome1");
        UserAttribute testInstanceNull = new UserAttribute("null_val",  "custom_attribute","substring", "chrome1");

        assertNull(testInstanceBoolean.evaluate(testTypedUserAttributes));
        assertNull(testInstanceInteger.evaluate(testTypedUserAttributes));
        assertNull(testInstanceDouble.evaluate(testTypedUserAttributes));
        assertNull(testInstanceObject.evaluate(testTypedUserAttributes));
        assertNull(testInstanceNull.evaluate(testTypedUserAttributes));
    }

    /**
     * Verify that NotCondition.evaluate returns true when its condition operand evaluates to false.
     */
    @Test
    public void notConditionEvaluateTrue() throws Exception {
        UserAttribute userAttribute = mock(UserAttribute.class);
        when(userAttribute.evaluate(testUserAttributes)).thenReturn(false);

        NotCondition notCondition = new NotCondition(userAttribute);
        assertTrue(notCondition.evaluate(testUserAttributes));
        verify(userAttribute, times(1)).evaluate(testUserAttributes);
    }

    /**
     * Verify that NotCondition.evaluate returns false when its condition operand evaluates to true.
     */
    @Test
    public void notConditionEvaluateFalse() throws Exception {
        UserAttribute userAttribute = mock(UserAttribute.class);
        when(userAttribute.evaluate(testUserAttributes)).thenReturn(true);

        NotCondition notCondition = new NotCondition(userAttribute);
        assertFalse(notCondition.evaluate(testUserAttributes));
        verify(userAttribute, times(1)).evaluate(testUserAttributes);
    }

    /**
     * Verify that OrCondition.evaluate returns true when at least one of its operand conditions evaluate to true.
     */
    @Test
    public void orConditionEvaluateTrue() throws Exception {
        UserAttribute userAttribute1 = mock(UserAttribute.class);
        when(userAttribute1.evaluate(testUserAttributes)).thenReturn(true);

        UserAttribute userAttribute2 = mock(UserAttribute.class);
        when(userAttribute2.evaluate(testUserAttributes)).thenReturn(false);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(userAttribute1);
        conditions.add(userAttribute2);

        OrCondition orCondition = new OrCondition(conditions);
        assertTrue(orCondition.evaluate(testUserAttributes));
        verify(userAttribute1, times(1)).evaluate(testUserAttributes);
        // shouldn't be called due to short-circuiting in 'Or' evaluation
        verify(userAttribute2, times(0)).evaluate(testUserAttributes);
    }

    /**
     * Verify that OrCondition.evaluate returns true when at least one of its operand conditions evaluate to true.
     */
    @Test
    public void orConditionEvaluateTrueWithNullAndTrue() throws Exception {
        UserAttribute userAttribute1 = mock(UserAttribute.class);
        when(userAttribute1.evaluate(testUserAttributes)).thenReturn(null);

        UserAttribute userAttribute2 = mock(UserAttribute.class);
        when(userAttribute2.evaluate(testUserAttributes)).thenReturn(true);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(userAttribute1);
        conditions.add(userAttribute2);

        OrCondition orCondition = new OrCondition(conditions);
        assertTrue(orCondition.evaluate(testUserAttributes));
        verify(userAttribute1, times(1)).evaluate(testUserAttributes);
        // shouldn't be called due to short-circuiting in 'Or' evaluation
        verify(userAttribute2, times(1)).evaluate(testUserAttributes);
    }

    /**
     * Verify that OrCondition.evaluate returns true when at least one of its operand conditions evaluate to true.
     */
    @Test
    public void orConditionEvaluateNullWithNullAndFalse() throws Exception {
        UserAttribute userAttribute1 = mock(UserAttribute.class);
        when(userAttribute1.evaluate(testUserAttributes)).thenReturn(null);

        UserAttribute userAttribute2 = mock(UserAttribute.class);
        when(userAttribute2.evaluate(testUserAttributes)).thenReturn(false);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(userAttribute1);
        conditions.add(userAttribute2);

        OrCondition orCondition = new OrCondition(conditions);
        assertNull(orCondition.evaluate(testUserAttributes));
        verify(userAttribute1, times(1)).evaluate(testUserAttributes);
        // shouldn't be called due to short-circuiting in 'Or' evaluation
        verify(userAttribute2, times(1)).evaluate(testUserAttributes);
    }

    /**
     * Verify that OrCondition.evaluate returns true when at least one of its operand conditions evaluate to true.
     */
    @Test
    public void orConditionEvaluateFalseWithFalseAndFalse() throws Exception {
        UserAttribute userAttribute1 = mock(UserAttribute.class);
        when(userAttribute1.evaluate(testUserAttributes)).thenReturn(false);

        UserAttribute userAttribute2 = mock(UserAttribute.class);
        when(userAttribute2.evaluate(testUserAttributes)).thenReturn(false);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(userAttribute1);
        conditions.add(userAttribute2);

        OrCondition orCondition = new OrCondition(conditions);
        assertFalse(orCondition.evaluate(testUserAttributes));
        verify(userAttribute1, times(1)).evaluate(testUserAttributes);
        // shouldn't be called due to short-circuiting in 'Or' evaluation
        verify(userAttribute2, times(1)).evaluate(testUserAttributes);
    }

    /**
     * Verify that OrCondition.evaluate returns false when all of its operand conditions evaluate to false.
     */
    @Test
    public void orConditionEvaluateFalse() throws Exception {
        UserAttribute userAttribute1 = mock(UserAttribute.class);
        when(userAttribute1.evaluate(testUserAttributes)).thenReturn(false);

        UserAttribute userAttribute2 = mock(UserAttribute.class);
        when(userAttribute2.evaluate(testUserAttributes)).thenReturn(false);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(userAttribute1);
        conditions.add(userAttribute2);

        OrCondition orCondition = new OrCondition(conditions);
        assertFalse(orCondition.evaluate(testUserAttributes));
        verify(userAttribute1, times(1)).evaluate(testUserAttributes);
        verify(userAttribute2, times(1)).evaluate(testUserAttributes);
    }

    /**
     * Verify that AndCondition.evaluate returns true when all of its operand conditions evaluate to true.
     */
    @Test
    public void andConditionEvaluateTrue() throws Exception {
        OrCondition orCondition1 = mock(OrCondition.class);
        when(orCondition1.evaluate(testUserAttributes)).thenReturn(true);

        OrCondition orCondition2 = mock(OrCondition.class);
        when(orCondition2.evaluate(testUserAttributes)).thenReturn(true);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(orCondition1);
        conditions.add(orCondition2);

        AndCondition andCondition = new AndCondition(conditions);
        assertTrue(andCondition.evaluate(testUserAttributes));
        verify(orCondition1, times(1)).evaluate(testUserAttributes);
        verify(orCondition2, times(1)).evaluate(testUserAttributes);
    }

    /**
     * Verify that AndCondition.evaluate returns true when all of its operand conditions evaluate to true.
     */
    @Test
    public void andConditionEvaluateFalseWithNullAndFalse() throws Exception {
        OrCondition orCondition1 = mock(OrCondition.class);
        when(orCondition1.evaluate(testUserAttributes)).thenReturn(null);

        OrCondition orCondition2 = mock(OrCondition.class);
        when(orCondition2.evaluate(testUserAttributes)).thenReturn(false);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(orCondition1);
        conditions.add(orCondition2);

        AndCondition andCondition = new AndCondition(conditions);
        assertFalse(andCondition.evaluate(testUserAttributes));
        verify(orCondition1, times(1)).evaluate(testUserAttributes);
        verify(orCondition2, times(1)).evaluate(testUserAttributes);
    }

    /**
     * Verify that AndCondition.evaluate returns true when all of its operand conditions evaluate to true.
     */
    @Test
    public void andConditionEvaluateNullWithNullAndTrue() throws Exception {
        OrCondition orCondition1 = mock(OrCondition.class);
        when(orCondition1.evaluate(testUserAttributes)).thenReturn(null);

        OrCondition orCondition2 = mock(OrCondition.class);
        when(orCondition2.evaluate(testUserAttributes)).thenReturn(true);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(orCondition1);
        conditions.add(orCondition2);

        AndCondition andCondition = new AndCondition(conditions);
        assertNull(andCondition.evaluate(testUserAttributes));
        verify(orCondition1, times(1)).evaluate(testUserAttributes);
        verify(orCondition2, times(1)).evaluate(testUserAttributes);
    }

    /**
     * Verify that AndCondition.evaluate returns false when any one of its operand conditions evaluate to false.
     */
    @Test
    public void andConditionEvaluateFalse() throws Exception {
        OrCondition orCondition1 = mock(OrCondition.class);
        when(orCondition1.evaluate(testUserAttributes)).thenReturn(false);

        OrCondition orCondition2 = mock(OrCondition.class);
        when(orCondition2.evaluate(testUserAttributes)).thenReturn(true);

        // and[false, true]
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(orCondition1);
        conditions.add(orCondition2);

        AndCondition andCondition = new AndCondition(conditions);
        assertFalse(andCondition.evaluate(testUserAttributes));
        verify(orCondition1, times(1)).evaluate(testUserAttributes);
        // shouldn't be called due to short-circuiting in 'And' evaluation
        verify(orCondition2, times(0)).evaluate(testUserAttributes);

        OrCondition orCondition3 = mock(OrCondition.class);
        when(orCondition3.evaluate(testUserAttributes)).thenReturn(null);

        // and[null, false]
        List<Condition> conditions2 = new ArrayList<Condition>();
        conditions2.add(orCondition3);
        conditions2.add(orCondition1);

        AndCondition andCondition2 = new AndCondition(conditions2);
        assertFalse(andCondition2.evaluate(testUserAttributes));

        // and[true, false, null]
        List<Condition> conditions3 = new ArrayList<Condition>();
        conditions3.add(orCondition2);
        conditions3.add(orCondition3);
        conditions3.add(orCondition1);

        AndCondition andCondition3 = new AndCondition(conditions3);
        assertFalse(andCondition3.evaluate(testUserAttributes));
    }

    /**
     * Verify that AndCondition.evaluate returns null when any one of its operand conditions evaluate to false.
     */
    // @Test
    // public void andConditionEvaluateNull() throws Exception {

    // }

    /**
     * Verify that {@link UserAttribute#evaluate(Map)}
     * called when its attribute value is null
     * returns True when the user's attribute value is also null
     *          True when the attribute is not in the map
     *          False when empty string is used.
     * @throws Exception
     */
    @Test
    public void nullValueEvaluate() throws Exception {
        String attributeName = "attribute_name";
        String attributeType = "attribute_type";
        String attributeValue = null;
        UserAttribute nullValueAttribute = new UserAttribute(
                attributeName,
                attributeType,
                "exact",
                attributeValue
        );

        assertNull(nullValueAttribute.evaluate(Collections.<String, String>emptyMap()));
        assertNull(nullValueAttribute.evaluate(Collections.singletonMap(attributeName, attributeValue)));
        assertNull(nullValueAttribute.evaluate((Collections.singletonMap(attributeName, ""))));
    }
}
