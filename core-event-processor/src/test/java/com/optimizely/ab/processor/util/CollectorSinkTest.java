/**
 *    Copyright 2019, Optimizely Inc. and contributors
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
package com.optimizely.ab.processor.util;

import com.optimizely.ab.processor.CollectorSink;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class CollectorSinkTest {
    @Test
    public void toListCollector() {
        CollectorSink<String, ?, List<String>> sink = new CollectorSink<>(Collectors.toList());
        assertThat(sink.get(), empty());

        sink.process("a");
        sink.processBatch(Arrays.asList("b", "c"));

        assertThat(sink.get(), contains("a", "b", "c"));

        sink.process("d");
        assertThat(sink.get(), contains("a", "b", "c", "d"));

        sink.clear();
        assertThat(sink.get(), empty());
    }

    @Test
    public void toSetCollector() {
        CollectorSink<String, ?, Map<String, Integer>> sink = new CollectorSink<>(
            Collectors.toMap(s -> s, String::length));

        assertThat(sink.get(), equalTo(Collections.emptyMap()));

        sink.process("a");
        sink.process("bb");
        sink.process("ccc");

        assertThat(sink.get(), allOf(
            hasEntry("a", 1),
            hasEntry("bb", 2),
            hasEntry("ccc", 3)
        ));

        sink.process("");

        assertThat(sink.get(), allOf(
            hasEntry("", 0),
            hasEntry("a", 1),
            hasEntry("bb", 2),
            hasEntry("ccc", 3)
        ));

        sink.clear();
        assertThat(sink.get(), equalTo(Collections.emptyMap()));
    }
}