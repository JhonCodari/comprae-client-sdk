# Cliente SDK Compraê

Biblioteca cliente responsável por facilitar a integração dos microserviços do ecossistema Compraê com o servidor de configuração centralizado.

## Funcionalidades

- **Injeção Automática**: Use a anotação `@ValorConfiguracao` para injetar valores de configuração automaticamente
- **Cache Local**: Cache inteligente com TTL configurável para otimizar performance
- **Atualização em Tempo Real**: Escuta mudanças via Kafka e atualiza configurações automaticamente
- **Sincronização Periódica**: Sincronização automática com o servidor para garantir consistência
- **Fallback**: Suporte a valores padrão quando configurações não estão disponíveis

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

### 2. Injeção de Configurações

```java
@Service
public class MeuServico {
    
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

## Estrutura do Projeto

```
src/main/java/com/configsystem/client/
├── anotacao/           # Anotações (@ValorConfiguracao)
├── configuracao/       # Classes de configuração
├── exemplos/          # Exemplos de uso
├── processador/       # Processadores de anotações
└── servico/           # Serviços principais
```
