package com.optimizely.ab.processor;

import java.util.function.Function;

/**
 * @param <T> the type of input elements
 * @param <R> the type of output elements
 */
public interface Stage<T, R> extends Processor<T>, Source<R> {
    static <T, R> Stage<T, R> of(Function<? super T, ? extends R> function) {
        return new FunctionProcessor<>(function);
    }
}
