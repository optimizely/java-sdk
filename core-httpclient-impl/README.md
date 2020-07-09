# Java SDK Async HTTP Client

This package provides default implementations of an Optimizely `EventHandler` and `ProjectConfigManager`.
The package also includes a factory class, `OptimizelyFactory`, which you can use to instantiate the Optimizely SDK
with the default configuration of `AsyncEventHandler` and `HttpProjectConfigManager`.

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


## Basic usage
```java
package com.optimizely;

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
package com.optimizely;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.config.ProjectConfigManager;
import com.optimizely.ab.config.HttpProjectConfigManager;
import com.optimizely.ab.event.AsyncEventHandler;
import com.optimizely.ab.event.EventHandler;
import java.util.concurrent.TimeUnit;

public class App {

    public static void main(String[] args) {
        String sdkKey = args[0];
        EventHandler eventHandler = AsyncEventHandler.builder()
            .withQueueCapacity(20000)
            .withNumWorkers(5)
            .build();

        ProjectConfigManager projectConfigManager = HttpProjectConfigManager.builder()
            .withSdkKey(sdkKey)
            .withPollingInterval(1L, TimeUnit.MINUTES)
            .build();

        Optimizely optimizely = Optimizely.builder()
            .withEventHandler(eventHandler)
            .withConfigManager(projectConfigManager)
            .build();
    }
}
```

## AsyncEventHandler

