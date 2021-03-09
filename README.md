Optimizely Java SDK
===================
[![Build Status](https://travis-ci.org/optimizely/java-sdk.svg?branch=master)](https://travis-ci.org/optimizely/java-sdk)
[![Apache 2.0](https://img.shields.io/badge/license-APACHE%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

This repository houses the Java SDK for use with Optimizely Full Stack and Optimizely Rollouts.

Optimizely Full Stack is A/B testing and feature flag management for product development teams. Experiment in any application. Make every feature on your roadmap an opportunity to learn. Learn more at https://www.optimizely.com/platform/full-stack/, or see the [documentation](https://docs.developers.optimizely.com/full-stack/docs).

Optimizely Rollouts is free feature flags for development teams. Easily roll out and roll back features in any application without code deploys. Mitigate risk for every feature on your roadmap. Learn more at https://www.optimizely.com/rollouts/, or see the [documentation](https://docs.developers.optimizely.com/rollouts/docs).


## Getting Started

### Installing the SDK

#### Gradle

The SDK is available through Bintray and is created with source and target compatibility of 1.8. The core-api and httpclient Bintray packages are [optimizely-sdk-core-api](https://bintray.com/optimizely/optimizely/optimizely-sdk-core-api)
and [optimizely-sdk-httpclient](https://bintray.com/optimizely/optimizely/optimizely-sdk-httpclient) respectively. To install, place the
following in your `build.gradle` and substitute `VERSION` for the latest SDK version available via MavenCentral.

---
**NOTE**

[Bintray/JCenter will be shut down](https://jfrog.com/blog/into-the-sunset-bintray-jcenter-gocenter-and-chartcenter/). The publish repository has been migrated to MavenCentral for the SDK version 3.8.1 or later. Older versions will be available in JCenter until February 1st, 2022.

---


```
repositories {
  mavenCentral()
  jcenter()
}

dependencies {
  compile 'com.optimizely.ab:core-api:{VERSION}'
  compile 'com.optimizely.ab:core-httpclient-impl:{VERSION}'
  // The SDK integrates with multiple JSON parsers, here we use
  // Jackson.
  compile 'com.fasterxml.jackson.core:jackson-core:2.7.1'
  compile 'com.fasterxml.jackson.core:jackson-annotations:2.7.1'
  compile 'com.fasterxml.jackson.core:jackson-databind:2.7.1'
}
```  

#### Dependencies

`core-api` requires [org.slf4j:slf4j-api:1.7.16](https://mvnrepository.com/artifact/org.slf4j/slf4j-api/1.7.16) and a supported JSON parser. 
We currently integrate with [Jackson](https://github.com/FasterXML/jackson), [GSON](https://github.com/google/gson), [json.org](http://www.json.org),
and [json-simple](https://code.google.com/archive/p/json-simple); if any of those packages are available at runtime, they will be used by `core-api`.
If none of those packages are already provided in your project's classpath, one will need to be added. `core-httpclient-impl` is an optional 
dependency that implements the event dispatcher and requires [org.apache.httpcomponents:httpclient:4.5.2](https://mvnrepository.com/artifact/org.apache.httpcomponents/httpclient/4.5.2).
The supplied `pom` files on Bintray define module dependencies.

### Feature Management Access
To access the Feature Management configuration in the Optimizely dashboard, please contact your Optimizely account executive.

### Using the SDK

See the Optimizely Full Stack [developer documentation](http://developers.optimizely.com/server/reference/index.html) to learn how to set
up your first Java project and use the SDK.

## Development

### Building the SDK

To build local jars which are outputted into the respective modules' `build/lib` directories:

```
./gradlew build
```

### Unit tests

#### Running all tests

You can run all unit tests with:

```
./gradlew test
```

### Checking for bugs

We utilize [FindBugs](http://findbugs.sourceforge.net/) to identify possible bugs in the SDK. To run the check:

```
./gradlew check
```

### Benchmarking

[JMH](http://openjdk.java.net/projects/code-tools/jmh/) benchmarks can be run through gradle:

```
./gradlew core-api:jmh
```

Results are generated in `$buildDir/reports/jmh`.

### Contributing

Please see [CONTRIBUTING](CONTRIBUTING.md).

### Credits

First-party code (under core-api/ and core-httpclient-impl) is copyright Optimizely, Inc. and contributors, licensed under Apache 2.0.

### Additional Code

This software incorporates code from the following open source projects:

#### core-api module

**SLF4J** [https://www.slf4j.org ](https://www.slf4j.org)  
Copyright &copy; 2004-2017 QOS.ch  
License (MIT): [https://www.slf4j.org/license.html](https://www.slf4j.org/license.html)

**Jackson Annotations** [https://github.com/FasterXML/jackson-annotations](https://github.com/FasterXML/jackson-annotations)  
License (Apache 2.0): [https://github.com/FasterXML/jackson-annotations/blob/master/src/main/resources/META-INF/LICENSE](https://github.com/FasterXML/jackson-annotations/blob/master/src/main/resources/META-INF/LICENSE)

**Gson** [https://github.com/google/gson ](https://github.com/google/gson)  
Copyright &copy; 2008 Google Inc.
License (Apache 2.0): [https://github.com/google/gson/blob/master/LICENSE](https://github.com/google/gson/blob/master/LICENSE)

**JSON-java** [https://github.com/stleary/JSON-java](https://github.com/stleary/JSON-java)  
Copyright &copy; 2002 JSON.org 
License (The JSON License): [https://github.com/stleary/JSON-java/blob/master/LICENSE](https://github.com/stleary/JSON-java/blob/master/LICENSE)

**JSON.simple** [https://code.google.com/archive/p/json-simple/](https://code.google.com/archive/p/json-simple/)  
Copyright &copy; January 2004  
License (Apache 2.0): [https://github.com/fangyidong/json-simple/blob/master/LICENSE.txt](https://github.com/fangyidong/json-simple/blob/master/LICENSE.txt)

**Jackson Databind** [https://github.com/FasterXML/jackson-databind](https://github.com/FasterXML/jackson-databind)   
License (Apache 2.0): [https://github.com/FasterXML/jackson-databind/blob/master/src/main/resources/META-INF/LICENSE](https://github.com/FasterXML/jackson-databind/blob/master/src/main/resources/META-INF/LICENSE)

#### core-httpclient-impl module

**Gson** [https://github.com/google/gson ](https://github.com/google/gson)  
Copyright &copy; 2008 Google Inc.
License (Apache 2.0): [https://github.com/google/gson/blob/master/LICENSE](https://github.com/google/gson/blob/master/LICENSE)

**Apache HttpClient** [https://hc.apache.org/httpcomponents-client-ga/index.html ](https://hc.apache.org/httpcomponents-client-ga/index.html)  
Copyright &copy; January 2004
License (Apache 2.0): [https://github.com/apache/httpcomponents-client/blob/master/LICENSE.txt](https://github.com/apache/httpcomponents-client/blob/master/LICENSE.txt)
