package com.github.wz2coo.localqueue.spring.model;

/**
 * Message acknowledgment mode
 */
public enum AckMode {
    
    /**
     * Auto acknowledgment mode
     * Automatically acknowledge after message processing completion
     */
    AUTO,
    
    /**
     * Manual acknowledgment mode
     * Requires manual call to Acknowledgment.acknowledge() method for confirmation
     */
    MANUAL,
    
    /**
     * Auto acknowledgment on success mode
     * Automatically acknowledge only when method execution succeeds (no exceptions)
     */
    AUTO_SUCCESS
}