package com.configsystem.client.performance;

import com.configsystem.client.cache.CacheInteligente;
import com.configsystem.client.validacao.ValidadorTipos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes de performance e benchmark para componentes críticos da SDK
 */
class PerformanceTest {

    private CacheInteligente cache;
    private ValidadorTipos validador;

    @BeforeEach
    void setUp() {
        cache = new CacheInteligente();
        cache.inicializar();
        validador = new ValidadorTipos();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void benchmarkOperacoesCacheSequenciais() {
        // Given
        int numeroOperacoes = 10_000;
        List<String> chaves = new ArrayList<>();
        
        // Preparar dados de teste
        for (int i = 0; i < numeroOperacoes; i++) {
            chaves.add("key_" + i);
        }

        // When - Benchmark de escrita
        long inicioEscrita = System.currentTimeMillis();
        for (int i = 0; i < numeroOperacoes; i++) {
            cache.armazenar(chaves.get(i), "value_" + i);
        }
        long tempoEscrita = System.currentTimeMillis() - inicioEscrita;

        // When - Benchmark de leitura
        long inicioLeitura = System.currentTimeMillis();
        for (String chave : chaves) {
            cache.buscar(chave);
        }
        long tempoLeitura = System.currentTimeMillis() - inicioLeitura;

        // Then
        System.out.printf("Performance Cache Sequencial:%n");
        System.out.printf("  Escritas: %d ops em %dms (%.2f ops/ms)%n", 
            numeroOperacoes, tempoEscrita, (double) numeroOperacoes / tempoEscrita);
        System.out.printf("  Leituras: %d ops em %dms (%.2f ops/ms)%n", 
            numeroOperacoes, tempoLeitura, (double) numeroOperacoes / tempoLeitura);

        // Verificações de performance
        assertThat(tempoEscrita).isLessThan(2000); // Menos de 2s para 10k escritas
        assertThat(tempoLeitura).isLessThan(1000); // Menos de 1s para 10k leituras
        assertThat(cache.getTamanho()).isEqualTo(numeroOperacoes);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void benchmarkOperacoesCacheConcorrentes() throws Exception {
        // Given
        int numeroThreads = 10;
        int operacoesPorThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(numeroThreads);
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // When
        long inicio = System.currentTimeMillis();
        
        for (int t = 0; t < numeroThreads; t++) {
            final int threadId = t;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int i = 0; i < operacoesPorThread; i++) {
                    String chave = String.format("concurrent_%d_%d", threadId, i);
                    String valor = String.format("value_%d_%d", threadId, i);
                    
                    cache.armazenar(chave, valor);
                    cache.buscar(chave); // Leitura imediata para testar concorrência
                }
            }, executor);
            
            futures.add(future);
        }

        // Aguardar conclusão de todas as threads
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long tempoTotal = System.currentTimeMillis() - inicio;

        executor.shutdown();

        // Then
        int totalOperacoes = numeroThreads * operacoesPorThread * 2; // 2 ops por iteração (write + read)
        System.out.printf("Performance Cache Concorrente:%n");
        System.out.printf("  %d threads x %d ops = %d operações totais em %dms%n", 
            numeroThreads, operacoesPorThread * 2, totalOperacoes, tempoTotal);
        System.out.printf("  Taxa: %.2f ops/ms%n", (double) totalOperacoes / tempoTotal);

