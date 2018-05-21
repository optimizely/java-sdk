package com.optimizely.ab.faultinjection;

public enum ExceptionSpot {

    none,

    Optimizely_Constructor_spot1,
    Optimizely_Constructor_spot2,
    Optimizely_Constructor_spot3,

    Optimizely_activate1_spot1,

    Optimizely_activate2_spot1,
    Optimizely_activate2_spot2,
    Optimizely_activate2_spot3,

    Optimizely_activate3_spot1,

    Optimizely_activate4_spot1,
    Optimizely_activate4_spot2,

    Optimizely_activate5_spot1,
    Optimizely_activate5_spot2,
    Optimizely_activate5_spot3,

    Optimizely_sendImpression_spot1,
    Optimizely_sendImpression_spot2,
    Optimizely_sendImpression_spot3,

    Optimizely_track1_spot1,

    Optimizely_track2_spot1,

    Optimizely_track3_spot1,
    Optimizely_track3_spot2,
    Optimizely_track3_spot3,
    Optimizely_track3_spot4,
    Optimizely_track3_spot5,
    Optimizely_track3_spot6,

    Optimizely_isFeatureEnabled1_spot1,

    Optimizely_isFeatureEnabled2_spot1,
    Optimizely_isFeatureEnabled2_spot2,
    Optimizely_isFeatureEnabled2_spot3,

    Optimizely_getFeatureVariableBoolean1_spot1,

    Optimizely_getFeatureVariableBoolean2_spot1,
    Optimizely_getFeatureVariableBoolean2_spot2,
    Optimizely_getFeatureVariableBoolean2_spot3,

    Optimizely_getFeatureVariableDouble1_spot1,

    Optimizely_getFeatureVariableDouble2_spot1,
    Optimizely_getFeatureVariableDouble2_spot2,
    Optimizely_getFeatureVariableDouble2_spot3,

    Optimizely_getFeatureVariableInteger1_spot1,

    Optimizely_getFeatureVariableInteger2_spot1,
    Optimizely_getFeatureVariableInteger2_spot2,
    Optimizely_getFeatureVariableInteger2_spot3,

    Optimizely_getFeatureVariableString1_spot1,

    Optimizely_getFeatureVariableString2_spot1,

    Optimizely_getFeatureVariableValueForType_spot1,
    Optimizely_getFeatureVariableValueForType_spot2,
    Optimizely_getFeatureVariableValueForType_spot3,
    Optimizely_getFeatureVariableValueForType_spot4,
    Optimizely_getFeatureVariableValueForType_spot5,

    Optimizely_getEnabledFeatures_spot1,
    Optimizely_getEnabledFeatures_spot2,
    Optimizely_getEnabledFeatures_spot3,

    Optimizely_getVariation1_spot1,

    Optimizely_getVariation2_spot1,

    Optimizely_getVariation3_spot1,

    Optimizely_getVariation4_spot1,
    Optimizely_getVariation4_spot2,
    Optimizely_getVariation4_spot3,
    Optimizely_getVariation4_spot4,
    Optimizely_getVariation4_spot5,

    Optimizely_setForcedVariation_spot1,

    Optimizely_getForcedVariation_spot1,

    Optimizely_getProjectConfig1_spot1,

    Optimizely_getProjectConfig2_spot1,
    Optimizely_getProjectConfig2_spot2,
    Optimizely_getProjectConfig2_spot3,
    Optimizely_getProjectConfig2_spot4,

    Optimizely_getUserProfileService_spot1,

    Optimizely_filterAttribute_spot1,
    Optimizely_filterAttribute_spot2,
    Optimizely_filterAttribute_spot3,
    Optimizely_filterAttribute_spot4,

    Optimizely_validateUserId_spot1,
    Optimizely_validateUserId_spot2,
    Optimizely_validateUserId_spot3,

    Optimizely_builder_spot1,

    // Builder

    Builder_constructor_spot1,
    Builder_withBucketing_spot1,
    Builder_withDecisionService_spot1,
    Builder_withErrorHandler_spot1,
    Builder_withUserProfileService_spot1,
    Builder_withClientEngine_spot1,
    Builder_withClientVersion_spot1,
    Builder_withEventBuilder_spot1,
    Builder_withConfig_spot1,

