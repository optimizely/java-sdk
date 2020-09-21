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
import com.optimizely.ab.UserContext;
import java.util.Map;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Example {

    private static void processVisitor(UserContext userContext) {
        Map<String, Object> optimizelyJSON = userContext.decide("background_experiment").toMap();

        if (optimizelyJSON.get("variation") != null) {
            userContext.track("sample_conversion");
            System.out.printf("Found variation %s%n", optimizelyJSON.get("variation"));
        }
        else {
            System.out.println("didn't get a variation");
        }

        if (userContext.decide("eet_feature") != null) {
            userContext.track("eet_conversion");
            System.out.println("feature enabled");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Optimizely optimizely = OptimizelyFactory.newDefaultInstance("BX9Y3bTa4YErpHZEMpAwHm");

        Random random = new Random();

        for (int i = 0; i < 10; i++) {
            String userId = String.valueOf(random.nextInt());
            UserContext userContext = optimizely.CreateUserContext(userId);
            processVisitor(userContext);
            TimeUnit.MILLISECONDS.sleep(500);
        }
    }
}
