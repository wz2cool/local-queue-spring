package com.github.wz2coo.localqueue.spring.core;

import com.github.wz2coo.localqueue.spring.annotation.LocalQueueListener;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ListenerRegistry {
    private final Map<String, LocalQueueListener> customerAnnotations = new ConcurrentHashMap<>();
    private final Map<String, LocalQueueHandler> customerHandlers = new ConcurrentHashMap<>();


    public void register(String customerId, LocalQueueListener annotation, LocalQueueHandler handler) {
        customerAnnotations.put(customerId, annotation);
        customerHandlers.put(customerId, handler);
    }

    public Set<String> getCustomerIds() {
        return customerHandlers.keySet();
    }

    public LocalQueueListener getCustomerAnnotation(String customerId) {
        return customerAnnotations.get(customerId);
    }

    public LocalQueueHandler getCustomerHandler(String customerId) {
        return customerHandlers.get(customerId);
    }
}