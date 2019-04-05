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

import java.util.LinkedList;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class SourceBlockTest {
    @Test
    public void testBaseClass() {
        SequenceBlock block = new SequenceBlock();

        LinkedList<Integer> output = new LinkedList<>();
        Block.Link link = block.linkTo(output::add);

        assertThat(link, notNullValue());
        assertThat(link.isClosed(), is(false));

        block.next();
        assertThat(output.poll(), is(0));

        block.until(5);
        assertThat(output.poll(), is(1));
        assertThat(output.poll(), is(2));
        assertThat(output.poll(), is(3));
        assertThat(output.poll(), is(4));

        block.next();
        assertThat(output.poll(), is(5));

        link.close();
        assertThat(link.isClosed(), is(true));

        try {
            block.next();
            fail("Expected to throw after unlinked");
        } catch (IllegalStateException e) {
            // good
        }
    }

    private static class SequenceBlock extends SourceBlock.Base<Integer> {
        int n = 0;

        void next() {
            target().post(n++);
        }

        void until(int m) {
            target().postBatch(IntStream.range(n, m).boxed().collect(toList()));
            n = Math.max(n, m);
        }
    }
}