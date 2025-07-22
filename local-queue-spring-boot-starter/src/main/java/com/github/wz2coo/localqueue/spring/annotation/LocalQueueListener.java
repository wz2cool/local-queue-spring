package com.github.wz2coo.localqueue.spring.annotation;

import com.github.wz2coo.localqueue.spring.model.AckMode;
import java.lang.annotation.*;

/**
 * Local queue listener annotation
 * Used to mark methods as message consumers
 * Similar to Spring Kafka's @KafkaListener
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LocalQueueListener {

    /**
     * Consumer ID, used to identify different consumers
     */
    String customerId();

    /**
     * Selector tag, used to filter messages
     * Default "*" means receive all messages
     */
    String selectorTag() default "*";

    /**
     * Maximum batch size
     * Takes effect when method parameter is List<QueueMessage>
     */
    int maxBatchSize() default 1;

    /**
     * Pull interval (milliseconds)
     */
    long pullInterval() default 500;
    
    /**
     * ACK acknowledgment mode
     * AUTO: Auto acknowledgment (default)
     * MANUAL: Manual acknowledgment, requires calling Acknowledgment.acknowledge() in method
     * AUTO_SUCCESS: Auto acknowledgment only when method executes successfully
     */
    AckMode ackMode() default AckMode.AUTO;
}