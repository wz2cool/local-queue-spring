package com.github.wz2coo.localqueue.spring.core;

import com.github.wz2coo.localqueue.spring.model.Acknowledgment;
import com.github.wz2cool.localqueue.impl.SimpleConsumer;
import com.github.wz2cool.localqueue.model.message.QueueMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default message acknowledgment implementation
 */
public class DefaultAcknowledgment implements Acknowledgment {
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private final SimpleConsumer consumer;
    private final List<QueueMessage> messages;
    private final AtomicBoolean acknowledged = new AtomicBoolean(false);
    
    public DefaultAcknowledgment(SimpleConsumer consumer, List<QueueMessage> messages) {
        this.consumer = consumer;
        this.messages = messages;
    }
    
    @Override
    public void acknowledge() {
        if (acknowledged.compareAndSet(false, true)) {
            try {
                consumer.ack(messages);
                logger.debug("[local-queue] Acknowledged {} messages", messages.size());
            } catch (Exception e) {
                logger.error("[local-queue] Failed to acknowledge messages", e);
                acknowledged.set(false); // Reset state to allow retry
                throw new RuntimeException("Failed to acknowledge messages", e);
            }
        } else {
            logger.warn("[local-queue] Messages already acknowledged");
        }
    }
    

    
    /**
     * Check if already acknowledged
     */
    public boolean isAcknowledged() {
        return acknowledged.get();
    }
}