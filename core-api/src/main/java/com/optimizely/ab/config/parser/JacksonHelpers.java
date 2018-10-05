/**
 *
 *    Copyright 2018, Optimizely and contributors
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
