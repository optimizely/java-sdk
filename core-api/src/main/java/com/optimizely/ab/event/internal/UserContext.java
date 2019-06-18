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
package com.optimizely.ab.event.internal;

import com.optimizely.ab.config.ProjectConfig;

import java.util.Map;

/**
 * UserContext stores the user and project context timestamp and global id.
 *
 * Alt name EventContext?
 */
public class UserContext {
    private final ProjectConfig projectConfig;
    private final String UUID;
    private final long timestamp;
    private final String userId;
    private final Map<String, ?> attributes;

    private UserContext(ProjectConfig projectConfig, String UUID, long timestamp, String userId, Map<String, ?> attributes) {
        this.projectConfig = projectConfig;
        this.UUID = UUID;
        this.timestamp = timestamp;
        this.userId = userId;
        this.attributes = attributes;
    }

    public ProjectConfig getProjectConfig() {
        return projectConfig;
    }

    public String getUUID() {
        return UUID;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, ?> getAttributes() {
        return attributes;
    }

    public static class Builder {

        private ProjectConfig projectConfig;
        private String uuid;
        private long timestamp;
        private String userId;
        private Map<String, ?> attributes;

        public Builder withProjectConfig(ProjectConfig projectConfig) {
            this.projectConfig = projectConfig;
            return this;
        }

        public Builder withUUID(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder withTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder withAttributes(Map<String, ?> attributes) {
            this.attributes = attributes;
            return this;
        }

        public UserContext build() {
            return new UserContext(projectConfig, uuid, timestamp, userId, attributes);
        }
    }
}
