package com.optimizely.ab.cmab;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import com.optimizely.ab.config.Cmab;

/**
 * Tests for {@link Cmab} configuration object.
 */
public class CmabTest {

    @Test
    public void testCmabConstructorWithValidData() {
        List<String> attributeIds = Arrays.asList("attr1", "attr2", "attr3");
        int trafficAllocation = 4000;

        Cmab cmab = new Cmab(attributeIds, trafficAllocation);

        assertEquals("AttributeIds should match", attributeIds, cmab.getAttributeIds());
        assertEquals("TrafficAllocation should match", trafficAllocation, cmab.getTrafficAllocation());
    }

    @Test
    public void testCmabConstructorWithEmptyAttributeIds() {
        List<String> attributeIds = Collections.emptyList();
        int trafficAllocation = 2000;

        Cmab cmab = new Cmab(attributeIds, trafficAllocation);

        assertEquals("AttributeIds should be empty", attributeIds, cmab.getAttributeIds());
        assertTrue("AttributeIds should be empty list", cmab.getAttributeIds().isEmpty());
        assertEquals("TrafficAllocation should match", trafficAllocation, cmab.getTrafficAllocation());
    }

    @Test
    public void testCmabConstructorWithSingleAttributeId() {
        List<String> attributeIds = Collections.singletonList("single_attr");
        int trafficAllocation = 3000;

        Cmab cmab = new Cmab(attributeIds, trafficAllocation);

        assertEquals("AttributeIds should match", attributeIds, cmab.getAttributeIds());
        assertEquals("Should have one attribute", 1, cmab.getAttributeIds().size());
        assertEquals("Single attribute should match", "single_attr", cmab.getAttributeIds().get(0));
        assertEquals("TrafficAllocation should match", trafficAllocation, cmab.getTrafficAllocation());
    }

    @Test
    public void testCmabConstructorWithZeroTrafficAllocation() {
        List<String> attributeIds = Arrays.asList("attr1", "attr2");
        int trafficAllocation = 0;

        Cmab cmab = new Cmab(attributeIds, trafficAllocation);

        assertEquals("AttributeIds should match", attributeIds, cmab.getAttributeIds());
        assertEquals("TrafficAllocation should be zero", 0, cmab.getTrafficAllocation());
    }

    @Test
    public void testCmabConstructorWithMaxTrafficAllocation() {
        List<String> attributeIds = Arrays.asList("attr1");
        int trafficAllocation = 10000;

        Cmab cmab = new Cmab(attributeIds, trafficAllocation);

        assertEquals("AttributeIds should match", attributeIds, cmab.getAttributeIds());
        assertEquals("TrafficAllocation should be 10000", 10000, cmab.getTrafficAllocation());
    }

    @Test
    public void testCmabEqualsAndHashCode() {
        List<String> attributeIds1 = Arrays.asList("attr1", "attr2");
        List<String> attributeIds2 = Arrays.asList("attr1", "attr2");
        List<String> attributeIds3 = Arrays.asList("attr1", "attr3");

        Cmab cmab1 = new Cmab(attributeIds1, 4000);
        Cmab cmab2 = new Cmab(attributeIds2, 4000);
        Cmab cmab3 = new Cmab(attributeIds3, 4000);
        Cmab cmab4 = new Cmab(attributeIds1, 5000);

        // Test equals
        assertEquals("CMAB with same data should be equal", cmab1, cmab2);
        assertNotEquals("CMAB with different attributeIds should not be equal", cmab1, cmab3);
        assertNotEquals("CMAB with different trafficAllocation should not be equal", cmab1, cmab4);

        // Test reflexivity
        assertEquals("CMAB should equal itself", cmab1, cmab1);

        // Test null comparison
        assertNotEquals("CMAB should not equal null", cmab1, null);

        // Test hashCode consistency
        assertEquals("Equal objects should have same hashCode", cmab1.hashCode(), cmab2.hashCode());
    }

    @Test
    public void testCmabToString() {
        List<String> attributeIds = Arrays.asList("attr1", "attr2");
        int trafficAllocation = 4000;

        Cmab cmab = new Cmab(attributeIds, trafficAllocation);
        String result = cmab.toString();

        assertNotNull("toString should not return null", result);
        assertTrue("toString should contain attributeIds", result.contains("attributeIds"));
        assertTrue("toString should contain trafficAllocation", result.contains("trafficAllocation"));
        assertTrue("toString should contain attr1", result.contains("attr1"));
        assertTrue("toString should contain attr2", result.contains("attr2"));
        assertTrue("toString should contain 4000", result.contains("4000"));
    }

    @Test
    public void testCmabToStringWithEmptyAttributeIds() {
        List<String> attributeIds = Collections.emptyList();
        int trafficAllocation = 2000;

        Cmab cmab = new Cmab(attributeIds, trafficAllocation);
        String result = cmab.toString();

        assertNotNull("toString should not return null", result);
        assertTrue("toString should contain attributeIds", result.contains("attributeIds"));
        assertTrue("toString should contain trafficAllocation", result.contains("trafficAllocation"));
        assertTrue("toString should contain 2000", result.contains("2000"));
    }

    @Test
    public void testCmabWithDuplicateAttributeIds() {
        List<String> attributeIds = Arrays.asList("attr1", "attr2", "attr1", "attr3");
        int trafficAllocation = 4000;

        Cmab cmab = new Cmab(attributeIds, trafficAllocation);

        assertEquals("AttributeIds should match exactly (including duplicates)",
            attributeIds, cmab.getAttributeIds());
        assertEquals("Should have 4 elements (including duplicate)", 4, cmab.getAttributeIds().size());
    }

    @Test
    public void testCmabWithRealWorldAttributeIds() {
        // Test with realistic attribute IDs from Optimizely
        List<String> attributeIds = Arrays.asList("808797688", "808797689", "10401066117");
        int trafficAllocation = 4000;

        Cmab cmab = new Cmab(attributeIds, trafficAllocation);

        assertEquals("AttributeIds should match", attributeIds, cmab.getAttributeIds());
        assertEquals("TrafficAllocation should match", trafficAllocation, cmab.getTrafficAllocation());
        assertTrue("Should contain first attribute ID", cmab.getAttributeIds().contains("808797688"));
        assertTrue("Should contain second attribute ID", cmab.getAttributeIds().contains("808797689"));
        assertTrue("Should contain third attribute ID", cmab.getAttributeIds().contains("10401066117"));
    }
}