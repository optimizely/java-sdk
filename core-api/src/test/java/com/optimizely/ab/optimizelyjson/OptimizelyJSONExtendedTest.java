/**
 *
 *    Copyright 2020, Optimizely and contributors
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
package com.optimizely.ab.optimizelyjson;

import com.optimizely.ab.config.parser.UnsupportedOperationException;
import com.optimizely.ab.optimizelyjson.types.MD1;
import com.optimizely.ab.optimizelyjson.types.MD2;
import com.optimizely.ab.optimizelyjson.types.MD3;
import org.junit.Test;

import static org.junit.Assert.*;

public class OptimizelyJSONExtendedTest extends OptimizelyJSONCoreTest {

    @Test
    public void testGetValueNullKeyPath() throws UnsupportedOperationException {
        OptimizelyJSON oj1 = new OptimizelyJSON(orgJson, getParser());

        MD1 md1 = oj1.getValue(null, MD1.class);
        assertNotNull(md1);
        assertEquals(md1.k1, "v1");
        assertEquals(md1.k2, true);
        assertEquals(md1.k3.kk1, 1.2, 0.01);
        assertEquals(md1.k3.kk2.kkk1, true);
        assertEquals((Double)md1.k3.kk2.kkk4[0], 5.7, 0.01);
        assertEquals(md1.k3.kk2.kkk4[2], "vvv4");

        // verify previous getValue does not destroy the data

        MD1 newMd1 = oj1.getValue(null, MD1.class);
        assertEquals(newMd1.k1, "v1");
        assertEquals(newMd1.k2, true);
        assertEquals(newMd1.k3.kk1, 1.2, 0.01);
        assertEquals(newMd1.k3.kk2.kkk1, true);
        assertEquals((Double)newMd1.k3.kk2.kkk4[0], 5.7, 0.01);
        assertEquals(newMd1.k3.kk2.kkk4[2], "vvv4");
    }

    @Test
    public void testGetValueEmptyKeyPath() throws UnsupportedOperationException {
        OptimizelyJSON oj1 = new OptimizelyJSON(orgJson, getParser());

        MD1 md1 = oj1.getValue("", MD1.class);
        assertEquals(md1.k1, "v1");
        assertEquals(md1.k2, true);
        assertEquals(md1.k3.kk1, 1.2, 0.01);
        assertEquals(md1.k3.kk2.kkk1, true);
        assertEquals((Double) md1.k3.kk2.kkk4[0], 5.7, 0.01);
        assertEquals(md1.k3.kk2.kkk4[2], "vvv4");
    }

    @Test
    public void testGetValueWithKeyPathToMapWithLevel1() throws UnsupportedOperationException {
        OptimizelyJSON oj1 = new OptimizelyJSON(orgJson, getParser());

        MD2 md2 = oj1.getValue("k3", MD2.class);
        assertNotNull(md2);
        assertEquals(md2.kk1, 1.2, 0.01);
        assertEquals(md2.kk2.kkk1, true);
    }

    @Test
    public void testGetValueWithKeyPathToMapWithLevel2() throws UnsupportedOperationException {
        OptimizelyJSON oj1 = new OptimizelyJSON(orgJson, getParser());

        MD3 md3 = oj1.getValue("k3.kk2", MD3.class);
        assertNotNull(md3);
        assertEquals(md3.kkk1, true);
    }

    @Test
    public void testGetValueWithKeyPathToBoolean() throws UnsupportedOperationException {
        OptimizelyJSON oj1 = new OptimizelyJSON(orgJson, getParser());

        Boolean value = oj1.getValue("k3.kk2.kkk1", Boolean.class);
        assertNotNull(value);
        assertEquals(value, true);
    }

    @Test
    public void testGetValueWithKeyPathToDouble() throws UnsupportedOperationException {
        OptimizelyJSON oj1 = new OptimizelyJSON(orgJson, getParser());

        Double value = oj1.getValue("k3.kk2.kkk2", Double.class);
        assertNotNull(value);
        assertEquals(value.doubleValue(), 3.5, 0.01);
    }

    @Test
    public void testGetValueWithKeyPathToString() throws UnsupportedOperationException {
        OptimizelyJSON oj1 = new OptimizelyJSON(orgJson, getParser());

        String value = oj1.getValue("k3.kk2.kkk3", String.class);
        assertNotNull(value);
        assertEquals(value, "vvv3");
    }

    @Test
    public void testGetValueWithInvalidKeyPath() throws UnsupportedOperationException {
        OptimizelyJSON oj1 = new OptimizelyJSON(orgJson, getParser());

        String value = oj1.getValue("k3..kkk3", String.class);
        assertNull(value);
    }

    @Test
    public void testGetValueWithInvalidKeyPath2() throws UnsupportedOperationException {
        OptimizelyJSON oj1 = new OptimizelyJSON(orgJson, getParser());

        String value = oj1.getValue("k1.", String.class);
        assertNull(value);
    }

    @Test
    public void testGetValueWithInvalidKeyPath3() throws UnsupportedOperationException {
        OptimizelyJSON oj1 = new OptimizelyJSON(orgJson, getParser());

        String value = oj1.getValue("x9", String.class);
        assertNull(value);
    }

    @Test
    public void testGetValueWithInvalidKeyPath4() throws UnsupportedOperationException {
        OptimizelyJSON oj1 = new OptimizelyJSON(orgJson, getParser());

        String value = oj1.getValue("k3.x9", String.class);
        assertNull(value);
    }

    @Test
    public void testGetValueWithWrongType() throws UnsupportedOperationException {
        OptimizelyJSON oj1 = new OptimizelyJSON(orgJson, getParser());

        Integer value = oj1.getValue("k3.kk2.kkk3", Integer.class);
        assertNull(value);
    }

}

