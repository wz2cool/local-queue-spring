package com.github.wz2coo.localqueue.spring.annotation;

import com.github.wz2coo.localqueue.spring.model.AckMode;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LocalQueueMessageListener {

    String customerId();

    String selectorTag() default "*";

    int maxBatchSize() default 1;

    long pullInterval() default 500;

    AckMode ackMode() default AckMode.AUTO;
}
