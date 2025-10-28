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
    

}