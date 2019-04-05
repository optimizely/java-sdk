package com.optimizely.ab.processor;

import com.optimizely.ab.common.internal.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

/**
 * This class represents processing pipeline as a list of {@link Block}.
 * Manages lifecycle and forwards to a {@link TargetBlock}
 */
public class Processor<TInput> implements TargetBlock<TInput> {
    public static final Logger logger = LoggerFactory.getLogger(Processor.class);

    private final TargetBlock<TInput> target;
    private final List<Block> blocks;

    public Processor(
        TargetBlock<TInput> target,
        List<Block> blocks
    ) {
        this.target = Assert.notNull(target, "target");
        this.blocks = blocks;
    }

    @Override
    public void onStart() {
        logger.debug("Starting...");
        ListIterator<Block> it = blocks.listIterator(blocks.size());
        while (it.hasPrevious()) {
            it.previous().onStart();
        }
        logger.debug("Started");
    }

    @Override
    public boolean onStop(long timeout, TimeUnit unit) {
        logger.debug("Stopping");
        for (final Block block : blocks) {
            try {
                block.onStop(timeout, unit);
            } catch (RuntimeException e) {
                logger.warn("Error while stopping {}", block, e);
            }
        }
        logger.debug("Stopped");
        return true;
    }

    @Override
    public void post(@Nonnull TInput element) {
        target.post(element);
    }

    @Override
    public void postBatch(@Nonnull Collection<? extends TInput> elements) {
        target.postBatch(elements);
    }

    @Override
    public void postNullable(TInput element) {
        target.postNullable(element);
    }
}
