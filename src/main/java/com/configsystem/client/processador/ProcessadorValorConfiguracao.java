package com.configsystem.client.processador;

import com.configsystem.client.anotacao.ValorConfiguracao;
import com.configsystem.client.servico.ServicoListenerMudancaConfiguracao;
import com.configsystem.client.servico.ServicoClienteConfiguracao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processador responsável por injetar valores de configuração em campos anotados com @ValorConfiguracao
 */
@Component
public class ProcessadorValorConfiguracao implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ProcessadorValorConfiguracao.class);

    @Autowired
    private ServicoClienteConfiguracao servicoCliente;

    // Mapeia objetos para seus campos anotados
    private final Map<Object, Map<Field, ValorConfiguracao>> beansAnotados = new ConcurrentHashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        Map<Field, ValorConfiguracao> camposAnotados = new HashMap<>();

        ReflectionUtils.doWithFields(clazz, field -> {
            ValorConfiguracao anotacao = field.getAnnotation(ValorConfiguracao.class);
            if (anotacao != null) {
                field.setAccessible(true);
                camposAnotados.put(field, anotacao);
                
                // Injetar valor inicial
                injetarValorConfiguracao(bean, field, anotacao);
            }
        });

        if (!camposAnotados.isEmpty()) {
            beansAnotados.put(bean, camposAnotados);
            logger.info("Registrado bean {} com {} campos anotados com @ValorConfiguracao", 
                       beanName, camposAnotados.size());
        }

        return bean;
    }

    /**
     * Escuta eventos de mudança de configuração
     */
    @EventListener
    public void processarMudancaConfiguracao(ServicoListenerMudancaConfiguracao.EventoMudancaConfiguracao evento) {
        logger.info("Processando mudança de configuração: {}.{}.{}", 
                   evento.getNamespace(), evento.getEnvironment(), evento.getChave());

        for (Map.Entry<Object, Map<Field, ValorConfiguracao>> entry : beansAnotados.entrySet()) {
            Object bean = entry.getKey();
            Map<Field, ValorConfiguracao> campos = entry.getValue();

            for (Map.Entry<Field, ValorConfiguracao> campoEntry : campos.entrySet()) {
                Field campo = campoEntry.getKey();
                ValorConfiguracao anotacao = campoEntry.getValue();

                if (deveAtualizarCampo(anotacao, evento) && anotacao.refreshable()) {
                    logger.info("Atualizando campo {} no bean {}", campo.getName(), bean.getClass().getSimpleName());
                    injetarValorConfiguracao(bean, campo, anotacao);
                }
            }
        }
    }

    /**
     * Injeta valor de configuração em um campo
     */
    private void injetarValorConfiguracao(Object bean, Field campo, ValorConfiguracao anotacao) {
        try {
            String namespace = anotacao.namespace().isEmpty() ? null : anotacao.namespace();
            String environment = anotacao.environment().isEmpty() ? null : anotacao.environment();
            
            String valor;
            if (namespace != null && environment != null) {
                valor = servicoCliente.buscarValorConfiguracao(anotacao.value(), namespace, environment);
            } else {
                valor = servicoCliente.buscarValorConfiguracao(anotacao.value());
            }

            if (valor == null) {
                valor = anotacao.defaultValue();
            }

            if (valor != null && !valor.isEmpty()) {
                Object valorConvertido = converterValor(valor, campo.getType());
                campo.set(bean, valorConvertido);
                
                logger.debug("Valor injetado no campo {}: {}", campo.getName(), valor);
            } else if (anotacao.required()) {
                throw new IllegalStateException("Configuração obrigatória não encontrada: " + anotacao.value());
            }

        } catch (Exception e) {
            logger.error("Erro ao injetar valor no campo {}: {}", campo.getName(), e.getMessage());
            if (anotacao.required()) {
                throw new RuntimeException("Falha ao injetar configuração obrigatória", e);
            }
        }
    }

    /**
     * Verifica se o campo deve ser atualizado com base no evento
     */
    private boolean deveAtualizarCampo(ValorConfiguracao anotacao, ServicoListenerMudancaConfiguracao.EventoMudancaConfiguracao evento) {
        String anotacaoNamespace = anotacao.namespace().isEmpty() ? null : anotacao.namespace();
        String anotacaoEnvironment = anotacao.environment().isEmpty() ? null : anotacao.environment();
        
        boolean namespaceCoincide = anotacaoNamespace == null || anotacaoNamespace.equals(evento.getNamespace());
        boolean environmentCoincide = anotacaoEnvironment == null || anotacaoEnvironment.equals(evento.getEnvironment());
        boolean chaveCoincide = anotacao.value().equals(evento.getChave());
        
        return namespaceCoincide && environmentCoincide && chaveCoincide;
    }

    /**
     * Converte string para o tipo do campo
     */
    private Object converterValor(String valor, Class<?> tipoDestino) {
        if (tipoDestino == String.class) {
            return valor;
        } else if (tipoDestino == Integer.class || tipoDestino == int.class) {
            return Integer.valueOf(valor);
        } else if (tipoDestino == Long.class || tipoDestino == long.class) {
            return Long.valueOf(valor);
        } else if (tipoDestino == Double.class || tipoDestino == double.class) {
            return Double.valueOf(valor);
        } else if (tipoDestino == Float.class || tipoDestino == float.class) {
            return Float.valueOf(valor);
        } else if (tipoDestino == Boolean.class || tipoDestino == boolean.class) {
            return Boolean.valueOf(valor);
        } else {
            // Para tipos complexos, tentar deserializar JSON
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(valor, tipoDestino);
            } catch (Exception e) {
                logger.warn("Não foi possível converter valor '{}' para tipo {}: {}", 
                           valor, tipoDestino.getName(), e.getMessage());
                return valor;
            }
        }
    }

    /**
     * Obtém a quantidade de beans anotados
     */
    public int getQuantidadeBeansAnotados() {
        return beansAnotados.size();
    }

    /**
     * Obtém todos os beans anotados
     */
    public Map<Object, Map<Field, ValorConfiguracao>> getBeansAnotados() {
        return new HashMap<>(beansAnotados);
    }
}
