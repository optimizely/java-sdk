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
package com.optimizely.ab.processor;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class CollectBlockTest {
    @Test
    public void toListCollector() {
        TerminalBlock<String, List<String>> sink = Blocks.collect(toList());
        assertThat(sink.get(), empty());

        sink.post("a");
        sink.postBatch(Arrays.asList("b", "c"));

        assertThat(sink.get(), contains("a", "b", "c"));

        sink.post("d");
        assertThat(sink.get(), contains("a", "b", "c", "d"));

        sink.reset();
        assertThat(sink.get(), empty());
    }

    @Test
    public void toSetCollector() {
        TerminalBlock<String, Map<String, Integer>> sink = Blocks.collect(toMap(s -> s, String::length));

        assertThat(sink.get(), equalTo(Collections.emptyMap()));

        sink.post("a");
        sink.post("bb");
        sink.post("ccc");

        assertThat(sink.get(), allOf(
            hasEntry("a", 1),
            hasEntry("bb", 2),
            hasEntry("ccc", 3)
        ));

        sink.post("");

        assertThat(sink.get(), allOf(
            hasEntry("", 0),
            hasEntry("a", 1),
            hasEntry("bb", 2),
            hasEntry("ccc", 3)
        ));

        sink.reset();
        assertThat(sink.get(), equalTo(Collections.emptyMap()));
    }
}