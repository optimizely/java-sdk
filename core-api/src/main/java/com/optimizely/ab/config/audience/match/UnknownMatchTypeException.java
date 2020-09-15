/**
 *
 *    Copyright 2019-2020, Optimizely and contributors
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

package com.optimizely.ab.config.audience.match;

/**
 * UnknownMatchTypeException is thrown when the specified match type cannot be mapped via the MatchRegistry.
 */
public class UnknownMatchTypeException extends Exception {
    private static String message = "uses an unknown match type. You may need to upgrade to a newer release of the Optimizely SDK.";

    public UnknownMatchTypeException() {
        super(message);
    }
}
