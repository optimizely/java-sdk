package com.optimizely.ab.processor;

import com.optimizely.ab.common.internal.Assert;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * A channel that performs a list of actions on inputs, presumably for side-effects.
 *
 * Catches any {@link RuntimeException} thrown from an action, logs it, then continues
 *
 * @param <T> the type of input items
 */
public class EventTransformChannel<T> extends AbstractEventChannel<T, T> {
    public static final String logLabel = EventTransformChannel.class.getSimpleName();

    private final List<EventTransformer<T>> actions;

    public EventTransformChannel(EventTransformer<T> action, EventSink<T> sink) {
        this(Collections.singletonList(Assert.notNull(action, "action")), sink);
    }

    public EventTransformChannel(List<EventTransformer<T>> actions, EventSink<T> sink) {
        super(sink);
        this.actions = Assert.notNull(actions, "actions");
    }

    @Override
    public void put(@Nonnull T item) {
        logger.trace("[{}] Invoking {} EventTransformer", logLabel, actions.size());

        for (final EventTransformer<T> action : actions) {
            try {
                action.transform(item);
            } catch (RuntimeException e) {
                logger.warn("[{}] Skipping EventTransformer", logLabel, e);
            }
        }

        emitItemIfPresent(item);
    }
}
