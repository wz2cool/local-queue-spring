package com.github.wz2coo.localqueue.spring.core;

import com.github.wz2coo.localqueue.spring.model.Acknowledgment;
import com.github.wz2cool.localqueue.impl.SimpleConsumer;
import com.github.wz2cool.localqueue.model.message.QueueMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
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
    
    @Override
    public void acknowledge(QueueMessage message) {
        if (acknowledged.compareAndSet(false, true)) {
            try {
                consumer.ack(Arrays.asList(message));
                logger.debug("[local-queue] Acknowledged single message");
            } catch (Exception e) {
                logger.error("[local-queue] Failed to acknowledge message", e);
                acknowledged.set(false);
                throw new RuntimeException("Failed to acknowledge message", e);
            }
        } else {
            logger.warn("[local-queue] Message already acknowledged");
        }
    }
    
    @Override
    public void acknowledge(List<QueueMessage> messagesToAck) {
        if (acknowledged.compareAndSet(false, true)) {
            try {
                consumer.ack(messagesToAck);
                logger.debug("[local-queue] Acknowledged {} messages", messagesToAck.size());
            } catch (Exception e) {
                logger.error("[local-queue] Failed to acknowledge messages", e);
                acknowledged.set(false);
                throw new RuntimeException("Failed to acknowledge messages", e);
            }
        } else {
            logger.warn("[local-queue] Messages already acknowledged");
        }
    }
    
    @Override
    public void reject() {
        if (acknowledged.compareAndSet(false, true)) {
            logger.info("[local-queue] Rejected {} messages, they will be reprocessed", messages.size());
            // Don't call consumer.ack(), message will re-enter queue
        } else {
            logger.warn("[local-queue] Messages already processed");
        }
    }
    
    @Override
    public void reject(QueueMessage message) {
        if (acknowledged.compareAndSet(false, true)) {
            logger.info("[local-queue] Rejected single message, it will be reprocessed");
            // Don't call consumer.ack(), message will re-enter queue
        } else {
            logger.warn("[local-queue] Message already processed");
        }
    }
    
    /**
     * Check if already acknowledged
     */
    public boolean isAcknowledged() {
        return acknowledged.get();
    }
}