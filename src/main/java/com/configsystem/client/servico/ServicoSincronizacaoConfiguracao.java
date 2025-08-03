package com.configsystem.client.servico;

import com.configsystem.client.configuracao.PropriedadesClienteConfiguracao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Serviço responsável pela sincronização periódica de configurações
 */
@Service
public class ServicoSincronizacaoConfiguracao {

    private static final Logger logger = LoggerFactory.getLogger(ServicoSincronizacaoConfiguracao.class);

    @Autowired
    private PropriedadesClienteConfiguracao propriedades;

    @Autowired
    private ServicoClienteConfiguracao servicoCliente;

    private boolean ultimoStatusServidor = false;
    private long ultimaSincronizacao = 0;

    /**
     * Sincronização periódica das configurações
     */
    @Scheduled(fixedDelayString = "${config.client.sync.interval:30000}")
    public void sincronizarConfiguracoes() {
        if (!propriedades.isSyncEnabled()) {
            return;
        }

        logger.debug("Iniciando sincronização periódica de configurações");
        
        try {
            boolean servidorDisponivel = servicoCliente.isServidorConfigDisponivel();
            
            if (servidorDisponivel) {
                // Servidor está disponível
                if (!ultimoStatusServidor) {
                    logger.info("Servidor de configurações voltou a ficar disponível - sincronizando todas as configurações");
                    servicoCliente.atualizarTodasConfiguracoes();
                } else {
                    // Sincronização normal
                    sincronizarSeNecessario();
                }
                ultimoStatusServidor = true;
                ultimaSincronizacao = System.currentTimeMillis();
            } else {
                // Servidor não está disponível
                if (ultimoStatusServidor) {
                    logger.warn("Servidor de configurações não está disponível - usando cache local");
                }
                ultimoStatusServidor = false;
            }
            
        } catch (Exception e) {
            logger.error("Erro durante sincronização periódica: {}", e.getMessage());
            ultimoStatusServidor = false;
        }
    }

    /**
     * Sincroniza apenas se necessário
     */
    private void sincronizarSeNecessario() {
        try {
            // Buscar todas as configurações do servidor
            Map<String, String> configsServidor = servicoCliente.buscarTodasConfiguracoes();
            Map<String, String> cacheLocal = servicoCliente.obterCacheLocal();
            
            // Verificar se há diferenças
            int diferencas = 0;
            for (Map.Entry<String, String> entry : configsServidor.entrySet()) {
                String valorLocal = cacheLocal.get(entry.getKey());
                if (!entry.getValue().equals(valorLocal)) {
                    diferencas++;
                }
            }
            
            if (diferencas > 0) {
                logger.info("Encontradas {} diferenças entre cache local e servidor - atualizando cache", diferencas);
                servicoCliente.atualizarTodasConfiguracoes();
            } else {
                logger.debug("Cache local está sincronizado com o servidor");
            }
            
        } catch (Exception e) {
            logger.error("Erro ao verificar sincronização: {}", e.getMessage());
        }
    }

    /**
     * Log do status de sincronização
     */
    @Scheduled(fixedDelay = 60000) // 1 minuto
    public void logStatusSincronizacao() {
        if (propriedades.isSyncEnabled()) {
            long tempoDesdeUltimaSinc = System.currentTimeMillis() - ultimaSincronizacao;
            logger.debug("Status de sincronização - Servidor disponível: {}, Tempo desde última sync: {}ms", 
                        ultimoStatusServidor, tempoDesdeUltimaSinc);
        }
    }

    /**
     * Verifica se o servidor está disponível
     */
    public boolean isServidorDisponivel() {
        return ultimoStatusServidor;
    }

    /**
     * Obtém o timestamp da última sincronização
     */
    public long getUltimaSincronizacao() {
        return ultimaSincronizacao;
    }

    /**
     * Força uma sincronização imediata
     */
    public void forcarSincronizacaoAgora() {
        logger.info("Forçando sincronização imediata");
        sincronizarConfiguracoes();
    }
}
