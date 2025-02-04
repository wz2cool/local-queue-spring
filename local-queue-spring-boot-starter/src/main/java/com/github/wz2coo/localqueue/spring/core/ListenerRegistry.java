package com.github.wz2coo.localqueue.spring.core;

import com.github.wz2coo.localqueue.spring.annotation.LocalQueueMessageListener;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ListenerRegistry {
    private final Map<String, LocalQueueMessageListener> customerAnnotations = new ConcurrentHashMap<>();
    private final Map<String, LocalQueueListener> customerHandlers = new ConcurrentHashMap<>();


    public void register(String customerId, LocalQueueMessageListener annotation, LocalQueueListener handler) {
        customerAnnotations.put(customerId, annotation);
        customerHandlers.put(customerId, handler);
    }

    public Set<String> getCustomerIds() {
        return customerAnnotations.keySet();
    }

    public LocalQueueMessageListener getCustomerAnnotation(String customerId) {
        return customerAnnotations.get(customerId);
    }

    public LocalQueueListener getCustomerHandler(String customerId) {
        return customerHandlers.get(customerId);
    }
}