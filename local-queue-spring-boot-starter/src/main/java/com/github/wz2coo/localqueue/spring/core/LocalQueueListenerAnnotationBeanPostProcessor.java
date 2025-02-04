package com.github.wz2coo.localqueue.spring.core;

import com.github.wz2coo.localqueue.spring.annotation.LocalQueueMessageListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;

public class LocalQueueListenerAnnotationBeanPostProcessor implements BeanPostProcessor {

    private final ListenerRegistry registry;
    private final ConfigurableApplicationContext context;

    public LocalQueueListenerAnnotationBeanPostProcessor(ListenerRegistry registry, ConfigurableApplicationContext context) {
        this.registry = registry;
        this.context = context;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        LocalQueueMessageListener annotation = beanClass.getAnnotation(LocalQueueMessageListener.class);
        if (annotation != null && bean instanceof LocalQueueListener) {
            String customerId = annotation.customerId();
            registry.register(customerId, annotation, (LocalQueueListener) bean);
        }
        return bean;
    }
}
