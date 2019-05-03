/****************************************************************************
 * Copyright 2016-2019, Optimizely, Inc. and contributors                   *
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
import com.optimizely.ab.config.HttpProjectConfigManager;
import com.optimizely.ab.event.AsyncEventHandler;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.config.Variation;

import java.util.Collections;
import java.util.Map;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Example {

    private final Optimizely optimizely;

    private Example(Optimizely optimizely) {
        this.optimizely = optimizely;
    }

    private void processVisitor(String userId, Map<String, String> attributes) {
        Variation variation = optimizely.activate("background_experiment", userId, attributes);

        if (variation != null) {
            optimizely.track("sample_conversion", userId, attributes);
            System.out.println(String.format("Found variation %s", variation.getKey()));
        }
        else {
            System.out.println("didn't get a variation");
        }

        if (optimizely.isFeatureEnabled("eet_feature", userId, attributes)) {
            optimizely.track("eet_conversion", userId, attributes);
            System.out.println("feature enabled");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        EventHandler eventHandler = AsyncEventHandler.builder()
            .withQueueCapacity(100)
            .withNumWorkers(2)
            .build();

        HttpProjectConfigManager projectConfigManager = HttpProjectConfigManager.builder()
            .withSdkKey("BX9Y3bTa4YErpHZEMpAwHm")
            .withPollingInterval(1, TimeUnit.SECONDS)
            .build(); // Block here since no default was provided.

        Optimizely optimizely = Optimizely.builder()
            .withConfigManager(projectConfigManager)
            .withEventHandler(eventHandler)
            .build();

        Example example = new Example(optimizely);
        Random random = new Random();

        for (int i = 0; i < 10; i++) {
            String userId = String.valueOf(random.nextInt());
            example.processVisitor(userId, Collections.emptyMap());
            TimeUnit.MILLISECONDS.sleep(500);
        }
    }
}
