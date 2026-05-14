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
import javax.annotation.Nullable;
import java.util.concurrent.Callable;

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
        Serializer serializer;
        if ((serializer = tryCreate("com.fasterxml.jackson.databind.ObjectMapper", JacksonSerializer::new)) != null ||
            (serializer = tryCreate("com.google.gson.Gson", GsonSerializer::new)) != null ||
            (serializer = tryCreate("org.json.simple.JSONObject", JsonSimpleSerializer::new)) != null ||
            (serializer = tryCreate("org.json.JSONObject", JsonSerializer::new)) != null) {
            logger.debug("using json serializer: {}", serializer.getClass().getSimpleName());
            return serializer;
        }
        throw new MissingJsonParserException("unable to locate a JSON parser. "
            + "Please see <link> for more information");
    }

    private static @Nullable Serializer tryCreate(String className, Callable<Serializer> factory) {
        if (!isPresent(className)) {
            return null;
        }
        try {
            return factory.call();
        } catch (Throwable t) {
            logger.warn("{} found on classpath but serializer init failed, trying next option.", className, t);
            return null;
        }
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
