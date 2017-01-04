/**
 *
 *    Copyright 2016, Optimizely and contributors
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
package com.optimizely.ab.config.parser;

import com.optimizely.ab.config.ProjectConfig;

import javax.annotation.Nonnull;

/**
 * Config parser wrapper to allow multiple library implementations to be used.
 *
 * @see GsonConfigParser
 * @see JacksonConfigParser
 * @see JsonConfigParser
 * @see JsonConfigParser
 *
 * @see <a href="http://developers.optimizely.com/server/reference/index.html#json">Project JSON</a>
 */
public interface ConfigParser {

    /**
     * @param json the json to parse
     * @return generates a {@code ProjectConfig} configuration from the provided json
     * @throws ConfigParseException when there's an issue parsing the provided project config
     */
    ProjectConfig parseProjectConfig(@Nonnull String json) throws ConfigParseException;
}
