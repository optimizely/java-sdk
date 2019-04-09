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

import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class BlocksTest {
    @Test
    public void testSource() {
        class TestSource extends Blocks.Source<Integer> {
            AtomicInteger seq = new AtomicInteger(1);

            void next() {
                emit(seq.getAndIncrement());
            }

            void until(int end) {
                int start = seq.getAndUpdate(curr -> Math.max(curr, end));
                emitBatch(IntStream.range(start, end).boxed().collect(toList()));
            }
        }

        TestSource block = new TestSource();

        LinkedList<Integer> output = new LinkedList<>();
        block.linkTo(output::add);

        block.next();
        Assert.assertThat(output.poll(), is(1));

        block.until(6);
        Assert.assertThat(output.poll(), is(2));
        Assert.assertThat(output.poll(), is(3));
        Assert.assertThat(output.poll(), is(4));
        Assert.assertThat(output.poll(), is(5));

        block.next();
        Assert.assertThat(output.poll(), is(6));
    }

    @Test
    public void testActor() {
        class TestActor extends Blocks.Actor<Integer, Integer> {
            @Override
            public void post(@Nonnull Integer element) {
                if (element <= 1) {
                    emit(element);
                } else {
                    emitBatch(Collections.nCopies(element, 1));
                }
            }
        }

        ActorBlock<Integer, Integer> block = new TestActor();

        Blocks.ValueBlock<Object, List<Object>> target = Blocks.collect(Collectors.toList());

        block.linkTo(target);

        block.post(1);
        assertThat(target.get(), contains(1));
        target.reset();

        block.post(2);
        assertThat(target.get(), contains(1, 1));
        target.reset();

        block.post(3);
        assertThat(target.get(), contains(1, 1, 1));

        block.linkTo(null);
        block.post(0);
    }

    @Test
    public void testEffect() {
        LongAdder adder1 = new LongAdder();
        LongAdder adder2 = new LongAdder();
        ActorBlock<LongAdder, LongAdder> block = Blocks.action(LongAdder::increment);

        AtomicReference<LongAdder> latest = new AtomicReference<>();
        block.linkTo(latest::set);

        assertThat(adder1.sum(), is(0L));
        assertThat(adder2.sum(), is(0L));
        assertThat(latest.get(), nullValue());

        block.post(adder1);

        assertThat(adder1.sum(), is(1L));
        assertThat(adder2.sum(), is(0L));
        assertThat(latest.get(), theInstance(adder1));

        block.post(adder1);

        assertThat(adder1.sum(), is(2L));
        assertThat(adder2.sum(), is(0L));
        assertThat(latest.get(), theInstance(adder1));

        block.post(adder2);

        assertThat(adder1.sum(), is(2L));
        assertThat(adder2.sum(), is(1L));
        assertThat(latest.get(), theInstance(adder2));
    }

    @Test
    public void testMap() {
        Map<String, String> dict = new HashMap<>();
        dict.put("foo", "bar");
        dict.put("bar", "baz");

        ActorBlock<String, String> block = Blocks.map(dict::get);

        LinkedList<String> output = new LinkedList<>();
        block.linkTo(output::push);

        block.post("foo");
        assertThat(output.pop(), is("bar"));

        block.post("bar");
        assertThat(output.pop(), is("baz"));

        block.post("baz");
        assertThat(output.isEmpty(), is(true));
    }

    @Test
    public void testFlatMap() {
        Map<String, Set<String>> dict = new HashMap<>();
        dict.put("foo", Collections.singleton("bar"));
        dict.put("bar", new HashSet<>(Arrays.asList("baz", "qux")));

        ActorBlock<String, String> block = Blocks.flatMap(dict::get);

        LinkedList<String> output = new LinkedList<>();
        block.linkTo(output::push);

        block.post("foo");
        assertThat(output.pop(), is("bar"));

        block.post("bar");
        assertThat(output.pop(), is("baz"));
        assertThat(output.pop(), is("qux"));

        block.post("baz");
        assertThat(output.isEmpty(), is(true));
    }

    @Test
    public void testFilter() {
        ActorBlock<Integer, Integer> block = Blocks.filter(i -> i % 2 == 0);

        LinkedList<Number> output = new LinkedList<>();
        block.linkTo(output::push);

        block.post(0);
        assertThat(output.pop(), is(0));

        block.post(1);
        assertThat(output.isEmpty(), is(true));

        block.post(2);
        assertThat(output.pop(), is(2));

        block.post(3);
        assertThat(output.isEmpty(), is(true));
    }

    @Test
    public void testCollector() {
        Blocks.ValueBlock<String, Set<String>> block = Blocks.collect(toSet());

        block.post("foo");
        assertThat(block.get(), is(Collections.singleton("foo")));

        block.post("foo");
        assertThat(block.get(), is(Collections.singleton("foo")));

        block.post("bar");
        assertThat(block.get(), is(new HashSet<>(Arrays.asList("foo", "bar"))));
    }


    @Test
    public void testIdentity() {
        ActorBlock<Object, Object> block = Blocks.identity();

        LinkedList<Object> output = new LinkedList<>();
        block.linkTo(output::push);

        block.post(0);
        assertThat(output.pop(), is(0));

        Object x = new Object();
        block.post(x);
        assertThat(output.pop(), sameInstance(x));
    }
}