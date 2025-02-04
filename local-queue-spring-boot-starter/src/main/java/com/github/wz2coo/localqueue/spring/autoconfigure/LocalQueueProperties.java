package com.github.wz2coo.localqueue.spring.autoconfigure;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "localqueue")
public class LocalQueueProperties {

    private Producer producer = new Producer();
    private Consumer consumer = new Consumer();

    public Producer getProducer() {
        return producer;
    }

    public void setProducer(Producer producer) {
        this.producer = producer;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }

    public static class Producer {
        private String dataDir;
        private int keepDays = -1;

        public String getDataDir() {
            return dataDir;
        }

        public void setDataDir(String dataDir) {
            this.dataDir = dataDir;
        }

        public int getKeepDays() {
            return keepDays;
        }

        public void setKeepDays(int keepDays) {
            this.keepDays = keepDays;
        }
    }

    public static class Consumer {
        private String dataDir;

        public String getDataDir() {
            return dataDir;
        }

        public void setDataDir(String dataDir) {
            this.dataDir = dataDir;
        }
    }
}
