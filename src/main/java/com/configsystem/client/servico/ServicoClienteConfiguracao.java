package com.configsystem.client.servico;

import com.configsystem.client.configuracao.PropriedadesClienteConfiguracao;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço principal para comunicação com o servidor de configurações
 */
@Service
public class ServicoClienteConfiguracao {

    private static final Logger logger = LoggerFactory.getLogger(ServicoClienteConfiguracao.class);

    @Autowired
    private PropriedadesClienteConfiguracao propriedades;

    @Autowired
    private ObjectMapper objectMapper;

    private final WebClient webClient;
    private final Map<String, String> cacheLocal = new ConcurrentHashMap<>();

    public ServicoClienteConfiguracao(PropriedadesClienteConfiguracao propriedades) {
        this.webClient = WebClient.builder()
            .baseUrl(propriedades.getServerUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, criarCabecalhoAuth(propriedades))
            .build();
    }

    /**
     * Busca uma configuração específica por chave
     */
    public String buscarValorConfiguracao(String chave) {
        return buscarValorConfiguracao(chave, propriedades.getNamespace(), propriedades.getEnvironment());
    }

    /**
     * Busca uma configuração com valor padrão
     */
    public String buscarValorConfiguracao(String chave, String valorPadrao) {
        String valor = buscarValorConfiguracao(chave);
        return valor != null ? valor : valorPadrao;
    }

    /**
     * Busca uma configuração específica por chave, namespace e environment
     */
    public String buscarValorConfiguracao(String chave, String namespace, String environment) {
        String chaveCache = namespace + ":" + environment + ":" + chave;
        
        // Verificar cache local primeiro
        if (propriedades.isCacheEnabled()) {
            String valorCacheado = cacheLocal.get(chaveCache);
            if (valorCacheado != null) {
                logger.debug("Valor encontrado no cache local: {}", chaveCache);
                return valorCacheado;
            }
        }

        // Buscar no servidor
        try {
            String valor = buscarDoServidor(namespace, environment, chave);
            if (valor != null && propriedades.isCacheEnabled()) {
                cacheLocal.put(chaveCache, valor);
            }
            return valor;
        } catch (Exception e) {
            logger.error("Erro ao buscar configuração do servidor: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Busca todas as configurações disponíveis
     */
    public Map<String, String> buscarTodasConfiguracoes() {
        return buscarTodasConfiguracoes(propriedades.getNamespace(), propriedades.getEnvironment());
    }

    /**
     * Busca todas as configurações para um namespace e environment específicos
     */
    public Map<String, String> buscarTodasConfiguracoes(String namespace, String environment) {
        try {
            String response = webClient.get()
                .uri("/api/configs/{namespace}/{environment}/map", namespace, environment)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (response != null) {
                Map<String, String> configs = objectMapper.readValue(response, new TypeReference<Map<String, String>>() {});
                
                // Atualizar cache local
                if (propriedades.isCacheEnabled()) {
                    configs.forEach((chave, valor) -> {
                        String chaveCache = namespace + ":" + environment + ":" + chave;
                        cacheLocal.put(chaveCache, valor);
                    });
                }
                
                return configs;
            }
        } catch (Exception e) {
            logger.error("Erro ao buscar todas as configurações: {}", e.getMessage());
        }
        
        return new ConcurrentHashMap<>();
    }

    /**
     * Atualiza uma configuração específica no cache
     */
    public void atualizarConfiguracao(String chave) {
        atualizarConfiguracao(chave, propriedades.getNamespace(), propriedades.getEnvironment());
    }

    /**
     * Atualiza uma configuração específica no cache
     */
    public void atualizarConfiguracao(String chave, String namespace, String environment) {
        String chaveCache = namespace + ":" + environment + ":" + chave;
        cacheLocal.remove(chaveCache);
        
        // Buscar nova configuração
        buscarValorConfiguracao(chave, namespace, environment);
    }

    /**
     * Atualiza todas as configurações no cache
     */
    public void atualizarTodasConfiguracoes() {
        cacheLocal.clear();
        buscarTodasConfiguracoes();
    }

    /**
     * Verifica se o servidor de configurações está disponível
     */
    public boolean isServidorConfigDisponivel() {
        try {
            String response = webClient.get()
                .uri("/actuator/health")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            return response != null && response.contains("UP");
        } catch (Exception e) {
            logger.warn("Servidor de configurações não está disponível: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Busca configuração diretamente do servidor
     */
    private String buscarDoServidor(String namespace, String environment, String chave) {
        try {
            return webClient.get()
                .uri("/api/configs/{namespace}/{environment}/{key}", namespace, environment, chave)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> config = (Map<String, Object>) objectMapper.readValue(response, Map.class);
                        return (String) config.get("value");
                    } catch (Exception e) {
                        logger.error("Erro ao deserializar resposta: {}", e.getMessage());
                        return null;
                    }
                })
                .block();
        } catch (Exception e) {
            logger.error("Erro ao buscar configuração do servidor: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Cria cabeçalho de autenticação
     */
    private String criarCabecalhoAuth(PropriedadesClienteConfiguracao propriedades) {
        String auth = propriedades.getUsername() + ":" + propriedades.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }

    /**
     * Obtém o cache local
     */
    public Map<String, String> obterCacheLocal() {
        return new ConcurrentHashMap<>(cacheLocal);
    }

    /**
     * Limpa o cache local
     */
    public void limparCache() {
        cacheLocal.clear();
    }
}
