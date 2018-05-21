/**
 *
 *    Copyright 2016-2017, Optimizely and contributors
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

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Group;
import com.optimizely.ab.config.TrafficAllocation;
import com.optimizely.ab.faultinjection.ExceptionSpot;
import com.optimizely.ab.faultinjection.FaultInjectionManager;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GroupGsonDeserializer implements JsonDeserializer<Group> {

    private static void injectFault(ExceptionSpot spot) {
        FaultInjectionManager.getInstance().injectFault(spot);
    }

    @Override
    public Group deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {

        injectFault(ExceptionSpot.GroupGsonDeserializer_deserialize_spot1);
        JsonObject jsonObject = json.getAsJsonObject();

        String id = jsonObject.get("id").getAsString();
        String policy = jsonObject.get("policy").getAsString();

        List<Experiment> experiments = new ArrayList<Experiment>();
        JsonArray experimentsJson = jsonObject.getAsJsonArray("experiments");
        for (Object obj : experimentsJson) {
            injectFault(ExceptionSpot.GroupGsonDeserializer_deserialize_spot2);
            JsonObject experimentObj = (JsonObject)obj;
            experiments.add(GsonHelpers.parseExperiment(experimentObj, id, context));
        }

        List<TrafficAllocation> trafficAllocations =
                GsonHelpers.parseTrafficAllocation(jsonObject.getAsJsonArray("trafficAllocation"));

        injectFault(ExceptionSpot.GroupGsonDeserializer_deserialize_spot3);
        return new Group(id, policy, experiments, trafficAllocations);
    }
}
