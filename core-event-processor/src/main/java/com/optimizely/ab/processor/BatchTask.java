/**
 *    Copyright 2019, Optimizely Inc. and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab.processor;

import com.optimizely.ab.common.internal.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A {@link Runnable} that holds a temporary buffer for batching. Exposes thread-safe
 * methods to produce into the {@link Collection} buffer while it is open.
 *
 * The buffer can be closed by any of the following conditions:
 * <ul>
 *   <li>Reached maximum size, if configured</li>
 *   <li>Reached maximum age, if configured (relative to time first element was added)</li>
 *   <li>Buffer has been explicitly flushed</li>
 * </ul>
 *
 * Additionally, the buffer will be closed if thread is interrupted.
 *
 * Once the buffer is closed, it will be flushed to the configured {@link Consumer}.
 * The buffer is always flushed from the thread executing {@link #run()}.
 *
 * @param <E> the type of elements in buffer
 * @param <C> the type of {@link Collection} used for buffer
 */
class BatchTask<E, C extends Collection<? super E>> implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(BatchTask.class);

    private final Supplier<C> bufferSupplier;
    private final Consumer<C> bufferConsumer;
    private final int maxSize;
    private final Duration maxAge;
    private final Lock lock;
    private final Condition condition;

    // The following fields should only be modified when lock is held
    private C buffer;
    private Date deadline;
    private volatile Status status = Status.OPEN;

    /**
     * State of the buffer
     */
    protected enum Status {
        /**
         * Accepting new items
         */
        OPEN(false),
        /**
         * No longer accepting new items (due to size or manual flush), but has not flushed yet (a non-terminal state)
         */
        CLOSED(false),
        /**
         * Terminal state; batch has been flushed.
         */
        COMPLETE(true);

        private final boolean terminal;

        Status(boolean terminal) {
            this.terminal = terminal;
        }

        public boolean isTerminal() {
            return terminal;
        }
    }

    /**
     * @param bufferSupplier supplies a fresh buffer on-demand (on first item)
     * @param bufferConsumer handles buffer being flushed
     * @param maxSize maximum size of an open buffer
     * @param maxAge
     */
    BatchTask(
        Supplier<C> bufferSupplier,
        Consumer<C> bufferConsumer,
        Integer maxSize,
        Duration maxAge
    ) {
        this(bufferSupplier, bufferConsumer, maxSize, maxAge, new ReentrantLock());
    }

    protected BatchTask(
        Supplier<C> bufferSupplier,
        Consumer<C> bufferConsumer,
        Integer maxSize,
        Duration maxAge,
        Lock lock
    ) {
        this.bufferSupplier = Assert.notNull(bufferSupplier, "bufferSupplier");
        this.bufferConsumer = bufferConsumer;
        this.maxSize = (maxSize != null && maxSize > 0) ? maxSize : Integer.MAX_VALUE;
        this.maxAge = (maxAge != null && !maxAge.isNegative() && !maxAge.isZero()) ? maxAge : null;
        this.lock = Assert.notNull(lock, "lock");
        this.condition = lock.newCondition();
    }

    /**
     * Waits until buffer is marked closed, thread is interrupted, or the configured deadline elapses, then invokes the
     * consumer with buffer contents.
     */
    @Override
    public void run() {
        lock.lock();
        try {
            // note: buffer can be closed
            Assert.state(!status.isTerminal(), "Task has already been run");

            while (status == Status.OPEN && !Thread.currentThread().isInterrupted()) {
                // deadline is not set until first insert (and maxAge non-null)
                if (deadline != null) {
                    // wait until deadline reached or closed by another thread
                    if (!condition.awaitUntil(deadline)) {
                        break;
                    }
                } else {
                    // unbounded wait until first item is added
                    condition.await();
                }
            }
        } catch (InterruptedException e) {
            logger.warn("Flushing buffer contents due to interrupt", e);
        } finally {
            lock.unlock();
        }

        status = Status.COMPLETE;

        if (bufferConsumer != null) {
            bufferConsumer.accept(buffer);
        }
    }

    /**
     * Inserts the specified element into this buffer if the buffer is still open.
     *
     * @param element object to add to buffer
     * @return true if element was inserted, otherwise false
     */
    public boolean offer(E element) {
        lock.lock();
        try {
            if (status != Status.OPEN) {
                return false;
            }

            boolean notify = false; // should threads waiting on lock be woken up?

            // initialize buffer on first element
            if (buffer == null) {
                buffer = bufferSupplier.get();

                // set the deadline when first added and signal to pick it up
                if (maxAge != null) {
                    // limited benefit to using Clock here since we are using awaitUntil
                    long now = System.currentTimeMillis();

                    deadline = new Date(now + maxAge.toMillis());

                    notify = true;
                }

                logger.debug("Initialized buffer with deadline: {}", deadline);
            }

            buffer.add(element);

            if (buffer.size() >= maxSize) {
                logger.debug("Closing buffer (reached max size)");
                status = Status.CLOSED;
                notify = true;
            }

            if (notify) {
                condition.signal();
            }
        } finally {
            lock.unlock();
        }

        return true;
    }

    /**
     * Marks the buffer as closed and signals for it to be flushed to consumer on buffering thread. The buffer will no
     * longer accept new elements after this method is invoked.
     */
    public void close() {
        lock.lock();
        try {
            if (status != Status.OPEN) {
                logger.trace("Buffer is not open");
                return;
            }

            status = Status.CLOSED;
            condition.signal();
            logger.trace("Buffer is now closed");
        } finally {
            lock.unlock();
        }
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }
}
