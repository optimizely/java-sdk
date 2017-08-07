/**
 *
 *    Copyright 2017, Optimizely and contributors
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

import com.optimizely.ab.config.audience.AndCondition;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.OrCondition;
import com.optimizely.ab.config.audience.UserAttribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidProjectConfigV4 {

    // simple properties
    private static final String     ACCOUNT_ID = "2360254204";
    private static final boolean    ANONYMIZE_IP = true;
    private static final String     PROJECT_ID = "3918735994";
    private static final String     REVISION = "1480511547";
    private static final String     VERSION = "4";

    // attributes
    private static final String     ATTRIBUTE_HOUSE_ID= "553339214";
    public  static final String     ATTRIBUTE_HOUSE_KEY = "house";
    private static final Attribute  ATTRIBUTE_HOUSE = new Attribute(ATTRIBUTE_HOUSE_ID, ATTRIBUTE_HOUSE_KEY);

    // audiences
    private static final String     CUSTOM_DIMENSION_TYPE = "custom_dimension";
    private static final String     AUDIENCE_GRYFFINDOR_ID = "3468206642";
    private static final String     AUDIENCE_GRYFFINDOR_KEY = "Gryffindors";
    public  static final String     AUDIENCE_GRYFFINDOR_VALUE = "Gryffindor";
    private static final Audience   AUDIENCE_GRYFFINDOR = new Audience(
            AUDIENCE_GRYFFINDOR_ID,
            AUDIENCE_GRYFFINDOR_KEY,
            new AndCondition(Collections.<Condition>singletonList(
                    new OrCondition(Collections.<Condition>singletonList(
                            new OrCondition(Collections.singletonList((Condition) new UserAttribute(ATTRIBUTE_HOUSE_KEY,
                                    CUSTOM_DIMENSION_TYPE,
                                    AUDIENCE_GRYFFINDOR_VALUE)))))))
    );

    // features
    private static final String     FEATURE_BOOLEAN_FEATURE_ID = "4195505407";
    private static final String     FEATURE_BOOLEAN_FEATURE_KEY = "boolean_feature";
    private static final FeatureFlag FEATURE_FLAG_BOOLEAN_FEATURE = new FeatureFlag(
            FEATURE_BOOLEAN_FEATURE_ID,
            FEATURE_BOOLEAN_FEATURE_KEY,
            "",
            Collections.<String>emptyList(),
            Collections.<LiveVariable>emptyList()
    );
    private static final String         FEATURE_SINGLE_VARIABLE_DOUBLE_ID = "3926744821";
    private static final String         FEATURE_SINGLE_VARIABLE_DOUBLE_KEY = "double_single_variable_feature";
    private static final String         VARIABLE_DOUBLE_VARIABLE_ID = "4111654444";
    private static final String         VARIABLE_DOUBLE_VARIABLE_KEY = "double_variable";
    private static final String         VARIABLE_DOUBLE_DEFAULT_VALUE = "14.99";
    private static final LiveVariable   VARIABLE_DOUBLE_VARIABLE = new LiveVariable(
            VARIABLE_DOUBLE_VARIABLE_ID,
            VARIABLE_DOUBLE_VARIABLE_KEY,
            VARIABLE_DOUBLE_DEFAULT_VALUE,
            null,
            LiveVariable.VariableType.DOUBLE
    );
    private static final FeatureFlag FEATURE_FLAG_SINGLE_VARIABLE_DOUBLE = new FeatureFlag(
            FEATURE_SINGLE_VARIABLE_DOUBLE_ID,
            FEATURE_SINGLE_VARIABLE_DOUBLE_KEY,
            "",
            Collections.<String>emptyList(),
            Collections.singletonList(
                    VARIABLE_DOUBLE_VARIABLE
            )
    );
    private static final String         FEATURE_SINGLE_VARIABLE_INTEGER_ID = "3281420120";
    private static final String         FEATURE_SINGLE_VARIABLE_INTEGER_KEY = "integer_single_variable_feature";
    private static final String         VARIABLE_INTEGER_VARIABLE_ID = "593964691";
    private static final String         VARIABLE_INTEGER_VARIABLE_KEY = "integer_variable";
    private static final String         VARIABLE_INTEGER_DEFAULT_VALUE = "7";
    private static final LiveVariable   VARIABLE_INTEGER_VARIABLE = new LiveVariable(
            VARIABLE_INTEGER_VARIABLE_ID,
            VARIABLE_INTEGER_VARIABLE_KEY,
            VARIABLE_INTEGER_DEFAULT_VALUE,
            null,
            LiveVariable.VariableType.INTEGER
    );
    private static final FeatureFlag FEATURE_FLAG_SINGLE_VARIABLE_INTEGER = new FeatureFlag(
            FEATURE_SINGLE_VARIABLE_INTEGER_ID,
            FEATURE_SINGLE_VARIABLE_INTEGER_KEY,
            "",
            Collections.<String>emptyList(),
            Collections.singletonList(
                    VARIABLE_INTEGER_VARIABLE
            )
    );
    private static final String         FEATURE_SINGLE_VARIABLE_BOOLEAN_ID = "2591051011";
    private static final String         FEATURE_SINGLE_VARIABLE_BOOLEAN_KEY = "boolean_single_variable_feature";
    private static final String         VARIABLE_BOOLEAN_VARIABLE_ID = "3974680341";
    private static final String         VARIABLE_BOOLEAN_VARIABLE_KEY = "boolean_variable";
    private static final String         VARIABLE_BOOLEAN_VARIABLE_DEFAULT_VALUE = "true";
    private static final LiveVariable   VARIABLE_BOOLEAN_VARIABLE = new LiveVariable(
            VARIABLE_BOOLEAN_VARIABLE_ID,
            VARIABLE_BOOLEAN_VARIABLE_KEY,
            VARIABLE_BOOLEAN_VARIABLE_DEFAULT_VALUE,
            null,
            LiveVariable.VariableType.BOOLEAN
    );
    private static final FeatureFlag FEATURE_FLAG_SINGLE_VARIABLE_BOOLEAN = new FeatureFlag(
            FEATURE_SINGLE_VARIABLE_BOOLEAN_ID,
            FEATURE_SINGLE_VARIABLE_BOOLEAN_KEY,
            "",
            Collections.<String>emptyList(),
            Collections.singletonList(
                    VARIABLE_BOOLEAN_VARIABLE
            )
    );
    private static final String         FEATURE_SINGLE_VARIABLE_STRING_ID = "2079378557";
    public  static final String         FEATURE_SINGLE_VARIABLE_STRING_KEY = "string_single_variable_feature";
    private static final String         VARIABLE_STRING_VARIABLE_ID = "2077511132";
    public  static final String         VARIABLE_STRING_VARIABLE_KEY = "string_variable";
    public  static final String         VARIABLE_STRING_VARIABLE_DEFAULT_VALUE = "wingardium leviosa";
    private static final LiveVariable   VARIABLE_STRING_VARIABLE = new LiveVariable(
            VARIABLE_STRING_VARIABLE_ID,
            VARIABLE_STRING_VARIABLE_KEY,
            VARIABLE_STRING_VARIABLE_DEFAULT_VALUE,
            null,
            LiveVariable.VariableType.STRING
    );
    private static final FeatureFlag FEATURE_FLAG_SINGLE_VARIABLE_STRING = new FeatureFlag(
            FEATURE_SINGLE_VARIABLE_STRING_ID,
            FEATURE_SINGLE_VARIABLE_STRING_KEY,
            "",
            Collections.<String>emptyList(),
            Collections.singletonList(
                    VARIABLE_STRING_VARIABLE
            )
    );
    private static final String         FEATURE_MULTI_VARIATE_FEATURE_ID = "3263342226";
    public  static final String         FEATURE_MULTI_VARIATE_FEATURE_KEY = "multi_variate_feature";
    private static final String         VARIABLE_FIRST_LETTER_ID = "675244127";
    public  static final String         VARIABLE_FIRST_LETTER_KEY = "first_letter";
    public  static final String         VARIABLE_FIRST_LETTER_DEFAULT_VALUE = "H";
    private static final LiveVariable   VARIABLE_FIRST_LETTER_VARIABLE = new LiveVariable(
            VARIABLE_FIRST_LETTER_ID,
            VARIABLE_FIRST_LETTER_KEY,
            VARIABLE_FIRST_LETTER_DEFAULT_VALUE,
            null,
            LiveVariable.VariableType.STRING
    );
    private static final String         VARIABLE_REST_OF_NAME_ID = "4052219963";
    private static final String         VARIABLE_REST_OF_NAME_KEY = "rest_of_name";
    private static final String         VARIABLE_REST_OF_NAME_DEFAULT_VALUE = "arry";
    private static final LiveVariable   VARIABLE_REST_OF_NAME_VARIABLE = new LiveVariable(
            VARIABLE_REST_OF_NAME_ID,
            VARIABLE_REST_OF_NAME_KEY,
            VARIABLE_REST_OF_NAME_DEFAULT_VALUE,
            null,
            LiveVariable.VariableType.STRING
    );
    private static final String         FEATURE_MUTEX_GROUP_FEATURE_ID = "3263342226";
    public  static final String         FEATURE_MUTEX_GROUP_FEATURE_KEY = "mutex_group_feature";
    private static final String         VARIABLE_CORRELATING_VARIATION_NAME_ID = "2059187672";
    private static final String         VARIABLE_CORRELATING_VARIATION_NAME_KEY = "correlating_variation_name";
    private static final String         VARIABLE_CORRELATING_VARIATION_NAME_DEFAULT_VALUE = "null";
    private static final LiveVariable   VARIABLE_CORRELATING_VARIATION_NAME_VARIABLE = new LiveVariable(
            VARIABLE_CORRELATING_VARIATION_NAME_ID,
            VARIABLE_CORRELATING_VARIATION_NAME_KEY,
            VARIABLE_CORRELATING_VARIATION_NAME_DEFAULT_VALUE,
            null,
            LiveVariable.VariableType.STRING
    );

    // group IDs
    private static final String GROUP_1_ID = "1015968292";
    private static final String GROUP_2_ID = "2606208781";

    // experiments
    private static final String     LAYER_BASIC_EXPERIMENT_ID = "1630555626";
    private static final String     EXPERIMENT_BASIC_EXPERIMENT_ID = "1323241596";
    public  static final String     EXPERIMENT_BASIC_EXPERIMENT_KEY = "basic_experiment";
    private static final String     VARIATION_BASIC_EXPERIMENT_VARIATION_A_ID = "1423767502";
    private static final String     VARIATION_BASIC_EXPERIMENT_VARIATION_A_KEY = "A";
    private static final Variation  VARIATION_BASIC_EXPERIMENT_VARIATION_A = new Variation(
            VARIATION_BASIC_EXPERIMENT_VARIATION_A_ID,
            VARIATION_BASIC_EXPERIMENT_VARIATION_A_KEY,
            Collections.<LiveVariableUsageInstance>emptyList()
    );
    private static final String     VARIATION_BASIC_EXPERIMENT_VARIATION_B_ID = "3433458314";
    private static final String     VARIATION_BASIC_EXPERIMENT_VARIATION_B_KEY = "B";
    private static final Variation  VARIATION_BASIC_EXPERIMENT_VARIATION_B = new Variation(
            VARIATION_BASIC_EXPERIMENT_VARIATION_B_ID,
            VARIATION_BASIC_EXPERIMENT_VARIATION_B_KEY,
            Collections.<LiveVariableUsageInstance>emptyList()
    );
    private static final String     BASIC_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_A = "Harry Potter";
    private static final String     BASIC_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_B = "Tom Riddle";
    private static final Experiment EXPERIMENT_BASIC_EXPERIMENT = new Experiment(
            EXPERIMENT_BASIC_EXPERIMENT_ID,
            EXPERIMENT_BASIC_EXPERIMENT_KEY,
            Experiment.ExperimentStatus.RUNNING.toString(),
            LAYER_BASIC_EXPERIMENT_ID,
            Collections.<String>emptyList(),
            ProjectConfigTestUtils.createListOfObjects(
                    VARIATION_BASIC_EXPERIMENT_VARIATION_A,
                    VARIATION_BASIC_EXPERIMENT_VARIATION_B
            ),
            ProjectConfigTestUtils.createMapOfObjects(
                    ProjectConfigTestUtils.createListOfObjects(
                            BASIC_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_A,
                            BASIC_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_B
                    ),
                    ProjectConfigTestUtils.createListOfObjects(
                            VARIATION_BASIC_EXPERIMENT_VARIATION_A_KEY,
                            VARIATION_BASIC_EXPERIMENT_VARIATION_B_KEY
                    )
            ),
            ProjectConfigTestUtils.createListOfObjects(
                    new TrafficAllocation(
                            VARIATION_BASIC_EXPERIMENT_VARIATION_A_ID,
                            5000
                    ),
                    new TrafficAllocation(
                            VARIATION_BASIC_EXPERIMENT_VARIATION_B_ID,
                            10000
                    )
            )
    );
    private static final String     LAYER_FIRST_GROUPED_EXPERIMENT_ID = "3301900159";
    private static final String     EXPERIMENT_FIRST_GROUPED_EXPERIMENT_ID = "2738374745";
    private static final String     EXPERIMENT_FIRST_GROUPED_EXPERIMENT_KEY = "first_grouped_experiment";
    private static final String     VARIATION_FIRST_GROUPED_EXPERIMENT_A_ID = "2377378132";
    private static final String     VARIATION_FIRST_GROUPED_EXPERIMENT_A_KEY = "A";
    private static final Variation  VARIATION_FIRST_GROUPED_EXPERIMENT_A = new Variation(
            VARIATION_FIRST_GROUPED_EXPERIMENT_A_ID,
            VARIATION_FIRST_GROUPED_EXPERIMENT_A_KEY,
            Collections.<LiveVariableUsageInstance>emptyList()
    );
    private static final String     VARIATION_FIRST_GROUPED_EXPERIMENT_B_ID = "1179171250";
    private static final String     VARIATION_FIRST_GROUPED_EXPERIMENT_B_KEY = "B";
    private static final Variation  VARIATION_FIRST_GROUPED_EXPERIMENT_B = new Variation(
            VARIATION_FIRST_GROUPED_EXPERIMENT_B_ID,
            VARIATION_FIRST_GROUPED_EXPERIMENT_B_KEY,
            Collections.<LiveVariableUsageInstance>emptyList()
    );
    private static final String     FIRST_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_A = "Harry Potter";
    private static final String     FIRST_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_B = "Tom Riddle";
    private static final Experiment EXPERIMENT_FIRST_GROUPED_EXPERIMENT = new Experiment(
            EXPERIMENT_FIRST_GROUPED_EXPERIMENT_ID,
            EXPERIMENT_FIRST_GROUPED_EXPERIMENT_KEY,
            Experiment.ExperimentStatus.RUNNING.toString(),
            LAYER_FIRST_GROUPED_EXPERIMENT_ID,
            Collections.singletonList(AUDIENCE_GRYFFINDOR_ID),
            ProjectConfigTestUtils.createListOfObjects(
                    VARIATION_FIRST_GROUPED_EXPERIMENT_A,
                    VARIATION_FIRST_GROUPED_EXPERIMENT_B
            ),
            ProjectConfigTestUtils.createMapOfObjects(
                    ProjectConfigTestUtils.createListOfObjects(
                            FIRST_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_A,
                            FIRST_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_B
                    ),
                    ProjectConfigTestUtils.createListOfObjects(
                            VARIATION_FIRST_GROUPED_EXPERIMENT_A_KEY,
                            VARIATION_FIRST_GROUPED_EXPERIMENT_B_KEY
                    )
            ),
            ProjectConfigTestUtils.createListOfObjects(
                    new TrafficAllocation(
                            VARIATION_FIRST_GROUPED_EXPERIMENT_A_ID,
                            5000
                    ),
                    new TrafficAllocation(
                            VARIATION_FIRST_GROUPED_EXPERIMENT_B_ID,
                            10000
                    )
            ),
            GROUP_1_ID
    );
    private static final String     LAYER_SECOND_GROUPED_EXPERIMENT_ID = "2625300442";
    private static final String     EXPERIMENT_SECOND_GROUPED_EXPERIMENT_ID = "3042640549";
    private static final String     EXPERIMENT_SECOND_GROUPED_EXPERIMENT_KEY = "second_grouped_experiment";
    private static final String     VARIATION_SECOND_GROUPED_EXPERIMENT_A_ID = "1558539439";
    private static final String     VARIATION_SECOND_GROUPED_EXPERIMENT_A_KEY = "A";
    private static final Variation  VARIATION_SECOND_GROUPED_EXPERIMENT_A = new Variation(
            VARIATION_SECOND_GROUPED_EXPERIMENT_A_ID,
            VARIATION_SECOND_GROUPED_EXPERIMENT_A_KEY,
            Collections.<LiveVariableUsageInstance>emptyList()
    );
    private static final String     VARIATION_SECOND_GROUPED_EXPERIMENT_B_ID = "2142748370";
    private static final String     VARIATION_SECOND_GROUPED_EXPERIMENT_B_KEY = "B";
    private static final Variation  VARIATION_SECOND_GROUPED_EXPERIMENT_B = new Variation(
            VARIATION_SECOND_GROUPED_EXPERIMENT_B_ID,
            VARIATION_SECOND_GROUPED_EXPERIMENT_B_KEY,
            Collections.<LiveVariableUsageInstance>emptyList()
    );
    private static final String     SECOND_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_A = "Hermione Granger";
    private static final String     SECOND_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_B = "Ronald Weasley";
    private static final Experiment EXPERIMENT_SECOND_GROUPED_EXPERIMENT = new Experiment(
            EXPERIMENT_SECOND_GROUPED_EXPERIMENT_ID,
            EXPERIMENT_SECOND_GROUPED_EXPERIMENT_KEY,
            Experiment.ExperimentStatus.RUNNING.toString(),
            LAYER_SECOND_GROUPED_EXPERIMENT_ID,
            Collections.singletonList(AUDIENCE_GRYFFINDOR_ID),
            ProjectConfigTestUtils.createListOfObjects(
                    VARIATION_SECOND_GROUPED_EXPERIMENT_A,
                    VARIATION_SECOND_GROUPED_EXPERIMENT_B
            ),
            ProjectConfigTestUtils.createMapOfObjects(
                    ProjectConfigTestUtils.createListOfObjects(
                            SECOND_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_A,
                            SECOND_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_B
                    ),
                    ProjectConfigTestUtils.createListOfObjects(
                            VARIATION_SECOND_GROUPED_EXPERIMENT_A_KEY,
                            VARIATION_SECOND_GROUPED_EXPERIMENT_B_KEY
                    )
            ),
            ProjectConfigTestUtils.createListOfObjects(
                    new TrafficAllocation(
                            VARIATION_SECOND_GROUPED_EXPERIMENT_A_ID,
                            5000
                    ),
                    new TrafficAllocation(
                            VARIATION_SECOND_GROUPED_EXPERIMENT_B_ID,
                            10000
                    )
            ),
            GROUP_1_ID
    );
    private static final String     LAYER_MULTIVARIATE_EXPERIMENT_ID = "3780747876";
    private static final String     EXPERIMENT_MULTIVARIATE_EXPERIMENT_ID = "3262035800";
    public  static final String     EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY = "multivariate_experiment";
    private static final String     VARIATION_MULTIVARIATE_EXPERIMENT_FRED_ID = "1880281238";
    private static final String     VARIATION_MULTIVARIATE_EXPERIMENT_FRED_KEY = "Fred";
    private static final Variation  VARIATION_MULTIVARIATE_EXPERIMENT_FRED = new Variation(
            VARIATION_MULTIVARIATE_EXPERIMENT_FRED_ID,
            VARIATION_MULTIVARIATE_EXPERIMENT_FRED_KEY,
            ProjectConfigTestUtils.createListOfObjects(
                    new LiveVariableUsageInstance(
                            VARIABLE_FIRST_LETTER_ID,
                            "F"
                    ),
                    new LiveVariableUsageInstance(
                            VARIABLE_REST_OF_NAME_ID,
                            "red"
                    )
            )
    );
    private static final String     VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE_ID = "3631049532";
    private static final String     VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE_KEY = "Feorge";
    private static final Variation  VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE = new Variation(
            VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE_ID,
            VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE_KEY,
            ProjectConfigTestUtils.createListOfObjects(
                    new LiveVariableUsageInstance(
                            VARIABLE_FIRST_LETTER_ID,
                            "F"
                    ),
                    new LiveVariableUsageInstance(
                            VARIABLE_REST_OF_NAME_ID,
                            "eorge"
                    )
            )
    );
    private static final String     VARIATION_MULTIVARIATE_EXPERIMENT_GRED_ID = "4204375027";
    public  static final String     VARIATION_MULTIVARIATE_EXPERIMENT_GRED_KEY = "Gred";
    public  static final Variation  VARIATION_MULTIVARIATE_EXPERIMENT_GRED = new Variation(
            VARIATION_MULTIVARIATE_EXPERIMENT_GRED_ID,
            VARIATION_MULTIVARIATE_EXPERIMENT_GRED_KEY,
            ProjectConfigTestUtils.createListOfObjects(
                    new LiveVariableUsageInstance(
                            VARIABLE_FIRST_LETTER_ID,
                            "G"
                    ),
                    new LiveVariableUsageInstance(
                            VARIABLE_REST_OF_NAME_ID,
                            "red"
                    )
            )
    );
    private static final String     VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE_ID = "2099211198";
    private static final String     VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE_KEY = "George";
    private static final Variation  VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE = new Variation(
            VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE_ID,
            VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE_KEY,
            ProjectConfigTestUtils.createListOfObjects(
                    new LiveVariableUsageInstance(
                            VARIABLE_FIRST_LETTER_ID,
                            "G"
                    ),
                    new LiveVariableUsageInstance(
                            VARIABLE_REST_OF_NAME_ID,
                            "eorge"
                    )
            )
    );
    private static final String     MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_FRED = "Fred";
    private static final String     MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_FEORGE = "Feorge";
    public  static final String     MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GRED = "Gred";
    private static final String     MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GEORGE = "George";
    private static final Experiment EXPERIMENT_MULTIVARIATE_EXPERIMENT = new Experiment(
            EXPERIMENT_MULTIVARIATE_EXPERIMENT_ID,
            EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY,
            Experiment.ExperimentStatus.RUNNING.toString(),
            LAYER_MULTIVARIATE_EXPERIMENT_ID,
            Collections.singletonList(AUDIENCE_GRYFFINDOR_ID),
            ProjectConfigTestUtils.createListOfObjects(
                    VARIATION_MULTIVARIATE_EXPERIMENT_FRED,
                    VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE,
                    VARIATION_MULTIVARIATE_EXPERIMENT_GRED,
                    VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE
            ),
            ProjectConfigTestUtils.createMapOfObjects(
                    ProjectConfigTestUtils.createListOfObjects(
                            MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_FRED,
                            MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_FEORGE,
                            MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GRED,
                            MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GEORGE
                    ),
                    ProjectConfigTestUtils.createListOfObjects(
                            VARIATION_MULTIVARIATE_EXPERIMENT_FRED_KEY,
                            VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE_KEY,
                            VARIATION_MULTIVARIATE_EXPERIMENT_GRED_KEY,
                            VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE_KEY
                    )
            ),
            ProjectConfigTestUtils.createListOfObjects(
                    new TrafficAllocation(
                            VARIATION_MULTIVARIATE_EXPERIMENT_FRED_ID,
                            2500
                    ),
                    new TrafficAllocation(
                            VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE_ID,
                            5000
                    ),
                    new TrafficAllocation(
                            VARIATION_MULTIVARIATE_EXPERIMENT_GRED_ID,
                            7500
                    ),
                    new TrafficAllocation(
                            VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE_ID,
                            10000
                    )
            )
    );
    private static final String     LAYER_PAUSED_EXPERIMENT_ID = "3949273892";
    private static final String     EXPERIMENT_PAUSED_EXPERIMENT_ID = "2667098701";
    public  static final String     EXPERIMENT_PAUSED_EXPERIMENT_KEY = "paused_experiment";
    private static final String     VARIATION_PAUSED_EXPERIMENT_CONTROL_ID = "391535909";
    private static final String     VARIATION_PAUSED_EXPERIMENT_CONTROL_KEY = "Control";
    private static final Variation  VARIATION_PAUSED_EXPERIMENT_CONTROL = new Variation(
            VARIATION_PAUSED_EXPERIMENT_CONTROL_ID,
            VARIATION_PAUSED_EXPERIMENT_CONTROL_KEY,
            Collections.<LiveVariableUsageInstance>emptyList()
    );
    public  static final String     PAUSED_EXPERIMENT_FORCED_VARIATION_USER_ID_CONTROL = "Harry Potter";
    private static final Experiment EXPERIMENT_PAUSED_EXPERIMENT = new Experiment(
            EXPERIMENT_PAUSED_EXPERIMENT_ID,
            EXPERIMENT_PAUSED_EXPERIMENT_KEY,
            Experiment.ExperimentStatus.PAUSED.toString(),
            LAYER_PAUSED_EXPERIMENT_ID,
            Collections.<String>emptyList(),
            Collections.singletonList(VARIATION_PAUSED_EXPERIMENT_CONTROL),
            Collections.singletonMap(PAUSED_EXPERIMENT_FORCED_VARIATION_USER_ID_CONTROL,
                    VARIATION_PAUSED_EXPERIMENT_CONTROL_KEY),
            Collections.singletonList(
                    new TrafficAllocation(
                            VARIATION_PAUSED_EXPERIMENT_CONTROL_ID,
                            10000
                    )
            )
    );
    private static final String     LAYER_LAUNCHED_EXPERIMENT_ID = "3587821424";
    private static final String     EXPERIMENT_LAUNCHED_EXPERIMENT_ID = "3072915611";
    public  static final String     EXPERIMENT_LAUNCHED_EXPERIMENT_KEY = "launched_experiment";
    private static final String     VARIATION_LAUNCHED_EXPERIMENT_CONTROL_ID = "1647582435";
    private static final String     VARIATION_LAUNCHED_EXPERIMENT_CONTROL_KEY = "launch_control";
    private static final Variation  VARIATION_LAUNCHED_EXPERIMENT_CONTROL = new Variation(
            VARIATION_LAUNCHED_EXPERIMENT_CONTROL_ID,
            VARIATION_LAUNCHED_EXPERIMENT_CONTROL_KEY,
            Collections.<LiveVariableUsageInstance>emptyList()
    );
    private static final Experiment EXPERIMENT_LAUNCHED_EXPERIMENT = new Experiment(
            EXPERIMENT_LAUNCHED_EXPERIMENT_ID,
            EXPERIMENT_LAUNCHED_EXPERIMENT_KEY,
            Experiment.ExperimentStatus.LAUNCHED.toString(),
            LAYER_LAUNCHED_EXPERIMENT_ID,
            Collections.<String>emptyList(),
            Collections.singletonList(VARIATION_LAUNCHED_EXPERIMENT_CONTROL),
            Collections.<String, String>emptyMap(),
            Collections.singletonList(
                    new TrafficAllocation(
                            VARIATION_LAUNCHED_EXPERIMENT_CONTROL_ID,
                            8000
                    )
            )
    );
    private static final String     LAYER_MUTEX_GROUP_EXPERIMENT_1_LAYER_ID = "3755588495";
    private static final String     EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1_EXPERIMENT_ID = "4138322202";
    private static final String     EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1_EXPERIMENT_KEY = "mutex_group_2_experiment_1";
    private static final String     VARIATION_MUTEX_GROUP_EXP_1_VAR_1_ID = "1394671166";
    private static final String     VARIATION_MUTEX_GROUP_EXP_1_VAR_1_KEY = "mutex_group_2_experiment_1_variation_1";
    private static final Variation  VARIATION_MUTEX_GROUP_EXP_1_VAR_1 = new Variation(
            VARIATION_MUTEX_GROUP_EXP_1_VAR_1_ID,
            VARIATION_MUTEX_GROUP_EXP_1_VAR_1_KEY,
            Collections.singletonList(
                    new LiveVariableUsageInstance(
                            VARIABLE_CORRELATING_VARIATION_NAME_ID,
                            VARIATION_MUTEX_GROUP_EXP_1_VAR_1_KEY
                    )
            )
    );
    public  static final Experiment EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1 = new Experiment(
            EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1_EXPERIMENT_ID,
            EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1_EXPERIMENT_KEY,
            Experiment.ExperimentStatus.RUNNING.toString(),
            LAYER_MUTEX_GROUP_EXPERIMENT_1_LAYER_ID,
            Collections.<String>emptyList(),
            Collections.singletonList(VARIATION_MUTEX_GROUP_EXP_1_VAR_1),
            Collections.<String, String>emptyMap(),
            Collections.singletonList(
                    new TrafficAllocation(
                            VARIATION_MUTEX_GROUP_EXP_1_VAR_1_ID,
                            10000
                    )
            ),
            GROUP_2_ID
    );
    private static final String     LAYER_MUTEX_GROUP_EXPERIMENT_2_LAYER_ID = "3818002538";
    private static final String     EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2_EXPERIMENT_ID = "1786133852";
    private static final String     EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2_EXPERIMENT_KEY = "mutex_group_2_experiment_2";
    private static final String     VARIATION_MUTEX_GROUP_EXP_2_VAR_1_ID = "1619235542";
    private static final String     VARIATION_MUTEX_GROUP_EXP_2_VAR_1_KEY = "mutex_group_2_experiment_2_variation_2";
    public  static final Variation  VARIATION_MUTEX_GROUP_EXP_2_VAR_1 = new Variation(
            VARIATION_MUTEX_GROUP_EXP_2_VAR_1_ID,
            VARIATION_MUTEX_GROUP_EXP_2_VAR_1_KEY,
            Collections.singletonList(
                    new LiveVariableUsageInstance(
                            VARIABLE_CORRELATING_VARIATION_NAME_ID,
                            VARIATION_MUTEX_GROUP_EXP_2_VAR_1_KEY
                    )
            )
    );
    public  static final Experiment EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2 = new Experiment(
            EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2_EXPERIMENT_ID,
            EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2_EXPERIMENT_KEY,
            Experiment.ExperimentStatus.RUNNING.toString(),
            LAYER_MUTEX_GROUP_EXPERIMENT_2_LAYER_ID,
            Collections.<String>emptyList(),
            Collections.singletonList(VARIATION_MUTEX_GROUP_EXP_2_VAR_1),
            Collections.<String, String>emptyMap(),
            Collections.singletonList(
                    new TrafficAllocation(
                            VARIATION_MUTEX_GROUP_EXP_2_VAR_1_ID,
                            10000
                    )
            ),
            GROUP_2_ID
    );

    // generate groups
    private static final Group      GROUP_1 = new Group(
            GROUP_1_ID,
            Group.RANDOM_POLICY,
            ProjectConfigTestUtils.createListOfObjects(
                    EXPERIMENT_FIRST_GROUPED_EXPERIMENT,
                    EXPERIMENT_SECOND_GROUPED_EXPERIMENT
            ),
            ProjectConfigTestUtils.createListOfObjects(
                    new TrafficAllocation(
                            EXPERIMENT_FIRST_GROUPED_EXPERIMENT_ID,
                            4000
                    ),
                    new TrafficAllocation(
                            EXPERIMENT_SECOND_GROUPED_EXPERIMENT_ID,
                            8000
                    )
            )
    );
    private static final Group      GROUP_2 = new Group(
            GROUP_2_ID,
            Group.RANDOM_POLICY,
            ProjectConfigTestUtils.createListOfObjects(
                    EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1,
                    EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2
            ),
            ProjectConfigTestUtils.createListOfObjects(
                    new TrafficAllocation(
                            EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1_EXPERIMENT_ID,
                            5000
                    ),
                    new TrafficAllocation(
                            EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2_EXPERIMENT_ID,
                            10000
                    )
            )
    );

    // events
    private static final String     EVENT_BASIC_EVENT_ID = "3785620495";
    public  static final String     EVENT_BASIC_EVENT_KEY = "basic_event";
    private static final EventType  EVENT_BASIC_EVENT = new EventType(
            EVENT_BASIC_EVENT_ID,
            EVENT_BASIC_EVENT_KEY,
            ProjectConfigTestUtils.createListOfObjects(
                    EXPERIMENT_BASIC_EXPERIMENT_ID,
                    EXPERIMENT_FIRST_GROUPED_EXPERIMENT_ID,
                    EXPERIMENT_SECOND_GROUPED_EXPERIMENT_ID,
                    EXPERIMENT_MULTIVARIATE_EXPERIMENT_ID,
                    EXPERIMENT_LAUNCHED_EXPERIMENT_ID
            )
    );
    private static final String     EVENT_PAUSED_EXPERIMENT_ID = "3195631717";
    public  static final String     EVENT_PAUSED_EXPERIMENT_KEY = "event_with_paused_experiment";
    private static final EventType  EVENT_PAUSED_EXPERIMENT = new EventType(
            EVENT_PAUSED_EXPERIMENT_ID,
            EVENT_PAUSED_EXPERIMENT_KEY,
            Collections.singletonList(
                    EXPERIMENT_PAUSED_EXPERIMENT_ID
            )
    );
    private static final String     EVENT_LAUNCHED_EXPERIMENT_ONLY_ID = "1987018666";
    public  static final String     EVENT_LAUNCHED_EXPERIMENT_ONLY_KEY = "event_with_launched_experiments_only";
    private static final EventType  EVENT_LAUNCHED_EXPERIMENT_ONLY = new EventType(
            EVENT_LAUNCHED_EXPERIMENT_ONLY_ID,
            EVENT_LAUNCHED_EXPERIMENT_ONLY_KEY,
            Collections.singletonList(
                    EXPERIMENT_LAUNCHED_EXPERIMENT_ID
            )
    );

    // finish features
    public  static final FeatureFlag FEATURE_FLAG_MULTI_VARIATE_FEATURE = new FeatureFlag(
            FEATURE_MULTI_VARIATE_FEATURE_ID,
            FEATURE_MULTI_VARIATE_FEATURE_KEY,
            "",
            Collections.singletonList(EXPERIMENT_MULTIVARIATE_EXPERIMENT_ID),
            ProjectConfigTestUtils.createListOfObjects(
                    VARIABLE_FIRST_LETTER_VARIABLE,
                    VARIABLE_REST_OF_NAME_VARIABLE
            )
    );
    public  static final FeatureFlag FEATURE_FLAG_MUTEX_GROUP_FEATURE = new FeatureFlag(
            FEATURE_MUTEX_GROUP_FEATURE_ID,
            FEATURE_MUTEX_GROUP_FEATURE_KEY,
            "",
            ProjectConfigTestUtils.createListOfObjects(
                    EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1_EXPERIMENT_ID,
                    EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2_EXPERIMENT_ID
            ),
            Collections.singletonList(
                    VARIABLE_CORRELATING_VARIATION_NAME_VARIABLE
            )
    );


    public static ProjectConfig generateValidProjectConfigV4() {

        // list attributes
        List<Attribute> attributes = new ArrayList<Attribute>();
        attributes.add(ATTRIBUTE_HOUSE);

        // list audiences
        List<Audience> audiences = new ArrayList<Audience>();
        audiences.add(AUDIENCE_GRYFFINDOR);

        // list events
        List<EventType> events = new ArrayList<EventType>();
        events.add(EVENT_BASIC_EVENT);
        events.add(EVENT_PAUSED_EXPERIMENT);
        events.add(EVENT_LAUNCHED_EXPERIMENT_ONLY);

        // list experiments
        List<Experiment> experiments = new ArrayList<Experiment>();
        experiments.add(EXPERIMENT_BASIC_EXPERIMENT);
        experiments.add(EXPERIMENT_MULTIVARIATE_EXPERIMENT);
        experiments.add(EXPERIMENT_PAUSED_EXPERIMENT);
        experiments.add(EXPERIMENT_LAUNCHED_EXPERIMENT);

        // list featureFlags
        List<FeatureFlag> featureFlags = new ArrayList<FeatureFlag>();
        featureFlags.add(FEATURE_FLAG_BOOLEAN_FEATURE);
        featureFlags.add(FEATURE_FLAG_SINGLE_VARIABLE_DOUBLE);
        featureFlags.add(FEATURE_FLAG_SINGLE_VARIABLE_INTEGER);
        featureFlags.add(FEATURE_FLAG_SINGLE_VARIABLE_BOOLEAN);
        featureFlags.add(FEATURE_FLAG_SINGLE_VARIABLE_STRING);
        featureFlags.add(FEATURE_FLAG_MULTI_VARIATE_FEATURE);
        featureFlags.add(FEATURE_FLAG_MUTEX_GROUP_FEATURE);

        List<Group> groups = new ArrayList<Group>();
        groups.add(GROUP_1);
        groups.add(GROUP_2);

        return new ProjectConfig(
                ACCOUNT_ID,
                ANONYMIZE_IP,
                PROJECT_ID,
                REVISION,
                VERSION,
                attributes,
                audiences,
                events,
                experiments,
                featureFlags,
                groups,
                Collections.<LiveVariable>emptyList()
        );
    }
}