        assertThat(tempoTotal).isLessThan(8000); // Menos de 8s para operações concorrentes
        assertThat(cache.getTamanho()).isEqualTo(numeroThreads * operacoesPorThread);
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void benchmarkConversoesTipos() {
        // Given
        int numeroConversoes = 50_000;
        
        // Test Integer conversions
        long inicioInteger = System.currentTimeMillis();
        for (int i = 0; i < numeroConversoes; i++) {
            validador.converter(String.valueOf(i), Integer.class);
        }
        long tempoInteger = System.currentTimeMillis() - inicioInteger;

        // Test Boolean conversions
        long inicioBoolean = System.currentTimeMillis();
        for (int i = 0; i < numeroConversoes; i++) {
            validador.converter(i % 2 == 0 ? "true" : "false", Boolean.class);
        }
        long tempoBoolean = System.currentTimeMillis() - inicioBoolean;

        // Test String conversions (controle)
        long inicioString = System.currentTimeMillis();
        for (int i = 0; i < numeroConversoes; i++) {
            validador.converter("test_" + i, String.class);
        }
        long tempoString = System.currentTimeMillis() - inicioString;

        // Then
        System.out.printf("Performance Conversões de Tipos:%n");
        System.out.printf("  Integer: %d conversões em %dms (%.2f conv/ms)%n", 
            numeroConversoes, tempoInteger, (double) numeroConversoes / Math.max(tempoInteger, 1));
        System.out.printf("  Boolean: %d conversões em %dms (%.2f conv/ms)%n", 
            numeroConversoes, tempoBoolean, (double) numeroConversoes / Math.max(tempoBoolean, 1));
        System.out.printf("  String:  %d conversões em %dms (%.2f conv/ms)%n", 
            numeroConversoes, tempoString, (double) numeroConversoes / Math.max(tempoString, 1));

        // Verificações de performance
        assertThat(tempoInteger).isLessThan(1000); // Menos de 1s para 50k conversões
        assertThat(tempoBoolean).isLessThan(1000);
        assertThat(tempoString).isLessThan(500);
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void benchmarkValidacoes() {
        // Given
        int numeroValidacoes = 20_000;
        List<String> emails = new ArrayList<>();
        List<String> urls = new ArrayList<>();
        
        // Preparar dados de teste
        for (int i = 0; i < numeroValidacoes; i++) {
            emails.add(String.format("user%d@domain%d.com", i, i % 100));
            urls.add(String.format("https://api%d.example.com/v1/resource/%d", i % 10, i));
        }

        // When - Benchmark validação de emails
        long inicioEmail = System.currentTimeMillis();
        for (String email : emails) {
            validador.validar(email, "email");
        }
        long tempoEmail = System.currentTimeMillis() - inicioEmail;

        // When - Benchmark validação de URLs
        long inicioUrl = System.currentTimeMillis();
        for (String url : urls) {
            validador.validar(url, "url");
        }
        long tempoUrl = System.currentTimeMillis() - inicioUrl;

        // Then
        System.out.printf("Performance Validações:%n");
        System.out.printf("  Email: %d validações em %dms (%.2f val/ms)%n", 
            numeroValidacoes, tempoEmail, (double) numeroValidacoes / Math.max(tempoEmail, 1));
        System.out.printf("  URL:   %d validações em %dms (%.2f val/ms)%n", 
            numeroValidacoes, tempoUrl, (double) numeroValidacoes / Math.max(tempoUrl, 1));

        // Verificações de performance
        assertThat(tempoEmail).isLessThan(1500); // Menos de 1.5s para 20k validações de email
        assertThat(tempoUrl).isLessThan(1500);   // Menos de 1.5s para 20k validações de URL
    }

    @Test
    void testMemoryUsageCache() {
        // Given
        Runtime runtime = Runtime.getRuntime();
        int numeroEntradas = 10_000;
        
        // Força garbage collection antes do teste
        System.gc();
        long memoriaInicial = runtime.totalMemory() - runtime.freeMemory();

        // When - Adicionar muitas entradas no cache
        for (int i = 0; i < numeroEntradas; i++) {
            String chave = "memory_test_key_" + i + "_" + UUID.randomUUID();
            String valor = "memory_test_value_" + i + "_" + "X".repeat(100); // Valor de ~100 chars
            cache.armazenar(chave, valor);
        }

        System.gc(); // Força garbage collection
        long memoriaFinal = runtime.totalMemory() - runtime.freeMemory();
        long memoriaUsada = memoriaFinal - memoriaInicial;

        // Then
        System.out.printf("Uso de Memória Cache:%n");
        System.out.printf("  Entradas: %d%n", numeroEntradas);
        System.out.printf("  Memória inicial: %d bytes%n", memoriaInicial);
        System.out.printf("  Memória final: %d bytes%n", memoriaFinal);
        System.out.printf("  Memória usada: %d bytes (%.2f MB)%n", memoriaUsada, memoriaUsada / 1024.0 / 1024.0);
        System.out.printf("  Bytes por entrada: %.2f%n", (double) memoriaUsada / numeroEntradas);

        assertThat(cache.getTamanho()).isEqualTo(numeroEntradas);
        
        // Verificação básica - não deve usar mais que 50MB para 10k entradas pequenas
        assertThat(memoriaUsada).isLessThan(50 * 1024 * 1024); // 50MB
    }

    @Test
    void testCacheEvictionBehavior() {
        // Given - Cache configurado para máximo de 10.000 entradas
        int maxEntradas = 10_000;
        int entradasAdicionais = 2_000;

        // When - Adicionar mais entradas que o limite
        for (int i = 0; i < maxEntradas + entradasAdicionais; i++) {
            cache.armazenar("eviction_test_" + i, "value_" + i);
        }

        // Then - Verificar eviction
        long tamanhoAtual = cache.getTamanho();
        var stats = cache.getEstatisticas();
        
        System.out.printf("Comportamento de Eviction:%n");
        System.out.printf("  Entradas adicionadas: %d%n", maxEntradas + entradasAdicionais);
        System.out.printf("  Tamanho atual do cache: %d%n", tamanhoAtual);
        System.out.printf("  Evictions realizadas: %d%n", stats.evictionCount());

        // O cache deve ter feito eviction para manter o tamanho dentro do limite
        assertThat(tamanhoAtual).isLessThanOrEqualTo(maxEntradas);
        assertThat(stats.evictionCount()).isGreaterThan(0);
    }
}