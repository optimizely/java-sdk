/**
 *
 *    Copyright 2022, Optimizely and contributors
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
package com.optimizely.ab.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.concurrent.Immutable;

/**
 * Represents the Optimizely Integration configuration.
 *
 * @see <a href="http://developers.optimizely.com/server/reference/index.html#json">Project JSON</a>
 */
@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class Integration {
    private final String key;
    private final String host;
    private final String publicKey;

    @JsonCreator
    public Integration(@JsonProperty("key") String key,
                       @JsonProperty("host") String host,
                       @JsonProperty("publicKey") String publicKey) {
        this.key = key;
        this.host = host;
        this.publicKey = publicKey;
    }

    public String getKey() {
        return key;
    }

    public String getHost() { return host; }

    public String getPublicKey() { return publicKey; }

    @Override
    public String toString() {
        return "Integration{" +
            "key='" + key + '\'' +
            ", host='" + host + '\'' +
            ", publicKey='" + publicKey + '\'' +
            '}';
    }
}
