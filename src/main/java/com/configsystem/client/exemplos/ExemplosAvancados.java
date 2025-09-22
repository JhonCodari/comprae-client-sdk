package com.configsystem.client.exemplos;

import com.configsystem.client.anotacao.ValorConfiguracao;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exemplos avançados usando os novos recursos da SDK
 */
@RestController
public class ExemplosAvancados {

    // === CONVERSÃO AUTOMÁTICA DE TIPOS ===
    
    @ValorConfiguracao(value = "database.port", type = Integer.class, min = 1, max = 65535, defaultValue = "5432")
    private Integer portaBanco;
    
    @ValorConfiguracao(value = "app.timeout", type = Duration.class, defaultValue = "PT30S")
    private Duration timeoutApp;
    
    @ValorConfiguracao(value = "feature.enabled", type = Boolean.class, defaultValue = "false")
    private Boolean featureHabilitada;
    
    @ValorConfiguracao(value = "price.max", type = BigDecimal.class, defaultValue = "999.99")
    private BigDecimal precoMaximo;
    
    // === VALIDAÇÃO AUTOMÁTICA ===
    
    @ValorConfiguracao(value = "admin.email", validator = "email", defaultValue = "admin@comprae.com.br")
    private String emailAdmin;
    
    @ValorConfiguracao(value = "api.endpoint", validator = "url", defaultValue = "https://api.comprae.com.br")
    private String endpointApi;
    
    @ValorConfiguracao(value = "server.host", validator = "ip", defaultValue = "127.0.0.1")
    private String hostServidor;
    
    // === LISTAS E ARRAYS ===
    
    @ValorConfiguracao(value = "allowed.origins", type = List.class, separator = ",", defaultValue = "localhost,127.0.0.1")
    private List<String> origensPermitidas;
    
    @ValorConfiguracao(value = "cache.levels", type = List.class, separator = ";", defaultValue = "L1;L2;L3")
    private List<String> niveisCache;
    
    // === PROFILES E FALLBACKS ===
    
    @ValorConfiguracao(
        value = "database.url.prod", 
        profiles = {"prod", "staging"}, 
        fallback = "database.url.default",
        defaultValue = "jdbc:postgresql://localhost:5432/comprae"
    )
    private String urlBancoProd;
    
    @ValorConfiguracao(
        value = "logging.level.dev",
        profiles = {"dev", "test"},
        fallback = "logging.level.default",
        defaultValue = "DEBUG"
    )
    private String logLevelDev;
    
    // === CONFIGURAÇÕES OBRIGATÓRIAS ===
    
    @ValorConfiguracao(value = "security.jwt.secret", required = true, refreshable = false)
    private String jwtSecret;
    
    @ValorConfiguracao(value = "payment.api.key", required = true, validator = "^[A-Za-z0-9]{32}$")
    private String paymentApiKey;
    
    // === ENDPOINT PARA DEMONSTRAR OS VALORES ===
    
    @GetMapping("/exemplos/configuracoes-avancadas")
    public Map<String, Object> obterConfiguracoes() {
        Map<String, Object> configs = new HashMap<>();
        
        // Tipos convertidos automaticamente
        configs.put("database.port", portaBanco);
        configs.put("app.timeout", timeoutApp);
        configs.put("feature.enabled", featureHabilitada);
        configs.put("price.max", precoMaximo);
        
        // Valores validados
        configs.put("admin.email", emailAdmin);
        configs.put("api.endpoint", endpointApi);
        configs.put("server.host", hostServidor);
        
        // Listas
        configs.put("allowed.origins", origensPermitidas);
        configs.put("cache.levels", niveisCache);
        
        // Com profiles/fallbacks
        configs.put("database.url.prod", urlBancoProd);
        configs.put("logging.level.dev", logLevelDev);
        
        // Informações de runtime
        configs.put("_timestamp", LocalDateTime.now());
        configs.put("_tipos", Map.of(
            "portaBanco", portaBanco != null ? portaBanco.getClass().getSimpleName() : "null",
            "timeoutApp", timeoutApp != null ? timeoutApp.getClass().getSimpleName() : "null",
            "origensPermitidas", origensPermitidas != null ? origensPermitidas.getClass().getSimpleName() : "null"
        ));
        
        return configs;
    }
    
    @GetMapping("/exemplos/validacao-demo")
    public Map<String, Object> demonstrarValidacao() {
        Map<String, Object> demo = new HashMap<>();
        
        demo.put("info", "Demonstração dos recursos de validação e conversão");
        demo.put("conversoes_automaticas", Map.of(
            "Integer", "database.port = " + portaBanco + " (tipo: " + (portaBanco != null ? portaBanco.getClass().getSimpleName() : "null") + ")",
            "Duration", "app.timeout = " + timeoutApp + " (tipo: " + (timeoutApp != null ? timeoutApp.getClass().getSimpleName() : "null") + ")",
            "Boolean", "feature.enabled = " + featureHabilitada + " (tipo: " + (featureHabilitada != null ? featureHabilitada.getClass().getSimpleName() : "null") + ")"
        ));
        
        demo.put("validacoes", Map.of(
            "email", "admin.email = " + emailAdmin + " (validado como email)",
            "url", "api.endpoint = " + endpointApi + " (validado como URL)",
            "ip", "server.host = " + hostServidor + " (validado como IP)"
        ));
        
        demo.put("listas", Map.of(
            "origens", origensPermitidas,
            "cache_levels", niveisCache
        ));
        
        return demo;
    }
}