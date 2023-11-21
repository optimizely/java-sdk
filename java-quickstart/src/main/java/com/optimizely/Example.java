/****************************************************************************
 * Copyright 2018-2019, Optimizely, Inc. and contributors                   *
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
package com.optimizely;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.OptimizelyFactory;
import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.optimizelydecision.OptimizelyDecision;
import com.optimizely.ab.optimizelyjson.OptimizelyJSON;

import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Example {

    private final Optimizely optimizely;

    private Example(Optimizely optimizely) {
        this.optimizely = optimizely;
    }

    private void processVisitor(String userId, Map<String, Object> attributes) {
        OptimizelyUserContext user = optimizely.createUserContext(userId, attributes);

        OptimizelyDecision decision = user.decide("eet_feature");
        String variationKey = decision.getVariationKey();

        if (variationKey != null) {
            boolean enabled = decision.getEnabled();
            System.out.println("[Example] feature enabled: " + enabled);

            OptimizelyJSON variables = decision.getVariables();
            System.out.println("[Example] feature variables: " + variables.toString());

            user.trackEvent("eet_conversion");
        }
        else {
            System.out.println("[Example] decision failed: " + decision.getReasons().toString());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Optimizely optimizely = OptimizelyFactory.newDefaultInstance("BX9Y3bTa4YErpHZEMpAwHm");
        
        Example example = new Example(optimizely);
        Random random = new Random();

        for (int i = 0; i < 10; i++) {
            String userId = String.valueOf(random.nextInt(Integer.MAX_VALUE));
            example.processVisitor(userId, Collections.emptyMap());
            TimeUnit.MILLISECONDS.sleep(500);
        }
    }
}
