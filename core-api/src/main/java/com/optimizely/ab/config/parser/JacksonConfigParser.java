/**
 *
 *    Copyright 2016-2018, Optimizely and contributors
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
package com.optimizely.ab.config.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.optimizely.ab.config.DatafileProjectConfig;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.TypedAudience;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * {@code Jackson}-based config parser implementation.
 */
final public class JacksonConfigParser implements ConfigParser {
    private ObjectMapper objectMapper;

    public JacksonConfigParser() {
        this(new ObjectMapper());
    }

    JacksonConfigParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new ProjectConfigModule());
    }

    @Override
    public ProjectConfig parseProjectConfig(@Nonnull String json) throws ConfigParseException {
        try {
            return objectMapper.readValue(json, DatafileProjectConfig.class);
        } catch (Exception e) {
            throw new ConfigParseException("Unable to parse datafile: " + json, e);
        }
    }

    class ProjectConfigModule extends SimpleModule {
        private final static String NAME = "ProjectConfigModule";

        public ProjectConfigModule() {
            super(NAME);
            addDeserializer(DatafileProjectConfig.class, new DatafileJacksonDeserializer());
            addDeserializer(Audience.class, new AudienceJacksonDeserializer(objectMapper));
            addDeserializer(TypedAudience.class, new TypedAudienceJacksonDeserializer(objectMapper));
            addDeserializer(Condition.class, new ConditionJacksonDeserializer(objectMapper));
        }
    }

    @Override
    public String toJson(Object src) throws ConfigParseException {
        try {
            return objectMapper.writeValueAsString(src);
        } catch (JsonProcessingException e) {
            throw new ConfigParseException("Serialization failed: " + e.toString());
        }
    }

    @Override
    public <T> T fromJson(String json, Class<T> clazz) throws ConfigParseException {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new ConfigParseException("Unable to parse JSON string: " + e.toString());
        }
    }

}
