/**
 *
 *    Copyright 2021, Optimizely and contributors
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

import com.optimizely.ab.config.audience.*;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;

public class ExperimentTest {

    @Test
    public void testStringifyConditionScenarios() {
        List<Condition> audienceConditionsScenarios = getAudienceConditionsList();
        Map<Integer, String> expectedScenarioStringsMap = getExpectedScenariosMap();
        Map<String, String> audiencesMap = new HashMap<>();
        audiencesMap.put("1", "us");
        audiencesMap.put("2", "female");
        audiencesMap.put("3", "adult");
        audiencesMap.put("11", "fr");
        audiencesMap.put("12", "male");
        audiencesMap.put("13", "kid");

        if (expectedScenarioStringsMap.size() == audienceConditionsScenarios.size()) {
            for (int i = 0; i < audienceConditionsScenarios.size() - 1; i++) {
                Experiment experiment = makeMockExperimentWithStatus(Experiment.ExperimentStatus.RUNNING,
                    audienceConditionsScenarios.get(i));
                String audiences = experiment.serializeConditions(audiencesMap);
                assertEquals(audiences, expectedScenarioStringsMap.get(i+1));
            }
        }

    }

    public Map<Integer, String> getExpectedScenariosMap() {
        Map<Integer, String> expectedScenarioStringsMap = new HashMap<>();
        expectedScenarioStringsMap.put(1, "");
        expectedScenarioStringsMap.put(2, "\"us\" OR \"female\"");
        expectedScenarioStringsMap.put(3, "\"us\" AND \"female\" AND \"adult\"");
        expectedScenarioStringsMap.put(4, "NOT \"us\"");
        expectedScenarioStringsMap.put(5, "\"us\"");
        expectedScenarioStringsMap.put(6, "\"us\"");
        expectedScenarioStringsMap.put(7, "\"us\"");
        expectedScenarioStringsMap.put(8, "\"us\" OR \"female\"");
        expectedScenarioStringsMap.put(9, "(\"us\" OR \"female\") AND \"adult\"");
        expectedScenarioStringsMap.put(10, "(\"us\" OR (\"female\" AND \"adult\")) AND (\"fr\" AND (\"male\" OR \"kid\"))");
        expectedScenarioStringsMap.put(11, "NOT (\"us\" AND \"female\")");
        expectedScenarioStringsMap.put(12, "\"us\" OR \"100000\"");
        expectedScenarioStringsMap.put(13, "");

        return expectedScenarioStringsMap;
    }

    public List<Condition> getAudienceConditionsList() {
        AudienceIdCondition one = new AudienceIdCondition("1");
        AudienceIdCondition two = new AudienceIdCondition("2");
        AudienceIdCondition three = new AudienceIdCondition("3");
        AudienceIdCondition eleven = new AudienceIdCondition("11");
        AudienceIdCondition twelve = new AudienceIdCondition("12");
        AudienceIdCondition thirteen = new AudienceIdCondition("13");

        // Scenario 1 - []
        EmptyCondition scenario1 = new EmptyCondition();

        // Scenario 2 - ["or", "1", "2"]
        List<Condition> scenario2List = new ArrayList<>();
        scenario2List.add(one);
        scenario2List.add(two);
        OrCondition scenario2 = new OrCondition(scenario2List);

        // Scenario 3 - ["and", "1", "2", "3"]
        List<Condition> scenario3List = new ArrayList<>();
        scenario3List.add(one);
        scenario3List.add(two);
        scenario3List.add(three);
        AndCondition scenario3 = new AndCondition(scenario3List);

        // Scenario 4 - ["not", "1"]
        NotCondition scenario4 = new NotCondition(one);

        // Scenario 5 - ["or", "1"]
        List<Condition> scenario5List = new ArrayList<>();
        scenario5List.add(one);
        OrCondition scenario5 = new OrCondition(scenario5List);

        // Scenario 6 - ["and", "1"]
        List<Condition> scenario6List = new ArrayList<>();
        scenario6List.add(one);
        AndCondition scenario6 = new AndCondition(scenario6List);

        // Scenario 7 - ["1"]
        AudienceIdCondition scenario7 = one;

        // Scenario 8 - ["1", "2"]
        // Defaults to Or in Datafile Parsing resulting in an OrCondition
        // Same as Scenario 2

        OrCondition scenario8 = scenario2;

        // Scenario 9 - ["and", ["or", "1", "2"], "3"]
        List<Condition> Scenario9List = new ArrayList<>();
        Scenario9List.add(scenario2);
        Scenario9List.add(three);
        AndCondition scenario9 = new AndCondition(Scenario9List);

        // Scenario 10 - ["and", ["or", "1", ["and", "2", "3"]], ["and", "11, ["or", "12", "13"]]]
        List<Condition> scenario10List = new ArrayList<>();

        List<Condition> or1213List = new ArrayList<>();
        or1213List.add(twelve);
        or1213List.add(thirteen);
        OrCondition or1213 = new OrCondition(or1213List);

        List<Condition> and11Or1213List = new ArrayList<>();
        and11Or1213List.add(eleven);
        and11Or1213List.add(or1213);
        AndCondition and11Or1213 = new AndCondition(and11Or1213List);

        List<Condition> and23List = new ArrayList<>();
        and23List.add(two);
        and23List.add(three);
        AndCondition and23 = new AndCondition(and23List);

        List<Condition> or1And23List = new ArrayList<>();
        or1And23List.add(one);
        or1And23List.add(and23);
        OrCondition or1And23 = new OrCondition(or1And23List);

        scenario10List.add(or1And23);
        scenario10List.add(and11Or1213);
        AndCondition scenario10 = new AndCondition(scenario10List);

        // Scenario 11 - ["not", ["and", "1", "2"]]
        List<Condition> and12List = new ArrayList<>();
        and12List.add(one);
        and12List.add(two);
        AndCondition and12 = new AndCondition(and12List);

        NotCondition scenario11 = new NotCondition(and12);

        // Scenario 12 - ["or", "1", "100000"]
        List<Condition> scenario12List = new ArrayList<>();
        scenario12List.add(one);
        AudienceIdCondition unknownAudience = new AudienceIdCondition("100000");
        scenario12List.add(unknownAudience);

        OrCondition scenario12 = new OrCondition(scenario12List);

        // Scenario 13 - Empty String "" already accounted for in Datafile parsing
        AudienceIdCondition invalidAudience = new AudienceIdCondition("5");
        List<Condition> invalidIdList = new ArrayList<>();
        invalidIdList.add(invalidAudience);
        AndCondition andCondition = new AndCondition(invalidIdList);
        List<Condition> andInvalidAudienceId = new ArrayList<>();
        andInvalidAudienceId.add(andCondition);
        AndCondition scenario13 = new AndCondition(andInvalidAudienceId);


        List<Condition> conditionTestScenarios = new ArrayList<>();
        conditionTestScenarios.add(scenario1);
        conditionTestScenarios.add(scenario2);
        conditionTestScenarios.add(scenario3);
        conditionTestScenarios.add(scenario4);
        conditionTestScenarios.add(scenario5);
        conditionTestScenarios.add(scenario6);
        conditionTestScenarios.add(scenario7);
        conditionTestScenarios.add(scenario8);
        conditionTestScenarios.add(scenario9);
        conditionTestScenarios.add(scenario10);
        conditionTestScenarios.add(scenario11);
        conditionTestScenarios.add(scenario12);
        conditionTestScenarios.add(scenario13);

        return conditionTestScenarios;
    }

    private Experiment makeMockExperimentWithStatus(Experiment.ExperimentStatus status, Condition audienceConditions) {
        return new Experiment("12345",
            "mockExperimentKey",
            status.toString(),
            "layerId",
            Collections.<String>emptyList(),
            audienceConditions,
            Collections.<Variation>emptyList(),
            Collections.<String, String>emptyMap(),
            Collections.<TrafficAllocation>emptyList()
        );
    }
}
