/**
 *    Copyright 2022, Optimizely Inc. and contributors
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
package com.optimizely.ab.internal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import static org.junit.Assert.fail;

/**
 * TODO As a usability improvement we should require expected messages be added after the message are expected to be
 * logged. This will allow us to map the failure immediately back to the test line number as opposed to the async
 * validation now that happens at the end of each individual test.
 *
 * From http://techblog.kenshoo.com/2013/08/junit-rule-for-verifying-logback-logging.html
 */
public class LogbackVerifier implements TestRule {

    private List<ExpectedLogEvent> expectedEvents = new LinkedList<ExpectedLogEvent>();

    private CaptureAppender appender;

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before();
                try {
                    base.evaluate();
                    verify();
                } finally {
                    after();
                }
            }
        };
    }

    public void expectMessage(Level level) {
        expectMessage(level, "");
    }

    public void expectMessage(Level level, String msg) {
        expectMessage(level, msg, (Class<? extends Throwable>) null);
    }

    public void expectMessage(Level level, String msg, Class<? extends Throwable> throwableClass) {
        expectMessage(level, msg, null, 1);
    }

    public void expectMessage(Level level, String msg, int times) {
        expectMessage(level, msg, null, times);
    }

    public void expectMessage(Level level,
                              String msg,
                              Class<? extends Throwable> throwableClass,
                              int times) {
        for (int i = 0; i < times; i++) {
            expectedEvents.add(new ExpectedLogEvent(level, msg, throwableClass));
        }
    }

    private void before() {
        appender = new CaptureAppender();
        appender.setName("MOCK");
        appender.start();
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).addAppender(appender);
    }

    private void verify() throws Throwable {
        ListIterator<ILoggingEvent> actualIterator = appender.getEvents().listIterator();

        for (final ExpectedLogEvent expectedEvent : expectedEvents) {
            boolean found = false;
            while (actualIterator.hasNext()) {
                ILoggingEvent actual = actualIterator.next();

                if (expectedEvent.matches(actual)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
              fail(expectedEvent.toString());
            }
        }
    }

    private void after() {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).detachAppender(appender);
    }

    private static class CaptureAppender extends AppenderBase<ILoggingEvent> {

        List<ILoggingEvent> actualLoggingEvent = new LinkedList<>();

        @Override
        protected void append(ILoggingEvent eventObject) {
            actualLoggingEvent.add(eventObject);
        }

        public List<ILoggingEvent> getEvents() {
            return actualLoggingEvent;
        }
    }

    private final static class ExpectedLogEvent {
        private final String message;
        private final Level level;
        private final Class<? extends Throwable> throwableClass;

        private ExpectedLogEvent(Level level,
                                 String message,
                                 Class<? extends Throwable> throwableClass) {
            this.message = message;
            this.level = level;
            this.throwableClass = throwableClass;
        }

        private boolean matches(ILoggingEvent actual) {
            boolean match = actual.getFormattedMessage().contains(message);
            match &= actual.getLevel().equals(level);
            match &= matchThrowables(actual);
            return match;
        }

        private boolean matchThrowables(ILoggingEvent actual) {
            IThrowableProxy eventProxy = actual.getThrowableProxy();
            return throwableClass == null || eventProxy != null && throwableClass.getName().equals(eventProxy.getClassName());
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("ExpectedLogEvent{");
            sb.append("level=").append(level);
            sb.append(", message='").append(message).append('\'');
            sb.append(", throwableClass=").append(throwableClass);
            sb.append('}');
            return sb.toString();
        }
    }
}
