package com.github.wz2coo.localqueue.spring.core;


import com.github.wz2coo.localqueue.spring.annotation.LocalQueueListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

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
        
        // Process method-level annotations
        ReflectionUtils.doWithMethods(beanClass, method -> {
            LocalQueueListener methodAnnotation = 
                method.getAnnotation(LocalQueueListener.class);
            if (methodAnnotation != null) {
                String customerId = methodAnnotation.customerId();
                MethodLocalQueueHandler methodListener = new MethodLocalQueueHandler(bean, method);
                registry.register(customerId, methodAnnotation, methodListener);
            }
        });
        
        return bean;
    }
}