    Builder_build_spot1,
    Builder_build_spot2,
    Builder_build_spot3,
    Builder_build_spot4,
    Builder_build_spot5,
    Builder_build_spot6,
    Builder_build_spot7,
    Builder_build_spot8,
    Builder_build_spot9,
    Builder_build_spot10,

    Bucketer_constructor_spot1,

    Bucketer_bucketToEntity_spot1,
    Bucketer_bucketToEntity_spot2,
    Bucketer_bucketToEntity_spot3,
    Bucketer_bucketToEntity_spot4,
    Bucketer_bucketToEntity_spot5,

    Bucketer_bucketToExperiment_spot1,
    Bucketer_bucketToExperiment_spot2,
    Bucketer_bucketToExperiment_spot3,
    Bucketer_bucketToExperiment_spot4,
    Bucketer_bucketToExperiment_spot5,

    Bucketer_bucketToVariation_spot1,
    Bucketer_bucketToVariation_spot2,
    Bucketer_bucketToVariation_spot3,
    Bucketer_bucketToVariation_spot4,
    Bucketer_bucketToVariation_spot5,
    Bucketer_bucketToVariation_spot6,
    Bucketer_bucketToVariation_spot7,

    Bucketer_bucket_spot1,
    Bucketer_bucket_spot2,
    Bucketer_bucket_spot3,
    Bucketer_bucket_spot4,
    Bucketer_bucket_spot5,
    Bucketer_bucket_spot6,
    Bucketer_bucket_spot7,

    Bucketer_generateBucketValue_spot1,

    // Decision

    Decision_constructor_spot1,

    Decision_equals_spot1,
    Decision_equals_spot2,
    Decision_equals_spot3,

    Decision_hashCode_spot1,

    Decision_toMap_spot1,
    Decision_toMap_spot2,

    // DecisionService

    DecisionService_constructor_spot1,

    DecisionService_getVariation_spot1,
    DecisionService_getVariation_spot2,
    DecisionService_getVariation_spot3,
    DecisionService_getVariation_spot4,
    DecisionService_getVariation_spot5,
    DecisionService_getVariation_spot6,
    DecisionService_getVariation_spot7,
    DecisionService_getVariation_spot8,
    DecisionService_getVariation_spot9,
    DecisionService_getVariation_spot10,

    DecisionService_getVariationForFeature_spot1,
    DecisionService_getVariationForFeature_spot2,
    DecisionService_getVariationForFeature_spot3,
    DecisionService_getVariationForFeature_spot4,

    DecisionService_getVariationForFeatureInRollout_spot1,
    DecisionService_getVariationForFeatureInRollout_spot2,
    DecisionService_getVariationForFeatureInRollout_spot3,
    DecisionService_getVariationForFeatureInRollout_spot4,
    DecisionService_getVariationForFeatureInRollout_spot5,
    DecisionService_getVariationForFeatureInRollout_spot6,
    DecisionService_getVariationForFeatureInRollout_spot7,
    DecisionService_getVariationForFeatureInRollout_spot8,
    DecisionService_getVariationForFeatureInRollout_spot9,
    DecisionService_getVariationForFeatureInRollout_spot10,
    DecisionService_getVariationForFeatureInRollout_spot11,
    DecisionService_getVariationForFeatureInRollout_spot12,

    DecisionService_getWhitelistedVariation_spot1,
    DecisionService_getWhitelistedVariation_spot2,
    DecisionService_getWhitelistedVariation_spot3,

    DecisionService_getStoredVariation_spot1,
    DecisionService_getStoredVariation_spot2,
    DecisionService_getStoredVariation_spot3,
    DecisionService_getStoredVariation_spot4,

    DecisionService_saveVariation_spot1,
    DecisionService_saveVariation_spot2,
    DecisionService_saveVariation_spot3,
    DecisionService_saveVariation_spot4,
    DecisionService_saveVariation_spot5,

    // FeatureDecision

