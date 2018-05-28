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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.faultinjection.ExceptionSpot;
import com.optimizely.ab.faultinjection.FaultInjectionManager;

import java.lang.reflect.Type;

final class ExperimentGsonDeserializer implements JsonDeserializer<Experiment> {

    @Override
    public Experiment deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
        //try {
            FaultInjectionManager.getInstance().injectFault(ExceptionSpot.ExperimentGsonDeserializer_deserialize_spot1);
            JsonObject jsonObject = json.getAsJsonObject();
            return GsonHelpers.parseExperiment(jsonObject, context);
        //} catch (Exception e) {
        //    FaultInjectionManager.getInstance().throwExceptionIfTreatmentDisabled();
        //    return null;
        //}
    }
}
