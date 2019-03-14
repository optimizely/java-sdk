package com.optimizely.ab.common.callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractSequentialList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * An aggregate collection of {@link Callback}s that get called in sequence.
 *
 * Delegates methods of {@link Callback} interface sequentially to each of the configured callbacks.
 *
 * By default, {@link RuntimeException}s thrown by callbacks are trapped; they are logged it using
 * WARN level, then callback chain continues.
 *
 * @param <T> the type of value received by callbacks
 */
public class AggregateCallback<T> extends AbstractSequentialList<Callback<T>> implements Callback<T> {
    private static final Logger logger = LoggerFactory.getLogger(AggregateCallback.class);

    private final List<Callback<T>> callbacks;

    public AggregateCallback() {
        this.callbacks = new LinkedList<>();
    }

    protected AggregateCallback(List<Callback<? super T>> callbacks) {
        this();
        for (final Callback<? super T> callback : callbacks) {
            if (callback instanceof AggregateCallback) {
                //noinspection unchecked
                callbacks.addAll(((AggregateCallback) callback).callbacks);
            } else if (callback != null) {
                callbacks.add(callback);
            }
        }
    }

    @Override
    public final void success(T input) {
        if (callbacks.isEmpty()) {
            logger.trace("No callbacks to invoke");
            return;
        }

        logger.trace("Invoking 'success' on {} callbacks", callbacks.size());
        for (final Callback<T> callback : callbacks) {
            try {
                callback.success(input);
            } catch (RuntimeException e) {
                handleCallbackException(callback, e);
            }
        }
    }

    @Override
    public final void failure(T input, Throwable error) {
        if (callbacks.isEmpty()) {
            logger.trace("No callbacks to invoke");
            return;
        }

        logger.trace("Invoking 'failure' on {} callbacks", callbacks.size());
        for (final Callback<T> callback : callbacks) {
            try {
                callback.failure(input, error);
            } catch (RuntimeException e) {
                // Respect a callback that re-throws.
                if (e.equals(error)) {
                    throw e;
                }

                handleCallbackException(callback, e);
            }
        }
    }

    /**
     * Overridable method to handle exception thrown by callback
     *
     * @param culprit the callback that threw
     * @param ex the thrown exception
     * @return true to proceed calling subsequent callbacks (if any), otherwise false
     */
    protected boolean handleCallbackException(Callback<T> culprit, RuntimeException ex) {
        logger.warn("Ignoring exception thrown from callback", ex);
        return true;
    }

    @Override
    public ListIterator<Callback<T>> listIterator(int index) {
        return callbacks.listIterator(index);
    }

    @Override
    public int size() {
        return callbacks.size();
    }
}
