package com.github.wz2coo.localqueue.spring.core;

import com.github.wz2cool.localqueue.model.message.QueueMessage;

import java.util.List;

public interface LocalQueueListener {

    void onMessages(List<QueueMessage> messages);
}
