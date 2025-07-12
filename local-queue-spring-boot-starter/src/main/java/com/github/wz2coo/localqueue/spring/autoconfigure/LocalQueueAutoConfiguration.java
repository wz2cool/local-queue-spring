package com.github.wz2coo.localqueue.spring.autoconfigure;

import com.github.wz2coo.localqueue.spring.core.ListenerRegistry;
import com.github.wz2coo.localqueue.spring.core.LocalQueueListenerAnnotationBeanPostProcessor;
import com.github.wz2coo.localqueue.spring.core.LocalQueueMessageListenerContainer;
import com.github.wz2cool.localqueue.IProducer;
import com.github.wz2cool.localqueue.impl.SimpleProducer;
import com.github.wz2cool.localqueue.model.config.SimpleProducerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;

import java.io.File;

@EnableConfigurationProperties(LocalQueueProperties.class)
@Configuration
public class LocalQueueAutoConfiguration {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean(destroyMethod = "close")
    public IProducer getProducer(LocalQueueProperties localQueueProperties) {
        String dataDir = localQueueProperties.getProducer().getDataDir();
        logger.info("[local-queue] init producer with data dir: {}", dataDir);
        SimpleProducerConfig config = new SimpleProducerConfig.Builder()
                .setDataDir(new File(dataDir))
                .setKeepDays(localQueueProperties.getProducer().getKeepDays())
                .build();
        return new SimpleProducer(config);
    }

    @Bean
    public ListenerRegistry listenerRegistry() {
        return new ListenerRegistry();
    }

    @Bean
    public LocalQueueListenerAnnotationBeanPostProcessor localQueueListenerAnnotationBeanPostProcessor(
            ListenerRegistry registry, ConfigurableApplicationContext context) {
        return new LocalQueueListenerAnnotationBeanPostProcessor(registry, context);
    }

    @Bean
    public LocalQueueMessageListenerContainer localQueueMessageListenerContainer(
            ListenerRegistry registry, LocalQueueProperties properties, ConfigurableApplicationContext context) {
        return new LocalQueueMessageListenerContainer(registry, properties, context);
    }

    @Bean
    public ApplicationRunner startListenerContainer(LocalQueueMessageListenerContainer container) {
        logger.info("[local-queue] start local queue listener container");
        return args -> container.start();
    }

    @Bean
    public ApplicationListener<ContextClosedEvent> contextClosedListener(LocalQueueMessageListenerContainer container) {
        return event -> {
            logger.info("[local-queue] ContextClosedEvent received, performing cleanup...");
            container.stop();
        };
    }
}
