/**
 *
 *    Copyright 2016-2017, 2019, Optimizely and contributors
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.optimizely.ab.config.parser.MissingJsonParserException;

import javax.annotation.Nonnull;

/**
 * Factory for generating {@link Serializer} instances, based on the json library available on the classpath.
 */
public final class DefaultJsonSerializer {

    private static final Logger logger = LoggerFactory.getLogger(DefaultJsonSerializer.class);

    private DefaultJsonSerializer() {
    }

    public static Serializer getInstance() {
        return LazyHolder.INSTANCE;
    }

    //======== Helper methods ========//

    /**
     * Creates and returns a {@link Serializer} using a json library available on the classpath.
     *
     * @return the created serializer
     * @throws MissingJsonParserException if there are no supported json libraries available on the classpath
     */
    private static @Nonnull
    Serializer create() {
        if (isPresent("com.fasterxml.jackson.databind.ObjectMapper")) {
            try {
                Serializer serializer = new JacksonSerializer();
                logger.debug("using json serializer: {}", serializer.getClass().getSimpleName());
                return serializer;
            } catch (Throwable t) {
                logger.warn("Jackson found on classpath but serializer initialization failed, trying next option.", t);
            }
        }
        if (isPresent("com.google.gson.Gson")) {
            try {
                Serializer serializer = new GsonSerializer();
                logger.debug("using json serializer: {}", serializer.getClass().getSimpleName());
                return serializer;
            } catch (Throwable t) {
                logger.warn("Gson found on classpath but serializer initialization failed, trying next option.", t);
            }
        }
        if (isPresent("org.json.simple.JSONObject")) {
            try {
                Serializer serializer = new JsonSimpleSerializer();
                logger.debug("using json serializer: {}", serializer.getClass().getSimpleName());
                return serializer;
            } catch (Throwable t) {
                logger.warn("json-simple found on classpath but serializer initialization failed, trying next option.", t);
            }
        }
        if (isPresent("org.json.JSONObject")) {
            try {
                Serializer serializer = new JsonSerializer();
                logger.debug("using json serializer: {}", serializer.getClass().getSimpleName());
                return serializer;
            } catch (Throwable t) {
                logger.warn("org.json found on classpath but serializer initialization failed.", t);
            }
        }

        throw new MissingJsonParserException("unable to locate a JSON parser. "
            + "Please see <link> for more information");
    }

    private static boolean isPresent(@Nonnull String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    //======== Lazy-init Holder ========//

    private static class LazyHolder {
        private static final Serializer INSTANCE = create();
    }
}
