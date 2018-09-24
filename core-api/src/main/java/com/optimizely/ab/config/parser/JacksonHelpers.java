package com.optimizely.ab.config.parser;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class JacksonHelpers {
    private JacksonHelpers() {
    }

    static <T> List<T> arrayNodeToList(JsonNode arrayNode, Class<T> itemClass, ObjectCodec codec) throws IOException {
        if (arrayNode == null || arrayNode.isNull() || !arrayNode.isArray()) {
            return null;
        }

        List<T> items = new ArrayList<>(arrayNode.size());

        for (int i = 0; i < arrayNode.size(); i++) {
            JsonNode itemNode = arrayNode.get(i);
            if (itemNode.isNull()) {
                continue;
            }
            items.add(codec.treeToValue(itemNode, itemClass));
        }

        return items;
    }
}
