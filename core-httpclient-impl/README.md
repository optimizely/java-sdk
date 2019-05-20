# Java SDK Async Http Client

This package provides default implementations of an Optimizely `EventHandler` and `ProjectConfigManager`. Also included
in this package is a factory class, `OptimizelyFactory`, which can be used to reliably instantiate the Optimizely SDK
with the default configuration of the `AsyncEventHandler` and `HttpProjectConfigManager`.

## Installation

### Gradle

```groovy
compile 'com.optimizely.ab:core-httpclient-impl:{VERSION}'
```

### Maven
```xml
<dependency>
    <groupId>com.optimizely.ab</groupId>
    <artifactId>core-httpclient-impl</artifactId>
    <version>{VERSION}</version>
</dependency>

```

### Basic usage
```java
import com.optimizely.ab.Optimizely;
import com.optimizely.ab.OptimizelyFactory;

public class App {

    public static void main(String[] args) {
        String sdkKey = args[0];
        Optimizely optimizely = OptimizelyFactory.newDefaultInstance(sdkKey);
    }
}

```

## Advanced usage
```java
import com.optimizely.ab.Optimizely;
import com.optimizely.ab.config.HttpProjectConfigManager;
import com.optimizely.ab.event.AsyncEventHandler;

import java.util.concurrent.TimeUnit;

public class App {

    public static void main(String[] args) {
        String sdkKey = args[0];

        EventHandler eventHandler = AsyncEventHandler.builder()
            .withQueueCapacity(20000)
            .withNumWorkers(5)
            .build();

        ProjectConfigManager projectConfigManager = HttpProjectConfigManager.buidler()
            .withSdkKey(sdkKey)
            .withPollingInterval(1, TimeUnit.MINUTES)
            .build();

        Optimizely optimizely = Optimizely.builder()
            .withConfig(projectConfigManager)
            .withEventHandler(eventHandler
            .build();
    }
}
```

## AsyncEventHandler

The AsyncEventHandler provides an implementation of the the EventHandler backed by a `ThreadPoolExecutor`. When events are
triggered from the Optimizely SDK, they are asynchronously queued as tasks to the executor and are processed in the
order they were submitted via the executor workers. Each worker is responsible for making outbound http requests to the 
Optimizely log endpoint for metric tracking. The default queue size and the number of workers are configurable via
global properties and can be overridden via the `AsyncEventHandler.Builder`.

### Usage

To use the AsyncEventHandler, an instance must be built via the `AsyncEventHandler.Builder` then passed to the `Optimizely.Builder`

```java
EventHandler eventHandler = AsyncEventHandler.builder()
    .withQueueCapacity(20000)
    .withNumWorkers(5)
    .build();
```

#### Queue capacity

The queue capacity can be set to initialize the backing queue for the executor service. If the queue fills up, then
events will be dropped and exception will be logged. Setting a higher queue value will prevent event loss, but will 
use up more memory in the event the workers can not keep up if the production rate.

#### Number of workers

The number of workers determines the number of threads used by the thread pool.

#### Advanced configurations

|Property Name|Default Value|Description|
|---|---|---|
|async.event.handler.queue.capacity|10000|Queue size for pending LogEvents|
|async.event.handler.num.workers|2|Number of worker threads|
|async.event.handler.max.connections|200|Max number of connections|
|async.event.handler.event.max.per.route|20|Max number of connections per route|
|async.event.handler.validate.after|5000|Time in milliseconds to maintain idol connections|


## HttpProjectConfigManager

The HttpProjectConfigManager is an implementation of the abstract PollingProjectConfigManager. The `poll` 
method is extended and makes an Http GET request to the configured url to asynchronously download the project data file
and initialize an instance of the ProjectConfig. By default, the HttpProjectConfigManager will block until the
first successful retrieval of the datafile, up to a configurable timeout. The frequency of the polling method and the 
blocking timeout can be set via the HttpProjectConfigManager.Builder with the default values being pulled from global
properties.

### Usage

```java
ProjectConfigManager projectConfigManager = HttpProjectConfigManager.buidler()
    .withSdkKey(sdkKey)
    .withPollingInterval(1, TimeUnit.MINUTES)
    .build();
```

#### SDK Key

 The SDK key is used to compose the outbound HTTP request to the default datafile location hosted on the Optimizely CDN.

#### Polling interval

The polling interval is used to determine a fixed delay between consecutive HTTP requests for the datafile.

#### Initial Datafile

An initial datafile can be provided via the builder to bootstrap the the ProjectConfigManager so that it can be used 
immediately without blocking execution.

#### Advanced configurations

|Property Name|Default Value|Description|
|---|---|---|
|http.project.config.manager.polling.duration|5|Fixed delay between fetches for the datafile|
|http.project.config.manager.polling.unit|MINUTES|Time unit corresponding to polling interval|
|http.project.config.manager.blocking.duration|10|Max duration spent waiting for initial bootstrapping|
|http.project.config.manager.blocking.unit|SECONDS|Time unit corresponding to blocking duration|
|http.project.config.manager.sdk.key|null|Optimizely project SDK key|


## Optimizely properties file

An Optimizely properties file, `optimizely.properties`, that is available within the runtime classpath can be used to configure
the default values of a given Optimizely resource. Refer to the resource implementation for available configuration
parameters.

#### Example:
```properties
http.project.config.manager.polling.duration = 1
http.project.config.manager.polling.unit = MINUTES

async.event.handler.queue.capacity = 20000
async.event.handler.num.workers = 5
```

## OptimizelyFactory

The `OptimizelyFactory` included in this package provides basic utility to instantiate the Optimizely SDK
with a minimal number of provided configuration options. Configuration properties are sourced from Java system properties,
environment variables or from an `optimizely.properties` file, in that order. Not all configuration and initialization 
are captured via the `OptimizelyFactory`, for those use cases the resources can be built via their respective builder
classes.
