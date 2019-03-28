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
package com.optimizely.util.time;

import javax.annotation.Nonnull;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import java.util.Objects;

public class MutableClock extends Clock {

    private Instant instant;
    private ZoneId zone;

    public MutableClock() {
        this(Instant.now(), ZoneId.systemDefault());
    }

    public MutableClock(@Nonnull Instant instant) {
        this(instant, ZoneId.systemDefault());
    }

    public MutableClock(@Nonnull ZoneId zone) {
        this(Instant.now(), zone);
    }

    public MutableClock(@Nonnull Instant instant, @Nonnull ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    @Override @Nonnull public Clock withZone(@Nonnull ZoneId zone) {
        return new MutableClock(instant, zone);
    }

    @Override @Nonnull public ZoneId getZone() {
        return zone;
    }

    @Override @Nonnull public Instant instant() {
        return instant;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Clock)) return false;
        Clock that = (Clock) o;
        return Objects.equals(instant, that.instant()) &&
            Objects.equals(zone, that.getZone());
    }

    @Override public int hashCode() {
        return Objects.hash(super.hashCode(), instant, zone);
    }

    @Override
    public String toString() {
        return String.format("MutableClock[%s,%s]", instant, zone);
    }

    public void advanceBy(@Nonnull TemporalAmount amount) {
        instant = instant.plus(amount);
    }

    public void instant(@Nonnull Instant newInstant) {
        instant = newInstant;
    }

    public Clock toFixed() {
        return Clock.fixed(instant, zone);
    }
}
