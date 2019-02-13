# Optimizely Java X SDK Changelog

## 3.0.0
February 13, 2019

The 3.0 release improves event tracking and supports additional audience targeting functionality.
### New Features:
* Event tracking:
    * The Track method now dispatches its conversion event _unconditionally_, without first determining whether the user is targeted by a known experiment that uses the event. This may increase outbound network traffic.
    * In Optimizely results, conversion events sent by 3.0 SDKs are automatically attributed to variations that the user has previously seen, as long as our backend has actually received the impression events for those variations.
    * Altogether, this allows you to track conversion events and attribute them to variations even when you don't know all of a user's attribute values, and even if the user's attribute values or the experiment's configuration have changed such that the user is no longer affected by the experiment. As a result, **you may observe an increase in the conversion rate for previously-instrumented events.** If that is undesirable, you can reset the results of previously-running experiments after upgrading to the 3.0 SDK.
    * This will also allow you to attribute events to variations from other Optimizely projects in your account, even though those experiments don't appear in the same datafile.
    * Note that for results segmentation in Optimizely results, the user attribute values from one event are automatically applied to all other events in the same session, as long as the events in question were actually received by our backend. This behavior was already in place and is not affected by the 3.0 release.
* Support for all types of attribute values, not just strings:
    * All values are passed through to notification listeners.
    * Strings, booleans, and valid numbers are passed to the event dispatcher and can be used for Optimizely results segmentation. A valid number is a finite number in the inclusive range [-2⁵³, 2⁵³]. 
    * Strings, booleans, and valid numbers are relevant for audience conditions.
* Support for additional matchers in audience conditions:
    * An `exists` matcher that passes if the user has a non-null value for the targeted user attribute and fails otherwise.
    * A `substring` matcher that resolves if the user has a string value for the targeted attribute.
    * `gt` (greater than) and `lt` (less than) matchers that resolve if the user has a valid number value for the targeted attribute. A valid number is a finite number in the inclusive range [-2⁵³, 2⁵³].
    * The original (`exact`) matcher can now be used to target booleans and valid numbers, not just strings.
* Support for A/B tests, feature tests, and feature rollouts whose audiences are combined using `"and"` and `"not"` operators, not just the `"or"` operator.
* Datafile-version compatibility check: The SDK will remain uninitialized (i.e., will gracefully fail to activate experiments and features) if given a datafile version greater than 4.
* Updated Pull Request template and commit message guidelines.
* When given an invalid datafile, the Optimizely client object now instantiates into a no-op state instead of throwing a `ConfigParseException`. This matches the behavior of the other Optimizely SDKs.
* Support for graceful shutdown in the default, async event dispatcher.
### Breaking Changes:
* Java 7 is no longer supported.
* Previously, notification listeners were only given string-valued user attributes because only strings could be passed into various method calls. That is no longer the case. The `ActivateNotificationListener` and `TrackNotificationListener` interfaces now receive user attributes as `Map<String, ?>` instead of `Map<String, String>`.
### Bug Fixes:
* Experiments and features can no longer activate when a negatively targeted attribute has a missing, null, or malformed value.
* Audience conditions (except for the new `exists` matcher) no longer resolve to `false` when they fail to find an legitimate value for the targeted user attribute. The result remains `null` (unknown). Therefore, an audience that negates such a condition (using the `"not"` operator) can no longer resolve to `true` unless there is an unrelated branch in the condition tree that itself resolves to `true`.
* Support for empty user IDs.
* Wrap the buffer reader in try...catch.

## 2.1.4

December 6th, 2018

