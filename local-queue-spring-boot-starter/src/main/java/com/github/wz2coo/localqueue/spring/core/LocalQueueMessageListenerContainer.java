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

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ListenerRegistry registry;
    private final LocalQueueProperties properties;
    private final ConfigurableApplicationContext context;
    private final Map<String, ExecutorService> customerIdExecutors = new ConcurrentHashMap<>();
    private final Map<String, SimpleConsumer> consumerMap = new ConcurrentHashMap<>();

    public LocalQueueMessageListenerContainer(ListenerRegistry registry, LocalQueueProperties properties, ConfigurableApplicationContext context) {
        this.registry = registry;
        this.properties = properties;
        this.context = context;
    }

    public void start() {
        logger.info("[local-queue] start local queue listener container");
        Set<String> customerIds = registry.getCustomerIds();
        for (String customerId : customerIds) {
            LocalQueueListener handler = registry.getCustomerHandler(customerId);
            LocalQueueMessageListener annotation = registry.getCustomerAnnotation(customerId);
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            SimpleConsumer consumer = getConsumer(annotation);
            customerIdExecutors.put(customerId, executorService);
            consumerMap.put(customerId, consumer);
            executorService.execute(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        List<QueueMessage> queueMessages = consumer.batchTake(annotation.maxBatchSize());
                        handler.onMessages(queueMessages);
                        consumer.ack(queueMessages);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        logger.error("[local-queue] consumer error", e);
                    }
                }
            });
            logger.info("[local-queue] start listener container for customerId: {}, selectorTag: {}",
                    customerId, annotation.selectorTag());
        }
    }

    public void stop() {
        logger.info("[local-queue] stop local queue listener container");
        for (Map.Entry<String, SimpleConsumer> entry : consumerMap.entrySet()) {
            SimpleConsumer consumer = entry.getValue();
            consumer.close();
        }

        for (Map.Entry<String, ExecutorService> entry : customerIdExecutors.entrySet()) {
            ExecutorService executorService = entry.getValue();
            executorService.shutdownNow();
        }
    }

    private SimpleConsumer getConsumer(LocalQueueMessageListener annotation) {
        String dataDir = properties.getConsumer().getDataDir();
        SimpleConsumerConfig config = new SimpleConsumerConfig.Builder()
                .setConsumerId(annotation.customerId())
                .setDataDir(new File(dataDir))
                .setSelectTag(annotation.selectorTag())
                .setPullInterval(annotation.pullInterval())
                .build();
        return new SimpleConsumer(config);
    }
}