[`AsyncEventHandler`](https://github.com/optimizely/java-sdk/blob/master/core-httpclient-impl/src/main/java/com/optimizely/ab/event/AsyncEventHandler.java)
provides an implementation of [`EventHandler`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/event/EventHandler.java)
backed by a `ThreadPoolExecutor`. Events triggered from the Optimizely SDK are queued immediately as discrete tasks to
the executor and processed in the order they were submitted.

Each worker is responsible for making outbound HTTP requests to the Optimizely log endpoint for metrics tracking.
Configure the default queue size and number of workers via global properties. Use `AsyncEventHandler.Builder` to
override the default queue size and number of workers.

### Use `AsyncEventHandler`

To use `AsyncEventHandler`, you must build an instance with `AsyncEventHandler.Builder` and pass the instance to the `Optimizely.Builder`:

```java
EventHandler eventHandler = AsyncEventHandler.builder()
    .withQueueCapacity(20000)
    .withNumWorkers(5)
    .build();
```

#### Queue capacity

You can set the queue capacity to initialize the backing queue for the executor service. If the queue fills up, events
will be dropped and an exception will be logged. Setting a higher queue value will prevent event loss but will use more
memory if the workers cannot keep up with the production rate.

#### Number of workers

The number of workers determines the number of threads the thread pool uses.

### Builder Methods
The following builder methods can be used to custom configure the `AsyncEventHandler`.

|Method Name|Default Value|Description|
|---|---|---|
|`withQueueCapacity(int)`|10000|Queue size for pending logEvents|
|`withNumWorkers(int)`|2|Number of worker threads|
|`withMaxTotalConnections(int)`|200|Maximum number of connections|
|`withMaxPerRoute(int)`|20|Maximum number of connections per route|
|`withValidateAfterInactivity(int)`|5000|Time to maintain idol connections (in milliseconds)|

### Advanced configuration
The following properties can be set to override the default configuration.

|Property Name|Default Value|Description|
|---|---|---|
|**async.event.handler.queue.capacity**|10000|Queue size for pending logEvents|
|**async.event.handler.num.workers**|2|Number of worker threads|
|**async.event.handler.max.connections**|200|Maximum number of connections|
|**async.event.handler.event.max.per.route**|20|Maximum number of connections per route|
|**async.event.handler.validate.after**|5000|Time to maintain idol connections (in milliseconds)|

## HttpProjectConfigManager

[`HttpProjectConfigManager`](https://github.com/optimizely/java-sdk/blob/master/core-httpclient-impl/src/main/java/com/optimizely/ab/config/HttpProjectConfigManager.java)
is an implementation of the abstract [`PollingProjectConfigManager`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/config/PollingProjectConfigManager.java).
The `poll` method is extended and makes an HTTP GET request to the configured URL to asynchronously download the
project datafile and initialize an instance of the ProjectConfig.

By default, `HttpProjectConfigManager` will block until the first successful datafile retrieval, up to a configurable timeout.
Set the frequency of the polling method and the blocking timeout with `HttpProjectConfigManager.Builder`,
pulling the default values from global properties.

### Use `HttpProjectConfigManager`

```java
ProjectConfigManager projectConfigManager = HttpProjectConfigManager.builder()
    .withSdkKey(sdkKey)
    .withPollingInterval(1, TimeUnit.MINUTES)
    .build();
```

#### SDK key

The SDK key is used to compose the outbound HTTP request to the default datafile location on the Optimizely CDN.

#### Polling interval

The polling interval is used to specify a fixed delay between consecutive HTTP requests for the datafile.

#### Initial datafile

You can provide an initial datafile via the builder to bootstrap the `ProjectConfigManager` so that it can be used
immediately without blocking execution. The initial datafile also serves as a fallback datafile if HTTP connection
cannot be established. This is useful in mobile environments, where internet connectivity is not guaranteed.
The initial datafile will be discarded after the first successful datafile poll.

### Builder Methods
The following builder methods can be used to custom configure the `HttpProjectConfigManager`.

|Builder Method|Default Value|Description|
|---|---|---|
|`withDatafile(String)`|null|Initial datafile, typically sourced from a local cached source.|
|`withUrl(String)`|null|URL override location used to specify custom HTTP source for the Optimizely datafile.|
|`withFormat(String)`|https://cdn.optimizely.com/datafiles/%s.json|Parameterized datafile URL by SDK key.|
|`withPollingInterval(Long, TimeUnit)`|5 minutes|Fixed delay between fetches for the datafile.|
|`withBlockingTimeout(Long, TimeUnit)`|10 seconds|Maximum time to wait for initial bootstrapping.|
|`withSdkKey(String)`|null|Optimizely project SDK key. Required unless source URL is overridden.|
|`withDatafileAccessToken(String)`|null|Token for authenticated datafile access.|

### Advanced configuration
The following properties can be set to override the default configuration.

|Property Name|Default Value|Description|
|---|---|---|
|**http.project.config.manager.polling.duration**|5|Fixed delay between fetches for the datafile|
|**http.project.config.manager.polling.unit**|MINUTES|Time unit corresponding to polling interval|
|**http.project.config.manager.blocking.duration**|10|Maximum time to wait for initial bootstrapping|
|**http.project.config.manager.blocking.unit**|SECONDS|Time unit corresponding to blocking duration|
|**http.project.config.manager.sdk.key**|null|Optimizely project SDK key|
|**http.project.config.manager.datafile.auth.token**|null|Token for authenticated datafile access|

## Update Config Notifications
A notification signal will be triggered whenever a _new_ datafile is fetched. To subscribe to these notifications you can
use the `Optimizely.addUpdateConfigNotificationHandler`:

```java
NotificationHandler<UpdateConfigNotification> handler = message ->
    System.out.println("Received new datafile configuration");

optimizely.addUpdateConfigNotificationHandler(handler);
```
or add the handler directly to the `NotificationCenter`:
```java
notificationCenter.addNotificationHandler(UpdateConfigNotification.class, handler);
```

## optimizely.properties

When an `optimizely.properties` file is available within the runtime classpath it can be used to provide
default values of a given Optimizely resource. Refer to the resource implementation for available configuration parameters.

### Example `optimizely.properties` file

```properties
http.project.config.manager.polling.duration = 1
http.project.config.manager.polling.unit = MINUTES

async.event.handler.queue.capacity = 20000
async.event.handler.num.workers = 5
```


## OptimizelyFactory

In this package, [`OptimizelyFactory`](https://github.com/optimizely/java-sdk/blob/master/core-httpclient-impl/src/main/java/com/optimizely/ab/OptimizelyFactory.java)
provides basic utility to instantiate the Optimizely SDK with a minimal number of configuration options.
Configuration properties are sourced from Java system properties, environment variables, or an
`optimizely.properties` file, in that order.

`OptimizelyFactory` does not capture all configuration and initialization options. For more use cases,
build the resources via their respective builder classes.

### Use `OptimizelyFactory`

You must provide the SDK key at runtime, either directly via the factory method:
```Java
Optimizely optimizely = OptimizelyFactory.newDefaultInstance(<<SDK_KEY>>);
```

If you provide the SDK via a global property, use the empty signature:
```Java
Optimizely optimizely = OptimizelyFactory.newDefaultInstance();
```

### Event batching
`OptimizelyFactory` uses the [`BatchEventProcessor`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/event/BatchEventProcessor.java)
to enable request batching to the Optimizely logging endpoint. By default, a maximum of 10 events are included in each batch
for a maximum interval of 30 seconds. These parameters are configurable via systems properties or through the
`OptimizelyFactory#setMaxEventBatchSize` and `OptimizelyFactory#setMaxEventBatchInterval` methods.
 