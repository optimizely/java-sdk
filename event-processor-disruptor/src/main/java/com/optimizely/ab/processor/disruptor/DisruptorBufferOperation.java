package com.optimizely.ab.processor.disruptor;

import com.optimizely.ab.processor.EventOperation;
import com.optimizely.ab.processor.EventOperator;
import com.optimizely.ab.processor.EventSink;

import javax.annotation.Nonnull;

public class DisruptorBufferOperation<T> implements EventOperation<T, T> {
    @Nonnull
    @Override
    public EventOperator<T> create(@Nonnull EventSink<T> sink) {
        return null;
    }
}
