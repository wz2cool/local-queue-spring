package com.github.wz2coo.localqueue.spring.core;

import com.github.wz2cool.localqueue.impl.SimpleConsumer;
import com.github.wz2cool.localqueue.model.message.QueueMessage;

import java.util.List;

/**
 * Local queue handler interface
 * Used for processing queue messages
 */
public interface LocalQueueHandler {
    
    /**
     * Process messages
     * @param messages message list
     * @param consumer consumer instance
     */
    void onMessages(List<QueueMessage> messages, SimpleConsumer consumer);
}