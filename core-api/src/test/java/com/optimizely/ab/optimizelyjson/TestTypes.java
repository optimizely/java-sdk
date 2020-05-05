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

/**
 * Test types for parsing JSON strings to Java objects (OptimizelyJSON)
 */
public class TestTypes {

    public static class MD1 {
        public String k1;
        public boolean k2;
        public MD2 k3;
    }

    public static class MD2 {
        public double kk1;
        public MD3 kk2;
    }

    public static class MD3 {
        public boolean kkk1;
        public double kkk2;
        public String kkk3;
        public Object[] kkk4;
    }

    // Invalid parse type

    public static class NotMatchingType {
        public String x99;
    }

    // Test types for integer parsing tests

    public static class MDN1 {
        public int k1;
        public double k2;
        public MDN2 k3;
    }

    public static class MDN2 {
        public int kk1;
        public double kk2;
    }

}
