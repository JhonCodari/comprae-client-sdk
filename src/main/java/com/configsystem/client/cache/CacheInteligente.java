package com.configsystem.client.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de cache inteligente para configurações com TTL, métricas e observabilidade
 */
@Component
public class CacheInteligente implements HealthIndicator {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheInteligente.class);
    
    private Cache<String, ValorConfiguracao> cache;
    private final Map<String, LocalDateTime> timestampsUltimaAtualizacao = new ConcurrentHashMap<>();
    
    @Autowired(required = false)
    private MeterRegistry meterRegistry;
    
    @PostConstruct
    public void inicializar() {
        this.cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .recordStats()
            .removalListener((chave, valor, causa) -> {
                logger.debug("Configuração removida do cache: chave={}, causa={}", chave, causa);
                timestampsUltimaAtualizacao.remove(chave);
            })
            .build();
            
        // Registrar métricas se Micrometer estiver disponível
        if (meterRegistry != null) {
            CaffeineCacheMetrics.monitor(meterRegistry, cache, "config-cache");
            logger.info("Métricas do cache registradas no Micrometer");
        }
        
        logger.info("Cache inteligente inicializado com TTL de 30min e máximo de 10.000 entradas");
    }
    
    /**
     * Busca um valor no cache
     */
    public Optional<String> buscar(String chave) {
        ValorConfiguracao valor = cache.getIfPresent(chave);
        if (valor != null) {
            logger.debug("Cache HIT para chave: {}", chave);
            return Optional.of(valor.getValor());
        }
        logger.debug("Cache MISS para chave: {}", chave);
        return Optional.empty();
    }
    
    /**
     * Armazena um valor no cache
     */
    public void armazenar(String chave, String valor) {
        ValorConfiguracao valorConfig = new ValorConfiguracao(valor, LocalDateTime.now());
        cache.put(chave, valorConfig);
        timestampsUltimaAtualizacao.put(chave, LocalDateTime.now());
        logger.debug("Valor armazenado no cache: chave={}", chave);
    }
    
    /**
     * Remove um valor específico do cache
     */
    public void invalidar(String chave) {
        cache.invalidate(chave);
        timestampsUltimaAtualizacao.remove(chave);
        logger.debug("Cache invalidado para chave: {}", chave);
    }
    
    /**
     * Limpa todo o cache
     */
    public void limparTudo() {
        cache.invalidateAll();
        timestampsUltimaAtualizacao.clear();
        logger.info("Cache completamente limpo");
    }
    
    /**
     * Verifica se uma chave existe no cache e não expirou
     */
    public boolean contemChave(String chave) {
        return cache.getIfPresent(chave) != null;
    }
    
    /**
     * Retorna estatísticas do cache
     */
    public CacheStats getEstatisticas() {
        return cache.stats();
    }
    
    /**
     * Retorna o tamanho atual do cache
     */
    public long getTamanho() {
        return cache.estimatedSize();
    }
    
    /**
     * Health check do cache
     */
    @Override
    public Health health() {
        CacheStats stats = cache.stats();
        double hitRate = stats.hitRate();
        long tamanho = cache.estimatedSize();
        
        Health.Builder builder = hitRate > 0.7 ? Health.up() : Health.down();
        
        return builder
            .withDetail("cache-size", tamanho)
            .withDetail("hit-rate", String.format("%.2f%%", hitRate * 100))
            .withDetail("hit-count", stats.hitCount())
            .withDetail("miss-count", stats.missCount())
            .withDetail("eviction-count", stats.evictionCount())
            .withDetail("load-exception-count", stats.loadExceptionCount())
            .withDetail("average-load-penalty", String.format("%.2fms", stats.averageLoadPenalty() / 1_000_000.0))
            .build();
    }
    
    /**
     * Classe interna para armazenar valor com timestamp
     */
    private static class ValorConfiguracao {
        private final String valor;
        private final LocalDateTime timestamp;
        
        public ValorConfiguracao(String valor, LocalDateTime timestamp) {
            this.valor = valor;
            this.timestamp = timestamp;
        }
        
        public String getValor() {
            return valor;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}