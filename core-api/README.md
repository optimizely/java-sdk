# Java SDK Core API
This package contains the core APIs and interfaces for the Optimizely Full Stack API in Java.

## Installation

### Gradle
```groovy
compile 'com.optimizely.ab:core-api:{VERSION}'
```

### Maven
```xml
<dependency>
    <groupId>com.optimizely.ab</groupId>
    <artifactId>core-api</artifactId>
    <version>{VERSION}</version>
</dependency>

```

## Optimizely
[`Optimizely`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/Optimizely.java)
provides top level API access to the Full Stack project.

The full product documentation can be found on the the Optimizely developers site [here](https://docs.developers.optimizely.com/full-stack/docs/welcome). 

### Usage
```Java
Optimizely optimizely = Optimizely.builder()
    .withConfigManager(configManager)
    .withEventProcessor(eventProcessor)
    .build();

Variation variation = optimizely.activate("ad-test");
optimizely.track("conversion");
```

## ErrorHandler
[`ErrorHandler`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/error/ErrorHandler.java)
interface is available for handling errors from the SDK without interfering with the host application.

### NoOpErrorHandler
[`NoOpErrorHandler`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/error/NoOpErrorHandler.java)
is the default `ErrorHandler` implemetation that silently consumes all errors raised from the SDK.

### RaiseExceptionErrorHandler
[`RaiseExceptionErrorHandler`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/error/RaiseExceptionErrorHandler.java)
is an implementation of `ErrorHandler` best suited for testing and development where **all** errors are raised, potentially crashing
the hosting application.

## EventProcessor
[`EventProcessor`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/event/EventProcessor.java) 
interface is used to provide an intermediary processing stage within event production.
It's assumed that the `EventProcessor` dispatches events via a provided `EventHandler`.

### BatchEventProcessor
[`BatchEventProcessor`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/event/BatchEventProcessor.java)
is a batched implementation of the `EventProcessor`. Events passed to the `BatchEventProcessor` are immediately "offered" to a `BlockingQueue`.
The `BatchEventProcessor` maintains a single consumer thread that pulls events off of the `BlockingQueue` and buffers them for either a 
configured batch size or for a maximum duration before the resulting `LogEvent` is sent to the `EventDispatcher` and `NoticifactionCenter`.

### ForwardingEventProcessor
[`ForwardingEventProcessor`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/event/ForwardingEventProcessor.java)
implements `EventProcessor` for backwards compatibility. Each event processed is converted into a [`LogEvent`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/event/ForwardingEventProcessor.java)
message before getting synchronously sent to the supplied `EventHandler`.

## EventHandler
[`EventHandler`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/event/EventHandler.java)
interface is used for dispatching events to the Optimizely event end-point. Implementations of `EventHandler#dispatchEvent(LogEvent)` are expected
to make an HTTP request of type `getRequestMethod()` to the `LogEvent#getEndpointUrl()` location. The corresponding request parameters and body
are available via `LogEvent#getRequestParams()` and `LogEvent#getBody()` respectively.

### NoopEventHandler
[`NoopEventHandler`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/event/NoopEventHandler.java)
implements `EventHandler` with no side-effects. Useful for testing or non-production environments.

## NotificationCenter
[`NotificationCenter`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/notification/NotificationCenter.java)
is the centralized component for subscribing to notifications from the SDK. Subscribers must implement the `NotificationHandler<T>` interface
and are registered via `NotificationCenter#addNotificationHandler`. Notifications are currently sent synchronously, so be sure that 
implementations do not block unnecessarily.

## ProjectConfig
[`ProjectConfig`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/config/ProjectConfig.java)
represents the current state of the Optimizely project as configured through [optimizely.com](https://www.optimizely.com/).
The interface is currently unstable and used only internally. All public access to these implementations are subject to change
with each subsequent version.

### DatafileProjectConfig
[`DatafileProjectConfig`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/config/DatafileProjectConfig.java)
is an implementation of `ProjectConfig` backed by a file, typically sourced from the Optimizely CDN.

## ProjectConfigManager
[`ProjectConfigManager`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/config/ProjectConfigManager.java)
is a factory class that serves a `ProjectConfig`. Implementations of this class provide a consistent representation
of a `ProjectConfig` that can be references between service calls.

### AtomicProjectConfigManager
[`AtomicProjectConfigManager`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/config/AtomicProjectConfigManager.java)
is a static provider that can be updated atomically to provide a consistent view of a `ProjectConfig`.

### PollingProjectConfigManager
[`PollingProjectConfigManager`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/config/PollingProjectConfigManager.java)
ia a abstract class that provides the framework for a dynamic factory that updates asynchronously within a background thread.
Implementations of this class can be used to poll from externalized sourced without blocking the main application thread.
