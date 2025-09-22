package com.configsystem.client.validacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Validador e conversor automático de tipos para configurações
 */
@Component
public class ValidadorTipos {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidadorTipos.class);
    
    private final Map<Class<?>, Function<String, Object>> conversores = new ConcurrentHashMap<>();
    private final Map<String, Pattern> padroes = new ConcurrentHashMap<>();
    
    public ValidadorTipos() {
        inicializarConversores();
        inicializarPadroes();
    }
    
    private void inicializarConversores() {
        // Tipos primitivos e wrappers
        conversores.put(String.class, valor -> valor);
        conversores.put(Integer.class, Integer::valueOf);
        conversores.put(int.class, Integer::valueOf);
        conversores.put(Long.class, Long::valueOf);
        conversores.put(long.class, Long::valueOf);
        conversores.put(Double.class, Double::valueOf);
        conversores.put(double.class, Double::valueOf);
        conversores.put(Float.class, Float::valueOf);
        conversores.put(float.class, Float::valueOf);
        conversores.put(Boolean.class, Boolean::valueOf);
        conversores.put(boolean.class, Boolean::valueOf);
        conversores.put(BigDecimal.class, BigDecimal::new);
        
        // Tipos de data/hora
        conversores.put(LocalDate.class, valor -> LocalDate.parse(valor));
        conversores.put(LocalDateTime.class, valor -> LocalDateTime.parse(valor));
        conversores.put(Duration.class, Duration::parse);
        
        // Listas (separadas por vírgula)
        conversores.put(List.class, valor -> Arrays.asList(valor.split(",")));
        
        logger.info("Conversores de tipo inicializados: {}", conversores.keySet());
    }
    
    private void inicializarPadroes() {
        padroes.put("email", Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"));
        padroes.put("url", Pattern.compile("^https?://[^\\s/$.?#].[^\\s]*$"));
        padroes.put("ip", Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"));
        padroes.put("port", Pattern.compile("^([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$"));
        
        logger.info("Padrões de validação inicializados: {}", padroes.keySet());
    }
    
    /**
     * Converte um valor string para o tipo especificado
     */
    @SuppressWarnings("unchecked")
    public <T> T converter(String valor, Class<T> tipoDestino) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        
        try {
            Function<String, Object> conversor = conversores.get(tipoDestino);
            if (conversor != null) {
                Object resultado = conversor.apply(valor.trim());
                logger.debug("Conversão bem-sucedida: '{}' -> {} ({})", valor, resultado, tipoDestino.getSimpleName());
                return (T) resultado;
            }
            
            // Fallback para tipos não registrados
            logger.warn("Conversor não encontrado para tipo: {}. Retornando string.", tipoDestino.getSimpleName());
            return (T) valor;
            
        } catch (Exception e) {
            logger.error("Erro na conversão de '{}' para {}: {}", valor, tipoDestino.getSimpleName(), e.getMessage());
            throw new ValidacaoException(
                String.format("Não foi possível converter '%s' para %s: %s", valor, tipoDestino.getSimpleName(), e.getMessage()),
                e
            );
        }
    }
    
    /**
     * Valida um valor usando um padrão nomeado
     */
    public boolean validar(String valor, String nomePadrao) {
        Pattern padrao = padroes.get(nomePadrao);
        if (padrao == null) {
            logger.warn("Padrão de validação não encontrado: {}", nomePadrao);
            return true; // Se não tem padrão, considera válido
        }
        
        boolean valido = padrao.matcher(valor).matches();
        logger.debug("Validação de '{}' com padrão '{}': {}", valor, nomePadrao, valido ? "VÁLIDO" : "INVÁLIDO");
        
        return valido;
    }
    
    /**
     * Valida um valor usando um padrão regex customizado
     */
    public boolean validarComRegex(String valor, String regex) {
        try {
            Pattern padrao = Pattern.compile(regex);
            boolean valido = padrao.matcher(valor).matches();
            logger.debug("Validação de '{}' com regex '{}': {}", valor, regex, valido ? "VÁLIDO" : "INVÁLIDO");
            return valido;
        } catch (Exception e) {
            logger.error("Erro na validação com regex '{}': {}", regex, e.getMessage());
            return false;
        }
    }
    
    /**
     * Valida range numérico
     */
    public boolean validarRange(String valor, Class<?> tipo, Number min, Number max) {
        try {
            Number numero = (Number) converter(valor, tipo);
            double valorDouble = numero.doubleValue();
            double minDouble = min != null ? min.doubleValue() : Double.MIN_VALUE;
            double maxDouble = max != null ? max.doubleValue() : Double.MAX_VALUE;
            
            boolean valido = valorDouble >= minDouble && valorDouble <= maxDouble;
            logger.debug("Validação de range para '{}': {} <= {} <= {} = {}", 
                valor, minDouble, valorDouble, maxDouble, valido);
            
            return valido;
        } catch (Exception e) {
            logger.error("Erro na validação de range para '{}': {}", valor, e.getMessage());
            return false;
        }
    }
    
    /**
     * Registra um novo conversor customizado
     */
    public <T> void registrarConversor(Class<T> tipo, Function<String, T> conversor) {
        conversores.put(tipo, valor -> conversor.apply(valor));
        logger.info("Conversor customizado registrado para tipo: {}", tipo.getSimpleName());
    }
    
    /**
     * Registra um novo padrão de validação
     */
    public void registrarPadrao(String nome, String regex) {
        padroes.put(nome, Pattern.compile(regex));
        logger.info("Padrão de validação '{}' registrado: {}", nome, regex);
    }
    
    /**
     * Exception específica para erros de validação
     */
    public static class ValidacaoException extends RuntimeException {
        public ValidacaoException(String message) {
            super(message);
        }
        
        public ValidacaoException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}