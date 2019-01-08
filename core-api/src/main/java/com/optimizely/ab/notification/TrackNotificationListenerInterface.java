/**
 *
 *    Copyright 2018-2019, Optimizely and contributors
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
package com.optimizely.ab.notification;

import com.optimizely.ab.event.LogEvent;

import javax.annotation.Nonnull;
import java.util.Map;

public interface TrackNotificationListenerInterface {
    /**
     * onTrack is called when a track event is triggered
     *
     * @param eventKey   - The event key that was triggered.
     * @param userId     - user id passed into track.
     * @param attributes - filtered attributes list after passed into track
     * @param eventTags  - event tags if any were passed in.
     * @param event      - The event being recorded.
     */
    public void onTrack(@Nonnull String eventKey,
                        @Nonnull String userId,
                        @Nonnull Map<String, ?> attributes,
                        @Nonnull Map<String, ?> eventTags,
                        @Nonnull LogEvent event);

}
