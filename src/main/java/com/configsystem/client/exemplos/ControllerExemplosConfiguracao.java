package com.configsystem.client.exemplos;

import com.configsystem.client.anotacao.ValorConfiguracao;
import com.configsystem.client.servico.ServicoClienteConfiguracao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Exemplos de uso do Config Client SDK
 */
@RestController
public class ControllerExemplosConfiguracao {

    @Autowired
    private ServicoClienteConfiguracao servicoCliente;

    // Exemplo 1: Injeção simples de configuração
    @ValorConfiguracao(value = "database.url", defaultValue = "jdbc:postgresql://localhost:5432/default")
    private String urlBancoDados;

    // Exemplo 2: Configuração numérica
    @ValorConfiguracao(value = "database.pool.size", defaultValue = "10")
    private Integer tamanhoPool;

    // Exemplo 3: Configuração booleana
    @ValorConfiguracao(value = "feature.nova-ui", defaultValue = "false")
    private Boolean novaUiHabilitada;

    // Exemplo 4: Configuração com namespace específico
    @ValorConfiguracao(value = "timeout", namespace = "api", defaultValue = "5000")
    private Long timeoutApi;

    // Exemplo 5: Configuração sem auto-refresh
    @ValorConfiguracao(value = "app.versao", refreshable = false, defaultValue = "1.0.0")
    private String versaoApp;

    @GetMapping("/exemplos/configuracoes-injetadas")
    public Map<String, Object> obterConfiguracoesInjetadas() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("urlBancoDados", urlBancoDados);
        configs.put("tamanhoPool", tamanhoPool);
        configs.put("novaUiHabilitada", novaUiHabilitada);
        configs.put("timeoutApi", timeoutApi);
        configs.put("versaoApp", versaoApp);
        return configs;
    }

    @GetMapping("/exemplos/acesso-programatico")
    public Map<String, Object> obterConfigsProgramaticas() {
        Map<String, Object> configs = new HashMap<>();
        
        // Exemplo 1: Buscar configuração diretamente
        String hostRedis = servicoCliente.buscarValorConfiguracao("redis.host", "localhost");
        configs.put("hostRedis", hostRedis);
        
        // Exemplo 2: Buscar com namespace e environment específicos
        String urlBdProd = servicoCliente.buscarValorConfiguracao("database.url", "app1", "prod");
        configs.put("urlBdProd", urlBdProd);
        
        // Exemplo 3: Buscar todas as configurações
        Map<String, String> todasConfigs = servicoCliente.buscarTodasConfiguracoes();
        configs.put("totalConfiguracoes", todasConfigs.size());
        
        return configs;
    }

    @GetMapping("/exemplos/operacoes-cache")
    public Map<String, Object> obterOperacoesCache() {
        Map<String, Object> resultado = new HashMap<>();
        
        // Verificar cache local
        Map<String, String> cacheLocal = servicoCliente.obterCacheLocal();
        resultado.put("tamanhoCache", cacheLocal.size());
        resultado.put("chavesCache", cacheLocal.keySet());
        
        // Forçar refresh de uma configuração específica
        servicoCliente.atualizarConfiguracao("database.url");
        resultado.put("configuracaoAtualizada", "database.url");
        
        return resultado;
    }
}

/**
 * Exemplo de classe de configuração complexa
 */
class ConfiguracoesApp {
    private String tema;
    private boolean modoEscuro;
    private int maxUsuarios;
    private String[] dominiosPermitidos;

    // Getters e setters
    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema; }
    
    public boolean isModoEscuro() { return modoEscuro; }
    public void setModoEscuro(boolean modoEscuro) { this.modoEscuro = modoEscuro; }
    
    public int getMaxUsuarios() { return maxUsuarios; }
    public void setMaxUsuarios(int maxUsuarios) { this.maxUsuarios = maxUsuarios; }
    
    public String[] getDominiosPermitidos() { return dominiosPermitidos; }
    public void setDominiosPermitidos(String[] dominiosPermitidos) { this.dominiosPermitidos = dominiosPermitidos; }
}

/**
 * Exemplo de uso em service
 */
class ServicoExemplo {
    
    @ValorConfiguracao("email.smtp.host")
    private String hostSmtp;
    
    @ValorConfiguracao(value = "email.smtp.porta", defaultValue = "587")
    private Integer portaSmtp;
    
    @ValorConfiguracao(value = "email.habilitado", defaultValue = "true")
    private Boolean emailHabilitado;
    
    public void enviarEmail(String para, String assunto, String corpo) {
        if (!emailHabilitado) {
            System.out.println("Email está desabilitado");
            return;
        }
        
        System.out.println("Enviando email via " + hostSmtp + ":" + portaSmtp);
        // Lógica de envio de email...
    }
}

/**
 * Exemplo de listener para mudanças de configuração
 */
class ManipuladorMudancaConfiguracao {
    
    @org.springframework.context.event.EventListener
    public void processarMudancaConfiguracao(com.configsystem.client.servico.ServicoListenerMudancaConfiguracao.EventoMudancaConfiguracao evento) {
        System.out.println("Configuração alterada: " + evento.getChave() + " = " + evento.getTipoMudanca());
        
        // Reagir a mudanças específicas
        if ("feature.nova-ui".equals(evento.getChave())) {
            System.out.println("Nova UI foi " + ("CREATE".equals(evento.getTipoMudanca()) ? "habilitada" : "desabilitada"));
        }
    }
}
