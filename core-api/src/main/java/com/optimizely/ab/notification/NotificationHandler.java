/**
 *
 *    Copyright 2019, Optimizely and contributors
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

/**
 * NotificationHandler is a generic interface Optimizely notification listeners.
 * This interface replaces {@link NotificationListener} which didn't provide adequate type safety.
 *
 * While this class adds generic handler implementations to be created, the domain of supported
 * implementations is maintained by the {@link NotificationCenter}
 */
public interface NotificationHandler<T extends Notification> {
    void handle(T message);
}
