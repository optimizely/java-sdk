/****************************************************************************
 * Copyright 2019, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/

package com.optimizely.ab.notification.decisionInfo;


import java.util.Map;

public class DecisionNotification {
    protected String type;
    protected String userId;
    protected Map<String, ?> attributes;
    protected Map<String, ?> decisionInfo;

    protected DecisionNotification() {
    }

    protected DecisionNotification(String type,
                                   String userId,
                                   Map<String, ?> attributes,
                                   Map<String, ?> decisionInfo) {
        this.type = type;
        this.userId = userId;
        this.attributes = attributes;
        this.decisionInfo = decisionInfo;
    }

    public String getType() {
        return type;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, ?> getAttributes() {
        return attributes;
    }

    public Map<String, ?> getDecisionInfo() {
        return decisionInfo;
    }
}
