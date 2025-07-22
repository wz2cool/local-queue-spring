# Method-Level LocalQueueListener Usage Guide

## Overview

The `@LocalQueueListener` annotation now supports method-level usage, similar to Spring Kafka's `@KafkaListener`. This provides a more flexible way to handle messages.

## Supported Method Signatures

### 1. Handle Single Message
```java
@LocalQueueListener(
    customerId = "user-service", 
    selectorTag = "user.created"
)
public void handleMessage(QueueMessage message) {
    // Handle single message
}
```

### 2. Batch Process Messages
```java
@LocalQueueListener(
    customerId = "order-service", 
    selectorTag = "order.*",
    maxBatchSize = 10
)
public void handleMessages(List<QueueMessage> messages) {
    // Batch process messages
}
```

### 3. No Parameter Method (Trigger Mode)
```java
@LocalQueueListener(
    customerId = "notification-service", 
    selectorTag = "notification.send"
)
public void sendNotification() {
    // Simple trigger, no message content needed
}
```

## Complete Example

```java
@Component
public class MessageConsumer {
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    @LocalQueueListener(
        customerId = "user-service", 
        selectorTag = "user.created",
        maxBatchSize = 1,
        pullInterval = 1000
    )
    public void handleUserCreated(QueueMessage message) {
        logger.info("Processing user created: {}", message.getContent());
    }
    
    @LocalQueueListener(
        customerId = "order-service", 
        selectorTag = "order.*",
        maxBatchSize = 10,
        pullInterval = 500
    )
    public void handleOrderEvents(List<QueueMessage> messages) {
        logger.info("Processing {} order messages", messages.size());
        messages.forEach(msg -> {
            // Process each order message
        });
    }
}
```

## Annotation Parameter Description

- `customerId`: Consumer ID, must be unique
- `selectorTag`: Message selector tag, supports wildcards (e.g., `order.*`)
- `maxBatchSize`: Maximum batch size, default is 1
- `pullInterval`: Pull interval (milliseconds), default is 500

## Important Notes

**Note: Only method-level @LocalQueueListener annotations are now supported, class-level annotations are no longer supported.**

If you were previously using class-level annotations, please migrate to method-level:

```java
// Old way (no longer supported)
// @LocalQueueListener(...)
// public class MessageListener implements LocalQueueHandler { ... }

// New way
@Component
public class MessageListener {
    @LocalQueueListener(
        customerId = "service-id",
        selectorTag = "tag.*"
    )
    public void handleMessages(List<QueueMessage> messages) {
        // Process messages
    }
}
```

## Advantages

1. **More Flexible**: Multiple different message listeners can be defined in one class
2. **More Concise**: No need to implement `LocalQueueHandler` interface
3. **Type Safe**: Supports different method signatures
4. **Easy to Test**: Method-level listeners are easier to unit test
5. **Spring Style**: Consistent programming model with Spring Kafka, Spring AMQP, etc.
6. **Clearer**: Focus on method level, avoiding confusion

## Considerations

1. Ensure each `customerId` is unique throughout the application
2. Methods must be public
3. Exception handling: If method throws exception, message processing will fail
4. Transaction support: Can use `@Transactional` annotation on methods