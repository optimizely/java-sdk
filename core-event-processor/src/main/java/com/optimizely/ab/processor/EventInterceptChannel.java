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
 * If a {@link EventInterceptor} returns null, no item will be emitted to the sink and
 * the EventInterceptors that follow it will not be invoked.
 *
 * Catches any {@link RuntimeException} thrown from an interceptor, logs it, then continues
 */
public class EventInterceptChannel<T> extends AbstractEventChannel<T, T> {
    public static final String logLabel = EventInterceptChannel.class.getSimpleName();

    private final List<EventInterceptor<T>> interceptors;

    public EventInterceptChannel(List<EventInterceptor<T>> interceptors, EventSink<T> sink) {
        super(sink);
        this.interceptors = Assert.notNull(interceptors, "interceptors");
    }

    @Override
    public void put(T item) {
        if (interceptors.isEmpty()) {
            emitItemIfPresent(item);
        }

        logger.trace("[{}] Invoking {} interceptors", logLabel, interceptors.size(), item);

        Iterator<EventInterceptor<T>> it = interceptors.iterator();
        while (item != null && it.hasNext()) {
            EventInterceptor<T> interceptor = it.next();

            try {
                T nextEvent = interceptor.intercept(item);

                if (nextEvent == null) {
                    logger.debug("[{}] Interceptor filtered event {}", logLabel, item);
                    return;
                }

                item = nextEvent;
            } catch (RuntimeException e) {
                logger.warn("[{}] Skipping interceptor", logLabel, e);
            }
        }

        emitItemIfPresent(item);
    }
}
