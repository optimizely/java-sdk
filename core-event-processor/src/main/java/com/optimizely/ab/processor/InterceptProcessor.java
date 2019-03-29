package com.optimizely.ab.processor;

import com.optimizely.ab.common.internal.Assert;

import java.util.Iterator;
import java.util.List;

/**
 * Performs filtering or replacement of input elements. Each of the {@code handlers} is synchronously invoked on every
 * input element in-order.
 *
 * Each {@link Handler} is invoked with the value returned from the previous EventInterceptor. The value
 * returned from the last EventInterceptor is emitted to the sink.
 *
 * If a {@link Handler} returns null, no element will be emitted to the sink and the EventInterceptors that
 * follow it will not be invoked.
 *
 * Catches any {@link RuntimeException} thrown from an interceptor, logs it, then continues
 */
public class InterceptProcessor<T> extends StageProcessor<T, T> {
    private static final String logLabel = InterceptProcessor.class.getSimpleName();

    private final List<Handler<T>> handlers;

    public InterceptProcessor(List<Handler<T>> handlers) {
        super();
        this.handlers = Assert.notNull(handlers, "handlers");
    }

    @Override
    public void process(T element) {
        if (handlers.isEmpty()) {
            emitElementIfPresent(element);
        }

        logger.trace("[{}] Invoking {} interceptors", logLabel, handlers.size(), element);

        Iterator<Handler<T>> it = handlers.iterator();
        while (element != null && it.hasNext()) {
            Handler<T> interceptor = it.next();

            try {
                T nextEvent = interceptor.intercept(element);

                if (nextEvent == null) {
                    logger.debug("[{}] Interceptor filtered event {}", logLabel, element);
                    return;
                }

                element = nextEvent;
            } catch (RuntimeException e) {
                logger.warn("[{}] Skipping interceptor that threw", logLabel, e);
            }
        }

        emitElementIfPresent(element);
    }

    /**
     * Intercepts events being processed; supports monitoring or filtering elements.
     *
     * @param <T> the type of input and output elements
     */
    @FunctionalInterface
    public interface Handler<T> {
        /**
         * Intercept element
         * @param element the input element
         * @return the element to pass to next step
         */
        T intercept(T element);
    }
}
