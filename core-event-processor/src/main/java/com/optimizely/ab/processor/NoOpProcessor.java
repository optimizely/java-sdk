package com.optimizely.ab.processor;

import javax.annotation.Nonnull;
import java.util.Collection;

public class NoOpProcessor<T> extends AbstractProcessor<T, T> {
    public NoOpProcessor(Processor<? super T> sink) {
        super(sink);
    }

    @Override
    public void process(@Nonnull T element) {
        emitElement(element);
    }

    @Override
    public void processBatch(Collection<? extends T> elements) {
        emitBatch(elements);
    }
}
