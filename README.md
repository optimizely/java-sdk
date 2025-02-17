# Optimizely Java SDK

[![Apache 2.0](https://img.shields.io/badge/license-APACHE%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

This repository houses the Java SDK for use with Optimizely Feature Experimentation and Optimizely Full Stack (legacy).

Optimizely Feature Experimentation is an A/B testing and feature management tool for product development teams that enables you to experiment at every step. Using Optimizely Feature Experimentation allows for every feature on your roadmap to be an opportunity to discover hidden insights. Learn more at [Optimizely.com](https://www.optimizely.com/products/experiment/feature-experimentation/), or see the [developer documentation](https://docs.developers.optimizely.com/experimentation/v4.0.0-full-stack/docs/welcome).

Optimizely Rollouts is [free feature flags](https://www.optimizely.com/free-feature-flagging/) for development teams. You can easily roll out and roll back features in any application without code deploys, mitigating risk for every feature on your roadmap.

## Get started

Refer to the [Java SDK's developer documentation](https://docs.developers.optimizely.com/experimentation/v4.0.0-full-stack/docs/java-sdk) for detailed instructions on getting started with using the SDK.

### Requirements

Java 8 or higher versions.

### Install the SDK

The Java SDK is distributed through Maven Central and is created with source and target compatibility of Java 1.8. The `core-api` and `httpclient` packages are [optimizely-sdk-core-api](https://mvnrepository.com/artifact/com.optimizely.ab/core-api) and [optimizely-sdk-httpclient](https://mvnrepository.com/artifact/com.optimizely.ab/core-httpclient-impl), respectively.


`core-api` requires [org.slf4j:slf4j-api:1.7.16](https://mvnrepository.com/artifact/org.slf4j/slf4j-api/1.7.16) and a supported JSON parser.
We currently integrate with [Jackson](https://github.com/FasterXML/jackson), [GSON](https://github.com/google/gson), [json.org](http://www.json.org), and [json-simple](https://code.google.com/archive/p/json-simple); if any of those packages are available at runtime, they will be used by `core-api`. If none of those packages are already provided in your project's classpath, one will need to be added. 

`core-httpclient-impl` is an optional dependency that implements the event dispatcher and requires [org.apache.httpcomponents:httpclient:4.5.2](https://mvnrepository.com/artifact/org.apache.httpcomponents/httpclient/4.5.2).

---

**NOTE**

Optimizely previously distributed the Java SDK through Bintray/JCenter. But, as of April 27, 2021, [Bintray/JCenter will become a read-only repository indefinitely](https://jfrog.com/blog/into-the-sunset-bintray-jcenter-gocenter-and-chartcenter/). The publish repository has been migrated to [MavenCentral](https://mvnrepository.com/artifact/com.optimizely.ab) for the SDK version 3.8.1 or later.

---

```
repositories {
  mavenCentral()
  jcenter()
}

dependencies {
  compile 'com.optimizely.ab:core-api:{VERSION}'
  compile 'com.optimizely.ab:core-httpclient-impl:{VERSION}'
  // The SDK integrates with multiple JSON parsers, here we use Jackson.
  compile 'com.fasterxml.jackson.core:jackson-core:2.7.1'
  compile 'com.fasterxml.jackson.core:jackson-annotations:2.7.1'
  compile 'com.fasterxml.jackson.core:jackson-databind:2.7.1'
}
```


## Use the Java SDK

See the Optimizely Feature Experimentation [developer documentation](https://docs.developers.optimizely.com/experimentation/v4.0-full-stack/docs/java-sdk) to learn how to set up your first Java project and use the SDK.


## SDK Development

### Unit tests

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

### Other Optimzely SDKs

- Agent - https://github.com/optimizely/agent

- Android - https://github.com/optimizely/android-sdk

- C# - https://github.com/optimizely/csharp-sdk

- Flutter - https://github.com/optimizely/optimizely-flutter-sdk

- Go - https://github.com/optimizely/go-sdk

- Java - https://github.com/optimizely/java-sdk

- JavaScript - https://github.com/optimizely/javascript-sdk

- PHP - https://github.com/optimizely/php-sdk

- Python - https://github.com/optimizely/python-sdk

- React - https://github.com/optimizely/react-sdk

- Ruby - https://github.com/optimizely/ruby-sdk

- Swift - https://github.com/optimizely/swift-sdk
  
