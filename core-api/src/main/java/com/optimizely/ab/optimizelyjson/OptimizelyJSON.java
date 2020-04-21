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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.optimizely.ab.Optimizely;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;

public class OptimizelyJSON {
    @Nullable
    private String payload;
    @Nullable
    private Map<String,Object> map;

    private static final Logger logger = LoggerFactory.getLogger(OptimizelyJSON.class);

    public OptimizelyJSON(@Nonnull String payload) {
        this.payload = payload;
    }

    public OptimizelyJSON(@Nonnull Map<String,Object> map) {
        this.map = map;
    }

    public String toString() {
        if (payload == null) {
            if (map == null) return null;

            try {
                Gson gson = new Gson();
                payload = gson.toJson(map);
            } catch (JsonSyntaxException e) {
                logger.error("Provided dictionary could not be converted to string.");
            }
        }

        return payload;
    }

    public Map<String,Object> toMap() {
        if (map == null) {
            if (payload == null) return null;

            try {
                Gson gson = new Gson();
                map = gson.fromJson(payload, Map.class);
            } catch (JsonSyntaxException e) {
                logger.error("Provided string could not be converted to dictionary.");
            }
        }

        return map;
    }

    public <T> T getValue(@Nullable String jsonKey, Class<T> clazz) {
        Map<String,Object> subMap = toMap();
        T result = null;

        if (jsonKey == null || jsonKey.isEmpty()) {
            return getValueInternal(subMap, clazz);
        }

        String[] keys = jsonKey.split("\\.");

        for(int i=0; i<keys.length; i++) {
            if (subMap == null) break;

            String key = keys[i];
            if (key.isEmpty()) break;

            if (i == keys.length - 1) {
                result = getValueInternal(subMap.get(key), clazz);
                break;
            }

            if (subMap.get(key) instanceof Map) {
                subMap = (Map<String, Object>) subMap.get(key);
            } else {
                logger.error("Value for JSON key ({}) not found.", jsonKey);
                break;
            }
        }

        if (result == null) {
            logger.error("Value for path could not be assigned to provided schema.");
        }
        return result;
    }

    private <T> T getValueInternal(@Nullable Object object, Class<T> clazz) {
        if (object == null) return null;

        if (clazz.isInstance(object)) return (T)object;  // primitive (String, Boolean, Integer, Double)

        try {
            Gson gson = new Gson();
            String payload = gson.toJson(object);
            return gson.fromJson(payload, clazz);
        } catch (Exception e) {
            //
        }

        return null;
    }

}

