/**
 *
 *    Copyright 2016-2017, 2019, 2025 Optimizely and contributors
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
package com.optimizely.ab.event.internal.serializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class JacksonSerializer implements Serializer {

    private ObjectMapper mapper = createMapper();

    /**
     * Creates an ObjectMapper with snake_case naming strategy.
     * Supports both Jackson 2.12+ (PropertyNamingStrategies) and earlier versions (PropertyNamingStrategy).
     * Uses reflection to avoid compile-time dependencies on either API.
     */
    static ObjectMapper createMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        Object namingStrategy = getSnakeCaseStrategy();
        
        try {
            // Use setPropertyNamingStrategy method (available in all versions)
            objectMapper.getClass()
                .getMethod("setPropertyNamingStrategy", 
                    Class.forName("com.fasterxml.jackson.databind.PropertyNamingStrategy"))
                .invoke(objectMapper, namingStrategy);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set snake_case naming strategy", e);
        }
        
        return objectMapper;
    }

    /**
     * Gets the snake case naming strategy, supporting both Jackson 2.12+ and earlier versions.
     */
    private static Object getSnakeCaseStrategy() {
        try {
            // Try Jackson 2.12+ API first
            Class<?> strategiesClass = Class.forName("com.fasterxml.jackson.databind.PropertyNamingStrategies");
            return strategiesClass.getField("SNAKE_CASE").get(null);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            try {
                // Fall back to Jackson 2.11 and earlier (deprecated but compatible)
                Class<?> strategyClass = Class.forName("com.fasterxml.jackson.databind.PropertyNamingStrategy");
                return strategyClass.getField("SNAKE_CASE").get(null);
            } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException ex) {
                throw new RuntimeException("Unable to find snake_case naming strategy in Jackson", ex);
            }
        }
    }

    public <T> String serialize(T payload) {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            return mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Unable to serialize payload", e);
        }
    }
}
