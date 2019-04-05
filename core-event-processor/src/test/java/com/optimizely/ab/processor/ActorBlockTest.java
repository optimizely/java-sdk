package com.optimizely.ab.processor;

import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

public class ActorBlockTest {
    @Test
    public void testBaseClass() {
        ActorBlock<Integer, String> block = new ActorBlock.Base<Integer, String>() {
            @Override
            public void post(@Nonnull Integer element) {
                if (element <= 1) {
                    target().post(element.toString());
                } else {
                    target().postBatch(Collections.nCopies(element, "1"));
                }
            }
        };

        TerminalBlock<Object, List<Object>> target = Blocks.collect(Collectors.toList());

        Block.Link link = block.linkTo(target);
        assertThat(link, notNullValue());
        assertThat(link.isClosed(), is(false));

        block.post(1);
        assertThat(target.get(), contains("1"));
        target.reset();

        block.post(2);
        assertThat(target.get(), contains("1", "1"));
        target.reset();

        block.post(3);
        assertThat(target.get(), contains("1", "1", "1"));

        link.close();
        assertThat(link.isClosed(), is(true));
        try {
            block.post(0);
            fail("Expected to throw after unlinked");
        } catch (IllegalStateException e) {
            // good
        }
    }

}