    FeatureDecision_constructor_spot1,

    FeatureDecision_equals_spot1,
    FeatureDecision_equals_spot2,
    FeatureDecision_equals_spot3,

    FeatureDecision_hashCode_spot1,
    FeatureDecision_hashCode_spot2,

    // UserProfile

    UserProfile_constructor_spot1,

    UserProfile_equals_spot1,
    UserProfile_equals_spot2,
    UserProfile_equals_spot3,

    UserProfile_hashCode_spot1,
    UserProfile_hashCode_spot2,

    UserProfile_toMap_spot1,
    UserProfile_toMap_spot2,
    UserProfile_toMap_spot3,

    //UserProfileUtils

    UserProfileUtils_isValidUserProfileMap_spot1,
    UserProfileUtils_isValidUserProfileMap_spot2,
    UserProfileUtils_isValidUserProfileMap_spot3,
    UserProfileUtils_isValidUserProfileMap_spot4,
    UserProfileUtils_isValidUserProfileMap_spot5,
    UserProfileUtils_isValidUserProfileMap_spot6,

    UserProfileUtils_convertMapToUserProfile_spot1,
    UserProfileUtils_convertMapToUserProfile_spot2,
    UserProfileUtils_convertMapToUserProfile_spot3,
    UserProfileUtils_convertMapToUserProfile_spot4,

    AndCondition_evaluate_spot1,

    AndCondition_toString_spot1,
    AndCondition_toString_spot2,
    AndCondition_toString_spot3,

    AndCondition_equals_spot1,
    AndCondition_equals_spot2,
    AndCondition_equals_spot3,

    AndCondition_hashCode_spot1,

    // Audience

    Audience_constructor_spot1,
    Audience_toString_spot1,

    // NotCondition

    NotCondition_constructor_spot1,
    NotCondition_evaluate_spot1,

    NotCondition_toString_spot1,
    NotCondition_toString_spot2,

    NotCondition_equals_spot1,
    NotCondition_equals_spot2,

    NotCondition_hashCode_spot1,

    // OrCondition

    OrCondition_constructor_spot1,

    OrCondition_evaluate_spot1,
    OrCondition_evaluate_spot2,

    OrCondition_toString_spot1,
    OrCondition_toString_spot2,
    OrCondition_toString_spot3,

    OrCondition_equals_spot1,
    OrCondition_equals_spot2,
    OrCondition_hasCode_spot1,

    // UserAttribute

    UserAttribute_constructor_spot1,
    UserAttribute_evaluate_spot1,
    UserAttribute_evaluate_spot2,

    UserAttribute_toString_spot1,

    UserAttribute_equals_spot1,
    UserAttribute_equals_spot2,

    UserAttribute_hasCode_spot1,

    // Attribute

    Attribute_constructor_spot1,
    Attribute_toString_spot1,

    // EventType

    EventType_constructor_spot1,
    EventType_toString_spot1,

    // Experiment

    Experiment_constructor1_spot1,
    Experiment_constructor2_spot1,

    Experiment_isActive_spot1,
    Experiment_isRunning_spot1,
    Experiment_isLaunched_spot1,
    Experiment_toString_spot1,

    // FeatureFlag

    FeatureFlag_constructor_spot1,
    FeatureFlag_toString_spot1,

    FeatureFlag_equals_spot1,
    FeatureFlag_equals_spot2,

    FeatureFlag_hashCode_spot1,

    // Group
    Group_constructor_spot1,
    Group_toString_spot1,

    // LiveVariable

    LiveVariable_constructor_spot1,
    LiveVariable_toString_spot1,

    LiveVariable_equals_spot1,
    LiveVariable_equals_spot2,

    LiveVariable_hasCode_spot1,

    // LiveVariableUsageInstance

    LiveVariableUsageInstance_constructor_spot1,

    LiveVariableUsageInstance_equals_spot1,
    LiveVariableUsageInstance_equals_spot2,
    LiveVariableUsageInstance_hashCode_spot1,

    // ProjectConfig

    ProjectConfig_constructor1_spot1,
    ProjectConfig_constructor2_spot1,

