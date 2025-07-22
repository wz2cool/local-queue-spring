package com.github.wz2coo.localqueue.spring.model;

import com.github.wz2cool.localqueue.model.message.QueueMessage;

import java.util.List;

/**
 * Message acknowledgment interface
 * Used for manually confirming message processing completion
 */
public interface Acknowledgment {
    
    /**
     * Confirm message processing completion
     * After calling this method, the message will be marked as processed
     */
    void acknowledge();
    
    /**
     * Acknowledge the specified message
     * @param message the message to acknowledge
     */
    void acknowledge(QueueMessage message);
    
    /**
     * Batch acknowledge messages
     * @param messages the list of messages to acknowledge
     */
    void acknowledge(List<QueueMessage> messages);
    
    /**
     * Reject message, the message will re-enter the queue
     */
    void reject();
    
    /**
     * Reject the specified message
     * @param message the message to reject
     */
    void reject(QueueMessage message);
}