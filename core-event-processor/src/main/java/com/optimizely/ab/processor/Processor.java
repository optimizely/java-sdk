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

import com.optimizely.ab.common.internal.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * This class represents processing pipeline as a sequence of {@link Block}s composed together.
 */
public class Processor<TInput, TOutput> implements ActorBlock<TInput, TOutput> {
    public static final Logger logger = LoggerFactory.getLogger(Processor.class);

    private final TargetBlock<TInput> in;
    private final SourceBlock<TOutput> out;
    private final Block[] blocks;

    /**
     * Creates a {@link Processor} from a series of {@link Block}.
     *
     * The first block is used as {@link TargetBlock} at the head of chain and last block is used as {@link SourceBlock} *
     * at the tail of chain.
     *
     * @param <T> the type of input elements
     * @param <R> the type of output elements
     * @return a new {@link ActorBlock}
     * @throws IllegalArgumentException if {@code blocks} is null or contains null
     */
    @SuppressWarnings("unchecked")
    public static <T, R> Processor<T, R> from(Block... blocks) {
        Assert.notNull(blocks, "blocks");

        // validation
        Assert.argument(blocks.length > 0, "blocks must not be empty");
        for (int i = 0; i < blocks.length; i++) {
            Block block = blocks[i];
            Assert.argument(block != null, "blocks must not contain nulls");
            for (int j = i + 1; j < blocks.length; j++) {
                Assert.argument(block != blocks[j], "blocks must not contain duplicates");
            }
        }

        TargetBlock<T> in = (TargetBlock<T>) blocks[0];
        Block last = blocks[blocks.length - 1];

        SourceBlock<R> out = null;
        if (last instanceof SourceBlock) {
            out = (SourceBlock<R>) last;
        }

        return new Processor<>(in, out, blocks);
    }

    private Processor(
        TargetBlock<TInput> in,
        SourceBlock<TOutput> out,
        Block[] blocks
    ) {
        this.in = Assert.notNull(in, "in");
        this.out = out;
        this.blocks = Assert.notNull(blocks, "blocks");
    }

    @SuppressWarnings("unchecked")
    public void link() {
        TargetBlock downstream = (TargetBlock) blocks[blocks.length - 1];
        for (int i = blocks.length - 2; i >= 0; i--) {
            SourceBlock upstream = (SourceBlock) blocks[i];
            upstream.linkTo(downstream);
            downstream = (TargetBlock) upstream;
        }
    }

    @Override
    public void onStart() {
        logger.debug("Starting {} components...", blocks.length);

        for (int i = blocks.length - 1; i >= 0; i--) {
            blocks[i].onStart();
        }

        logger.debug("Started");
    }

    @Override
    public boolean onStop(long timeout, TimeUnit unit) {
        logger.debug("Stopping {} components...", blocks.length);


        for (final Block block : blocks) {
            block.onStop();
        }

        logger.debug("Stopped");
        return true;
    }

    @Override
    public void post(@Nonnull TInput element) {
        in.post(element);
    }

    @Override
    public void postBatch(@Nonnull Collection<? extends TInput> elements) {
        in.postBatch(elements);
    }

    @Override
    public void linkTo(TargetBlock<? super TOutput> target, LinkOptions options) {
        if (out == null) {
            throw new UnsupportedOperationException();
        }
        out.linkTo(target, options);
    }

    TargetBlock<TInput> getTargetBlock() {
        return in;
    }

    SourceBlock<TOutput> getSourceBlock() {
        return out;
    }

    Block[] getBlocks() {
        return blocks;
    }
}
