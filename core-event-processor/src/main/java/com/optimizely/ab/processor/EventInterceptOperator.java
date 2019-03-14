package com.optimizely.ab.processor;

import com.optimizely.ab.common.internal.Assert;

import java.util.Iterator;
import java.util.List;

/**
 * A channel that invokes a list of {@link EventInterceptor} for filtering/replacing behavior.
 *
 * Each {@link EventInterceptor} is invoked with the value returned from the previous
 * EventInterceptor. The value returned from the last EventInterceptor is emitted to the sink.
 *
 * If a {@link EventInterceptor} returns null, no element will be emitted to the sink and
 * the EventInterceptors that follow it will not be invoked.
 *
 * Catches any {@link RuntimeException} thrown from an interceptor, logs it, then continues
 */
public class EventInterceptOperator<T> extends BaseEventOperator<T, T> {
    public static final String logLabel = EventInterceptOperator.class.getSimpleName();

    private final List<EventInterceptor<T>> interceptors;

    public EventInterceptOperator(List<EventInterceptor<T>> interceptors, EventSink<T> sink) {
        super(sink);
        this.interceptors = Assert.notNull(interceptors, "interceptors");
    }

    @Override
    public void send(T element) {
        if (interceptors.isEmpty()) {
            emitElementIfPresent(element);
        }

        logger.trace("[{}] Invoking {} interceptors", logLabel, interceptors.size(), element);

        Iterator<EventInterceptor<T>> it = interceptors.iterator();
        while (element != null && it.hasNext()) {
            EventInterceptor<T> interceptor = it.next();

            try {
                T nextEvent = interceptor.intercept(element);

                if (nextEvent == null) {
                    logger.debug("[{}] Interceptor filtered event {}", logLabel, element);
                    return;
                }

                element = nextEvent;
            } catch (RuntimeException e) {
                logger.warn("[{}] Skipping interceptor", logLabel, e);
            }
        }

        emitElementIfPresent(element);
    }
}