    ProjectConfig_constructor3_spot1,
    ProjectConfig_constructor3_spot2,
    ProjectConfig_constructor3_spot3,
    ProjectConfig_constructor3_spot4,
    ProjectConfig_constructor3_spot5,

    ProjectConfig_getExperimentForKey_spot1,
    ProjectConfig_getExperimentForKey_spot2,

    ProjectConfig_getEventTypeForName_spot1,
    ProjectConfig_getEventTypeForName_spot2,

    ProjectConfig_getExperimentForVariationId_spot1,

    ProjectConfig_aggregateGroupExperiments_spot1,

    ProjectConfig_getExperimentsForEventKey_spot1,

    ProjectConfig_getAudienceConditionsFromId_spot1,

    ProjectConfig_setForcedVariation_spot1,
    ProjectConfig_setForcedVariation_spot2,
    ProjectConfig_setForcedVariation_spot3,
    ProjectConfig_setForcedVariation_spot4,

    ProjectConfig_getForcedVariation_spot1,
    ProjectConfig_getForcedVariation_spot2,
    ProjectConfig_getForcedVariation_spot3,
    ProjectConfig_getForcedVariation_spot4,

    ProjectConfig_toString_spot1,

    // ProjectConfigUtils

    ProjectConfigUtils_generateNameMapping_spot1,
    ProjectConfigUtils_generateIdMapping_spot1,

    ProjectConfigUtils_generateLiveVariableIdToExperimentsMapping_spot1,
    ProjectConfigUtils_generateLiveVariableIdToExperimentsMapping_spot2,
    ProjectConfigUtils_generateLiveVariableIdToExperimentsMapping_spot3,
    ProjectConfigUtils_generateLiveVariableIdToExperimentsMapping_spot4,

    ProjectConfigUtils_generateVariationToLiveVariableUsageInstancesMap_spot1,
    ProjectConfigUtils_generateVariationToLiveVariableUsageInstancesMap_spot2,
    ProjectConfigUtils_generateVariationToLiveVariableUsageInstancesMap_spot3,
    ProjectConfigUtils_generateVariationToLiveVariableUsageInstancesMap_spot4,

    // Rollout

    Rollout_constructor_spot1,
    Rollout_toString_spot1,


    // TrafficAllocation

    TrafficAllocation_constructor_spot1,
    TrafficAllocation_toString_spot1,

    // Variation

    Variation_constructor1_spot1,
    Variation_constructor2_spot1,
    Variation_constructor3_spot1,

    Variation_is_spot1,

    Variation_toString_spot1,

    // DefaultJsonSerializer

    DefaultJsonSerializer_getInstance_spot1,
    DefaultJsonSerializer_create_spot1,
    DefaultJsonSerializer_isPresent_spot1,

    // GsonSerializer
    GsonSerializer_serialize_spot1,

    // JacksonSerializer
    JacksonSerializer_serialize_spot1,

    // JsonSerializer
    JsonSerializer_serialize_spot1,
    JsonSerializer_serialize_spot2,
    JsonSerializer_serialize_spot3,
    JsonSerializer_serialize_spot4,

    // JsonSimpleSerializer

    JsonSimpleSerializer_serialize_spot1,
    JsonSimpleSerializer_serializeEventBatch_spot1,
    JsonSimpleSerializer_serializeVisitors_spot1,
    JsonSimpleSerializer_serializeVisitor_spot1,
    JsonSimpleSerializer_serializeSnapshots_spot1,
    JsonSimpleSerializer_serializeSnapshot_spot1,
    JsonSimpleSerializer_serializeEvents_spot1,
    JsonSimpleSerializer_serializeEvent_spot1,
    JsonSimpleSerializer_serializeTags_spot1,
    JsonSimpleSerializer_serializeDecision_spot1,
    JsonSimpleSerializer_serializeFeatures_spot1,
    JsonSimpleSerializer_serializeFeature_spot1,
    JsonSimpleSerializer_serializeDecisions_spot1,

    // BuildVersionInfo

    BuildVersionInfo_readVersionNumber_spot1,

    // EventBuilder

