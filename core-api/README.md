# Java SDK Core API
This package contains the core APIs and interfaces for the Optimizely Full Stack API in Java.

Full product documentation is in the [Optimizely developers documentation](https://docs.developers.optimizely.com/full-stack/docs/welcome).

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
The [`ErrorHandler`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/error/ErrorHandler.java)
interface is available for handling errors from the SDK without interfering with the host application.

### NoOpErrorHandler
The [`NoOpErrorHandler`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/error/NoOpErrorHandler.java)
is the default `ErrorHandler` implemetation that silently consumes all errors raised from the SDK.

### RaiseExceptionErrorHandler
The [`RaiseExceptionErrorHandler`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/error/RaiseExceptionErrorHandler.java)
is an implementation of `ErrorHandler` best suited for testing and development where **all** errors are raised, potentially crashing
the hosting application.

## EventProcessor
The [`EventProcessor`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/event/EventProcessor.java) 
interface is used to provide an intermediary processing stage within event production.
It's assumed that the `EventProcessor` dispatches events via a provided `EventHandler`.

### BatchEventProcessor
[`BatchEventProcessor`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/event/BatchEventProcessor.java)
is an implementation of `EventProcessor` where events are batched. The class maintains a single consumer thread that pulls
events off of the `BlockingQueue` and buffers them for either a
configured batch size or a maximum duration before the resulting `LogEvent` is sent to the `EventDispatcher` and `NotificationCenter`.

### ForwardingEventProcessor
The [`ForwardingEventProcessor`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/event/ForwardingEventProcessor.java)
implements `EventProcessor` for backwards compatibility. Each event processed is converted into a [`LogEvent`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/event/ForwardingEventProcessor.java)
message before it is sent synchronously to the supplied `EventHandler`.

## EventHandler
The [`EventHandler`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/event/EventHandler.java)
interface is used for dispatching events to the Optimizely event endpoint. Implementations of `EventHandler#dispatchEvent(LogEvent)` are expected
to make an HTTP request of type `getRequestMethod()` to the `LogEvent#getEndpointUrl()` location. The corresponding request parameters and body
are available via `LogEvent#getRequestParams()` and `LogEvent#getBody()` respectively.

### NoopEventHandler
The [`NoopEventHandler`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/event/NoopEventHandler.java)
implements `EventHandler` with no side-effects. `NoopEventHandler` is useful for testing or non-production environments.

## NotificationCenter
The [`NotificationCenter`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/notification/NotificationCenter.java)
is the centralized component for subscribing to notifications from the SDK. Subscribers must implement the `NotificationHandler<T>` interface
and are registered via `NotificationCenterxaddNotificationHandler`. Note that notifications are called synchronously and have the potential to
block the main thread.

## ProjectConfig
The [`ProjectConfig`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/config/ProjectConfig.java)
represents the current state of the Optimizely project as configured through [optimizely.com](https://www.optimizely.com/).
The interface is currently unstable and only used internally. All public access to this implementation is subject to change
with each subsequent version.

### DatafileProjectConfig
The [`DatafileProjectConfig`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/config/DatafileProjectConfig.java)
is an implementation of `ProjectConfig` backed by a file, typically sourced from the Optimizely CDN.

## ProjectConfigManager
The [`ProjectConfigManager`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/config/ProjectConfigManager.java)
is a factory class that provides `ProjectConfig`. Implementations of this class provide a consistent representation
of a `ProjectConfig` that can be references between service calls.

### AtomicProjectConfigManager
The [`AtomicProjectConfigManager`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/config/AtomicProjectConfigManager.java)
is a static provider that can be updated atomically to provide a consistent view of a `ProjectConfig`.

### PollingProjectConfigManager
The [`PollingProjectConfigManager`](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/config/PollingProjectConfigManager.java)
is an abstract class that provides the framework for a dynamic factory that updates asynchronously within a background thread.
Implementations of this class can be used to poll from an externalized sourced without blocking the main application thread.
