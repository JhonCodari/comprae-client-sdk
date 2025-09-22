# Cliente SDK Compraê 🚀

Biblioteca cliente avançada para integração com o sistema de configurações centralizado do Compraê.

## ✨ Funcionalidades

- **🎯 Injeção Automática**: Use `@ValorConfiguracao` com conversão automática de tipos
- **⚡ Cache Inteligente**: Cache com TTL, métricas e eviction policies otimizadas  
- **🔄 Atualização em Tempo Real**: Sincronização via Kafka com fallback automático
- **✅ Validação Avançada**: Validação de email, URL, IP, ranges numéricos e regex customizado
- **📊 Observabilidade**: Métricas detalhadas, health checks e monitoramento integrado
- **🛡️ Robustez**: Fallbacks, profiles, configurações obrigatórias e tratamento de erros

## Como Usar

### 1. Configuração Básica

```properties
# application.properties
config.client.server-url=http://servidor-config:8080
config.client.namespace=meu-app
config.client.environment=producao
config.client.username=usuario
config.client.password=senha
```

### 2. Injeção Básica de Configurações

```java
@Service
public class MeuServico {
    
    @ValorConfiguracao("database.url")
    private String urlBanco;
}
```

### 3. Conversão Automática de Tipos

```java
@Service
public class ConfiguracaoAvancada {
    
    // Conversão automática para Integer com validação de range
    @ValorConfiguracao(value = "server.port", type = Integer.class, min = 1, max = 65535, defaultValue = "8080")
    private Integer porta;
    
    // Conversão para Duration
    @ValorConfiguracao(value = "app.timeout", type = Duration.class, defaultValue = "PT30S")
    private Duration timeout;
    
    // Lista separada por vírgulas
    @ValorConfiguracao(value = "allowed.hosts", type = List.class, defaultValue = "localhost,127.0.0.1")
    private List<String> hostsPermitidos;
}
```

### 4. Validação Automática

```java
@Service
public class ConfiguracaoValidada {
    
    // Validação de email
    @ValorConfiguracao(value = "admin.email", validator = "email", defaultValue = "admin@comprae.com")
    private String emailAdmin;
    
    // Validação de URL
    @ValorConfiguracao(value = "api.endpoint", validator = "url", required = true)
    private String endpointApi;
    
    // Validação customizada com regex
    @ValorConfiguracao(value = "api.key", validator = "^[A-Za-z0-9]{32}$", required = true)
    private String chaveApi;
}
```

### 5. Profiles e Fallbacks

```java
@Service
public class ConfiguracaoCondicional {
    
    // Configuração específica para produção com fallback
    @ValorConfiguracao(
        value = "database.url.prod",
        profiles = {"prod", "staging"},
        fallback = "database.url.default",
        defaultValue = "jdbc:postgresql://localhost:5432/comprae"
    )
    private String urlBanco;
}
    
    @ValorConfiguracao("database.url")
    private String urlBancoDados;
    
    @ValorConfiguracao(value = "pool.size", defaultValue = "10")
    private Integer tamanhoPool;
    
    @ValorConfiguracao(value = "feature.habilitada", defaultValue = "false")
    private Boolean featureHabilitada;
}
```

### 3. Acesso Programático

```java
@Autowired
private ServicoClienteConfiguracao servicoCliente;

public void exemploUso() {
    // Buscar configuração
    String valor = servicoCliente.buscarValorConfiguracao("minha.config");
    
    // Com valor padrão
    String valor = servicoCliente.buscarValorConfiguracao("config", "padrao");
    
    // Todas as configurações
    Map<String, String> todas = servicoCliente.buscarTodasConfiguracoes();
}
```

## Configurações Disponíveis

| Propriedade | Padrão | Descrição |
|------------|--------|-----------|
| `config.client.server-url` | `http://localhost:8080` | URL do servidor de configurações |
| `config.client.namespace` | `default` | Namespace da aplicação |
| `config.client.environment` | `dev` | Ambiente (dev, prod, etc) |
| `config.client.cache.enabled` | `true` | Habilitar cache local |
| `config.client.cache.ttl` | `300000` | TTL do cache em ms |
| `config.client.sync.enabled` | `true` | Habilitar sincronização |
| `config.client.sync.interval` | `30000` | Intervalo de sync em ms |
| `config.client.kafka.enabled` | `true` | Habilitar Kafka |
| `config.client.kafka.bootstrap-servers` | `localhost:9092` | Servidores Kafka |

## 🧪 Execução de Testes

A SDK possui uma suíte abrangente de testes:

### Tipos de Teste

- **Testes Unitários**: Validação de componentes individuais
- **Testes de Integração**: Validação do fluxo completo
- **Testes de Performance**: Benchmarks e análise de performance

### Executar Testes

```bash
# Todos os testes
./scripts/test-runner.ps1 all

# Apenas testes unitários  
./scripts/test-runner.ps1 unit

# Apenas testes de integração
./scripts/test-runner.ps1 integration

# Apenas testes de performance
./scripts/test-runner.ps1 performance
```

### Cobertura de Código

Após executar os testes, o relatório de cobertura estará disponível em:
`target/site/jacoco/index.html`

## 📊 Monitoramento

### Métricas Disponíveis

A SDK expõe métricas via Micrometer:

```properties
# Endpoint de monitoramento
management.endpoints.web.exposure.include=health,metrics,config-client

# Acesso via:
# GET /actuator/config-client - Informações detalhadas da SDK
# GET /actuator/health       - Status geral
# GET /actuator/metrics      - Métricas do sistema
```

### Health Checks

- **Cache Health**: Monitora hit rate e status do cache
- **Server Connectivity**: Verifica conectividade com o servidor
- **Configuration Status**: Status das configurações carregadas

## 🏗️ Estrutura do Projeto

```
src/main/java/com/configsystem/client/
├── anotacao/           # Anotações (@ValorConfiguracao)
├── cache/             # Cache inteligente com TTL
├── configuracao/       # Classes de configuração
├── exemplos/          # Exemplos de uso
├── monitoramento/     # Health checks e métricas
├── processador/       # Processadores de anotações
├── servico/           # Serviços principais
└── validacao/         # Validação e conversão de tipos

src/test/java/com/configsystem/client/
├── cache/             # Testes do cache
├── integracao/        # Testes de integração
├── performance/       # Benchmarks e testes de carga
└── validacao/         # Testes de validação
```
