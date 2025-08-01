/**
 *
 *    Copyright 2017-2021, Optimizely and contributors
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
import com.optimizely.ab.config.audience.AudienceIdCondition;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.EmptyCondition;
import com.optimizely.ab.config.audience.OrCondition;
import com.optimizely.ab.config.audience.UserAttribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ValidProjectConfigV4 {

    // simple properties
    private static final String ACCOUNT_ID = "2360254204";
    private static final boolean ANONYMIZE_IP = true;
    private static final boolean BOT_FILTERING = true;
    private static final String PROJECT_ID = "3918735994";
    private static final String REVISION = "1480511547";
    private static final String SDK_KEY = "ValidProjectConfigV4";
    private static final String ENVIRONMENT_KEY = "production";
    private static final String VERSION = "4";
    private static final Boolean SEND_FLAG_DECISIONS = true;
    private static final String REGION = "US";

    // attributes
    private static final String ATTRIBUTE_HOUSE_ID = "553339214";
    public static final String ATTRIBUTE_HOUSE_KEY = "house";
    private static final Attribute ATTRIBUTE_HOUSE = new Attribute(ATTRIBUTE_HOUSE_ID, ATTRIBUTE_HOUSE_KEY);

    private static final String ATTRIBUTE_NATIONALITY_ID = "58339410";
    public static final String ATTRIBUTE_NATIONALITY_KEY = "nationality";
    private static final Attribute ATTRIBUTE_NATIONALITY = new Attribute(ATTRIBUTE_NATIONALITY_ID, ATTRIBUTE_NATIONALITY_KEY);

    private static final String ATTRIBUTE_OPT_ID = "583394100";
    public static final String ATTRIBUTE_OPT_KEY = "$opt_test";
    private static final Attribute ATTRIBUTE_OPT = new Attribute(ATTRIBUTE_OPT_ID, ATTRIBUTE_OPT_KEY);

    private static final String ATTRIBUTE_BOOLEAN_ID = "323434545";
    public static final String ATTRIBUTE_BOOLEAN_KEY = "booleanKey";
    private static final Attribute ATTRIBUTE_BOOLEAN = new Attribute(ATTRIBUTE_BOOLEAN_ID, ATTRIBUTE_BOOLEAN_KEY);

    private static final String ATTRIBUTE_INTEGER_ID = "616727838";
    public static final String ATTRIBUTE_INTEGER_KEY = "integerKey";
    private static final Attribute ATTRIBUTE_INTEGER = new Attribute(ATTRIBUTE_INTEGER_ID, ATTRIBUTE_INTEGER_KEY);

    private static final String ATTRIBUTE_DOUBLE_ID = "808797686";
    public static final String ATTRIBUTE_DOUBLE_KEY = "doubleKey";
    private static final Attribute ATTRIBUTE_DOUBLE = new Attribute(ATTRIBUTE_DOUBLE_ID, ATTRIBUTE_DOUBLE_KEY);

    private static final String ATTRIBUTE_EMPTY_KEY_ID = "808797686";
    public static final String ATTRIBUTE_EMPTY_KEY = "";
    private static final Attribute ATTRIBUTE_EMPTY = new Attribute(ATTRIBUTE_EMPTY_KEY_ID, ATTRIBUTE_EMPTY_KEY);

    // audiences
    private static final String CUSTOM_ATTRIBUTE_TYPE = "custom_attribute";
    private static final String AUDIENCE_BOOL_ID = "3468206643";
    private static final String AUDIENCE_BOOL_KEY = "BOOL";
    public static final Boolean AUDIENCE_BOOL_VALUE = true;
    private static final Audience TYPED_AUDIENCE_BOOL = new Audience(
        AUDIENCE_BOOL_ID,
        AUDIENCE_BOOL_KEY,
        new AndCondition(Collections.<Condition>singletonList(
            new OrCondition(Collections.<Condition>singletonList(
                new OrCondition(Collections.singletonList((Condition) new UserAttribute(ATTRIBUTE_BOOLEAN_KEY,
                    CUSTOM_ATTRIBUTE_TYPE, "exact",
                    AUDIENCE_BOOL_VALUE)))))))
    );
    private static final String AUDIENCE_INT_ID = "3468206644";
    private static final String AUDIENCE_INT_KEY = "INT";
    public static final Number AUDIENCE_INT_VALUE = 1.0;
    private static final Audience TYPED_AUDIENCE_INT = new Audience(
        AUDIENCE_INT_ID,
        AUDIENCE_INT_KEY,
        new AndCondition(Collections.<Condition>singletonList(
            new OrCondition(Collections.<Condition>singletonList(
                new OrCondition(Collections.singletonList((Condition) new UserAttribute(ATTRIBUTE_INTEGER_KEY,
                    CUSTOM_ATTRIBUTE_TYPE, "gt",
                    AUDIENCE_INT_VALUE)))))))
    );

    private static final String AUDIENCE_INT_EXACT_ID = "3468206646";
    private static final String AUDIENCE_INT_EXACT_KEY = "INTEXACT";
    private static final Audience TYPED_AUDIENCE_EXACT_INT = new Audience(
        AUDIENCE_INT_EXACT_ID,
        AUDIENCE_INT_EXACT_KEY,
        new AndCondition(Collections.<Condition>singletonList(
            new OrCondition(Collections.<Condition>singletonList(
                new OrCondition(Collections.singletonList((Condition) new UserAttribute(ATTRIBUTE_INTEGER_KEY,
                    CUSTOM_ATTRIBUTE_TYPE, "exact",
                    AUDIENCE_INT_VALUE)))))))
    );
    private static final String AUDIENCE_DOUBLE_ID = "3468206645";
    private static final String AUDIENCE_DOUBLE_KEY = "DOUBLE";
    public static final Double AUDIENCE_DOUBLE_VALUE = 100.0;
    private static final Audience TYPED_AUDIENCE_DOUBLE = new Audience(
        AUDIENCE_DOUBLE_ID,
        AUDIENCE_DOUBLE_KEY,
        new AndCondition(Collections.<Condition>singletonList(
            new OrCondition(Collections.<Condition>singletonList(
                new OrCondition(Collections.singletonList((Condition) new UserAttribute(ATTRIBUTE_DOUBLE_KEY,
                    CUSTOM_ATTRIBUTE_TYPE, "lt",
                    AUDIENCE_DOUBLE_VALUE)))))))
    );
    private static final String AUDIENCE_GRYFFINDOR_ID = "3468206642";
    private static final String AUDIENCE_GRYFFINDOR_KEY = "Gryffindors";
    public static final String AUDIENCE_GRYFFINDOR_VALUE = "Gryffindor";
    private static final Audience AUDIENCE_GRYFFINDOR = new Audience(
        AUDIENCE_GRYFFINDOR_ID,
        AUDIENCE_GRYFFINDOR_KEY,
        new AndCondition(Collections.<Condition>singletonList(
            new OrCondition(Collections.<Condition>singletonList(
                new OrCondition(Collections.singletonList((Condition) new UserAttribute(ATTRIBUTE_HOUSE_KEY,
                    CUSTOM_ATTRIBUTE_TYPE, null,
                    AUDIENCE_GRYFFINDOR_VALUE)))))))
    );
    private static final Audience TYPED_AUDIENCE_GRYFFINDOR = new Audience(
        AUDIENCE_GRYFFINDOR_ID,
        AUDIENCE_GRYFFINDOR_KEY,
        new AndCondition(Collections.<Condition>singletonList(
            new OrCondition(Collections.<Condition>singletonList(
                new OrCondition(Collections.singletonList((Condition) new UserAttribute(ATTRIBUTE_HOUSE_KEY,
                    CUSTOM_ATTRIBUTE_TYPE, "exact",
                    AUDIENCE_GRYFFINDOR_VALUE)))))))
    );

    private static final String AUDIENCE_SLYTHERIN_ID = "3988293898";
    private static final String AUDIENCE_SLYTHERIN_KEY = "Slytherins";
    public static final String AUDIENCE_SLYTHERIN_VALUE = "Slytherin";
    private static final Audience AUDIENCE_SLYTHERIN = new Audience(
        AUDIENCE_SLYTHERIN_ID,
        AUDIENCE_SLYTHERIN_KEY,
        new AndCondition(Collections.<Condition>singletonList(
            new OrCondition(Collections.<Condition>singletonList(
                new OrCondition(Collections.singletonList((Condition) new UserAttribute(ATTRIBUTE_HOUSE_KEY,
                    CUSTOM_ATTRIBUTE_TYPE, null,
                    AUDIENCE_SLYTHERIN_VALUE)))))))
    );

    private static final Audience TYPED_AUDIENCE_SLYTHERIN = new Audience(
        AUDIENCE_SLYTHERIN_ID,
        AUDIENCE_SLYTHERIN_KEY,
        new AndCondition(Collections.<Condition>singletonList(
            new OrCondition(Collections.<Condition>singletonList(
                new OrCondition(Collections.singletonList((Condition) new UserAttribute(ATTRIBUTE_HOUSE_KEY,
                    CUSTOM_ATTRIBUTE_TYPE, "substring",
                    AUDIENCE_SLYTHERIN_VALUE)))))))
    );

    private static final String AUDIENCE_ENGLISH_CITIZENS_ID = "4194404272";
    private static final String AUDIENCE_ENGLISH_CITIZENS_KEY = "english_citizens";
    public static final String AUDIENCE_ENGLISH_CITIZENS_VALUE = "English";
    private static final Audience AUDIENCE_ENGLISH_CITIZENS = new Audience(
        AUDIENCE_ENGLISH_CITIZENS_ID,
        AUDIENCE_ENGLISH_CITIZENS_KEY,
        new AndCondition(Collections.<Condition>singletonList(
            new OrCondition(Collections.<Condition>singletonList(
                new OrCondition(Collections.singletonList((Condition) new UserAttribute(ATTRIBUTE_NATIONALITY_KEY,
                    CUSTOM_ATTRIBUTE_TYPE, null,
                    AUDIENCE_ENGLISH_CITIZENS_VALUE)))))))
    );
    private static final Audience TYPED_AUDIENCE_ENGLISH_CITIZENS = new Audience(
        AUDIENCE_ENGLISH_CITIZENS_ID,
        AUDIENCE_ENGLISH_CITIZENS_KEY,
        new AndCondition(Collections.<Condition>singletonList(
            new OrCondition(Collections.<Condition>singletonList(
                new OrCondition(Collections.singletonList((Condition) new UserAttribute(ATTRIBUTE_NATIONALITY_KEY,
                    CUSTOM_ATTRIBUTE_TYPE, "exact",
                    AUDIENCE_ENGLISH_CITIZENS_VALUE)))))))
    );
    private static final String AUDIENCE_WITH_MISSING_VALUE_ID = "2196265320";
    private static final String AUDIENCE_WITH_MISSING_VALUE_KEY = "audience_with_missing_value";
    public static final String AUDIENCE_WITH_MISSING_VALUE_VALUE = "English";
    private static final UserAttribute ATTRIBUTE_WITH_VALUE = new UserAttribute(
        ATTRIBUTE_NATIONALITY_KEY,
        CUSTOM_ATTRIBUTE_TYPE, null,
        AUDIENCE_WITH_MISSING_VALUE_VALUE
    );
    private static final UserAttribute ATTRIBUTE_WITHOUT_VALUE = new UserAttribute(
        ATTRIBUTE_NATIONALITY_KEY,
        CUSTOM_ATTRIBUTE_TYPE,
        null,
        null
    );
    private static final Audience AUDIENCE_WITH_MISSING_VALUE = new Audience(
        AUDIENCE_WITH_MISSING_VALUE_ID,
        AUDIENCE_WITH_MISSING_VALUE_KEY,
        new AndCondition(Collections.<Condition>singletonList(
            new OrCondition(Collections.<Condition>singletonList(
                new OrCondition(DatafileProjectConfigTestUtils.<Condition>createListOfObjects(
                    ATTRIBUTE_WITH_VALUE,
                    ATTRIBUTE_WITHOUT_VALUE
                ))
            ))
        ))
    );

    private static final Condition AUDIENCE_COMBINATION_WITH_AND_CONDITION = new AndCondition(Arrays.<Condition>asList(
        new AudienceIdCondition(AUDIENCE_BOOL_ID),
        new AudienceIdCondition(AUDIENCE_INT_ID),
        new AudienceIdCondition(AUDIENCE_DOUBLE_ID)));

    // audienceConditions
    private static final Condition AUDIENCE_COMBINATION_LEAF_CONDITION =
        new AudienceIdCondition(AUDIENCE_BOOL_ID);

    // audienceConditions
    private static final Condition AUDIENCE_COMBINATION =
        new OrCondition(Arrays.<Condition>asList(
            new AudienceIdCondition(AUDIENCE_BOOL_ID),
            new AudienceIdCondition(AUDIENCE_INT_ID),
            new AudienceIdCondition(AUDIENCE_INT_EXACT_ID),
            new AudienceIdCondition(AUDIENCE_DOUBLE_ID)));

    // features
    private static final String FEATURE_BOOLEAN_FEATURE_ID = "4195505407";
    private static final String FEATURE_BOOLEAN_FEATURE_KEY = "boolean_feature";
    private static final FeatureFlag FEATURE_FLAG_BOOLEAN_FEATURE = new FeatureFlag(
        FEATURE_BOOLEAN_FEATURE_ID,
        FEATURE_BOOLEAN_FEATURE_KEY,
        "",
        Collections.<String>emptyList(),
        Collections.<FeatureVariable>emptyList()
    );
    private static final String FEATURE_SINGLE_VARIABLE_DOUBLE_ID = "3926744821";
    public static final String FEATURE_SINGLE_VARIABLE_DOUBLE_KEY = "double_single_variable_feature";
    private static final String VARIABLE_DOUBLE_VARIABLE_ID = "4111654444";
    public static final String VARIABLE_DOUBLE_VARIABLE_KEY = "double_variable";
    public static final String VARIABLE_DOUBLE_DEFAULT_VALUE = "14.99";
    private static final FeatureVariable VARIABLE_DOUBLE_VARIABLE = new FeatureVariable(
        VARIABLE_DOUBLE_VARIABLE_ID,
        VARIABLE_DOUBLE_VARIABLE_KEY,
        VARIABLE_DOUBLE_DEFAULT_VALUE,
        null,
        FeatureVariable.DOUBLE_TYPE,
        null
    );
    private static final String FEATURE_SINGLE_VARIABLE_INTEGER_ID = "3281420120";
    public static final String FEATURE_SINGLE_VARIABLE_INTEGER_KEY = "integer_single_variable_feature";
    private static final String VARIABLE_INTEGER_VARIABLE_ID = "593964691";
    public static final String VARIABLE_INTEGER_VARIABLE_KEY = "integer_variable";
    private static final String VARIABLE_INTEGER_DEFAULT_VALUE = "7";
    private static final FeatureVariable VARIABLE_INTEGER_VARIABLE = new FeatureVariable(
        VARIABLE_INTEGER_VARIABLE_ID,
        VARIABLE_INTEGER_VARIABLE_KEY,
        VARIABLE_INTEGER_DEFAULT_VALUE,
        null,
        FeatureVariable.INTEGER_TYPE,
        null
    );
    private static final String FEATURE_SINGLE_VARIABLE_LONG_ID = "964006971";
    public static final String FEATURE_SINGLE_VARIABLE_LONG_KEY = "long_single_variable_feature";
    private static final String VARIABLE_LONG_VARIABLE_ID = "4339640697";
    public static final String VARIABLE_LONG_VARIABLE_KEY = "long_variable";
    private static final String VARIABLE_LONG_DEFAULT_VALUE = "379993881340";
    private static final FeatureVariable VARIABLE_LONG_VARIABLE = new FeatureVariable(
        VARIABLE_LONG_VARIABLE_ID,
        VARIABLE_LONG_VARIABLE_KEY,
        VARIABLE_LONG_DEFAULT_VALUE,
        null,
        FeatureVariable.INTEGER_TYPE,
        null
    );
    private static final String FEATURE_SINGLE_VARIABLE_BOOLEAN_ID = "2591051011";
    public static final String FEATURE_SINGLE_VARIABLE_BOOLEAN_KEY = "boolean_single_variable_feature";
    private static final String VARIABLE_BOOLEAN_VARIABLE_ID = "3974680341";
    public static final String VARIABLE_BOOLEAN_VARIABLE_KEY = "boolean_variable";
    public static final String VARIABLE_BOOLEAN_VARIABLE_DEFAULT_VALUE = "true";
    private static final FeatureVariable VARIABLE_BOOLEAN_VARIABLE = new FeatureVariable(
        VARIABLE_BOOLEAN_VARIABLE_ID,
        VARIABLE_BOOLEAN_VARIABLE_KEY,
        VARIABLE_BOOLEAN_VARIABLE_DEFAULT_VALUE,
        null,
        FeatureVariable.BOOLEAN_TYPE,
        null
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
    private static final String FEATURE_SINGLE_VARIABLE_STRING_ID = "2079378557";
    public static final String FEATURE_SINGLE_VARIABLE_STRING_KEY = "string_single_variable_feature";
    private static final String VARIABLE_STRING_VARIABLE_ID = "2077511132";
    public static final String VARIABLE_STRING_VARIABLE_KEY = "string_variable";
    public static final String VARIABLE_STRING_VARIABLE_DEFAULT_VALUE = "wingardium leviosa";
    private static final FeatureVariable VARIABLE_STRING_VARIABLE = new FeatureVariable(
        VARIABLE_STRING_VARIABLE_ID,
        VARIABLE_STRING_VARIABLE_KEY,
        VARIABLE_STRING_VARIABLE_DEFAULT_VALUE,
        null,
        FeatureVariable.STRING_TYPE,
        null
    );
    private static final String ROLLOUT_1_ID = "1058508303";
    private static final String ROLLOUT_1_EVERYONE_ELSE_EXPERIMENT_ID = "1785077004";
    private static final String ROLLOUT_1_EVERYONE_ELSE_RULE_ENABLED_VARIATION_ID = "1566407342";
    private static final String ROLLOUT_1_EVERYONE_ELSE_RULE_ENABLED_VARIATION_STRING_VALUE = "lumos";
    private static final Boolean ROLLOUT_1_FEATURE_ENABLED_VALUE = true;
    private static final Variation ROLLOUT_1_EVERYONE_ELSE_RULE_ENABLED_VARIATION = new Variation(
        ROLLOUT_1_EVERYONE_ELSE_RULE_ENABLED_VARIATION_ID,
        ROLLOUT_1_EVERYONE_ELSE_RULE_ENABLED_VARIATION_ID,
        ROLLOUT_1_FEATURE_ENABLED_VALUE,
        Collections.singletonList(
            new FeatureVariableUsageInstance(
                VARIABLE_STRING_VARIABLE_ID,
                ROLLOUT_1_EVERYONE_ELSE_RULE_ENABLED_VARIATION_STRING_VALUE
            )
        )
    );
    private static final Experiment ROLLOUT_1_EVERYONE_ELSE_RULE = new Experiment(
        ROLLOUT_1_EVERYONE_ELSE_EXPERIMENT_ID,
        ROLLOUT_1_EVERYONE_ELSE_EXPERIMENT_ID,
        Experiment.ExperimentStatus.RUNNING.toString(),
        ROLLOUT_1_ID,
        Collections.<String>emptyList(),
        null,
        Collections.singletonList(
            ROLLOUT_1_EVERYONE_ELSE_RULE_ENABLED_VARIATION
        ),
        Collections.<String, String>emptyMap(),
        Collections.singletonList(
            new TrafficAllocation(
                ROLLOUT_1_EVERYONE_ELSE_RULE_ENABLED_VARIATION_ID,
                5000
            )
        )
    );
    public static final Rollout ROLLOUT_1 = new Rollout(
        ROLLOUT_1_ID,
        Collections.singletonList(
            ROLLOUT_1_EVERYONE_ELSE_RULE
        )
    );
    public static final FeatureFlag FEATURE_FLAG_SINGLE_VARIABLE_STRING = new FeatureFlag(
        FEATURE_SINGLE_VARIABLE_STRING_ID,
        FEATURE_SINGLE_VARIABLE_STRING_KEY,
        ROLLOUT_1_ID,
        Collections.<String>emptyList(),
        Collections.singletonList(
            VARIABLE_STRING_VARIABLE
        )
    );
    private static final String ROLLOUT_3_ID = "2048875663";
    private static final String ROLLOUT_3_EVERYONE_ELSE_EXPERIMENT_ID = "3794675122";
    private static final String ROLLOUT_3_EVERYONE_ELSE_RULE_ENABLED_VARIATION_ID = "589640735";
    private static final Boolean ROLLOUT_3_FEATURE_ENABLED_VALUE = true;
    public static final Variation ROLLOUT_3_EVERYONE_ELSE_RULE_ENABLED_VARIATION = new Variation(
        ROLLOUT_3_EVERYONE_ELSE_RULE_ENABLED_VARIATION_ID,
        ROLLOUT_3_EVERYONE_ELSE_RULE_ENABLED_VARIATION_ID,
        ROLLOUT_3_FEATURE_ENABLED_VALUE,
        Collections.<FeatureVariableUsageInstance>emptyList()
    );
    public static final Experiment ROLLOUT_3_EVERYONE_ELSE_RULE = new Experiment(
        ROLLOUT_3_EVERYONE_ELSE_EXPERIMENT_ID,
        ROLLOUT_3_EVERYONE_ELSE_EXPERIMENT_ID,
        Experiment.ExperimentStatus.RUNNING.toString(),
        ROLLOUT_3_ID,
        Collections.<String>emptyList(),
        null,
        Collections.singletonList(
            ROLLOUT_3_EVERYONE_ELSE_RULE_ENABLED_VARIATION
        ),
        Collections.<String, String>emptyMap(),
        Collections.singletonList(
            new TrafficAllocation(
                ROLLOUT_3_EVERYONE_ELSE_RULE_ENABLED_VARIATION_ID,
                10000
            )
        )
    );
    public static final Rollout ROLLOUT_3 = new Rollout(
        ROLLOUT_3_ID,
        Collections.singletonList(
            ROLLOUT_3_EVERYONE_ELSE_RULE
        )
    );

    private static final String FEATURE_MULTI_VARIATE_FEATURE_ID = "3263342226";
    public static final String FEATURE_MULTI_VARIATE_FEATURE_KEY = "multi_variate_feature";
    private static final String VARIABLE_FIRST_LETTER_ID = "675244127";
    public static final String VARIABLE_FIRST_LETTER_KEY = "first_letter";
    public static final String VARIABLE_FIRST_LETTER_DEFAULT_VALUE = "H";
    private static final FeatureVariable VARIABLE_FIRST_LETTER_VARIABLE = new FeatureVariable(
        VARIABLE_FIRST_LETTER_ID,
        VARIABLE_FIRST_LETTER_KEY,
        VARIABLE_FIRST_LETTER_DEFAULT_VALUE,
        null,
        FeatureVariable.STRING_TYPE,
        null
    );
    private static final String VARIABLE_REST_OF_NAME_ID = "4052219963";
    private static final String VARIABLE_REST_OF_NAME_KEY = "rest_of_name";
    private static final String VARIABLE_REST_OF_NAME_DEFAULT_VALUE = "arry";
    private static final FeatureVariable VARIABLE_REST_OF_NAME_VARIABLE = new FeatureVariable(
        VARIABLE_REST_OF_NAME_ID,
        VARIABLE_REST_OF_NAME_KEY,
        VARIABLE_REST_OF_NAME_DEFAULT_VALUE,
        null,
        FeatureVariable.STRING_TYPE,
        null
    );
    private static final String VARIABLE_JSON_PATCHED_TYPE_ID = "4111661000";
    public static final String VARIABLE_JSON_PATCHED_TYPE_KEY = "json_patched";
    public static final String VARIABLE_JSON_PATCHED_TYPE_DEFAULT_VALUE = "{\"k1\":\"v1\",\"k2\":3.5,\"k3\":true,\"k4\":{\"kk1\":\"vv1\",\"kk2\":false}}";
    private static final FeatureVariable VARIABLE_JSON_PATCHED_TYPE_VARIABLE = new FeatureVariable(
        VARIABLE_JSON_PATCHED_TYPE_ID,
        VARIABLE_JSON_PATCHED_TYPE_KEY,
        VARIABLE_JSON_PATCHED_TYPE_DEFAULT_VALUE,
        null,
        FeatureVariable.STRING_TYPE,
        FeatureVariable.JSON_TYPE
    );

    private static final String FEATURE_MULTI_VARIATE_FUTURE_FEATURE_ID = "3263342227";
    public static final String FEATURE_MULTI_VARIATE_FUTURE_FEATURE_KEY = "multi_variate_future_feature";
    private static final String VARIABLE_JSON_NATIVE_TYPE_ID = "4111661001";
    public static final String VARIABLE_JSON_NATIVE_TYPE_KEY = "json_native";
    public static final String VARIABLE_JSON_NATIVE_TYPE_DEFAULT_VALUE = "{\"k1\":\"v1\",\"k2\":3.5,\"k3\":true,\"k4\":{\"kk1\":\"vv1\",\"kk2\":false}}";
    private static final FeatureVariable VARIABLE_JSON_NATIVE_TYPE_VARIABLE = new FeatureVariable(
        VARIABLE_JSON_NATIVE_TYPE_ID,
        VARIABLE_JSON_NATIVE_TYPE_KEY,
        VARIABLE_JSON_NATIVE_TYPE_DEFAULT_VALUE,
        null,
        FeatureVariable.JSON_TYPE,
        null
    );
    private static final String VARIABLE_FUTURE_TYPE_ID = "4111661002";
    public static final String VARIABLE_FUTURE_TYPE_KEY = "future_variable";
    public static final String VARIABLE_FUTURE_TYPE_DEFAULT_VALUE = "future_value";
    private static final FeatureVariable VARIABLE_FUTURE_TYPE_VARIABLE = new FeatureVariable(
        VARIABLE_FUTURE_TYPE_ID,
        VARIABLE_FUTURE_TYPE_KEY,
        VARIABLE_FUTURE_TYPE_DEFAULT_VALUE,
        null,
        "future_type",
        null
    );

    private static final String FEATURE_MUTEX_GROUP_FEATURE_ID = "3263342226";
    public static final String FEATURE_MUTEX_GROUP_FEATURE_KEY = "mutex_group_feature";
    private static final String VARIABLE_CORRELATING_VARIATION_NAME_ID = "2059187672";
    private static final String VARIABLE_CORRELATING_VARIATION_NAME_KEY = "correlating_variation_name";
    private static final String VARIABLE_CORRELATING_VARIATION_NAME_DEFAULT_VALUE = "null";
    private static final FeatureVariable VARIABLE_CORRELATING_VARIATION_NAME_VARIABLE = new FeatureVariable(
        VARIABLE_CORRELATING_VARIATION_NAME_ID,
        VARIABLE_CORRELATING_VARIATION_NAME_KEY,
        VARIABLE_CORRELATING_VARIATION_NAME_DEFAULT_VALUE,
        null,
        FeatureVariable.STRING_TYPE,
        null
    );

    // group IDs
    private static final String GROUP_1_ID = "1015968292";
    private static final String GROUP_2_ID = "2606208781";

    // experiments
    private static final String LAYER_BASIC_EXPERIMENT_ID = "1630555626";
    private static final String EXPERIMENT_BASIC_EXPERIMENT_ID = "1323241596";
    public static final String EXPERIMENT_BASIC_EXPERIMENT_KEY = "basic_experiment";
    private static final String VARIATION_BASIC_EXPERIMENT_VARIATION_A_ID = "1423767502";
    private static final String VARIATION_BASIC_EXPERIMENT_VARIATION_A_KEY = "A";
    private static final Variation VARIATION_BASIC_EXPERIMENT_VARIATION_A = new Variation(
        VARIATION_BASIC_EXPERIMENT_VARIATION_A_ID,
        VARIATION_BASIC_EXPERIMENT_VARIATION_A_KEY,
        Collections.<FeatureVariableUsageInstance>emptyList()
    );
    private static final Variation VARIATION_HOLDOUT_VARIATION_OFF = new Variation(
        "$opt_dummy_variation_id",
        "ho_off_key",
        false
    );
    private static final String VARIATION_BASIC_EXPERIMENT_VARIATION_B_ID = "3433458314";
    private static final String VARIATION_BASIC_EXPERIMENT_VARIATION_B_KEY = "B";
    private static final Variation VARIATION_BASIC_EXPERIMENT_VARIATION_B = new Variation(
        VARIATION_BASIC_EXPERIMENT_VARIATION_B_ID,
        VARIATION_BASIC_EXPERIMENT_VARIATION_B_KEY,
        Collections.<FeatureVariableUsageInstance>emptyList()
    );
    private static final String BASIC_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_A = "Harry Potter";
    private static final String BASIC_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_B = "Tom Riddle";
    private static final Experiment EXPERIMENT_BASIC_EXPERIMENT = new Experiment(
        EXPERIMENT_BASIC_EXPERIMENT_ID,
        EXPERIMENT_BASIC_EXPERIMENT_KEY,
        Experiment.ExperimentStatus.RUNNING.toString(),
        LAYER_BASIC_EXPERIMENT_ID,
        Collections.<String>emptyList(),
        null,
        DatafileProjectConfigTestUtils.createListOfObjects(
            VARIATION_BASIC_EXPERIMENT_VARIATION_A,
            VARIATION_BASIC_EXPERIMENT_VARIATION_B
        ),
        DatafileProjectConfigTestUtils.createMapOfObjects(
            DatafileProjectConfigTestUtils.createListOfObjects(
                BASIC_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_A,
                BASIC_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_B
            ),
            DatafileProjectConfigTestUtils.createListOfObjects(
                VARIATION_BASIC_EXPERIMENT_VARIATION_A_KEY,
                VARIATION_BASIC_EXPERIMENT_VARIATION_B_KEY
            )
        ),
        DatafileProjectConfigTestUtils.createListOfObjects(
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
    private static final Holdout HOLDOUT_BASIC_HOLDOUT = new Holdout(
        "10075323428",
        "basic_holdout",
        Holdout.HoldoutStatus.RUNNING.toString(),
        Collections.<String>emptyList(),
        null,
        DatafileProjectConfigTestUtils.createListOfObjects(
            VARIATION_HOLDOUT_VARIATION_OFF
        ),
        DatafileProjectConfigTestUtils.createListOfObjects(
            new TrafficAllocation(
                "327323",
                500
            )
        ),
        null,
        null
    );

    private static final Holdout HOLDOUT_ZERO_TRAFFIC_HOLDOUT = new Holdout(
            "1007532345428",
            "holdout_zero_traffic",
            Holdout.HoldoutStatus.RUNNING.toString(),
            Collections.<String>emptyList(),
            null,
            DatafileProjectConfigTestUtils.createListOfObjects(
                VARIATION_HOLDOUT_VARIATION_OFF
            ),
            DatafileProjectConfigTestUtils.createListOfObjects(
                new TrafficAllocation(
                        "327323",
                        0
                )
            ),
            null,
            null
    );

    private static final Holdout HOLDOUT_INCLUDED_FLAGS_HOLDOUT = new Holdout(
            "1007543323427",
            "holdout_included_flags",
            Holdout.HoldoutStatus.RUNNING.toString(),
            Collections.<String>emptyList(),
            null,
            DatafileProjectConfigTestUtils.createListOfObjects(
                VARIATION_HOLDOUT_VARIATION_OFF
            ),
            DatafileProjectConfigTestUtils.createListOfObjects(
                new TrafficAllocation(
                        "327323",
                        2000
                )
            ),
            DatafileProjectConfigTestUtils.createListOfObjects(
                "4195505407",
                "3926744821",
                "3281420120"
            ),
            null
    );

    private static final Holdout HOLDOUT_EXCLUDED_FLAGS_HOLDOUT = new Holdout(
            "100753234214",
            "holdout_excluded_flags",
            Holdout.HoldoutStatus.RUNNING.toString(),
            Collections.<String>emptyList(),
            null,
            DatafileProjectConfigTestUtils.createListOfObjects(
                VARIATION_HOLDOUT_VARIATION_OFF
            ),
            DatafileProjectConfigTestUtils.createListOfObjects(
                new TrafficAllocation(
                        "327323",
                        1500
                )
            ),
            null,
            DatafileProjectConfigTestUtils.createListOfObjects(
                "2591051011",
                "2079378557",
                "3263342226"
            )
    );

    private static final Holdout HOLDOUT_TYPEDAUDIENCE_HOLDOUT = new Holdout(
            "10075323429",
            "typed_audience_holdout",
            Holdout.HoldoutStatus.RUNNING.toString(),
            DatafileProjectConfigTestUtils.createListOfObjects(
                    AUDIENCE_BOOL_ID,
                    AUDIENCE_INT_ID,
                    AUDIENCE_INT_EXACT_ID,
                    AUDIENCE_DOUBLE_ID
            ),
            AUDIENCE_COMBINATION,
            DatafileProjectConfigTestUtils.createListOfObjects(
                    VARIATION_HOLDOUT_VARIATION_OFF
            ),
            DatafileProjectConfigTestUtils.createListOfObjects(
                new TrafficAllocation(
                    "327323",
                    1000
                )
            ),
            Collections.<String>emptyList(),
            Collections.<String>emptyList()
    );
    private static final String LAYER_TYPEDAUDIENCE_EXPERIMENT_ID = "1630555627";
    private static final String EXPERIMENT_TYPEDAUDIENCE_EXPERIMENT_ID = "1323241597";
    public static final String EXPERIMENT_TYPEDAUDIENCE_EXPERIMENT_KEY = "typed_audience_experiment";
    private static final String VARIATION_TYPEDAUDIENCE_EXPERIMENT_VARIATION_A_ID = "1423767503";
    private static final String VARIATION_TYPEDAUDIENCE_EXPERIMENT_VARIATION_A_KEY = "A";
    private static final Variation VARIATION_TYPEDAUDIENCE_EXPERIMENT_VARIATION_A = new Variation(
        VARIATION_TYPEDAUDIENCE_EXPERIMENT_VARIATION_A_ID,
        VARIATION_TYPEDAUDIENCE_EXPERIMENT_VARIATION_A_KEY,
        Collections.<FeatureVariableUsageInstance>emptyList()
    );
    private static final String VARIATION_TYPEDAUDIENCE_EXPERIMENT_VARIATION_B_ID = "3433458315";
    private static final String VARIATION_TYPEDAUDIENCE_EXPERIMENT_VARIATION_B_KEY = "B";
    private static final Variation VARIATION_TYPEDAUDIENCE_EXPERIMENT_VARIATION_B = new Variation(
        VARIATION_TYPEDAUDIENCE_EXPERIMENT_VARIATION_B_ID,
        VARIATION_TYPEDAUDIENCE_EXPERIMENT_VARIATION_B_KEY,
        Collections.<FeatureVariableUsageInstance>emptyList()
    );

    private static final Experiment EXPERIMENT_TYPEDAUDIENCE_EXPERIMENT = new Experiment(
        EXPERIMENT_TYPEDAUDIENCE_EXPERIMENT_ID,
        EXPERIMENT_TYPEDAUDIENCE_EXPERIMENT_KEY,
        Experiment.ExperimentStatus.RUNNING.toString(),
        LAYER_TYPEDAUDIENCE_EXPERIMENT_ID,
        DatafileProjectConfigTestUtils.createListOfObjects(
            AUDIENCE_BOOL_ID,
            AUDIENCE_INT_ID,
            AUDIENCE_INT_EXACT_ID,
            AUDIENCE_DOUBLE_ID
        ),
        AUDIENCE_COMBINATION,
        DatafileProjectConfigTestUtils.createListOfObjects(
            VARIATION_TYPEDAUDIENCE_EXPERIMENT_VARIATION_A,
            VARIATION_TYPEDAUDIENCE_EXPERIMENT_VARIATION_B
        ),
        Collections.EMPTY_MAP,
        DatafileProjectConfigTestUtils.createListOfObjects(
            new TrafficAllocation(
                VARIATION_TYPEDAUDIENCE_EXPERIMENT_VARIATION_A_ID,
                5000
            ),
            new TrafficAllocation(
                VARIATION_TYPEDAUDIENCE_EXPERIMENT_VARIATION_B_ID,
                10000
            )
        )
    );
    private static final String LAYER_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_ID = "1630555628";
    private static final String EXPERIMENT_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_ID = "1323241598";
    public static final String EXPERIMENT_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_KEY = "typed_audience_experiment_with_and";
    private static final String VARIATION_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_VARIATION_A_ID = "1423767504";
    private static final String VARIATION_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_VARIATION_A_KEY = "A";
    private static final Variation VARIATION_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_VARIATION_A = new Variation(
        VARIATION_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_VARIATION_A_ID,
        VARIATION_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_VARIATION_A_KEY,
        Collections.<FeatureVariableUsageInstance>emptyList()
    );
    private static final String VARIATION_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_VARIATION_B_ID = "3433458316";
    private static final String VARIATION_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_VARIATION_B_KEY = "B";
    private static final Variation VARIATION_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_VARIATION_B = new Variation(
        VARIATION_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_VARIATION_B_ID,
        VARIATION_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_VARIATION_B_KEY,
        Collections.<FeatureVariableUsageInstance>emptyList()
    );

    private static final Experiment EXPERIMENT_TYPEDAUDIENCE_WITH_AND_EXPERIMENT = new Experiment(
        EXPERIMENT_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_ID,
        EXPERIMENT_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_KEY,
        Experiment.ExperimentStatus.RUNNING.toString(),
        LAYER_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_ID,
        DatafileProjectConfigTestUtils.createListOfObjects(
            AUDIENCE_BOOL_ID,
            AUDIENCE_INT_ID,
            AUDIENCE_DOUBLE_ID
        ),
        AUDIENCE_COMBINATION_WITH_AND_CONDITION,
        DatafileProjectConfigTestUtils.createListOfObjects(
            VARIATION_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_VARIATION_A,
            VARIATION_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_VARIATION_B
        ),
        Collections.EMPTY_MAP,
        DatafileProjectConfigTestUtils.createListOfObjects(
            new TrafficAllocation(
                VARIATION_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_VARIATION_A_ID,
                5000
            ),
            new TrafficAllocation(
                VARIATION_TYPEDAUDIENCE_WITH_AND_EXPERIMENT_VARIATION_B_ID,
                10000
            )
        )
    );
    private static final String LAYER_TYPEDAUDIENCE_LEAF_EXPERIMENT_ID = "1630555629";
    private static final String EXPERIMENT_TYPEDAUDIENCE_LEAF_EXPERIMENT_ID = "1323241599";
    public static final String EXPERIMENT_TYPEDAUDIENCE_LEAF_EXPERIMENT_KEY = "typed_audience_experiment_leaf_condition";
    private static final String VARIATION_TYPEDAUDIENCE_LEAF_EXPERIMENT_VARIATION_A_ID = "1423767505";
    private static final String VARIATION_TYPEDAUDIENCE_LEAF_EXPERIMENT_VARIATION_A_KEY = "A";
    private static final Variation VARIATION_TYPEDAUDIENCE_LEAF_EXPERIMENT_VARIATION_A = new Variation(
        VARIATION_TYPEDAUDIENCE_LEAF_EXPERIMENT_VARIATION_A_ID,
        VARIATION_TYPEDAUDIENCE_LEAF_EXPERIMENT_VARIATION_A_KEY,
        Collections.<FeatureVariableUsageInstance>emptyList()
    );
    private static final String VARIATION_TYPEDAUDIENCE_LEAF_EXPERIMENT_VARIATION_B_ID = "3433458317";
    private static final String VARIATION_TYPEDAUDIENCE_LEAF_EXPERIMENT_VARIATION_B_KEY = "B";
    private static final Variation VARIATION_TYPEDAUDIENCE_LEAF_EXPERIMENT_VARIATION_B = new Variation(
        VARIATION_TYPEDAUDIENCE_LEAF_EXPERIMENT_VARIATION_B_ID,
        VARIATION_TYPEDAUDIENCE_LEAF_EXPERIMENT_VARIATION_B_KEY,
        Collections.<FeatureVariableUsageInstance>emptyList()
    );

    private static final Experiment EXPERIMENT_TYPEDAUDIENCE_LEAF_EXPERIMENT = new Experiment(
        EXPERIMENT_TYPEDAUDIENCE_LEAF_EXPERIMENT_ID,
        EXPERIMENT_TYPEDAUDIENCE_LEAF_EXPERIMENT_KEY,
        Experiment.ExperimentStatus.RUNNING.toString(),
        LAYER_TYPEDAUDIENCE_LEAF_EXPERIMENT_ID,
        Collections.<String>emptyList(),
        AUDIENCE_COMBINATION_LEAF_CONDITION,
        DatafileProjectConfigTestUtils.createListOfObjects(
            VARIATION_TYPEDAUDIENCE_LEAF_EXPERIMENT_VARIATION_A,
            VARIATION_TYPEDAUDIENCE_LEAF_EXPERIMENT_VARIATION_B
        ),
        Collections.EMPTY_MAP,
        DatafileProjectConfigTestUtils.createListOfObjects(
            new TrafficAllocation(
                VARIATION_TYPEDAUDIENCE_LEAF_EXPERIMENT_VARIATION_A_ID,
                5000
            ),
            new TrafficAllocation(
                VARIATION_TYPEDAUDIENCE_LEAF_EXPERIMENT_VARIATION_B_ID,
                10000
            )
        )
    );
    private static final String LAYER_FIRST_GROUPED_EXPERIMENT_ID = "3301900159";
    private static final String EXPERIMENT_FIRST_GROUPED_EXPERIMENT_ID = "2738374745";
    private static final String EXPERIMENT_FIRST_GROUPED_EXPERIMENT_KEY = "first_grouped_experiment";
    private static final String VARIATION_FIRST_GROUPED_EXPERIMENT_A_ID = "2377378132";
    private static final String VARIATION_FIRST_GROUPED_EXPERIMENT_A_KEY = "A";
    private static final Variation VARIATION_FIRST_GROUPED_EXPERIMENT_A = new Variation(
        VARIATION_FIRST_GROUPED_EXPERIMENT_A_ID,
        VARIATION_FIRST_GROUPED_EXPERIMENT_A_KEY,
        Collections.<FeatureVariableUsageInstance>emptyList()
    );
    private static final String VARIATION_FIRST_GROUPED_EXPERIMENT_B_ID = "1179171250";
    private static final String VARIATION_FIRST_GROUPED_EXPERIMENT_B_KEY = "B";
    private static final Variation VARIATION_FIRST_GROUPED_EXPERIMENT_B = new Variation(
        VARIATION_FIRST_GROUPED_EXPERIMENT_B_ID,
        VARIATION_FIRST_GROUPED_EXPERIMENT_B_KEY,
        Collections.<FeatureVariableUsageInstance>emptyList()
    );
    private static final String FIRST_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_A = "Harry Potter";
    private static final String FIRST_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_B = "Tom Riddle";
    private static final Experiment EXPERIMENT_FIRST_GROUPED_EXPERIMENT = new Experiment(
        EXPERIMENT_FIRST_GROUPED_EXPERIMENT_ID,
        EXPERIMENT_FIRST_GROUPED_EXPERIMENT_KEY,
        Experiment.ExperimentStatus.RUNNING.toString(),
        LAYER_FIRST_GROUPED_EXPERIMENT_ID,
        Collections.singletonList(AUDIENCE_GRYFFINDOR_ID),
        null,
        DatafileProjectConfigTestUtils.createListOfObjects(
            VARIATION_FIRST_GROUPED_EXPERIMENT_A,
            VARIATION_FIRST_GROUPED_EXPERIMENT_B
        ),
        DatafileProjectConfigTestUtils.createMapOfObjects(
            DatafileProjectConfigTestUtils.createListOfObjects(
                FIRST_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_A,
                FIRST_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_B
            ),
            DatafileProjectConfigTestUtils.createListOfObjects(
                VARIATION_FIRST_GROUPED_EXPERIMENT_A_KEY,
                VARIATION_FIRST_GROUPED_EXPERIMENT_B_KEY
            )
        ),
        DatafileProjectConfigTestUtils.createListOfObjects(
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
    private static final String LAYER_SECOND_GROUPED_EXPERIMENT_ID = "2625300442";
    private static final String EXPERIMENT_SECOND_GROUPED_EXPERIMENT_ID = "3042640549";
    private static final String EXPERIMENT_SECOND_GROUPED_EXPERIMENT_KEY = "second_grouped_experiment";
    private static final String VARIATION_SECOND_GROUPED_EXPERIMENT_A_ID = "1558539439";
    private static final String VARIATION_SECOND_GROUPED_EXPERIMENT_A_KEY = "A";
    private static final Variation VARIATION_SECOND_GROUPED_EXPERIMENT_A = new Variation(
        VARIATION_SECOND_GROUPED_EXPERIMENT_A_ID,
        VARIATION_SECOND_GROUPED_EXPERIMENT_A_KEY,
        Collections.<FeatureVariableUsageInstance>emptyList()
    );
    private static final String VARIATION_SECOND_GROUPED_EXPERIMENT_B_ID = "2142748370";
    private static final String VARIATION_SECOND_GROUPED_EXPERIMENT_B_KEY = "B";
    private static final Variation VARIATION_SECOND_GROUPED_EXPERIMENT_B = new Variation(
        VARIATION_SECOND_GROUPED_EXPERIMENT_B_ID,
        VARIATION_SECOND_GROUPED_EXPERIMENT_B_KEY,
        Collections.<FeatureVariableUsageInstance>emptyList()
    );
    private static final String SECOND_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_A = "Hermione Granger";
    private static final String SECOND_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_B = "Ronald Weasley";
    private static final Experiment EXPERIMENT_SECOND_GROUPED_EXPERIMENT = new Experiment(
        EXPERIMENT_SECOND_GROUPED_EXPERIMENT_ID,
        EXPERIMENT_SECOND_GROUPED_EXPERIMENT_KEY,
        Experiment.ExperimentStatus.RUNNING.toString(),
        LAYER_SECOND_GROUPED_EXPERIMENT_ID,
        Collections.singletonList(AUDIENCE_GRYFFINDOR_ID),
        null,
        DatafileProjectConfigTestUtils.createListOfObjects(
            VARIATION_SECOND_GROUPED_EXPERIMENT_A,
            VARIATION_SECOND_GROUPED_EXPERIMENT_B
        ),
        DatafileProjectConfigTestUtils.createMapOfObjects(
            DatafileProjectConfigTestUtils.createListOfObjects(
                SECOND_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_A,
                SECOND_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_B
            ),
            DatafileProjectConfigTestUtils.createListOfObjects(
                VARIATION_SECOND_GROUPED_EXPERIMENT_A_KEY,
                VARIATION_SECOND_GROUPED_EXPERIMENT_B_KEY
            )
        ),
        DatafileProjectConfigTestUtils.createListOfObjects(
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
    private static final String LAYER_MULTIVARIATE_EXPERIMENT_ID = "3780747876";
    private static final String EXPERIMENT_MULTIVARIATE_EXPERIMENT_ID = "3262035800";
    public static final String EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY = "multivariate_experiment";
    private static final String VARIATION_MULTIVARIATE_EXPERIMENT_FRED_ID = "1880281238";
    private static final String VARIATION_MULTIVARIATE_EXPERIMENT_FRED_KEY = "Fred";
    private static final Boolean VARIATION_MULTIVARIATE_FEATURE_ENABLED_VALUE = true;
    private static final Variation VARIATION_MULTIVARIATE_EXPERIMENT_FRED = new Variation(
        VARIATION_MULTIVARIATE_EXPERIMENT_FRED_ID,
        VARIATION_MULTIVARIATE_EXPERIMENT_FRED_KEY,
        VARIATION_MULTIVARIATE_FEATURE_ENABLED_VALUE,
        DatafileProjectConfigTestUtils.createListOfObjects(
            new FeatureVariableUsageInstance(
                VARIABLE_FIRST_LETTER_ID,
                "F"
            ),
            new FeatureVariableUsageInstance(
                VARIABLE_REST_OF_NAME_ID,
                "red"
            ),
            new FeatureVariableUsageInstance(
                VARIABLE_JSON_PATCHED_TYPE_ID,
                "{\"k1\":\"s1\",\"k2\":103.5,\"k3\":false,\"k4\":{\"kk1\":\"ss1\",\"kk2\":true}}"
            )
        )
    );
    private static final String VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE_ID = "3631049532";
    private static final String VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE_KEY = "Feorge";
    private static final Variation VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE = new Variation(
        VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE_ID,
        VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE_KEY,
        VARIATION_MULTIVARIATE_FEATURE_ENABLED_VALUE,
        DatafileProjectConfigTestUtils.createListOfObjects(
            new FeatureVariableUsageInstance(
                VARIABLE_FIRST_LETTER_ID,
                "F"
            ),
            new FeatureVariableUsageInstance(
                VARIABLE_REST_OF_NAME_ID,
                "eorge"
            ),
            new FeatureVariableUsageInstance(
                VARIABLE_JSON_PATCHED_TYPE_ID,
                "{\"k1\":\"s2\",\"k2\":203.5,\"k3\":true,\"k4\":{\"kk1\":\"ss2\",\"kk2\":true}}"
            )
        )
    );
    private static final String VARIATION_MULTIVARIATE_EXPERIMENT_GRED_ID = "4204375027";
    public static final String VARIATION_MULTIVARIATE_EXPERIMENT_GRED_KEY = "Gred";
    public static final Boolean VARIATION_MULTIVARIATE_VARIATION_FEATURE_ENABLED_GRED_KEY = false;
    public static final Variation VARIATION_MULTIVARIATE_EXPERIMENT_GRED = new Variation(
        VARIATION_MULTIVARIATE_EXPERIMENT_GRED_ID,
        VARIATION_MULTIVARIATE_EXPERIMENT_GRED_KEY,
        VARIATION_MULTIVARIATE_VARIATION_FEATURE_ENABLED_GRED_KEY,
        DatafileProjectConfigTestUtils.createListOfObjects(
            new FeatureVariableUsageInstance(
                VARIABLE_FIRST_LETTER_ID,
                "G"
            ),
            new FeatureVariableUsageInstance(
                VARIABLE_REST_OF_NAME_ID,
                "red"
            ),
            new FeatureVariableUsageInstance(
                VARIABLE_JSON_PATCHED_TYPE_ID,
                "{\"k1\":\"s3\",\"k2\":303.5,\"k3\":true,\"k4\":{\"kk1\":\"ss3\",\"kk2\":false}}"
            )
        )
    );
    private static final String VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE_ID = "2099211198";
    private static final String VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE_KEY = "George";
    private static final Variation VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE = new Variation(
        VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE_ID,
        VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE_KEY,
        VARIATION_MULTIVARIATE_FEATURE_ENABLED_VALUE,
        DatafileProjectConfigTestUtils.createListOfObjects(
            new FeatureVariableUsageInstance(
                VARIABLE_FIRST_LETTER_ID,
                "G"
            ),
            new FeatureVariableUsageInstance(
                VARIABLE_REST_OF_NAME_ID,
                "eorge"
            ),
            new FeatureVariableUsageInstance(
                VARIABLE_JSON_PATCHED_TYPE_ID,
                "{\"k1\":\"s4\",\"k2\":403.5,\"k3\":false,\"k4\":{\"kk1\":\"ss4\",\"kk2\":true}}"
            )
        )
    );
    private static final String MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_FRED = "Fred";
    private static final String MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_FEORGE = "Feorge";
    public static final String MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GRED = "Gred";
    private static final String MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GEORGE = "George";
    private static final Experiment EXPERIMENT_MULTIVARIATE_EXPERIMENT = new Experiment(
        EXPERIMENT_MULTIVARIATE_EXPERIMENT_ID,
        EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY,
        Experiment.ExperimentStatus.RUNNING.toString(),
        LAYER_MULTIVARIATE_EXPERIMENT_ID,
        Collections.singletonList(AUDIENCE_GRYFFINDOR_ID),
        null,
        DatafileProjectConfigTestUtils.createListOfObjects(
            VARIATION_MULTIVARIATE_EXPERIMENT_FRED,
            VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE,
            VARIATION_MULTIVARIATE_EXPERIMENT_GRED,
            VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE
        ),
        DatafileProjectConfigTestUtils.createMapOfObjects(
            DatafileProjectConfigTestUtils.createListOfObjects(
                MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_FRED,
                MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_FEORGE,
                MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GRED,
                MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GEORGE
            ),
            DatafileProjectConfigTestUtils.createListOfObjects(
                VARIATION_MULTIVARIATE_EXPERIMENT_FRED_KEY,
                VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE_KEY,
                VARIATION_MULTIVARIATE_EXPERIMENT_GRED_KEY,
                VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE_KEY
            )
        ),
        DatafileProjectConfigTestUtils.createListOfObjects(
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

    private static final String LAYER_DOUBLE_FEATURE_EXPERIMENT_ID = "1278722008";
    private static final String EXPERIMENT_DOUBLE_FEATURE_EXPERIMENT_ID = "2201520193";
    public static final String EXPERIMENT_DOUBLE_FEATURE_EXPERIMENT_KEY = "double_single_variable_feature_experiment";
    private static final String VARIATION_DOUBLE_FEATURE_PI_VARIATION_ID = "1505457580";
    private static final String VARIATION_DOUBLE_FEATURE_PI_VARIATION_KEY = "pi_variation";
    private static final Boolean VARIATION_DOUBLE_FEATURE_ENABLED_VALUE = true;
    private static final Variation VARIATION_DOUBLE_FEATURE_PI_VARIATION = new Variation(
        VARIATION_DOUBLE_FEATURE_PI_VARIATION_ID,
        VARIATION_DOUBLE_FEATURE_PI_VARIATION_KEY,
        VARIATION_DOUBLE_FEATURE_ENABLED_VALUE,
        Collections.singletonList(
            new FeatureVariableUsageInstance(
                VARIABLE_DOUBLE_VARIABLE_ID,
                "3.14"
            )
        )
    );
    private static final String VARIATION_DOUBLE_FEATURE_EULER_VARIATION_ID = "119616179";
    private static final String VARIATION_DOUBLE_FEATURE_EULER_VARIATION_KEY = "euler_variation";
    private static final Variation VARIATION_DOUBLE_FEATURE_EULER_VARIATION = new Variation(
        VARIATION_DOUBLE_FEATURE_EULER_VARIATION_ID,
        VARIATION_DOUBLE_FEATURE_EULER_VARIATION_KEY,
        Collections.singletonList(
            new FeatureVariableUsageInstance(
                VARIABLE_DOUBLE_VARIABLE_ID,
                "2.718"
            )
        )
    );
    private static final Experiment EXPERIMENT_DOUBLE_FEATURE_EXPERIMENT = new Experiment(
        EXPERIMENT_DOUBLE_FEATURE_EXPERIMENT_ID,
        EXPERIMENT_DOUBLE_FEATURE_EXPERIMENT_KEY,
        Experiment.ExperimentStatus.RUNNING.toString(),
        LAYER_DOUBLE_FEATURE_EXPERIMENT_ID,
        Collections.singletonList(AUDIENCE_SLYTHERIN_ID),
        null,
        DatafileProjectConfigTestUtils.createListOfObjects(
            VARIATION_DOUBLE_FEATURE_PI_VARIATION,
            VARIATION_DOUBLE_FEATURE_EULER_VARIATION
        ),
        Collections.<String, String>emptyMap(),
        DatafileProjectConfigTestUtils.createListOfObjects(
            new TrafficAllocation(
                VARIATION_DOUBLE_FEATURE_PI_VARIATION_ID,
                4000
            ),
            new TrafficAllocation(
                VARIATION_DOUBLE_FEATURE_EULER_VARIATION_ID,
                8000
            )
        )
    );

    private static final String LAYER_PAUSED_EXPERIMENT_ID = "3949273892";
    private static final String EXPERIMENT_PAUSED_EXPERIMENT_ID = "2667098701";
    public static final String EXPERIMENT_PAUSED_EXPERIMENT_KEY = "paused_experiment";
    private static final String VARIATION_PAUSED_EXPERIMENT_CONTROL_ID = "391535909";
    private static final String VARIATION_PAUSED_EXPERIMENT_CONTROL_KEY = "Control";
    private static final Variation VARIATION_PAUSED_EXPERIMENT_CONTROL = new Variation(
        VARIATION_PAUSED_EXPERIMENT_CONTROL_ID,
        VARIATION_PAUSED_EXPERIMENT_CONTROL_KEY,
        Collections.<FeatureVariableUsageInstance>emptyList()
    );
    public static final String PAUSED_EXPERIMENT_FORCED_VARIATION_USER_ID_CONTROL = "Harry Potter";
    private static final Experiment EXPERIMENT_PAUSED_EXPERIMENT = new Experiment(
        EXPERIMENT_PAUSED_EXPERIMENT_ID,
        EXPERIMENT_PAUSED_EXPERIMENT_KEY,
        Experiment.ExperimentStatus.PAUSED.toString(),
        LAYER_PAUSED_EXPERIMENT_ID,
        Collections.<String>emptyList(),
        null,
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
    private static final String LAYER_LAUNCHED_EXPERIMENT_ID = "3587821424";
    private static final String EXPERIMENT_LAUNCHED_EXPERIMENT_ID = "3072915611";
    public static final String EXPERIMENT_LAUNCHED_EXPERIMENT_KEY = "launched_experiment";
    private static final String VARIATION_LAUNCHED_EXPERIMENT_CONTROL_ID = "1647582435";
    private static final String VARIATION_LAUNCHED_EXPERIMENT_CONTROL_KEY = "launch_control";
    private static final Variation VARIATION_LAUNCHED_EXPERIMENT_CONTROL = new Variation(
        VARIATION_LAUNCHED_EXPERIMENT_CONTROL_ID,
        VARIATION_LAUNCHED_EXPERIMENT_CONTROL_KEY,
        Collections.<FeatureVariableUsageInstance>emptyList()
    );
    private static final Experiment EXPERIMENT_LAUNCHED_EXPERIMENT = new Experiment(
        EXPERIMENT_LAUNCHED_EXPERIMENT_ID,
        EXPERIMENT_LAUNCHED_EXPERIMENT_KEY,
        Experiment.ExperimentStatus.LAUNCHED.toString(),
        LAYER_LAUNCHED_EXPERIMENT_ID,
        Collections.<String>emptyList(),
        null,
        Collections.singletonList(VARIATION_LAUNCHED_EXPERIMENT_CONTROL),
        Collections.<String, String>emptyMap(),
        Collections.singletonList(
            new TrafficAllocation(
                VARIATION_LAUNCHED_EXPERIMENT_CONTROL_ID,
                8000
            )
        )
    );
    private static final String LAYER_MUTEX_GROUP_EXPERIMENT_1_LAYER_ID = "3755588495";
    private static final String EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1_EXPERIMENT_ID = "4138322202";
    private static final String EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1_EXPERIMENT_KEY = "mutex_group_2_experiment_1";
    private static final String VARIATION_MUTEX_GROUP_EXP_1_VAR_1_ID = "1394671166";
    private static final String VARIATION_MUTEX_GROUP_EXP_1_VAR_1_KEY = "mutex_group_2_experiment_1_variation_1";
    private static final Boolean VARIATION_MUTEX_GROUP_EXP_FEATURE_ENABLED_VALUE = true;
    private static final Variation VARIATION_MUTEX_GROUP_EXP_1_VAR_1 = new Variation(
        VARIATION_MUTEX_GROUP_EXP_1_VAR_1_ID,
        VARIATION_MUTEX_GROUP_EXP_1_VAR_1_KEY,
        VARIATION_MUTEX_GROUP_EXP_FEATURE_ENABLED_VALUE,
        Collections.singletonList(
            new FeatureVariableUsageInstance(
                VARIABLE_CORRELATING_VARIATION_NAME_ID,
                VARIATION_MUTEX_GROUP_EXP_1_VAR_1_KEY
            )
        )
    );
    public static final Experiment EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1 = new Experiment(
        EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1_EXPERIMENT_ID,
        EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1_EXPERIMENT_KEY,
        Experiment.ExperimentStatus.RUNNING.toString(),
        LAYER_MUTEX_GROUP_EXPERIMENT_1_LAYER_ID,
        Collections.<String>emptyList(),
        null,
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
    private static final String LAYER_MUTEX_GROUP_EXPERIMENT_2_LAYER_ID = "3818002538";
    private static final String EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2_EXPERIMENT_ID = "1786133852";
    private static final String EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2_EXPERIMENT_KEY = "mutex_group_2_experiment_2";
    private static final String VARIATION_MUTEX_GROUP_EXP_2_VAR_1_ID = "1619235542";
    private static final String VARIATION_MUTEX_GROUP_EXP_2_VAR_1_KEY = "mutex_group_2_experiment_2_variation_2";
    private static final Boolean VARIATION_MUTEX_GROUP_EXP_2_FEATURE_ENABLED_VALUE = true;
    public static final Variation VARIATION_MUTEX_GROUP_EXP_2_VAR_1 = new Variation(
        VARIATION_MUTEX_GROUP_EXP_2_VAR_1_ID,
        VARIATION_MUTEX_GROUP_EXP_2_VAR_1_KEY,
        VARIATION_MUTEX_GROUP_EXP_2_FEATURE_ENABLED_VALUE,
        Collections.singletonList(
            new FeatureVariableUsageInstance(
                VARIABLE_CORRELATING_VARIATION_NAME_ID,
                VARIATION_MUTEX_GROUP_EXP_2_VAR_1_KEY
            )
        )
    );
    public static final Experiment EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2 = new Experiment(
        EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2_EXPERIMENT_ID,
        EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2_EXPERIMENT_KEY,
        Experiment.ExperimentStatus.RUNNING.toString(),
        LAYER_MUTEX_GROUP_EXPERIMENT_2_LAYER_ID,
        Collections.<String>emptyList(),
        null,
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

    private static final String EXPERIMENT_WITH_MALFORMED_AUDIENCE_ID = "748215081";
    public static final String EXPERIMENT_WITH_MALFORMED_AUDIENCE_KEY = "experiment_with_malformed_audience";
    private static final String LAYER_EXPERIMENT_WITH_MALFORMED_AUDIENCE_ID = "1238149537";
    private static final String VARIATION_EXPERIMENT_WITH_MALFORMED_AUDIENCE_ID = "535538389";
    public static final String VARIATION_EXPERIMENT_WITH_MALFORMED_AUDIENCE_KEY = "var1";
    private static final Variation VARIATION_EXPERIMENT_WITH_MALFORMED_AUDIENCE = new Variation(
        VARIATION_EXPERIMENT_WITH_MALFORMED_AUDIENCE_ID,
        VARIATION_EXPERIMENT_WITH_MALFORMED_AUDIENCE_KEY,
        Collections.<FeatureVariableUsageInstance>emptyList()
    );
    private static final Experiment EXPERIMENT_WITH_MALFORMED_AUDIENCE = new Experiment(
        EXPERIMENT_WITH_MALFORMED_AUDIENCE_ID,
        EXPERIMENT_WITH_MALFORMED_AUDIENCE_KEY,
        Experiment.ExperimentStatus.RUNNING.toString(),
        LAYER_EXPERIMENT_WITH_MALFORMED_AUDIENCE_ID,
        Collections.singletonList(AUDIENCE_WITH_MISSING_VALUE_ID),
        null,
        Collections.singletonList(VARIATION_EXPERIMENT_WITH_MALFORMED_AUDIENCE),
        Collections.<String, String>emptyMap(),
        Collections.singletonList(
            new TrafficAllocation(
                VARIATION_EXPERIMENT_WITH_MALFORMED_AUDIENCE_ID,
                10000
            )
        )
    );

    // generate groups
    private static final Group GROUP_1 = new Group(
        GROUP_1_ID,
        Group.RANDOM_POLICY,
        DatafileProjectConfigTestUtils.createListOfObjects(
            EXPERIMENT_FIRST_GROUPED_EXPERIMENT,
            EXPERIMENT_SECOND_GROUPED_EXPERIMENT
        ),
        DatafileProjectConfigTestUtils.createListOfObjects(
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
    private static final Group GROUP_2 = new Group(
        GROUP_2_ID,
        Group.RANDOM_POLICY,
        DatafileProjectConfigTestUtils.createListOfObjects(
            EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1,
            EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2
        ),
        DatafileProjectConfigTestUtils.createListOfObjects(
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
    private static final String EVENT_BASIC_EVENT_ID = "3785620495";
    public static final String EVENT_BASIC_EVENT_KEY = "basic_event";
    private static final EventType EVENT_BASIC_EVENT = new EventType(
        EVENT_BASIC_EVENT_ID,
        EVENT_BASIC_EVENT_KEY,
        DatafileProjectConfigTestUtils.createListOfObjects(
            EXPERIMENT_BASIC_EXPERIMENT_ID,
            EXPERIMENT_FIRST_GROUPED_EXPERIMENT_ID,
            EXPERIMENT_SECOND_GROUPED_EXPERIMENT_ID,
            EXPERIMENT_MULTIVARIATE_EXPERIMENT_ID,
            EXPERIMENT_LAUNCHED_EXPERIMENT_ID
        )
    );
    private static final String EVENT_PAUSED_EXPERIMENT_ID = "3195631717";
    public static final String EVENT_PAUSED_EXPERIMENT_KEY = "event_with_paused_experiment";
    private static final EventType EVENT_PAUSED_EXPERIMENT = new EventType(
        EVENT_PAUSED_EXPERIMENT_ID,
        EVENT_PAUSED_EXPERIMENT_KEY,
        Collections.singletonList(
            EXPERIMENT_PAUSED_EXPERIMENT_ID
        )
    );
    private static final String EVENT_LAUNCHED_EXPERIMENT_ONLY_ID = "1987018666";
    public static final String EVENT_LAUNCHED_EXPERIMENT_ONLY_KEY = "event_with_launched_experiments_only";
    private static final EventType EVENT_LAUNCHED_EXPERIMENT_ONLY = new EventType(
        EVENT_LAUNCHED_EXPERIMENT_ONLY_ID,
        EVENT_LAUNCHED_EXPERIMENT_ONLY_KEY,
        Collections.singletonList(
            EXPERIMENT_LAUNCHED_EXPERIMENT_ID
        )
    );

    // rollouts
    public static final String ROLLOUT_2_ID = "813411034";
    private static final Experiment ROLLOUT_2_RULE_1 = new Experiment(
        "3421010877",
        "3421010877",
        Experiment.ExperimentStatus.RUNNING.toString(),
        ROLLOUT_2_ID,
        Collections.singletonList(AUDIENCE_GRYFFINDOR_ID),
        null,
        Collections.singletonList(
            new Variation(
                "521740985",
                "521740985",
                true,
                DatafileProjectConfigTestUtils.createListOfObjects(
                    new FeatureVariableUsageInstance(
                        "675244127",
                        "G"
                    ),
                    new FeatureVariableUsageInstance(
                        "4052219963",
                        "odric"
                    )
                )
            )
        ),
        Collections.<String, String>emptyMap(),
        Collections.singletonList(
            new TrafficAllocation(
                "521740985",
                5000
            )
        )
    );
    private static final Experiment ROLLOUT_2_RULE_2 = new Experiment(
        "600050626",
        "600050626",
        Experiment.ExperimentStatus.RUNNING.toString(),
        ROLLOUT_2_ID,
        Collections.singletonList(AUDIENCE_SLYTHERIN_ID),
        null,
        Collections.singletonList(
            new Variation(
                "180042646",
                "180042646",
                true,
                DatafileProjectConfigTestUtils.createListOfObjects(
                    new FeatureVariableUsageInstance(
                        "675244127",
                        "S"
                    ),
                    new FeatureVariableUsageInstance(
                        "4052219963",
                        "alazar"
                    )
                )
            )
        ),
        Collections.<String, String>emptyMap(),
        Collections.singletonList(
            new TrafficAllocation(
                "180042646",
                5000
            )
        )
    );
    private static final Experiment ROLLOUT_2_RULE_3 = new Experiment(
        "2637642575",
        "2637642575",
        Experiment.ExperimentStatus.RUNNING.toString(),
        ROLLOUT_2_ID,
        Collections.singletonList(AUDIENCE_ENGLISH_CITIZENS_ID),
        null,
        Collections.singletonList(
            new Variation(
                "2346257680",
                "2346257680",
                true,
                DatafileProjectConfigTestUtils.createListOfObjects(
                    new FeatureVariableUsageInstance(
                        "675244127",
                        "D"
                    ),
                    new FeatureVariableUsageInstance(
                        "4052219963",
                        "udley"
                    )
                )
            )
        ),
        Collections.<String, String>emptyMap(),
        Collections.singletonList(
            new TrafficAllocation(
                "2346257680",
                5000
            )
        )
    );
    private static final Experiment ROLLOUT_2_EVERYONE_ELSE_RULE = new Experiment(
        "828245624",
        "828245624",
        Experiment.ExperimentStatus.RUNNING.toString(),
        ROLLOUT_2_ID,
        Collections.<String>emptyList(),
        null,
        Collections.singletonList(
            new Variation(
                "3137445031",
                "3137445031",
                true,
                DatafileProjectConfigTestUtils.createListOfObjects(
                    new FeatureVariableUsageInstance(
                        "675244127",
                        "M"
                    ),
                    new FeatureVariableUsageInstance(
                        "4052219963",
                        "uggle"
                    )
                )
            )
        ),
        Collections.<String, String>emptyMap(),
        Collections.singletonList(
            new TrafficAllocation(
                "3137445031",
                5000
            )
        )
    );
    public static final Rollout ROLLOUT_2 = new Rollout(
        ROLLOUT_2_ID,
        DatafileProjectConfigTestUtils.createListOfObjects(
            ROLLOUT_2_RULE_1,
            ROLLOUT_2_RULE_2,
            ROLLOUT_2_RULE_3,
            ROLLOUT_2_EVERYONE_ELSE_RULE
        )
    );

    // finish features
    public static final FeatureFlag FEATURE_FLAG_MULTI_VARIATE_FEATURE = new FeatureFlag(
        FEATURE_MULTI_VARIATE_FEATURE_ID,
        FEATURE_MULTI_VARIATE_FEATURE_KEY,
        ROLLOUT_2_ID,
        Collections.singletonList(EXPERIMENT_MULTIVARIATE_EXPERIMENT_ID),
        DatafileProjectConfigTestUtils.createListOfObjects(
            VARIABLE_FIRST_LETTER_VARIABLE,
            VARIABLE_REST_OF_NAME_VARIABLE,
            VARIABLE_JSON_PATCHED_TYPE_VARIABLE
        )
    );
    public static final FeatureFlag FEATURE_FLAG_MULTI_VARIATE_FUTURE_FEATURE = new FeatureFlag(
        FEATURE_MULTI_VARIATE_FUTURE_FEATURE_ID,
        FEATURE_MULTI_VARIATE_FUTURE_FEATURE_KEY,
        ROLLOUT_2_ID,
        Collections.singletonList(EXPERIMENT_MULTIVARIATE_EXPERIMENT_ID),
        DatafileProjectConfigTestUtils.createListOfObjects(
            VARIABLE_JSON_NATIVE_TYPE_VARIABLE,
            VARIABLE_FUTURE_TYPE_VARIABLE
        )
    );
    public static final FeatureFlag FEATURE_FLAG_MUTEX_GROUP_FEATURE = new FeatureFlag(
        FEATURE_MUTEX_GROUP_FEATURE_ID,
        FEATURE_MUTEX_GROUP_FEATURE_KEY,
        "",
        DatafileProjectConfigTestUtils.createListOfObjects(
            EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1_EXPERIMENT_ID,
            EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2_EXPERIMENT_ID
        ),
        Collections.singletonList(
            VARIABLE_CORRELATING_VARIATION_NAME_VARIABLE
        )
    );
    public static final FeatureFlag FEATURE_FLAG_SINGLE_VARIABLE_DOUBLE = new FeatureFlag(
        FEATURE_SINGLE_VARIABLE_DOUBLE_ID,
        FEATURE_SINGLE_VARIABLE_DOUBLE_KEY,
        "",
        Collections.singletonList(
            EXPERIMENT_DOUBLE_FEATURE_EXPERIMENT_ID
        ),
        Collections.singletonList(
            VARIABLE_DOUBLE_VARIABLE
        )
    );
    public static final FeatureFlag FEATURE_FLAG_SINGLE_VARIABLE_INTEGER = new FeatureFlag(
        FEATURE_SINGLE_VARIABLE_INTEGER_ID,
        FEATURE_SINGLE_VARIABLE_INTEGER_KEY,
        ROLLOUT_3_ID,
        Collections.<String>emptyList(),
        Collections.singletonList(
            VARIABLE_INTEGER_VARIABLE
        )
    );
    public static final Integration odpIntegration = new Integration("odp", "https://example.com", "test-key");

    public static ProjectConfig generateValidProjectConfigV4() {

        // list attributes
        List<Attribute> attributes = new ArrayList<Attribute>();
        attributes.add(ATTRIBUTE_HOUSE);
        attributes.add(ATTRIBUTE_NATIONALITY);
        attributes.add(ATTRIBUTE_OPT);
        attributes.add(ATTRIBUTE_BOOLEAN);
        attributes.add(ATTRIBUTE_INTEGER);
        attributes.add(ATTRIBUTE_DOUBLE);
        attributes.add(ATTRIBUTE_EMPTY);

        // list audiences
        List<Audience> audiences = new ArrayList<Audience>();
        audiences.add(AUDIENCE_GRYFFINDOR);
        audiences.add(AUDIENCE_SLYTHERIN);
        audiences.add(AUDIENCE_ENGLISH_CITIZENS);
        audiences.add(AUDIENCE_WITH_MISSING_VALUE);

        List<Audience> typedAudiences = new ArrayList<Audience>();
        typedAudiences.add(TYPED_AUDIENCE_BOOL);
        typedAudiences.add(TYPED_AUDIENCE_EXACT_INT);
        typedAudiences.add(TYPED_AUDIENCE_INT);
        typedAudiences.add(TYPED_AUDIENCE_DOUBLE);
        typedAudiences.add(TYPED_AUDIENCE_GRYFFINDOR);
        typedAudiences.add(TYPED_AUDIENCE_SLYTHERIN);
        typedAudiences.add(TYPED_AUDIENCE_ENGLISH_CITIZENS);
        typedAudiences.add(AUDIENCE_WITH_MISSING_VALUE);

        // list events
        List<EventType> events = new ArrayList<EventType>();
        events.add(EVENT_BASIC_EVENT);
        events.add(EVENT_PAUSED_EXPERIMENT);
        events.add(EVENT_LAUNCHED_EXPERIMENT_ONLY);

        // list experiments
        List<Experiment> experiments = new ArrayList<Experiment>();
        experiments.add(EXPERIMENT_BASIC_EXPERIMENT);
        experiments.add(EXPERIMENT_TYPEDAUDIENCE_EXPERIMENT);
        experiments.add(EXPERIMENT_TYPEDAUDIENCE_WITH_AND_EXPERIMENT);
        experiments.add(EXPERIMENT_TYPEDAUDIENCE_LEAF_EXPERIMENT);
        experiments.add(EXPERIMENT_MULTIVARIATE_EXPERIMENT);
        experiments.add(EXPERIMENT_DOUBLE_FEATURE_EXPERIMENT);
        experiments.add(EXPERIMENT_PAUSED_EXPERIMENT);
        experiments.add(EXPERIMENT_LAUNCHED_EXPERIMENT);
        experiments.add(EXPERIMENT_WITH_MALFORMED_AUDIENCE);

        // list featureFlags
        List<FeatureFlag> featureFlags = new ArrayList<FeatureFlag>();
        featureFlags.add(FEATURE_FLAG_BOOLEAN_FEATURE);
        featureFlags.add(FEATURE_FLAG_SINGLE_VARIABLE_DOUBLE);
        featureFlags.add(FEATURE_FLAG_SINGLE_VARIABLE_INTEGER);
        featureFlags.add(FEATURE_FLAG_SINGLE_VARIABLE_BOOLEAN);
        featureFlags.add(FEATURE_FLAG_SINGLE_VARIABLE_STRING);
        featureFlags.add(FEATURE_FLAG_MULTI_VARIATE_FEATURE);
        featureFlags.add(FEATURE_FLAG_MULTI_VARIATE_FUTURE_FEATURE);
        featureFlags.add(FEATURE_FLAG_MUTEX_GROUP_FEATURE);

        List<Group> groups = new ArrayList<Group>();
        groups.add(GROUP_1);
        groups.add(GROUP_2);

        // list rollouts
        List<Rollout> rollouts = new ArrayList<Rollout>();
        rollouts.add(ROLLOUT_1);
        rollouts.add(ROLLOUT_2);
        rollouts.add(ROLLOUT_3);

        List<Integration> integrations = new ArrayList<>();
        integrations.add(odpIntegration);

        return new DatafileProjectConfig(
            ACCOUNT_ID,
            ANONYMIZE_IP,
            SEND_FLAG_DECISIONS,
            BOT_FILTERING,
            REGION,
            PROJECT_ID,
            REVISION,
            SDK_KEY,
            ENVIRONMENT_KEY,
            VERSION,
            attributes,
            audiences,
            typedAudiences,
            events,
            experiments,
            null,
            featureFlags,
            groups,
            rollouts,
            integrations
        );
    }

    public static ProjectConfig generateValidProjectConfigV4_holdout() {

        // list attributes
        List<Attribute> attributes = new ArrayList<Attribute>();
        attributes.add(ATTRIBUTE_HOUSE);
        attributes.add(ATTRIBUTE_NATIONALITY);
        attributes.add(ATTRIBUTE_OPT);
        attributes.add(ATTRIBUTE_BOOLEAN);
        attributes.add(ATTRIBUTE_INTEGER);
        attributes.add(ATTRIBUTE_DOUBLE);
        attributes.add(ATTRIBUTE_EMPTY);

        // list audiences
        List<Audience> audiences = new ArrayList<Audience>();
        audiences.add(AUDIENCE_GRYFFINDOR);
        audiences.add(AUDIENCE_SLYTHERIN);
        audiences.add(AUDIENCE_ENGLISH_CITIZENS);
        audiences.add(AUDIENCE_WITH_MISSING_VALUE);

        List<Audience> typedAudiences = new ArrayList<Audience>();
        typedAudiences.add(TYPED_AUDIENCE_BOOL);
        typedAudiences.add(TYPED_AUDIENCE_EXACT_INT);
        typedAudiences.add(TYPED_AUDIENCE_INT);
        typedAudiences.add(TYPED_AUDIENCE_DOUBLE);
        typedAudiences.add(TYPED_AUDIENCE_GRYFFINDOR);
        typedAudiences.add(TYPED_AUDIENCE_SLYTHERIN);
        typedAudiences.add(TYPED_AUDIENCE_ENGLISH_CITIZENS);
        typedAudiences.add(AUDIENCE_WITH_MISSING_VALUE);

        // list events
        List<EventType> events = new ArrayList<EventType>();
        events.add(EVENT_BASIC_EVENT);
        events.add(EVENT_PAUSED_EXPERIMENT);
        events.add(EVENT_LAUNCHED_EXPERIMENT_ONLY);

        // list experiments
        List<Experiment> experiments = new ArrayList<Experiment>();
        experiments.add(EXPERIMENT_BASIC_EXPERIMENT);
        experiments.add(EXPERIMENT_TYPEDAUDIENCE_EXPERIMENT);
        experiments.add(EXPERIMENT_TYPEDAUDIENCE_WITH_AND_EXPERIMENT);
        experiments.add(EXPERIMENT_TYPEDAUDIENCE_LEAF_EXPERIMENT);
        experiments.add(EXPERIMENT_MULTIVARIATE_EXPERIMENT);
        experiments.add(EXPERIMENT_DOUBLE_FEATURE_EXPERIMENT);
        experiments.add(EXPERIMENT_PAUSED_EXPERIMENT);
        experiments.add(EXPERIMENT_LAUNCHED_EXPERIMENT);
        experiments.add(EXPERIMENT_WITH_MALFORMED_AUDIENCE);

        // list holdouts
        List<Holdout> holdouts = new ArrayList<Holdout>();
        holdouts.add(HOLDOUT_ZERO_TRAFFIC_HOLDOUT);
        holdouts.add(HOLDOUT_INCLUDED_FLAGS_HOLDOUT);
        holdouts.add(HOLDOUT_BASIC_HOLDOUT);
        holdouts.add(HOLDOUT_TYPEDAUDIENCE_HOLDOUT);
        holdouts.add(HOLDOUT_EXCLUDED_FLAGS_HOLDOUT);

        // list featureFlags
        List<FeatureFlag> featureFlags = new ArrayList<FeatureFlag>();
        featureFlags.add(FEATURE_FLAG_BOOLEAN_FEATURE);
        featureFlags.add(FEATURE_FLAG_SINGLE_VARIABLE_DOUBLE);
        featureFlags.add(FEATURE_FLAG_SINGLE_VARIABLE_INTEGER);
        featureFlags.add(FEATURE_FLAG_SINGLE_VARIABLE_BOOLEAN);
        featureFlags.add(FEATURE_FLAG_SINGLE_VARIABLE_STRING);
        featureFlags.add(FEATURE_FLAG_MULTI_VARIATE_FEATURE);
        featureFlags.add(FEATURE_FLAG_MULTI_VARIATE_FUTURE_FEATURE);
        featureFlags.add(FEATURE_FLAG_MUTEX_GROUP_FEATURE);

        List<Group> groups = new ArrayList<Group>();
        groups.add(GROUP_1);
        groups.add(GROUP_2);

        // list rollouts
        List<Rollout> rollouts = new ArrayList<Rollout>();
        rollouts.add(ROLLOUT_1);
        rollouts.add(ROLLOUT_2);
        rollouts.add(ROLLOUT_3);

        List<Integration> integrations = new ArrayList<>();
        integrations.add(odpIntegration);

        return new DatafileProjectConfig(
            ACCOUNT_ID,
            ANONYMIZE_IP,
            SEND_FLAG_DECISIONS,
            BOT_FILTERING,
            REGION,
            PROJECT_ID,
            REVISION,
            SDK_KEY,
            ENVIRONMENT_KEY,
            VERSION,
            attributes,
            audiences,
            typedAudiences,
            events,
            experiments,
            holdouts,
            featureFlags,
            groups,
            rollouts,
            integrations
        );
    }
}
