/**
 *
 *    Copyright 2016-2017, 2019-2020, Optimizely and contributors
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.optimizely.ab.config.*;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.TypedAudience;

import javax.annotation.Nonnull;

/**
 * {@link Gson}-based config parser implementation.
 */
final public class GsonConfigParser implements ConfigParser {
    private Gson gson;

    public GsonConfigParser() {
        this(new GsonBuilder()
            .registerTypeAdapter(Audience.class, new AudienceGsonDeserializer())
            .registerTypeAdapter(TypedAudience.class, new AudienceGsonDeserializer())
            .registerTypeAdapter(Experiment.class, new ExperimentGsonDeserializer())
            .registerTypeAdapter(FeatureFlag.class, new FeatureFlagGsonDeserializer())
            .registerTypeAdapter(Group.class, new GroupGsonDeserializer())
            .registerTypeAdapter(DatafileProjectConfig.class, new DatafileGsonDeserializer())
            .create());
    }

    GsonConfigParser(Gson gson) {
        this.gson = gson;
    }

    @Override
    public ProjectConfig parseProjectConfig(@Nonnull String json) throws ConfigParseException {
        if (json == null) {
            throw new ConfigParseException("Unable to parse null json.");
        }
        if (json.length() == 0) {
            throw new ConfigParseException("Unable to parse empty json.");
        }

        try {
            return gson.fromJson(json, DatafileProjectConfig.class);
        } catch (Exception e) {
            throw new ConfigParseException("Unable to parse datafile: " + json, e);
        }
    }

    public String toJson(Object src) {
        return gson.toJson(src);
    }

    public <T> T fromJson(String json, Class<T> clazz) throws JsonParseException {
        try {
            return gson.fromJson(json, clazz);
        } catch (Exception e) {
            throw new JsonParseException("Unable to parse JSON string: " + e.toString());
        }
    }

}