### Bug Fixes
* fix/wrap in try catch for getting build version in static init which might crash ([#241](https://github.com/optimizely/java-sdk/pull/241))

## 3.0.0-RC2

November 20th, 2018

This is the release candidate for the 3.0 SDK, which includes a number of improvements to audience targeting along with a few bug fixes.

### New Features
* Support for number-valued and boolean-valued attributes. ([#213](https://github.com/optimizely/java-sdk/pull/213))
* Support for audiences with new match conditions for attribute values, including “substring” and “exists” matches for strings; “greater than”, “less than”, exact, and “exists” matches for numbers; and “exact”, and “exists” matches for booleans. 
* Built-in datafile version compatibility checks so that SDKs will not initialize with a newer datafile it is not compatible with. ([#209](https://github.com/optimizely/java-sdk/pull/209))
* Audience combinations within an experiment are unofficially supported in this release.
* Refactor EventDispatcher to handle graceful shutdown via a call to AsyncEventHandler.shutdownAndAwaitTermination.

### Breaking Changes
* Previously, notification listeners filtered non-string attribute values from the data passed to registered listeners. To support our growing list of supported attribute values, we’ve changed this behavior. Notification listeners will now post any value type passed as an attribute. Therefore, the interface of the notification listeners has changed to accept a `Map<String, ?>`.
* Update to use Java 1.7 ([#208](https://github.com/optimizely/java-sdk/pull/208))

### Bug Fixes
* refactor: Performance improvements for JacksonConfigParser ([#209](https://github.com/optimizely/java-sdk/pull/209))
* refactor: typeAudience.combinations will not be string encoded like audience.combinations.  To handle this we created a new parsing type TypedAudience.
* fix for exact match when dealing with integers and doubles.  Created a new Numeric match type.
* make a copy of attributes passed in to avoid any concurrency problems. Addresses GitHub isue in Optimizely Andriod SDK.
* allow single root node for audience.conditions, typedAudience.conditions, and Experiment.audienceCombinations.
 
## 3.0.0-RC

November 7th, 2018

This is the release candidate for the 3.0 SDK, which includes a number of improvements to audience targeting along with a few bug fixes.

### New Features
* Support for number-valued and boolean-valued attributes. ([#213](https://github.com/optimizely/java-sdk/pull/213))
* Support for audiences with new match conditions for attribute values, including “substring” and “exists” matches for strings; “greater than”, “less than”, exact, and “exists” matches for numbers; and “exact”, and “exists” matches for booleans. 
* Built-in datafile version compatibility checks so that SDKs will not initialize with a newer datafile it is not compatible with. ([#209](https://github.com/optimizely/java-sdk/pull/209))
* Audience combinations within an experiment are unofficially supported in this release.

### Breaking Changes
* Previously, notification listeners filtered non-string attribute values from the data passed to registered listeners. To support our growing list of supported attribute values, we’ve changed this behavior. Notification listeners will now post any value type passed as an attribute. Therefore, the interface of the notification listeners has changed to accept a `Map<String, ?>`.
* Update to use Java 1.7 ([#208](https://github.com/optimizely/java-sdk/pull/208))

### Bug Fixes
* refactor: Performance improvements for JacksonConfigParser ([#209](https://github.com/optimizely/java-sdk/pull/209))
* refactor: typeAudience.combinations will not be string encoded like audience.combinations.  To handle this we created a new parsing type TypedAudience.
* fix for exact match when dealing with integers and doubles.  Created a new Numeric match type.
* make a copy of attributes passed in to avoid any concurrency problems. Addresses GitHub isue in Optimizely Andriod SDK.

## 3.0.0-alpha

October 10th, 2018

This is the alpha release of the 3.0 SDK, which includes a number of improvements to audience targeting along with a few bug fixes.

### New Features
* Support for number-valued and boolean-valued attributes. ([#213](https://github.com/optimizely/java-sdk/pull/213))
* Support for audiences with new match conditions for attribute values, including “substring” and “exists” matches for strings; “greater than”, “less than”, exact, and “exists” matches for numbers; and “exact”, and “exists” matches for booleans. 
* Built-in datafile version compatibility checks so that SDKs will not initialize with a newer datafile it is not compatible with. ([#209](https://github.com/optimizely/java-sdk/pull/209))

### Breaking Changes
* Previously, notification listeners filtered non-string attribute values from the data passed to registered listeners. To support our growing list of supported attribute values, we’ve changed this behavior. Notification listeners will now post any value type passed as an attribute. Therefore, the interface of the notification listeners has changed to accept a `Map<String, ?>`.
* Update to use Java 1.7 ([#208](https://github.com/optimizely/java-sdk/pull/208))

### Bug Fixes
* refactor: Performance improvements for JacksonConfigParser ([#209](https://github.com/optimizely/java-sdk/pull/209))

## 2.1.3

September 21st, 2018

### Bug Fixes
* fix(attributes): Filters out attributes with null values from the event payload ([#204](https://github.com/optimizely/java-sdk/pull/204))

## 2.1.2

August 1st, 2018

### Bug Fixes
* Move serialization to LogEvent.getBody() to improve performance of API calls ([#201](https://github.com/optimizely/java-sdk/pull/201))

## 2.1.1

June 19th, 2018

This is a patch release of the Optimizely SDK for 2.1.0 which is a major release.

### Bug Fixes
* Send impression event for Feature Test with Feature disabled ([#193](https://github.com/optimizely/java-sdk/pull/193))

## 2.0.2

June 19th, 2018

This is a patch release of the Optimizely SDK for 2.0.0 which is a major release.

### Bug Fixes
* Send impression event for Feature Test with Feature disabled ([#193](https://github.com/optimizely/java-sdk/pull/193))

## 2.1.0

June 15th, 2018

* Introduces support for bot filtering.

## 2.0.1

April 25th, 2018

This is a patch release of the Optimizely SDK for 2.0.0 which is a major release.

### Bug Fixes
* Checking for invalid variables passed into getEnabledFeature, Activate, getVariation and track.

## 2.0.0

April 12th, 2018

This major release of the Optimizely SDK introduces APIs for Feature Management. It also introduces some breaking changes listed below.

### New Features
* Introduces the `isFeatureEnabled` API to determine whether to show a feature to a user or not.
```
Boolean enabled = optimizelyClient.isFeatureEnabled("my_feature_key", "user_1", userAttributes);
```

* You can also get all the enabled features for the user by calling the following method which returns a list of strings representing the feature keys:
```
ArrayList<String> enabledFeatures = optimizelyClient.getEnabledFeatures("user_1", userAttributes);
```

* Introduces Feature Variables to configure or parameterize your feature. There are four variable types: `Integer`, `String`, `Double`, `Boolean`.
```
String stringVariable = optimizelyClient.getFeatureVariableString("my_feature_key", "string_variable_key", "user_1");
Integer integerVariable = optimizelyClient.getFeatureVariableInteger("my_feature_key", "integer_variable_key", "user_1");
Double doubleVariable = optimizelyClient.getFeatureVariableDouble("my_feature_key", "double_variable_key", "user_1");
Boolean booleanVariable = optimizelyClient.getFeatureVariableBoolean("my_feature_key", "boolean_variable_key", "user_1");
```

### Breaking changes
* The `track` API with revenue value as a stand-alone parameter has been removed. The revenue value should be passed in as an entry of the event tags map. The key for the revenue tag is `revenue` and will be treated by Optimizely as the key for analyzing revenue data in results.
```
Map<String, Object> eventTags = new HashMap<String, Object>();

// reserved "revenue" tag
eventTags.put("revenue", 6432);

optimizelyClient.track("event_key", "user_id", userAttributes, eventTags);
```

* We have removed deprecated classes with the `NotificationBroadcaster` in favor of the new API with the `NotificationCenter`. We have streamlined the API so that it is easily usable with Java Lambdas in *Java 1.8+*. We have also added some convenience methods to add these listeners. Finally, some of the API names have changed slightly (e.g. `clearAllNotifications()` is now `clearAllNotificationListeners()`)

## 2.0.0-beta6

March 29th, 2018

This major release of the Optimizely SDK introduces APIs for Feature Management. It also introduces some breaking changes listed below.

### New Features
* Introduces the `isFeatureEnabled` API to determine whether to show a feature to a user or not.
```
Boolean enabled = optimizelyClient.isFeatureEnabled("my_feature_key", "user_1", userAttributes);
```

* You can also get all the enabled features for the user by calling the following method which returns a list of strings representing the feature keys:
```
ArrayList<String> enabledFeatures = optimizelyClient.getEnabledFeatures("user_1", userAttributes);
```

* Introduces Feature Variables to configure or parameterize your feature. There are four variable types: `Integer`, `String`, `Double`, `Boolean`.
```
String stringVariable = optimizelyClient.getFeatureVariableString("my_feature_key", "string_variable_key", "user_1");
Integer integerVariable = optimizelyClient.getFeatureVariableInteger("my_feature_key", "integer_variable_key", "user_1");
Double doubleVariable = optimizelyClient.getFeatureVariableDouble("my_feature_key", "double_variable_key", "user_1");
Boolean booleanVariable = optimizelyClient.getFeatureVariableBoolean("my_feature_key", "boolean_variable_key", "user_1");
```

### Breaking changes
* The `track` API with revenue value as a stand-alone parameter has been removed. The revenue value should be passed in as an entry of the event tags map. The key for the revenue tag is `revenue` and will be treated by Optimizely as the key for analyzing revenue data in results.
```
Map<String, Object> eventTags = new HashMap<String, Object>();

// reserved "revenue" tag
eventTags.put("revenue", 6432);

optimizelyClient.track("event_key", "user_id", userAttributes, eventTags);
```

## 1.9.0

January 30, 2018

This release adds support for bucketing id (By passing in `$opt_bucketing_id` in the attribute map to override the user id as the bucketing variable. This is useful when wanting a set of users to share the same experience such as two players in a game).

This release also depricates the old notification broadcaster in favor of a notification center that supports a wide range of notifications.  The notification listener is now registered for the specific notification type such as ACTIVATE and TRACK.  This is accomplished by allowing for a variable argument call to notify (a new var arg method added to the NotificationListener).  Specific abstract classes exist for the associated notification type (ActivateNotification and TrackNotification).  These abstract classes enforce the strong typing that exists in Java.  You may also add custom notification types and fire them through the notification center.  The notification center is implemented using this var arg approach in all Optimizely SDKs.

### New Features

- Added `$opt_bucketing_id` in the attribute map for overriding bucketing using the user id.  It is available as a static string in DecisionService.ATTRIBUTE_BUCKETING_ID
- Optimizely notification center for activate and track notifications.

## 2.0.0 Beta 3
January 5, 2018

This is a patch release for 2.0.0 Beta. It contains a minor bug fix.

### Bug Fixes
SDK checks for null values in the Feature API parameters.
- If `isFeatureEnabled` is called with a null featureKey or a null userId, it will return false immediately.
- If any of `getFeatureVariable<Type>` are called with a null featureKey, variableKey, or userId, null will be returned immediately.

## 1.8.1
December 12, 2017

This is a patch release for 1.8.0.  It contains two bug fixes mentioned below.

### Bug Fixes
SDK returns NullPointerException when activating with unknown attribute.

Pooled connection times out if it is idle for a long time (AsyncEventHandler's HttpClient uses PoolingHttpClientConnectionManager setting a validate interval).

## 2.0.0 Beta 2
October 5, 2017

This release is a second beta release supporting feature flags and rollouts. It includes all the same new features and breaking changes as the last beta release.

### Bug Fixes
Fall back to default feature variable value when there is no variable usage in the variation a user is bucketed into. For more information see [PR #149](https://github.com/optimizely/java-sdk/pull/149).

## 2.0.0 Beta
September 29, 2017

This release is a beta release supporting feature flags and rollouts.

### New Features
#### Feature Flag Accessors
You can now use feature flags in the Java SDK. You can experiment on features and rollout features through the Optimizely UI.

- `isFeatureEnabled`
- `getFeatureVariableBoolean`
- `getFeatureVariableDouble`
- `getFeatureVariableInteger`
- `getFeatureVariableString`

### Breaking Changes

- Remove Live Variables accessors
  - `getVariableString`
  - `getVariableBoolean`
  - `getVariableInteger`
  - `getVariableDouble`
- Remove track with revenue as a parameter. Pass the revenue value as an event tag instead
  - `track(String, String, long)`
  - `track(String, String, Map<String, String>, long)`
- We will no longer run all unit tests in travis-ci against Java 7.
  We will still continue to set `sourceCompatibility` and `targetCompatibility` to 1.6 so that we build for Java 6.

## 1.8.0

August 29, 2017

This release adds support for numeric metrics and forced bucketing (in code as opposed to whitelisting via project file).

### New Features

- Added `setForcedVariation` and `getForcedVariation`
- Added any numeric metric to event metrics.

### Breaking Changes

- Nothing breaking from 1.7.0

## 1.7.0

July 12, 2017

This release will support Android SDK release 1.4.0

### New Features

- Added `UserProfileService` interface to allow for sticky bucketing

### Breaking Changes

- Removed `UserProfile` interface. Replaced with `UserProfileService` interface.
- Removed support for v1 datafiles.

## 2.0.0-alpha

May 19, 2017

### New Features

- Added `UserProfileService` interface to allow for sticky bucketing

### Breaking Changes

- Removed `UserProfile` interface. Replaced with `UserProfileService` interface.
- Removed support for v1 datafiles.

## 1.6.0

March 17, 2017

- Add event tags to `track` API and include in the event payload
- Deprecates the `eventValue` parameter from the `track` method. Should use event tags to pass in event value instead
- Gracefully handle a null attributes parameter
- Gracefully handle a null/empty datafile when using the Gson parser

## 1.5.0

February 16, 2017

- Support Android TV SDK client engine

## 1.4.1

February 1, 2017

- Default `null` status in datafile to `Not started`

## 1.4.0

January 31, 2017

- Add `sessionId` parameter to `activate` and `track` and include in event payload
- Append datafile `revision` to event payload
- Add support for "Launched" experiment status

## 1.3.0

January 17, 2017

- Add `onEventTracked` listener
- Change `getVariableFloat` to `getVariableDouble`
- Persist experiment and variation IDs instead of keys in the `UserProfile`

## 1.2.0

December 15, 2016

- Change position of `activateExperiment` parameter in the method signatures of `getVariableString`, `getVariableBoolean`, `getVariableInteger`, and `getVariableFloat`
- Change `UserExperimentRecord` to `UserProfile`
- Add support for IP anonymization
- Add `NotificationListener` for SDK events

## 1.1.0

December 8, 2016

- Add support for live variables

## 1.0.3

November 28, 2016

- Remove extraneous log message in `AsyncEventHandler`
- Add `jackson-annotations` as a compiled dependency

## 1.0.2

October 5, 2016

- Gracefully handle datafile that doesn't contain required fields

## 1.0.1

October 5, 2016

- Allow for configurability of `clientEngine` and `clientVersion` through `Optimizely.Builder`
- Remove ppid query string from V1 events

## 1.0.0

October 3, 2016

- Introduce support for Full Stack projects in Optimizely X with no breaking changes from previous version
- Update whitelisting to take precedence over audience condition evaluation
- Introduce more graceful exception handling in instantiation and core methods

## 0.1.71

September 19, 2016

- Add support for v2 backend endpoint and datafile

## 0.1.70

August 29, 2016

- Add a `UserExperimentRecord` interface
    - Implementors will get a chance to save and restore activations during bucketing
    - Can be used to make bucketing persistent or to keep a bucketing history
    - Pass implementations to `Optimizely.Builder#withUserExperimentRecord(UserExperimentRecord)` when creating `Optimizely` instances

## 0.1.68

July 26, 2016

- Beta release of the Java SDK for server-side testing
