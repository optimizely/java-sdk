Optimizely Java QuickStart
===================

This repository houses the Java Quickstart app that can be used to test that your java SDK can communicate with the cdn and Optimizely results.  Simply create a new project with an experiment called "background_experiment" with 2 variations and a conversion event named "sample_conversion".  From there you can test different attributes setups and other Optimizely Java SDK APIs.  This is just a simple example that gets you up and running.  Normally, the Java SDK would be running with a application server such as Dropwizard.

## Getting Started
Simply create a experiment on app.optimizely.com named "background_experiment" with variation_a and variation_b and one conversion event "simple_conversion."  Use that SDK key in the Example.java and you are set to test.

### Installing the SDK
In the instance here we are using jar files copied into the libs directory for linking with the Optimizely Java SDK.

#### Gradle

You can do a ```./gradlew``` build and run via run.sh.
