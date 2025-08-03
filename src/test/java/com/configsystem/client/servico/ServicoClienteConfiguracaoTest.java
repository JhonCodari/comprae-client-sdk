package com.configsystem.client.servico;

import com.configsystem.client.configuracao.PropriedadesClienteConfiguracao;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ServicoClienteConfiguracaoTest {

    private MockWebServer servidorMock;
    private ServicoClienteConfiguracao servicoCliente;
    private PropriedadesClienteConfiguracao propriedades;

    @BeforeEach
    void configurar() throws IOException {
        servidorMock = new MockWebServer();
        servidorMock.start();

        propriedades = new PropriedadesClienteConfiguracao();
        propriedades.setServerUrl(servidorMock.url("/").toString());
        propriedades.setNamespace("teste");
        propriedades.setEnvironment("dev");
        propriedades.setUsername("admin");
        propriedades.setPassword("admin123");
        propriedades.setCacheEnabled(true);

        servicoCliente = new ServicoClienteConfiguracao(propriedades);
    }

    @AfterEach
    void finalizar() throws IOException {
        servidorMock.shutdown();
    }

    @Test
    void testBuscarValorConfiguracao() {
        // Arrange
        String corpoResposta = "{\"id\":1,\"key\":\"teste.chave\",\"value\":\"teste.valor\",\"namespace\":\"teste\",\"environment\":\"dev\"}";
        servidorMock.enqueue(new MockResponse()
                .setBody(corpoResposta)
                .addHeader("Content-Type", "application/json"));

        // Act
        String resultado = servicoCliente.buscarValorConfiguracao("teste.chave");

        // Assert
        assertEquals("teste.valor", resultado);
    }

    @Test
    void testBuscarValorConfiguracaoComPadrao() {
        // Arrange
        servidorMock.enqueue(new MockResponse().setResponseCode(404));

        // Act
        String resultado = servicoCliente.buscarValorConfiguracao("chave.inexistente", "valor.padrao");

        // Assert
        assertEquals("valor.padrao", resultado);
    }

    @Test
    void testBuscarTodasConfiguracoes() {
        // Arrange
        String corpoResposta = "{\"chave1\":\"valor1\",\"chave2\":\"valor2\",\"chave3\":\"valor3\"}";
        servidorMock.enqueue(new MockResponse()
                .setBody(corpoResposta)
                .addHeader("Content-Type", "application/json"));

        // Act
        Map<String, String> resultado = servicoCliente.buscarTodasConfiguracoes();

        // Assert
        assertEquals(3, resultado.size());
        assertEquals("valor1", resultado.get("chave1"));
        assertEquals("valor2", resultado.get("chave2"));
        assertEquals("valor3", resultado.get("chave3"));
    }

    @Test
    void testIsServidorConfigDisponivel() {
        // Arrange
        servidorMock.enqueue(new MockResponse()
                .setBody("{\"status\":\"UP\"}")
                .addHeader("Content-Type", "application/json"));

        // Act
        boolean resultado = servicoCliente.isServidorConfigDisponivel();

        // Assert
        assertTrue(resultado);
    }

    @Test
    void testIsServidorConfigNaoDisponivel() {
        // Arrange
        servidorMock.enqueue(new MockResponse().setResponseCode(500));

        // Act
        boolean resultado = servicoCliente.isServidorConfigDisponivel();

        // Assert
        assertFalse(resultado);
    }

    @Test
    void testOperacoesCache() {
        // Arrange
        String corpoResposta = "{\"id\":1,\"key\":\"teste.chave\",\"value\":\"teste.valor\",\"namespace\":\"teste\",\"environment\":\"dev\"}";
        servidorMock.enqueue(new MockResponse()
                .setBody(corpoResposta)
                .addHeader("Content-Type", "application/json"));

        // Act - primeira chamada deve buscar do servidor
        String resultado1 = servicoCliente.buscarValorConfiguracao("teste.chave");
        
        // Act - segunda chamada deve usar cache local
        String resultado2 = servicoCliente.buscarValorConfiguracao("teste.chave");

        // Assert
        assertEquals("teste.valor", resultado1);
        assertEquals("teste.valor", resultado2);
        assertEquals(1, servidorMock.getRequestCount()); // Apenas uma chamada ao servidor
        
        // Verificar cache local
        Map<String, String> cache = servicoCliente.obterCacheLocal();
        assertTrue(cache.containsKey("teste:dev:teste.chave"));
        assertEquals("teste.valor", cache.get("teste:dev:teste.chave"));
    }

    @Test
    void testLimparCache() {
        // Arrange
        String corpoResposta = "{\"id\":1,\"key\":\"teste.chave\",\"value\":\"teste.valor\",\"namespace\":\"teste\",\"environment\":\"dev\"}";
        servidorMock.enqueue(new MockResponse()
                .setBody(corpoResposta)
                .addHeader("Content-Type", "application/json"));

        // Act
        servicoCliente.buscarValorConfiguracao("teste.chave");
        assertFalse(servicoCliente.obterCacheLocal().isEmpty());
        
        servicoCliente.limparCache();
        
        // Assert
        assertTrue(servicoCliente.obterCacheLocal().isEmpty());
    }

    @Test
    void testAtualizarConfiguracao() {
        // Arrange
        String corpoResposta1 = "{\"id\":1,\"key\":\"teste.chave\",\"value\":\"valor.antigo\",\"namespace\":\"teste\",\"environment\":\"dev\"}";
        String corpoResposta2 = "{\"id\":1,\"key\":\"teste.chave\",\"value\":\"valor.novo\",\"namespace\":\"teste\",\"environment\":\"dev\"}";
        
        servidorMock.enqueue(new MockResponse()
                .setBody(corpoResposta1)
                .addHeader("Content-Type", "application/json"));
        servidorMock.enqueue(new MockResponse()
                .setBody(corpoResposta2)
                .addHeader("Content-Type", "application/json"));

        // Act
        String valorAntigo = servicoCliente.buscarValorConfiguracao("teste.chave");
        servicoCliente.atualizarConfiguracao("teste.chave");
        String valorNovo = servicoCliente.buscarValorConfiguracao("teste.chave");

        // Assert
        assertEquals("valor.antigo", valorAntigo);
        assertEquals("valor.novo", valorNovo);
        assertEquals(2, servidorMock.getRequestCount());
    }
}
