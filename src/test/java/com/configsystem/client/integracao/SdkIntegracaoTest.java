package com.configsystem.client.integracao;

import com.configsystem.client.anotacao.ValorConfiguracao;
import com.configsystem.client.cache.CacheInteligente;
import com.configsystem.client.configuracao.PropriedadesClienteConfiguracao;
import com.configsystem.client.servico.ServicoClienteConfiguracao;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes de integração para a SDK do cliente de configuração
 */
@SpringBootTest
@TestPropertySource(properties = {
    "config.client.namespace=test-app",
    "config.client.environment=test",
    "config.client.username=test-user",
    "config.client.password=test-pass"
})
class SdkIntegracaoTest {

    private MockWebServer mockServer;

    @Autowired
    private ServicoClienteConfiguracao servicoCliente;

    @Autowired
    private CacheInteligente cache;

    @Autowired
    private ComponenteTesteDinamico componenteTeste;

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    @TestConfiguration
    static class TestConfig {
        
        @Bean
        @Primary
        public PropriedadesClienteConfiguracao propriedadesTest() {
            PropriedadesClienteConfiguracao props = new PropriedadesClienteConfiguracao();
            props.setServerUrl("http://localhost:8080"); // Será substituído pelo mock
            props.setNamespace("test-app");
            props.setEnvironment("test");
            props.setUsername("test-user");
            props.setPassword("test-pass");
            return props;
        }

        @Bean
        public ComponenteTesteDinamico componenteTesteDinamico() {
            return new ComponenteTesteDinamico();
        }
    }

    /**
     * Componente de teste com injeção dinâmica de configurações
     */
    static class ComponenteTesteDinamico {
        
        @ValorConfiguracao(value = "database.port", type = Integer.class, defaultValue = "5432")
        private Integer portaBanco;
        
        @ValorConfiguracao(value = "app.timeout", type = Duration.class, defaultValue = "PT30S")
        private Duration timeout;
        
        @ValorConfiguracao(value = "feature.enabled", type = Boolean.class, defaultValue = "false")
        private Boolean featureHabilitada;
        
        @ValorConfiguracao(value = "allowed.hosts", type = List.class, defaultValue = "localhost")
        private List<String> hostsPermitidos;

        public Integer getPortaBanco() { return portaBanco; }
        public Duration getTimeout() { return timeout; }
        public Boolean getFeatureHabilitada() { return featureHabilitada; }
        public List<String> getHostsPermitidos() { return hostsPermitidos; }
    }

    @Test
    void deveInjetarConfiguracoesPadrao() {
        // Given & When - configurações padrão devem ser injetadas automaticamente
        
        // Then
        assertThat(componenteTeste.getPortaBanco()).isEqualTo(5432);
        assertThat(componenteTeste.getTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(componenteTeste.getFeatureHabilitada()).isFalse();
        assertThat(componenteTeste.getHostsPermitidos()).containsExactly("localhost");
    }

    @Test
    void deveBuscarConfiguracaoDoServidor() {
        // Given
        String chave = "test.config";
        String valor = "test.value";
        
        mockServer.enqueue(new MockResponse()
            .setBody(String.format("{\"chave\":\"%s\",\"valor\":\"%s\"}", chave, valor))
            .addHeader("Content-Type", "application/json"));

        // When
        String resultado = servicoCliente.buscarValorConfiguracao(chave);

        // Then
        assertThat(resultado).isEqualTo(valor);
    }

    @Test
    void deveUsarCacheParaRequisicoesSubsequentes() {
        // Given
        String chave = "cached.config";
        String valor = "cached.value";
        
        cache.armazenar(chave, valor);

        // When - primeira busca (do cache)
        String resultado1 = servicoCliente.buscarValorConfiguracao(chave);
        
        // Then
        assertThat(resultado1).isEqualTo(valor);
        assertThat(cache.contemChave(chave)).isTrue();
    }

    @Test
    void deveRetornarValorPadraoQuandoConfiguracaoNaoEncontrada() {
        // Given
        String chave = "nonexistent.config";
        String valorPadrao = "default.value";
        
        mockServer.enqueue(new MockResponse().setResponseCode(404));

        // When
        String resultado = servicoCliente.buscarValorConfiguracao(chave, valorPadrao);

        // Then
        assertThat(resultado).isEqualTo(valorPadrao);
    }

    @Test
    void deveInvalidarCacheQuandoSolicitado() {
        // Given
        String chave = "cache.test";
        String valor = "initial.value";
        
        cache.armazenar(chave, valor);
        assertThat(cache.contemChave(chave)).isTrue();

        // When
        cache.invalidar(chave);

        // Then
        assertThat(cache.contemChave(chave)).isFalse();
        assertThat(cache.buscar(chave)).isEmpty();
    }

    @Test
    void deveFuncionarComNamespaceEEnvironment() {
        // Given
        String chave = "env.specific.config";
        String namespace = "test-app";
        String environment = "test";
        String valor = "env.specific.value";
        
        mockServer.enqueue(new MockResponse()
            .setBody(String.format("{\"chave\":\"%s\",\"valor\":\"%s\",\"namespace\":\"%s\",\"ambiente\":\"%s\"}", 
                chave, valor, namespace, environment))
            .addHeader("Content-Type", "application/json"));

        // When
        String resultado = servicoCliente.buscarValorConfiguracao(chave, namespace, environment);

        // Then
        assertThat(resultado).isEqualTo(valor);
    }

    @Test
    void deveManterEstatisticasDoCache() {
        // Given
        String chave1 = "stats.test.1";
        String chave2 = "stats.test.2";
        String valor = "test.value";

        // When - armazenar e acessar valores para gerar estatísticas
        cache.armazenar(chave1, valor);
        cache.armazenar(chave2, valor);
        
        cache.buscar(chave1); // hit
        cache.buscar(chave2); // hit
        cache.buscar("nonexistent"); // miss

        // Then
        var stats = cache.getEstatisticas();
        assertThat(stats.hitCount()).isEqualTo(2);
        assertThat(stats.missCount()).isEqualTo(1);
        assertThat(stats.hitRate()).isEqualTo(2.0/3.0);
    }

    @Test
    void deveProcessarMultiplasConfiguracoes() {
        // Given
        Map<String, String> configuracoes = Map.of(
            "config1", "value1",
            "config2", "value2",
            "config3", "value3"
        );

        // When - armazenar múltiplas configurações
        configuracoes.forEach(cache::armazenar);

        // Then - verificar se todas foram armazenadas corretamente
        configuracoes.forEach((key, expectedValue) -> {
            assertThat(cache.buscar(key)).isPresent();
            assertThat(cache.buscar(key).get()).isEqualTo(expectedValue);
        });

        assertThat(cache.getTamanho()).isEqualTo(3);
    }

    @Test
    void deveManterHealthCheckFuncional() {
        // Given - simular atividade no cache
        cache.armazenar("health.test", "value");
        cache.buscar("health.test"); // hit

        // When
        var health = cache.health();

        // Then
        assertThat(health.getStatus().getCode()).isIn("UP", "DOWN");
        assertThat(health.getDetails()).containsKeys("cache-size", "hit-rate", "hit-count", "miss-count");
    }
}