    EventBuilder_constructor_spot1,

    EventBuilder_createImpressionEvent_spot1,
    EventBuilder_createImpressionEvent_spot2,
    EventBuilder_createImpressionEvent_spot3,

    EventBuilder_createConversionEvent_spot1,
    EventBuilder_createConversionEvent_spot2,
    EventBuilder_createConversionEvent_spot3,

    EventBuilder_buildAttributeList_spot1,
    EventBuilder_buildAttributeList_spot2,
    EventBuilder_buildAttributeList_spot3,

    // LogEvent

    LogEvent_constructor_spot1,
    LogEvent_toString_spot1,

    // EventTagUtils

    EventTagUtils_getRevenueValue_spot1,
    EventTagUtils_getNumericValue_spot1,

    // ExperimentUtils

    ExperimentUtils_isExperimentActive_spot1,
    ExperimentUtils_isUserInExperiment_spot1,
    ExperimentUtils_isUserInExperiment_spot2,

    // ActivateNotificationListener

    ActivateNotificationListener_notify_spot1,

    // NotificationCenter

    NotificationCenter_constructor_spot1,
    NotificationCenter_addActivateNotificationListener_spot1,
    NotificationCenter_addTrackNotificationListener_spot1,

    NotificationCenter_addNotificationListener_spot1,
    NotificationCenter_addNotificationListener_spot2,
    NotificationCenter_addNotificationListener_spot3,

    NotificationCenter_removeNotificationListener_spot1,
    NotificationCenter_clearAllNotificationListeners_spot1,
    NotificationCenter_clearNotificationListeners_spot1,
    NotificationCenter_sendNotifications_spot1,

    // TrackNotificationListener

    TrackNotificationListener_notify_spot1,

    // AudienceGsonDeserializer

    AudienceGsonDeserializer_deserialize_spot1,
    AudienceGsonDeserializer_deserialize_spot2,
    AudienceGsonDeserializer_deserialize_spot3,

    AudienceGsonDeserializer_parseConditions_spot1,
    AudienceGsonDeserializer_parseConditions_spot2,
    AudienceGsonDeserializer_parseConditions_spot3,

    // AudienceJacksonDeserializer

    AudienceJacksonDeserializer_deserialize_spot1,
    AudienceJacksonDeserializer_deserialize_spot2,
    AudienceJacksonDeserializer_deserialize_spot3,

    AudienceJacksonDeserializer_parseConditions_spot1,
    AudienceJacksonDeserializer_parseConditions_spot2,
    AudienceJacksonDeserializer_parseConditions_spot3,

    // DefaultConfigParser

    DefaultConfigParser_create_spot1,
    DefaultConfigParser_isPresent_spot1,

    // ExperimentGsonDeserializer

    ExperimentGsonDeserializer_deserialize_spot1,

    // FeatureFlagGsonDeserializer

    FeatureFlagGsonDeserializer_deserialize_spot1,

    // GroupGsonDeserializer

    GroupGsonDeserializer_deserialize_spot1,
    GroupGsonDeserializer_deserialize_spot2,
    GroupGsonDeserializer_deserialize_spot3,

    // GroupJacksonDeserializer

    GroupJacksonDeserializer_deserialize_spot1,
    GroupJacksonDeserializer_deserialize_spot2,
    GroupJacksonDeserializer_deserialize_spot3,

    GroupJacksonDeserializer_parseExperiment_spot1,
    GroupJacksonDeserializer_parseExperiment_spot2,
    GroupJacksonDeserializer_parseExperiment_spot3,

    // GsonConfigParser
    GsonConfigParser_parseProjectConfig_spot1,

    // GsonHelpers

    GsonHelpers_parseVariations_spot1,
    GsonHelpers_parseVariations_spot2,
    GsonHelpers_parseVariations_spot3,

    GsonHelpers_parseForcedVariations_spot1,
    GsonHelpers_parseTrafficAllocation_spot1,

    GsonHelpers_parseExperiment1_spot1,
    GsonHelpers_parseExperiment1_spot2,
    GsonHelpers_parseExperiment1_spot3,

