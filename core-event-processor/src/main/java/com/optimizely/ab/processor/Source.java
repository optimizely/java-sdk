package com.optimizely.ab.processor;

public interface Source<T> {
    void configure(Processor<? super T> sink);
}
