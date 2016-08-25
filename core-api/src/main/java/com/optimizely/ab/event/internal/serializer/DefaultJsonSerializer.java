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

    private DefaultJsonSerializer() { }

    public static Serializer getInstance() {
        return LazyHolder.INSTANCE;
    }

    //======== Helper methods ========//

    /**
     * Creates and returns a {@link Serializer} using a json library available on the classpath.
     * @return the created serializer
     * @throws MissingJsonParserException if there are no supported json libraries available on the classpath
     */
    private static @Nonnull
    Serializer create() {
        Serializer serializer;

        if (isPresent("com.fasterxml.jackson.databind.ObjectMapper")) {
            serializer = new JacksonSerializer();
        } else if (isPresent("com.google.gson.Gson")) {
            serializer = new GsonSerializer();
        } else if (isPresent("org.json.simple.JSONObject")) {
            serializer = new JsonSimpleSerializer();
        } else if (isPresent("org.json.JSONObject")) {
            serializer = new JsonSerializer();
        } else {
            throw new MissingJsonParserException("unable to locate a JSON parser. "
                    + "Please see <link> for more information");
        }

        logger.info("using json serializer: {}", serializer.getClass().getSimpleName());
        return serializer;
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
