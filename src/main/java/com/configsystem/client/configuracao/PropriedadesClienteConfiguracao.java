package com.configsystem.client.configuracao;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propriedades de configuração do cliente
 */
@Component
@ConfigurationProperties(prefix = "config.client")
public class PropriedadesClienteConfiguracao {

    private String serverUrl = "http://localhost:8080";
    private String namespace = "default";
    private String environment = "dev";
    private String username = "admin";
    private String password = "admin123";
    private boolean cacheEnabled = true;
    private long cacheTtl = 300000; // 5 minutos
    private boolean syncEnabled = true;
    private long syncInterval = 30000; // 30 segundos
    
    // Kafka configuration
    private KafkaConfig kafka = new KafkaConfig();
    
    // Getters and setters
    public String getServerUrl() {
        return serverUrl;
    }
    
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
    
    public String getNamespace() {
        return namespace;
    }
    
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
    
    public String getEnvironment() {
        return environment;
    }
    
    public void setEnvironment(String environment) {
        this.environment = environment;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }
    
    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }
    
    public long getCacheTtl() {
        return cacheTtl;
    }
    
    public void setCacheTtl(long cacheTtl) {
        this.cacheTtl = cacheTtl;
    }
    
    public boolean isSyncEnabled() {
        return syncEnabled;
    }
    
    public void setSyncEnabled(boolean syncEnabled) {
        this.syncEnabled = syncEnabled;
    }
    
    public long getSyncInterval() {
        return syncInterval;
    }
    
    public void setSyncInterval(long syncInterval) {
        this.syncInterval = syncInterval;
    }
    
    public KafkaConfig getKafka() {
        return kafka;
    }
    
    public void setKafka(KafkaConfig kafka) {
        this.kafka = kafka;
    }
    
    public static class KafkaConfig {
        private boolean enabled = false;
        private String bootstrapServers = "localhost:9092";
        private String topic = "config-changes";
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getBootstrapServers() {
            return bootstrapServers;
        }
        
        public void setBootstrapServers(String bootstrapServers) {
            this.bootstrapServers = bootstrapServers;
        }
        
        public String getTopic() {
            return topic;
        }
        
        public void setTopic(String topic) {
            this.topic = topic;
        }
    }
}
