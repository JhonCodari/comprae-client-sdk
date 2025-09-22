package com.configsystem.client.monitoramento;

import com.configsystem.client.cache.CacheInteligente;
import com.configsystem.client.servico.ServicoClienteConfiguracao;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Endpoint customizado para monitoramento da SDK de configurações
 */
@Component
@Endpoint(id = "config-client")
public class EndpointMonitoramento {
    
    @Autowired
    private CacheInteligente cache;
    
    @Autowired
    private ServicoClienteConfiguracao servicoCliente;
    
    @ReadOperation
    public Map<String, Object> informacoesDetalhadas() {
        Map<String, Object> info = new HashMap<>();
        
        // Informações gerais
        info.put("timestamp", LocalDateTime.now());
        info.put("status", "ATIVO");
        info.put("versao", "1.1.0");
        
        // Estatísticas do cache
        CacheStats stats = cache.getEstatisticas();
        Map<String, Object> cacheInfo = new HashMap<>();
        cacheInfo.put("tamanho", cache.getTamanho());
        cacheInfo.put("hit_rate", String.format("%.2f%%", stats.hitRate() * 100));
        cacheInfo.put("miss_rate", String.format("%.2f%%", stats.missRate() * 100));
        cacheInfo.put("total_hits", stats.hitCount());
        cacheInfo.put("total_misses", stats.missCount());
        cacheInfo.put("evictions", stats.evictionCount());
        cacheInfo.put("load_time_avg_ms", String.format("%.2f", stats.averageLoadPenalty() / 1_000_000.0));
        
        info.put("cache", cacheInfo);
        
        // Métricas de performance
        Map<String, Object> performance = new HashMap<>();
        performance.put("cache_efficiency", stats.hitRate() > 0.8 ? "EXCELENTE" : 
                                           stats.hitRate() > 0.6 ? "BOA" : 
                                           stats.hitRate() > 0.4 ? "REGULAR" : "RUIM");
        performance.put("recomendacoes", gerarRecomendacoes(stats));
        
        info.put("performance", performance);
        
        return info;
    }
    
    private String gerarRecomendacoes(CacheStats stats) {
        if (stats.hitRate() < 0.5) {
            return "Cache com baixa eficiência. Considere aumentar o TTL ou revisar os padrões de acesso.";
        } else if (stats.evictionCount() > stats.hitCount() * 0.1) {
            return "Muitas evictions detectadas. Considere aumentar o tamanho máximo do cache.";
        } else if (stats.hitRate() > 0.9) {
            return "Cache funcionando de forma excelente!";
        } else {
            return "Cache funcionando dentro dos parâmetros normais.";
        }
    }
}