    GsonHelpers_parseExperiment2_spot1,

    GsonHelpers_parseFeatureFlag_spot1,
    GsonHelpers_parseFeatureFlag_spot2,
    GsonHelpers_parseFeatureFlag_spot3,

    // JacksonConfigParser

    JacksonConfigParser_parseProjectConfig_spot1,
    JacksonConfigParser_parseProjectConfig_spot2,

    // JsonConfigParser

    JsonConfigParser_parseProjectConfig_spot1,
    JsonConfigParser_parseProjectConfig_spot2,
    JsonConfigParser_parseProjectConfig_spot3,
    JsonConfigParser_parseProjectConfig_spot4,

    JsonConfigParser_parseExperiments_spot1,
    JsonConfigParser_parseExperiments_spot2,
    JsonConfigParser_parseExperiments_spot3,

    JsonConfigParser_parseExperimentIds_spot1,

    JsonConfigParser_parseFeatureFlags_spot1,
    JsonConfigParser_parseFeatureFlags_spot2,

    JsonConfigParser_parseVariations_spot1,
    JsonConfigParser_parseVariations_spot2,

    JsonConfigParser_parseForcedVariations_spot1,

    JsonConfigParser_parseTrafficAllocations_spot1,

    JsonConfigParser_parseAttributes_spot1,

    JsonConfigParser_parseEvents_spot1,

    JsonConfigParser_parseAudiences_spot1,

    JsonConfigParser_parseConditions_spot1,
    JsonConfigParser_parseConditions_spot2,

    JsonConfigParser_parseGroups_spot1,

    JsonConfigParser_parseLiveVariables_spot1,

    JsonConfigParser_parseLiveVariableInstances_spot1,
    JsonConfigParser_parseRollouts_spot1,

    // JsonSimpleConfigParser

    JsonSimpleConfigParser_parseProjectConfig_spot1,
    JsonSimpleConfigParser_parseProjectConfig_spot2,
    JsonSimpleConfigParser_parseProjectConfig_spot3,
    JsonSimpleConfigParser_parseProjectConfig_spot4,

    JsonSimpleConfigParser_parseExperiments_spot1,
    JsonSimpleConfigParser_parseExperiments_spot2,
    JsonSimpleConfigParser_parseExperiments_spot3,

    JsonSimpleConfigParser_parseExperimentIds_spot1,

    JsonSimpleConfigParser_parseFeatureFlags_spot1,
    JsonSimpleConfigParser_parseFeatureFlags_spot2,

    JsonSimpleConfigParser_parseVariations_spot1,
    JsonSimpleConfigParser_parseVariations_spot2,

    JsonSimpleConfigParser_parseForcedVariations_spot1,

    JsonSimpleConfigParser_parseTrafficAllocations_spot1,

    JsonSimpleConfigParser_parseAttributes_spot1,

    JsonSimpleConfigParser_parseEvents_spot1,

    JsonSimpleConfigParser_parseAudiences_spot1,

    JsonSimpleConfigParser_parseConditions_spot1,
    JsonSimpleConfigParser_parseConditions_spot2,

    JsonSimpleConfigParser_parseGroups_spot1,

    JsonSimpleConfigParser_parseLiveVariables_spot1,

    JsonSimpleConfigParser_parseLiveVariableInstances_spot1,
    JsonSimpleConfigParser_parseRollouts_spot1,

    // ProjectConfigGsonDeserializer

    ProjectConfigGsonDeserializer_deserialize_spot1,
    ProjectConfigGsonDeserializer_deserialize_spot2,
    ProjectConfigGsonDeserializer_deserialize_spot3,
    ProjectConfigGsonDeserializer_deserialize_spot4,

    // ProjectConfigJacksonDeserializer

    ProjectConfigJacksonDeserializer_deserialize_spot1,
    ProjectConfigJacksonDeserializer_deserialize_spot2,
    ProjectConfigJacksonDeserializer_deserialize_spot3,
    ProjectConfigJacksonDeserializer_deserialize_spot4;

    public String getReadableName() {
        return name().replaceAll("_", " ");
    }
}