package com.configsystem.client.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para o CacheInteligente
 */
@ExtendWith(MockitoExtension.class)
class CacheInteligenteTest {

    private CacheInteligente cache;
    
    @Mock
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        cache = new CacheInteligente();
        cache.inicializar();
    }

    @Test
    void deveArmazenarERecuperarValor() {
        // Given
        String chave = "test.key";
        String valor = "test.value";

        // When
        cache.armazenar(chave, valor);
        Optional<String> resultado = cache.buscar(chave);

        // Then
        assertThat(resultado).isPresent();
        assertThat(resultado.get()).isEqualTo(valor);
        assertThat(cache.contemChave(chave)).isTrue();
    }

    @Test
    void deveRetornarVazioParaChaveInexistente() {
        // Given
        String chave = "chave.inexistente";

        // When
        Optional<String> resultado = cache.buscar(chave);

        // Then
        assertThat(resultado).isEmpty();
        assertThat(cache.contemChave(chave)).isFalse();
    }

    @Test
    void deveInvalidarChaveEspecifica() {
        // Given
        String chave = "test.key";
        String valor = "test.value";
        cache.armazenar(chave, valor);

        // When
        cache.invalidar(chave);
        Optional<String> resultado = cache.buscar(chave);

        // Then
        assertThat(resultado).isEmpty();
        assertThat(cache.contemChave(chave)).isFalse();
    }

    @Test
    void deveLimparTodoOCache() {
        // Given
        cache.armazenar("key1", "value1");
        cache.armazenar("key2", "value2");
        cache.armazenar("key3", "value3");

        // When
        cache.limparTudo();

        // Then
        assertThat(cache.buscar("key1")).isEmpty();
        assertThat(cache.buscar("key2")).isEmpty();
        assertThat(cache.buscar("key3")).isEmpty();
        assertThat(cache.getTamanho()).isEqualTo(0);
    }

    @Test
    void deveRetornarEstatisticasCorretas() {
        // Given
        cache.armazenar("key1", "value1");
        cache.buscar("key1"); // hit
        cache.buscar("key2"); // miss

        // When
        var stats = cache.getEstatisticas();

        // Then
        assertThat(stats.hitCount()).isEqualTo(1);
        assertThat(stats.missCount()).isEqualTo(1);
        assertThat(stats.hitRate()).isEqualTo(0.5);
    }

    @Test
    void deveRetornarHealthUpComBomHitRate() {
        // Given - simular cache com bom hit rate
        for (int i = 0; i < 10; i++) {
            cache.armazenar("key" + i, "value" + i);
        }
        for (int i = 0; i < 8; i++) {
            cache.buscar("key" + i); // 8 hits
        }
        cache.buscar("inexistente1"); // 1 miss
        cache.buscar("inexistente2"); // 1 miss
        // Hit rate = 8/10 = 0.8 > 0.7

        // When
        Health health = cache.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("hit-rate");
        assertThat(health.getDetails()).containsKey("cache-size");
    }

    @Test
    void deveRetornarHealthDownComBaixoHitRate() {
        // Given - simular cache com baixo hit rate
        cache.armazenar("key1", "value1");
        cache.buscar("key1"); // 1 hit
        for (int i = 0; i < 9; i++) {
            cache.buscar("inexistente" + i); // 9 misses
        }
        // Hit rate = 1/10 = 0.1 < 0.7

        // When
        Health health = cache.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void deveAtualizarTamanhoCorretamente() {
        // Given
        assertThat(cache.getTamanho()).isEqualTo(0);

        // When
        cache.armazenar("key1", "value1");
        cache.armazenar("key2", "value2");

        // Then
        assertThat(cache.getTamanho()).isEqualTo(2);

        // When
        cache.invalidar("key1");

        // Then
        assertThat(cache.getTamanho()).isEqualTo(1);
    }

    @Test
    void deveSubstituirValorExistente() {
        // Given
        String chave = "test.key";
        String valorOriginal = "valor.original";
        String valorNovo = "valor.novo";

        // When
        cache.armazenar(chave, valorOriginal);
        cache.armazenar(chave, valorNovo);

        // Then
        Optional<String> resultado = cache.buscar(chave);
        assertThat(resultado).isPresent();
        assertThat(resultado.get()).isEqualTo(valorNovo);
        assertThat(cache.getTamanho()).isEqualTo(1); // Não deve duplicar
    }
}