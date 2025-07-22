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
import java.util.Collections;
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
        DefaultAcknowledgment batchAcknowledgment = new DefaultAcknowledgment(consumer, messages);
        
        try {
            Parameter[] parameters = method.getParameters();
            
            if (parameters.length == 0) {
                // No parameter method
                method.invoke(bean);
                handleAutoAck(batchAcknowledgment);
            } else if (parameters.length == 1) {
                invokeWithSingleParameter(parameters[0], messages, consumer, batchAcknowledgment);
            } else if (parameters.length == 2) {
                invokeWithTwoParameters(parameters, messages, consumer, batchAcknowledgment);
            } else {
                logger.warn("Unsupported method signature with {} parameters", parameters.length);
            }
            
        } catch (Exception e) {
            logger.error("Error invoking listener method", e);
            
            // If AUTO_SUCCESS mode and exception occurs, do not ACK
            if (ackMode == AckMode.AUTO_SUCCESS) {
                logger.info("[local-queue] Message not acknowledged due to exception in AUTO_SUCCESS mode");
            } else if (ackMode == AckMode.AUTO && !batchAcknowledgment.isAcknowledged()) {
                // In AUTO mode, ACK even if there are exceptions
                batchAcknowledgment.acknowledge();
            }
            
            throw new RuntimeException("Failed to invoke listener method", e);
        }
    }
    
    private void invokeWithSingleParameter(Parameter parameter, List<QueueMessage> messages, SimpleConsumer consumer, DefaultAcknowledgment batchAcknowledgment) throws Exception {
        Class<?> paramType = parameter.getType();
        
        if (List.class.isAssignableFrom(paramType)) {
            // Parameter is List<QueueMessage> - batch processing
            method.invoke(bean, messages);
            handleAutoAck(batchAcknowledgment);
        } else if (QueueMessage.class.isAssignableFrom(paramType)) {
            // Parameter is single QueueMessage, call one by one
            for (QueueMessage message : messages) {
                DefaultAcknowledgment singleAck = new DefaultAcknowledgment(consumer, Collections.singletonList(message));
                try {
                    method.invoke(bean, message);
                    handleAutoAck(singleAck);
                } catch (Exception e) {
                    handleExceptionForSingleMessage(singleAck, e);
                }
            }
        } else if (Acknowledgment.class.isAssignableFrom(paramType)) {
            // Parameter is Acknowledgment - batch processing
            method.invoke(bean, batchAcknowledgment);
            // No auto ACK here, user controls it manually
        } else {
            logger.warn("Unsupported parameter type: {}", paramType.getName());
        }
    }
    
    private void invokeWithTwoParameters(Parameter[] parameters, List<QueueMessage> messages, SimpleConsumer consumer, DefaultAcknowledgment batchAcknowledgment) throws Exception {
        Class<?> param1Type = parameters[0].getType();
        Class<?> param2Type = parameters[1].getType();
        
        // Check if this is single message processing (first param is QueueMessage)
        boolean isSingleMessageProcessing = QueueMessage.class.isAssignableFrom(param1Type) && !List.class.isAssignableFrom(param1Type);
        
        if (isSingleMessageProcessing) {
            // Process each message individually
            for (QueueMessage message : messages) {
                DefaultAcknowledgment singleAck = new DefaultAcknowledgment(consumer, java.util.Arrays.asList(message));
                Object arg2 = null;
                
                // Determine second parameter
                if (Acknowledgment.class.isAssignableFrom(param2Type)) {
                    arg2 = singleAck;
                } else if (QueueMessage.class.isAssignableFrom(param2Type)) {
                    arg2 = message;
                }
                
                if (arg2 != null) {
                    try {
                        method.invoke(bean, message, arg2);
                        if (!Acknowledgment.class.isAssignableFrom(param2Type)) {
                            // Only auto ACK if second param is not Acknowledgment
                            handleAutoAck(singleAck);
                        }
                    } catch (Exception e) {
                        handleExceptionForSingleMessage(singleAck, e);
                    }
                } else {
                    logger.warn("Unsupported second parameter type for single message processing: {}", param2Type.getName());
                }
            }
        } else {
            // Batch processing
            Object arg1 = null;
            Object arg2 = null;
            
            // Determine first parameter
            if (List.class.isAssignableFrom(param1Type)) {
                arg1 = messages;
            } else if (Acknowledgment.class.isAssignableFrom(param1Type)) {
                arg1 = batchAcknowledgment;
            }
            
            // Determine second parameter
            if (Acknowledgment.class.isAssignableFrom(param2Type)) {
                arg2 = batchAcknowledgment;
            } else if (List.class.isAssignableFrom(param2Type) && arg1 != messages) {
                arg2 = messages;
            }
            
            if (arg1 != null && arg2 != null) {
                method.invoke(bean, arg1, arg2);
                if (!Acknowledgment.class.isAssignableFrom(param1Type) && !Acknowledgment.class.isAssignableFrom(param2Type)) {
                    // Only auto ACK if neither param is Acknowledgment
                    handleAutoAck(batchAcknowledgment);
                }
            } else {
                logger.warn("Unsupported two-parameter method signature: {} and {}", param1Type.getName(), param2Type.getName());
            }
        }
    }
    
    private void handleAutoAck(DefaultAcknowledgment acknowledgment) {
        // Decide whether to auto acknowledge based on ACK mode
        if (ackMode == AckMode.AUTO || ackMode == AckMode.AUTO_SUCCESS) {
            if (!acknowledgment.isAcknowledged()) {
                acknowledgment.acknowledge();
            }
        }
    }
    
    private void handleExceptionForSingleMessage(DefaultAcknowledgment singleAck, Exception e) throws Exception {
        // If AUTO_SUCCESS mode and exception occurs, do not ACK
        if (ackMode == AckMode.AUTO_SUCCESS) {
            logger.info("[local-queue] Single message not acknowledged due to exception in AUTO_SUCCESS mode");
        } else if (ackMode == AckMode.AUTO && !singleAck.isAcknowledged()) {
            // In AUTO mode, ACK even if there are exceptions
            singleAck.acknowledge();
        }
        
        throw e;
    }
    
    public Object getBean() {
        return bean;
    }
    
    public Method getMethod() {
        return method;
    }
}