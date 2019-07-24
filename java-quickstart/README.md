Optimizely Java QuickStart
===================

This package contains the Java QuickStart app that can be used to test that the Java SDK can communicate with the CDN and Optimizely results.  
Simply create a new project with an experiment called "background_experiment" with 2 variations and a conversion event named "sample_conversion".  
From there you can test different attributes setups and other Optimizely Java SDK APIs.  
This is just a simple example that gets you up and running quickly!

## Getting Started
Create an experiment on app.optimizely.com named "background_experiment" with variation_a and variation_b and one conversion event "simple_conversion.".
Use that SDK key in the [`com.optimizely.Example.java`](https://github.com/optimizely/java-sdk/blob/master/java-quickstart/src/main/java/com/optimizely/Example.java) 
and you are set to test.

### Run Example

Running `./gradlew runExample` will build and execute [`com.optimizely.Example.java`](https://github.com/optimizely/java-sdk/blob/master/java-quickstart/src/main/java/com/optimizely/Example.java).
