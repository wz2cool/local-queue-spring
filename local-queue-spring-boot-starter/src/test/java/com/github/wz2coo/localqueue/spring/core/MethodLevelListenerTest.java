package com.github.wz2coo.localqueue.spring.core;

import com.github.wz2cool.localqueue.model.message.QueueMessage;
import com.github.wz2cool.localqueue.impl.SimpleConsumer;
import com.github.wz2coo.localqueue.spring.annotation.LocalQueueListener;
import com.github.wz2coo.localqueue.spring.model.AckMode;
import com.github.wz2coo.localqueue.spring.model.Acknowledgment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MethodLevelListenerTest {

    private ListenerRegistry registry;
    private LocalQueueListenerAnnotationBeanPostProcessor processor;
    private ConfigurableApplicationContext context;

    @BeforeEach
    void setUp() {
        registry = new ListenerRegistry();
        context = mock(ConfigurableApplicationContext.class);
        processor = new LocalQueueListenerAnnotationBeanPostProcessor(registry, context);
    }

    @Test
    void testMethodLevelAnnotationProcessing() {
        // Given
        TestMessageConsumer consumer = new TestMessageConsumer();
        
        // When
        processor.postProcessAfterInitialization(consumer, "testConsumer");
        
        // Then
        assertEquals(6, registry.getCustomerIds().size());
        assertTrue(registry.getCustomerIds().contains("test-single"));
        assertTrue(registry.getCustomerIds().contains("test-batch"));
        assertTrue(registry.getCustomerIds().contains("test-trigger"));
        assertTrue(registry.getCustomerIds().contains("manual-ack-customer"));
        assertTrue(registry.getCustomerIds().contains("auto-success-customer"));
        assertTrue(registry.getCustomerIds().contains("manual-only-customer"));
        
        assertNotNull(registry.getCustomerHandler("test-single"));
        assertNotNull(registry.getCustomerHandler("test-batch"));
        assertNotNull(registry.getCustomerHandler("test-trigger"));
        
        // Verify all listeners are MethodLocalQueueHandler type
        assertTrue(registry.getCustomerHandler("test-single") instanceof MethodLocalQueueHandler);
        assertTrue(registry.getCustomerHandler("test-batch") instanceof MethodLocalQueueHandler);
        assertTrue(registry.getCustomerHandler("test-trigger") instanceof MethodLocalQueueHandler);
    }

    @Test
    void testMethodLocalQueueHandlerExecution() throws Exception {
        // Given
        TestMessageConsumer consumer = new TestMessageConsumer();
        Method method = TestMessageConsumer.class.getMethod("handleSingleMessage", QueueMessage.class);
        MethodLocalQueueHandler listener = new MethodLocalQueueHandler(consumer, method);
        SimpleConsumer simpleConsumer = mock(SimpleConsumer.class);
        
        QueueMessage message = mock(QueueMessage.class);
        when(message.getContent()).thenReturn("test message");
        
        // When
        listener.onMessages(Arrays.asList(message), simpleConsumer);
        
        // Then
        assertTrue(consumer.singleMessageProcessed);
        assertEquals("test message", consumer.lastProcessedMessage);
    }

    @Test
    void testBatchMethodExecution() throws Exception {
        // Given
        TestMessageConsumer consumer = new TestMessageConsumer();
        Method method = TestMessageConsumer.class.getMethod("handleBatchMessages", List.class);
        MethodLocalQueueHandler listener = new MethodLocalQueueHandler(consumer, method);
        SimpleConsumer simpleConsumer = mock(SimpleConsumer.class);
        
        QueueMessage message1 = mock(QueueMessage.class);
        when(message1.getContent()).thenReturn("message1");
        QueueMessage message2 = mock(QueueMessage.class);
        when(message2.getContent()).thenReturn("message2");
        
        List<QueueMessage> messages = Arrays.asList(message1, message2);
        
        // When
        listener.onMessages(messages, simpleConsumer);
        
        // Then
        assertTrue(consumer.batchMessagesProcessed);
        assertEquals(2, consumer.lastBatchSize);
    }

    @Test
    void testTriggerMethodExecution() throws Exception {
        // Given
        TestMessageConsumer consumer = new TestMessageConsumer();
        Method method = TestMessageConsumer.class.getMethod("handleTriggerMessage");
        MethodLocalQueueHandler listener = new MethodLocalQueueHandler(consumer, method);
        SimpleConsumer simpleConsumer = mock(SimpleConsumer.class);
        
        QueueMessage message = mock(QueueMessage.class);
        when(message.getContent()).thenReturn("trigger");
        
        // When
        listener.onMessages(Arrays.asList(message), simpleConsumer);
        
        // Then
        assertTrue(consumer.triggerProcessed);
    }
    
    @Test
    void testManualAckMode() throws Exception {
        // Given
        TestMessageConsumer consumer = new TestMessageConsumer();
        Method method = TestMessageConsumer.class.getMethod("handleManualAckMessage", QueueMessage.class, Acknowledgment.class);
        MethodLocalQueueHandler listener = new MethodLocalQueueHandler(consumer, method);
        SimpleConsumer simpleConsumer = mock(SimpleConsumer.class);
        
        QueueMessage message = mock(QueueMessage.class);
        when(message.getContent()).thenReturn("manual-ack-test");
        List<QueueMessage> messages = Arrays.asList(message);
        
        // When
        listener.onMessages(messages, simpleConsumer);
        
        // Then
        assertTrue(consumer.singleMessageProcessed);
        assertEquals("manual-ack-test", consumer.lastProcessedMessage);
        verify(simpleConsumer, times(1)).ack(messages);
    }
    
    @Test
    void testAutoSuccessMode() throws Exception {
        // Given
        TestMessageConsumer consumer = new TestMessageConsumer();
        Method method = TestMessageConsumer.class.getMethod("handleAutoSuccessMessage", QueueMessage.class);
        MethodLocalQueueHandler listener = new MethodLocalQueueHandler(consumer, method);
        SimpleConsumer simpleConsumer = mock(SimpleConsumer.class);
        
        QueueMessage successMessage = mock(QueueMessage.class);
        when(successMessage.getContent()).thenReturn("success");
        List<QueueMessage> messages = Arrays.asList(successMessage);
        
        // When
        listener.onMessages(messages, simpleConsumer);
        
        // Then
        verify(simpleConsumer, times(1)).ack(messages);
    }
    
    @Test
    void testAutoSuccessModeWithError() throws Exception {
        // Given
        TestMessageConsumer consumer = new TestMessageConsumer();
        Method method = TestMessageConsumer.class.getMethod("handleAutoSuccessMessage", QueueMessage.class);
        MethodLocalQueueHandler listener = new MethodLocalQueueHandler(consumer, method);
        SimpleConsumer simpleConsumer = mock(SimpleConsumer.class);
        
        QueueMessage errorMessage = mock(QueueMessage.class);
        when(errorMessage.getContent()).thenReturn("error");
        List<QueueMessage> messages = Arrays.asList(errorMessage);
        
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            listener.onMessages(messages, simpleConsumer);
        });
        
        verify(simpleConsumer, never()).ack(messages);
    }
    
    @Test
    void testManualOnlyAckMode() throws Exception {
        // Given
        TestMessageConsumer consumer = new TestMessageConsumer();
        Method method = TestMessageConsumer.class.getMethod("handleManualOnlyMessage", Acknowledgment.class);
        MethodLocalQueueHandler listener = new MethodLocalQueueHandler(consumer, method);
        SimpleConsumer simpleConsumer = mock(SimpleConsumer.class);
        
        QueueMessage message = mock(QueueMessage.class);
        when(message.getContent()).thenReturn("manual-only-test");
        List<QueueMessage> messages = Arrays.asList(message);
        
        // When
        listener.onMessages(messages, simpleConsumer);
        
        // Then
        assertTrue(consumer.manualAckCalled.get());
        verify(simpleConsumer, times(1)).ack(messages);
    }

    // Test class with method-level annotations
    public static class TestMessageConsumer {
        
        public boolean singleMessageProcessed = false;
        public boolean batchMessagesProcessed = false;
        public boolean triggerMethodCalled = false;
        public boolean triggerProcessed = false;
        public String lastProcessedMessage;
        public int lastBatchSize;
        
        @LocalQueueListener(
            customerId = "test-single",
            selectorTag = "test.single"
        )
        public void handleSingleMessage(QueueMessage message) {
            System.out.println("[DEBUG] handleSingleMessage called! Message content: " + message.getContent());
            singleMessageProcessed = true;
            lastProcessedMessage = message.getContent();
        }
        
        @LocalQueueListener(
            customerId = "manual-ack-customer",
            selectorTag = "test.manual",
            ackMode = AckMode.MANUAL
        )
        public void handleManualAckMessage(QueueMessage message, Acknowledgment ack) {
            System.out.println("[DEBUG] handleManualAckMessage called! Message content: " + message.getContent());
            singleMessageProcessed = true;
            lastProcessedMessage = message.getContent();
            ack.acknowledge();
        }
        
        @LocalQueueListener(
            customerId = "auto-success-customer",
            selectorTag = "test.auto",
            ackMode = AckMode.AUTO_SUCCESS
        )
        public void handleAutoSuccessMessage(QueueMessage message) {
            System.out.println("[DEBUG] handleAutoSuccessMessage called! Message content: " + message.getContent());
            singleMessageProcessed = true;
            lastProcessedMessage = message.getContent();
            if ("error".equals(message.getContent())) {
                throw new RuntimeException("Simulated error");
            }
        }
        
        private final AtomicBoolean manualAckCalled = new AtomicBoolean(false);
        
        @LocalQueueListener(
            customerId = "manual-only-customer",
            selectorTag = "test.manual.only",
            ackMode = AckMode.MANUAL
        )
        public void handleManualOnlyMessage(Acknowledgment ack) {
            System.out.println("[DEBUG] handleManualOnlyMessage called!");
            manualAckCalled.set(true);
            ack.acknowledge();
        }
        
        @LocalQueueListener(
            customerId = "test-batch",
            selectorTag = "test.batch",
            maxBatchSize = 10
        )
        public void handleBatchMessages(List<QueueMessage> messages) {
            batchMessagesProcessed = true;
            lastBatchSize = messages.size();
        }
        
        @LocalQueueListener(
            customerId = "test-trigger",
            selectorTag = "test.trigger"
        )
        public void handleTriggerMessage() {
            triggerProcessed = true;
        }
    }
}