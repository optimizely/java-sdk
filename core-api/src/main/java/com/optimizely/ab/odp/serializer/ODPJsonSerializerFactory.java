/**
 *    Copyright 2022, Optimizely Inc. and contributors
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
package com.optimizely.ab.odp.serializer;

import com.optimizely.ab.internal.JsonParserProvider;
import com.optimizely.ab.odp.serializer.impl.GsonSerializer;
import com.optimizely.ab.odp.serializer.impl.JacksonSerializer;
import com.optimizely.ab.odp.serializer.impl.JsonSerializer;
import com.optimizely.ab.odp.serializer.impl.JsonSimpleSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ODPJsonSerializerFactory {
    private static final Logger logger = LoggerFactory.getLogger(ODPJsonSerializerFactory.class);

    public static ODPJsonSerializer getSerializer() {
        JsonParserProvider parserProvider = JsonParserProvider.getDefaultParser();
        ODPJsonSerializer jsonSerializer = null;
        switch (parserProvider) {
            case GSON_CONFIG_PARSER:
                jsonSerializer = new GsonSerializer();
                break;
            case JACKSON_CONFIG_PARSER:
                jsonSerializer = new JacksonSerializer();
                break;
            case JSON_CONFIG_PARSER:
                jsonSerializer = new JsonSerializer();
                break;
            case JSON_SIMPLE_CONFIG_PARSER:
                jsonSerializer = new JsonSimpleSerializer();
                break;
        }
        logger.info("Using " + parserProvider.toString() + " serializer");
        return jsonSerializer;
    }
}
