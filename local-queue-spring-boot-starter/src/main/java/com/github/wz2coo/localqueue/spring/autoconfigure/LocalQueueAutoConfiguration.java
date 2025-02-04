package com.github.wz2coo.localqueue.spring.autoconfigure;

import com.github.wz2cool.localqueue.IProducer;
import com.github.wz2cool.localqueue.impl.SimpleProducer;
import com.github.wz2cool.localqueue.model.config.SimpleProducerConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@EnableConfigurationProperties(LocalQueueProperties.class)
@Configuration
public class LocalQueueAutoConfiguration {

    @Bean(destroyMethod = "close")
    public IProducer getProducer(LocalQueueProperties localQueueProperties) {
        SimpleProducerConfig config = new SimpleProducerConfig.Builder()
                .setDataDir(new File(localQueueProperties.getProducer().getDataDir()))
                .setKeepDays(localQueueProperties.getProducer().getKeepDays())
                .build();
        return new SimpleProducer(config);
    }
}
