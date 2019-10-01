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
import java.util.StringJoiner;

/**
 * UserContext stores the user id, attributes and a reference to the current {@link ProjectConfig}.
 */
public class UserContext {
    private final ProjectConfig projectConfig;
    private final String userId;
    private final Map<String, ?> attributes;

    private UserContext(ProjectConfig projectConfig, String userId, Map<String, ?> attributes) {
        this.projectConfig = projectConfig;
        this.userId = userId;
        this.attributes = attributes;
    }

    public ProjectConfig getProjectConfig() {
        return projectConfig;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, ?> getAttributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserContext userContext = (UserContext) o;
        if(!userId.equals(userContext.getUserId())) return false;
        if(!projectConfig.equals(userContext.getProjectConfig())) return false;
        return attributes != null ? attributes.equals(userContext.attributes)
            : (userContext.getAttributes() == null || userContext.getAttributes().isEmpty()) ;
    }


    @Override
    public int hashCode() {
        int result = userId.hashCode();
        result = 31 * result + projectConfig.hashCode();
        return result;
    }

    public static class Builder {

        private ProjectConfig projectConfig;
        private String userId;
        private Map<String, ?> attributes;

        public Builder withProjectConfig(ProjectConfig projectConfig) {
            this.projectConfig = projectConfig;
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
            return new UserContext(projectConfig, userId, attributes);
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", UserContext.class.getSimpleName() + "[", "]")
            .add("projectConfig=" + projectConfig.getRevision())
            .add("userId='" + userId + "'")
            .add("attributes=" + attributes)
            .toString();
    }
}
