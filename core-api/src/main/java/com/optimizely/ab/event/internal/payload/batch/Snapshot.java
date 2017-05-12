/*
 * Copyright 2016-2017, Optimizely and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.optimizely.ab.event.internal.payload.batch;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class Snapshot {

    @Nonnull private List<Decision> decisions = new ArrayList<Decision>();
    @Nonnull private List<Event> events = new ArrayList<Event>();

    public Snapshot(@Nonnull List<Decision> decisions, @Nonnull List<Event> events) {

        this.decisions = decisions;
        this.events = events;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Snapshot snapshot = (Snapshot) o;

        if (!decisions.equals(snapshot.decisions)) return false;
        return events.equals(snapshot.events);
    }

    @Override
    public int hashCode() {
        int result = decisions.hashCode();
        result = 31 * result + events.hashCode();
        return result;
    }

    @Nonnull
    public List<Decision> getDecisions() {
        return decisions;
    }

    public void setDecisions(@Nonnull List<Decision> decisions) {
        this.decisions = decisions;
    }

    @Nonnull
    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(@Nonnull List<Event> events) {
        this.events = events;
    }


}
