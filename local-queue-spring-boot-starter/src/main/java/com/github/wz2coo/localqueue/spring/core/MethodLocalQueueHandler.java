package com.github.wz2coo.localqueue.spring.core;

import com.github.wz2coo.localqueue.spring.annotation.LocalQueueListener;
import com.github.wz2coo.localqueue.spring.model.AckMode;
import com.github.wz2coo.localqueue.spring.model.Acknowledgment;
import com.github.wz2cool.localqueue.impl.SimpleConsumer;
import com.github.wz2cool.localqueue.model.message.QueueMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

public class MethodLocalQueueHandler implements LocalQueueHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private final Object bean;
    private final Method method;
    private final AckMode ackMode;
    
    public MethodLocalQueueHandler(Object bean, Method method) {
        this.bean = bean;
        this.method = method;
        this.method.setAccessible(true);
        
        // Get ACK mode
        LocalQueueListener annotation = method.getAnnotation(LocalQueueListener.class);
        this.ackMode = annotation != null ? annotation.ackMode() : AckMode.AUTO;
    }
    
    @Override
    public void onMessages(List<QueueMessage> messages, SimpleConsumer consumer) {
        DefaultAcknowledgment acknowledgment = new DefaultAcknowledgment(consumer, messages);
        boolean shouldAutoAck = true;
        
        try {
            Parameter[] parameters = method.getParameters();
            
            if (parameters.length == 0) {
                // No parameter method
                method.invoke(bean);
            } else if (parameters.length == 1) {
                invokeWithSingleParameter(parameters[0], messages, acknowledgment);
            } else if (parameters.length == 2) {
                invokeWithTwoParameters(parameters, messages, acknowledgment);
            } else {
                logger.warn("Unsupported method signature with {} parameters", parameters.length);
            }
            
            // Decide whether to auto acknowledge based on ACK mode
            if (ackMode == AckMode.AUTO || ackMode == AckMode.AUTO_SUCCESS) {
                if (!acknowledgment.isAcknowledged()) {
                    acknowledgment.acknowledge();
                }
            }
            
        } catch (Exception e) {
            logger.error("Error invoking listener method", e);
            
            // If AUTO_SUCCESS mode and exception occurs, do not ACK
            if (ackMode == AckMode.AUTO_SUCCESS) {
                shouldAutoAck = false;
                logger.info("[local-queue] Message not acknowledged due to exception in AUTO_SUCCESS mode");
            } else if (ackMode == AckMode.AUTO && !acknowledgment.isAcknowledged()) {
                // In AUTO mode, ACK even if there are exceptions
                acknowledgment.acknowledge();
            }
            
            throw new RuntimeException("Failed to invoke listener method", e);
        }
    }
    
    private void invokeWithSingleParameter(Parameter parameter, List<QueueMessage> messages, Acknowledgment acknowledgment) throws Exception {
        Class<?> paramType = parameter.getType();
        
        if (List.class.isAssignableFrom(paramType)) {
            // Parameter is List<QueueMessage>
            method.invoke(bean, messages);
        } else if (QueueMessage.class.isAssignableFrom(paramType)) {
            // Parameter is single QueueMessage, call one by one
            for (QueueMessage message : messages) {
                method.invoke(bean, message);
            }
        } else if (Acknowledgment.class.isAssignableFrom(paramType)) {
            // Parameter is Acknowledgment
            method.invoke(bean, acknowledgment);
        } else {
            logger.warn("Unsupported parameter type: {}", paramType.getName());
        }
    }
    
    private void invokeWithTwoParameters(Parameter[] parameters, List<QueueMessage> messages, Acknowledgment acknowledgment) throws Exception {
        Class<?> param1Type = parameters[0].getType();
        Class<?> param2Type = parameters[1].getType();
        
        Object arg1 = null;
        Object arg2 = null;
        
        // Determine first parameter
        if (List.class.isAssignableFrom(param1Type)) {
            arg1 = messages;
        } else if (QueueMessage.class.isAssignableFrom(param1Type)) {
            // For single message, only process the first one
            arg1 = messages.isEmpty() ? null : messages.get(0);
        } else if (Acknowledgment.class.isAssignableFrom(param1Type)) {
            arg1 = acknowledgment;
        }
        
        // Determine second parameter
        if (Acknowledgment.class.isAssignableFrom(param2Type)) {
            arg2 = acknowledgment;
        } else if (QueueMessage.class.isAssignableFrom(param2Type) && arg1 != acknowledgment) {
            arg2 = messages.isEmpty() ? null : messages.get(0);
        } else if (List.class.isAssignableFrom(param2Type) && arg1 != messages) {
            arg2 = messages;
        }
        
        if (arg1 != null && arg2 != null) {
            if (QueueMessage.class.isAssignableFrom(param1Type) && !List.class.isAssignableFrom(param1Type)) {
                // If first parameter is single QueueMessage, process one by one
                for (QueueMessage message : messages) {
                    method.invoke(bean, message, arg2);
                }
            } else {
                method.invoke(bean, arg1, arg2);
            }
        } else {
            logger.warn("Unsupported two-parameter method signature: {} and {}", param1Type.getName(), param2Type.getName());
        }
    }
    
    public Object getBean() {
        return bean;
    }
    
    public Method getMethod() {
        return method;
    }
}