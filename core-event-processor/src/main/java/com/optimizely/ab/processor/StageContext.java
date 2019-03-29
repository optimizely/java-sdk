/**
 * Copyright 2019, Optimizely Inc. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.optimizely.ab.processor;

import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.common.lifecycle.LifecycleAware;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import java.util.List;

public abstract class StageContext implements LifecycleAware {
    enum State {
        START,
        STARTING,
        RUNNING,
        STOPPING,
        STOPPED,
        ERROR;
    }

    private Logger logger = NOPLogger.NOP_LOGGER;
    private State state = State.START;

    public synchronized void setState(State nextState) {
        Assert.argument(state.ordinal() <= nextState.ordinal(),
            String.format("Illegal state transition: %s -> %s", state, nextState));
        logger.debug("State transition: {} -> {}", state, nextState);
        this.state = nextState;
    }

    public State getState() {
        return state;
    }

    abstract public Object getProperty(String key);

    abstract public void setProperty(String key, Object value);

    abstract public List<Processor<?>> getProcessors();

    abstract public void handleError(Throwable e);

    abstract public List<ProcessingException> getProcessingExceptions();

    public Logger getLogger() {
        return logger;
    }

    protected void setLogger(Logger logger) {
        this.logger = Assert.notNull(logger, "logger");
    }
}
