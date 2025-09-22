package com.configsystem.client.validacao;

import com.configsystem.client.validacao.ValidadorTipos.ValidacaoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitários para o ValidadorTipos
 */
class ValidadorTiposTest {

    private ValidadorTipos validador;

    @BeforeEach
    void setUp() {
        validador = new ValidadorTipos();
    }

    // === TESTES DE CONVERSÃO DE TIPOS ===

    @Test
    void deveConverterString() {
        String resultado = validador.converter("test", String.class);
        assertThat(resultado).isEqualTo("test");
    }

    @Test
    void deveConverterInteger() {
        Integer resultado = validador.converter("123", Integer.class);
        assertThat(resultado).isEqualTo(123);
    }

    @Test
    void deveConverterLong() {
        Long resultado = validador.converter("123456789", Long.class);
        assertThat(resultado).isEqualTo(123456789L);
    }

    @Test
    void deveConverterDouble() {
        Double resultado = validador.converter("123.45", Double.class);
        assertThat(resultado).isEqualTo(123.45);
    }

    @Test
    void deveConverterBoolean() {
        Boolean resultadoTrue = validador.converter("true", Boolean.class);
        Boolean resultadoFalse = validador.converter("false", Boolean.class);
        
        assertThat(resultadoTrue).isTrue();
        assertThat(resultadoFalse).isFalse();
    }

    @Test
    void deveConverterBigDecimal() {
        BigDecimal resultado = validador.converter("999.99", BigDecimal.class);
        assertThat(resultado).isEqualTo(new BigDecimal("999.99"));
    }

    @Test
    void deveConverterLocalDate() {
        LocalDate resultado = validador.converter("2023-12-25", LocalDate.class);
        assertThat(resultado).isEqualTo(LocalDate.of(2023, 12, 25));
    }

    @Test
    void deveConverterDuration() {
        Duration resultado = validador.converter("PT30S", Duration.class);
        assertThat(resultado).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void deveConverterLista() {
        @SuppressWarnings("unchecked")
        List<String> resultado = validador.converter("item1,item2,item3", List.class);
        assertThat(resultado).containsExactly("item1", "item2", "item3");
    }

    @Test
    void deveRetornarNullParaValorVazio() {
        String resultado = validador.converter("", String.class);
        assertThat(resultado).isNull();
        
        Integer resultadoNull = validador.converter(null, Integer.class);
        assertThat(resultadoNull).isNull();
    }

    @Test
    void deveLancarExcecaoParaConversaoInvalida() {
        assertThatThrownBy(() -> validador.converter("abc", Integer.class))
            .isInstanceOf(ValidacaoException.class)
            .hasMessageContaining("Não foi possível converter 'abc' para Integer");
    }

    // === TESTES DE VALIDAÇÃO ===

    @ParameterizedTest
    @ValueSource(strings = {"test@example.com", "user@domain.com.br", "admin@localhost.local"})
    void deveValidarEmailsValidos(String email) {
        boolean resultado = validador.validar(email, "email");
        assertThat(resultado).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"email-invalido", "@domain.com", "test@", "test@.com"})
    void deveInvalidarEmailsInvalidos(String email) {
        boolean resultado = validador.validar(email, "email");
        assertThat(resultado).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://www.example.com", "http://localhost:8080", "https://api.comprae.com.br/v1"})
    void deveValidarUrlsValidas(String url) {
        boolean resultado = validador.validar(url, "url");
        assertThat(resultado).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-url", "ftp://example.com", "www.example.com"})
    void deveInvalidarUrlsInvalidas(String url) {
        boolean resultado = validador.validar(url, "url");
        assertThat(resultado).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"192.168.1.1", "127.0.0.1", "10.0.0.1", "255.255.255.255"})
    void deveValidarIpsValidos(String ip) {
        boolean resultado = validador.validar(ip, "ip");
        assertThat(resultado).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"256.1.1.1", "192.168.1", "192.168.1.256", "abc.def.ghi.jkl"})
    void deveInvalidarIpsInvalidos(String ip) {
        boolean resultado = validador.validar(ip, "ip");
        assertThat(resultado).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"80", "8080", "443", "65535"})
    void deveValidarPortasValidas(String porta) {
        boolean resultado = validador.validar(porta, "port");
        assertThat(resultado).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "65536", "99999", "abc"})
    void deveInvalidarPortasInvalidas(String porta) {
        boolean resultado = validador.validar(porta, "port");
        assertThat(resultado).isFalse();
    }

    @Test
    void deveValidarComRegexCustomizado() {
        String regex = "^[A-Z]{3}[0-9]{3}$"; // 3 letras maiúsculas + 3 números
        
        assertThat(validador.validarComRegex("ABC123", regex)).isTrue();
        assertThat(validador.validarComRegex("XYZ999", regex)).isTrue();
        assertThat(validador.validarComRegex("abc123", regex)).isFalse();
        assertThat(validador.validarComRegex("AB123", regex)).isFalse();
        assertThat(validador.validarComRegex("ABC12", regex)).isFalse();
    }

    @Test
    void deveValidarRange() {
        // Range válido
        assertThat(validador.validarRange("50", Integer.class, 1, 100)).isTrue();
        assertThat(validador.validarRange("1", Integer.class, 1, 100)).isTrue();
        assertThat(validador.validarRange("100", Integer.class, 1, 100)).isTrue();
        
        // Range inválido
        assertThat(validador.validarRange("0", Integer.class, 1, 100)).isFalse();
        assertThat(validador.validarRange("101", Integer.class, 1, 100)).isFalse();
        assertThat(validador.validarRange("-1", Integer.class, 1, 100)).isFalse();
    }

    @Test
    void deveRegistrarConversorCustomizado() {
        // Given
        class CustomType {
            private final String value;
            public CustomType(String value) { this.value = value; }
            public String getValue() { return value; }
        }
        
        validador.registrarConversor(CustomType.class, CustomType::new);

        // When
        CustomType resultado = validador.converter("test", CustomType.class);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getValue()).isEqualTo("test");
    }

    @Test
    void deveRegistrarPadraoCustomizado() {
        // Given
        String nomePadrao = "codigo-produto";
        String regex = "^PROD-[0-9]{4}$";
        
        validador.registrarPadrao(nomePadrao, regex);

        // When & Then
        assertThat(validador.validar("PROD-1234", nomePadrao)).isTrue();
        assertThat(validador.validar("PROD-12", nomePadrao)).isFalse();
        assertThat(validador.validar("USER-1234", nomePadrao)).isFalse();
    }

    @Test
    void deveRetornarTrueParaPadraoInexistente() {
        // Given
        String padraoInexistente = "padrao-que-nao-existe";
        
        // When
        boolean resultado = validador.validar("qualquer-valor", padraoInexistente);
        
        // Then
        assertThat(resultado).isTrue(); // Deve considerar válido se não há padrão
    }

    @Test
    void deveRetornarFalseParaRegexInvalido() {
        // Given
        String regexInvalido = "[invalid-regex";
        
        // When
        boolean resultado = validador.validarComRegex("test", regexInvalido);
        
        // Then
        assertThat(resultado).isFalse();
    }

    @Test
    void deveRemoverEspacosAntesDaConversao() {
        // Given
        String valorComEspacos = "  123  ";
        
        // When
        Integer resultado = validador.converter(valorComEspacos, Integer.class);
        
        // Then
        assertThat(resultado).isEqualTo(123);
    }
}