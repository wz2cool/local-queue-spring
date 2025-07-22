# ACK Mechanism Usage Guide

This document describes how to use the ACK (acknowledgment) mechanism in local-queue-spring.

## ACK Modes

### 1. AUTO (Auto Acknowledgment) - Default Mode

Automatically acknowledges messages after processing completion, regardless of success or failure.

```java
@Component
public class MessageConsumer {
    
    @LocalQueueListener(customerId = "auto-consumer")
    public void handleMessage(QueueMessage message) {
        // Message processing logic
        System.out.println("Processing message: " + message.getBody());
        // Automatically ACK after method execution, even if exception is thrown
    }
}
```

### 2. MANUAL (Manual Acknowledgment)

Requires manual call to `Acknowledgment.acknowledge()` to confirm messages.

```java
@Component
public class MessageConsumer {
    
    // Single message + manual acknowledgment
    @LocalQueueListener(customerId = "manual-consumer", ackMode = AckMode.MANUAL)
    public void handleMessage(QueueMessage message, Acknowledgment ack) {
        try {
            // Message processing logic
            System.out.println("Processing message: " + message.getBody());
            
            // Manually acknowledge after successful processing
            ack.acknowledge();
        } catch (Exception e) {
            // Processing failed, can choose to reject message
            ack.reject();
        }
    }
    
    // Batch messages + manual acknowledgment
    @LocalQueueListener(customerId = "batch-manual-consumer", ackMode = AckMode.MANUAL)
    public void handleBatchMessages(List<QueueMessage> messages, Acknowledgment ack) {
        try {
            for (QueueMessage message : messages) {
                // Process each message
                System.out.println("Processing message: " + message.getBody());
            }
            
            // Batch acknowledge all messages
            ack.acknowledge();
        } catch (Exception e) {
            // Batch reject all messages
            ack.reject();
        }
    }
    
    // Method with only acknowledgment parameter
    @LocalQueueListener(customerId = "ack-only-consumer", ackMode = AckMode.MANUAL)
    public void handleAckOnly(Acknowledgment ack) {
        // Execute logic that doesn't depend on specific message content
        System.out.println("Trigger processing logic");
        ack.acknowledge();
    }
}
```

### 3. AUTO_SUCCESS (Auto Acknowledgment on Success)

Automatically acknowledges only when method executes successfully (no exceptions).

```java
@Component
public class MessageConsumer {
    
    @LocalQueueListener(customerId = "auto-success-consumer", ackMode = AckMode.AUTO_SUCCESS)
    public void handleMessage(QueueMessage message) {
        // Message processing logic
        if ("error".equals(message.getBody())) {
            throw new RuntimeException("Processing failed");
            // No ACK when exception is thrown, message will re-enter queue
        }
        
        System.out.println("Processing message: " + message.getBody());
        // Automatically ACK after normal execution completion
    }
}
```

## Acknowledgment Interface Methods

```java
public interface Acknowledgment {
    
    // Acknowledge all messages
    void acknowledge();
    
    // Acknowledge specified message
    void acknowledge(QueueMessage message);
    
    // Batch acknowledge messages
    void acknowledge(List<QueueMessage> messages);
    
    // Reject all messages (messages re-enter queue)
    void reject();
    
    // Reject specified message
    void reject(QueueMessage message);
}
```

## Usage Scenario Recommendations

### AUTO Mode
- Suitable for scenarios where message processing failures don't require retries
- Non-critical business like logging, statistics

### MANUAL Mode
- Need precise control over message acknowledgment timing
- Complex business logic requiring acknowledgment decisions based on processing results
- Need to implement custom retry logic

### AUTO_SUCCESS Mode
- Want failed messages to be reprocessed
- Critical business logic ensuring no message loss
- Simple retry scenarios

## Important Notes

1. **Thread Safety**: `Acknowledgment` implementation is thread-safe, but each message can only be acknowledged once
2. **Duplicate Acknowledgment**: Repeated calls to `acknowledge()` will be ignored and warning logs will be recorded
3. **Exception Handling**: In MANUAL mode, if you forget to call acknowledgment methods, messages will re-enter the queue
4. **Performance Considerations**: AUTO mode has the best performance, MANUAL mode requires additional acknowledgment overhead

## Complete Example

```java
@Component
public class OrderMessageConsumer {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private NotificationService notificationService;
    
    // Order processing - use AUTO_SUCCESS to ensure no order loss
    @LocalQueueListener(
        customerId = "order-processor",
        ackMode = AckMode.AUTO_SUCCESS
    )
    public void processOrder(QueueMessage message) {
        String orderId = message.getBody();
        orderService.processOrder(orderId);
        // If processing fails and throws exception, message will be reprocessed
    }
    
    // Notification sending - use MANUAL for precise control
    @LocalQueueListener(
        customerId = "notification-sender",
        ackMode = AckMode.MANUAL
    )
    public void sendNotification(QueueMessage message, Acknowledgment ack) {
        try {
            String notification = message.getBody();
            boolean success = notificationService.send(notification);
            
            if (success) {
                ack.acknowledge();
            } else {
                // Send failed, reject message for retry
                ack.reject();
            }
        } catch (Exception e) {
            ack.reject();
        }
    }
    
    // Log recording - use AUTO, no retry even on failure
    @LocalQueueListener(
        customerId = "log-recorder",
        ackMode = AckMode.AUTO
    )
    public void recordLog(QueueMessage message) {
        try {
            // Record log, no retry even on failure
            System.out.println("Log: " + message.getBody());
        } catch (Exception e) {
            // Ignore exception, message will still be acknowledged
        }
    }
}
```