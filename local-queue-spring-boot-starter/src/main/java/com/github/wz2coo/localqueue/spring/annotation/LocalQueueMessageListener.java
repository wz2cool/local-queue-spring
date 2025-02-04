package com.github.wz2coo.localqueue.spring.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LocalQueueMessageListener {

    String customerId();

    String selectorTag() default "*";

    int maxBatchSize() default 1;
}
