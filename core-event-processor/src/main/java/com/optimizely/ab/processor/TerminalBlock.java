package com.optimizely.ab.processor;

import java.util.function.Supplier;

/**
 * Represents a {@link Block} with value.
 *
 * @param <TInput> the type of
 * @param <TValue> the type of value held by block
 */
public interface TerminalBlock<TInput, TValue> extends Block, TargetBlock<TInput>, Supplier<TValue> {
    /**
     * Clears any state held by block
     */
    void reset();
}
