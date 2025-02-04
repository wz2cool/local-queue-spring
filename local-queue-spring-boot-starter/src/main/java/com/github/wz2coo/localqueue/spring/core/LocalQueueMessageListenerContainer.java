package com.github.wz2coo.localqueue.spring.core;

import com.github.wz2coo.localqueue.spring.annotation.LocalQueueMessageListener;
import com.github.wz2coo.localqueue.spring.autoconfigure.LocalQueueProperties;
import com.github.wz2cool.localqueue.impl.SimpleConsumer;
import com.github.wz2cool.localqueue.model.config.SimpleConsumerConfig;
import com.github.wz2cool.localqueue.model.message.QueueMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalQueueMessageListenerContainer {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ListenerRegistry registry;
    private final LocalQueueProperties properties;
    private final ConfigurableApplicationContext context;
    private final Map<String, ExecutorService> customerIdExecutors = new ConcurrentHashMap<>();
    private final Map<String, SimpleConsumer> consumers = new ConcurrentHashMap<>();

    public LocalQueueMessageListenerContainer(ListenerRegistry registry, LocalQueueProperties properties, ConfigurableApplicationContext context) {
        this.registry = registry;
        this.properties = properties;
        this.context = context;
    }

    public void start() {
        Set<String> customerIds = registry.getCustomerIds();
        for (String customerId : customerIds) {
            LocalQueueListener handler = registry.getCustomerHandler(customerId);
            LocalQueueMessageListener annotation = registry.getCustomerAnnotation(customerId);
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            SimpleConsumer consumer = getConsumer(annotation);
            customerIdExecutors.put(customerId, executorService);
            executorService.execute(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        List<QueueMessage> queueMessages = consumer.batchTake(annotation.maxBatchSize());
                        handler.onMessages(queueMessages);
                        consumer.ack(queueMessages);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        logger.error("consumer error", e);
                    }
                }
            });
        }
    }

    public void stop() {
        for (Map.Entry<String, ExecutorService> entry : customerIdExecutors.entrySet()) {
            String customerId = entry.getKey();
            ExecutorService executorService = entry.getValue();
            SimpleConsumer consumer = consumers.get(customerId);
            consumer.close();
            executorService.shutdownNow();
        }
    }

    private SimpleConsumer getConsumer(LocalQueueMessageListener annotation) {
        String dataDir = properties.getConsumer().getDataDir();
        SimpleConsumerConfig config = new SimpleConsumerConfig.Builder()
                .setConsumerId(annotation.customerId())
                .setDataDir(new File(dataDir))
                .setSelectTag(annotation.selectorTag())
                .build();


        return new SimpleConsumer(config);
    }
}
