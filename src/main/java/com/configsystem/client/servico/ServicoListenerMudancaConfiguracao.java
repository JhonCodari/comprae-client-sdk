package com.configsystem.client.servico;

import com.configsystem.client.configuracao.PropriedadesClienteConfiguracao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Serviço para escutar mudanças de configuração via Kafka
 */
@Service
@ConditionalOnProperty(name = "config.client.kafka.enabled", havingValue = "true")
public class ServicoListenerMudancaConfiguracao {

    private static final Logger logger = LoggerFactory.getLogger(ServicoListenerMudancaConfiguracao.class);

    @Autowired
    private PropriedadesClienteConfiguracao propriedades;

    @Autowired
    private ServicoClienteConfiguracao servicoCliente;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationEventPublisher publicadorEventos;

    /**
     * Escuta eventos de mudança de configuração do Kafka
     */
    @KafkaListener(topics = "${config.client.kafka.topic:config-changes}")
    public void processarMudancaConfiguracao(@Payload String mensagem, 
                                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topico,
                                        @Header(KafkaHeaders.RECEIVED_PARTITION) int particao,
                                        @Header(KafkaHeaders.OFFSET) long offset) {
        
        logger.info("Recebido evento de mudança de configuração: topic={}, partition={}, offset={}", 
                   topico, particao, offset);
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> evento = (Map<String, Object>) objectMapper.readValue(mensagem, Map.class);
            
            String namespace = (String) evento.get("namespace");
            String environment = (String) evento.get("environment");
            String chave = (String) evento.get("key");
            String tipoMudanca = (String) evento.get("changeType");
            
            // Verificar se o evento é relevante para este cliente
            if (isEventoRelevante(namespace, environment)) {
                logger.info("Processando mudança de configuração: {}.{}.{} - {}", 
                           namespace, environment, chave, tipoMudanca);
                
                // Atualizar cache local
                servicoCliente.atualizarConfiguracao(chave, namespace, environment);
                
                // Publicar evento local para beans interessados
                EventoMudancaConfiguracao eventoLocal = new EventoMudancaConfiguracao(
                    namespace, environment, chave, tipoMudanca, mensagem
                );
                publicadorEventos.publishEvent(eventoLocal);
                
                logger.info("Configuração atualizada com sucesso: {}.{}.{}", namespace, environment, chave);
            } else {
                logger.debug("Evento ignorado - não é relevante para este cliente: {}.{}", namespace, environment);
            }
            
        } catch (Exception e) {
            logger.error("Erro ao processar evento de mudança de configuração: {}", e.getMessage(), e);
        }
    }

    /**
     * Verifica se o evento é relevante para este cliente
     */
    private boolean isEventoRelevante(String namespace, String environment) {
        return propriedades.getNamespace().equals(namespace) && 
               propriedades.getEnvironment().equals(environment);
    }

    /**
     * Classe que representa um evento de mudança de configuração
     */
    public static class EventoMudancaConfiguracao {
        private final String namespace;
        private final String environment;
        private final String chave;
        private final String tipoMudanca;
        private final String mensagemOriginal;

        public EventoMudancaConfiguracao(String namespace, String environment, String chave, 
                                      String tipoMudanca, String mensagemOriginal) {
            this.namespace = namespace;
            this.environment = environment;
            this.chave = chave;
            this.tipoMudanca = tipoMudanca;
            this.mensagemOriginal = mensagemOriginal;
        }

        public String getNamespace() {
            return namespace;
        }

        public String getEnvironment() {
            return environment;
        }

        public String getChave() {
            return chave;
        }

        public String getTipoMudanca() {
            return tipoMudanca;
        }

        public String getMensagemOriginal() {
            return mensagemOriginal;
        }

        @Override
        public String toString() {
            return "EventoMudancaConfiguracao{" +
                    "namespace='" + namespace + '\'' +
                    ", environment='" + environment + '\'' +
                    ", chave='" + chave + '\'' +
                    ", tipoMudanca='" + tipoMudanca + '\'' +
                    '}';
        }
    }
}
