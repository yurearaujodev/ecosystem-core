# Ecosystem Core

Sistema Enterprise para Gerenciamento de Licenças de Software, Multi-Tenant, Controle de Acesso, Auditoria e Gestão Corporativa desenvolvido em **Java Puro**, **JavaFX**, **JDBC**, **MySQL** e **HikariCP**.

> Plataforma Enterprise para Gestão Multi-Tenant, Licenciamento de Software, Controle de Acesso e Auditoria Corporativa construída em Java Puro.

---

# 📖 Visão Geral

O **Ecosystem Core** é uma plataforma modular construída para servir como núcleo de sistemas SaaS modernos, permitindo gerenciamento de clientes, empresas, usuários, permissões, auditoria, licenciamento de software e monetização.

O projeto foi desenvolvido seguindo princípios de:

- Clean Architecture
- SOLID
- DDD (Domain-Driven Design)
- Repository Pattern
- Outbox Pattern
- Multi-Tenancy
- RBAC (Role Based Access Control)
- Optimistic Locking

Sem dependência de frameworks pesados.

---

![Java](https://img.shields.io/badge/Java-21-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue)
![MySQL](https://img.shields.io/badge/MySQL-8-blue)
![JDBC](https://img.shields.io/badge/JDBC-Pure-green)
![Architecture](https://img.shields.io/badge/Architecture-Clean%20Architecture-success)
![Status](https://img.shields.io/badge/Status-In%20Development-yellow)

---

# 📑 Sumário

- Visão Geral
- Tecnologias
- Objetivos
- Status Atual
- Arquitetura
- Recursos Implementados
- Segurança
- Banco de Dados
- Roadmap
- Roadmap Técnico
- Licenciamento e Assinaturas
- Padrões de Projeto
- Modelo de Domínio
- Configuração do Ambiente
- Contribuição
- Licença

---

# 🚀 Tecnologias

| Tecnologia | Utilização |
|------------|-----------|
| Java 21+ | Backend |
| JavaFX | Interface Desktop |
| JDBC | Persistência de dados |
| MySQL 8+ | Banco de Dados |
| HikariCP | Pool de conexões |
| BCrypt | Hash de senhas |
| AES | Criptografia |
| Maven | Build e dependências |

---

# 🧱 Arquitetura Geral

O sistema é construído com foco em:

- Java puro (sem frameworks)
- Arquitetura em camadas
- Separação clara entre domínio, aplicação e infraestrutura
- Persistência desacoplada via JDBC
- Independência de frameworks externos

---

# 🎯 Objetivos do Projeto

- Criar uma plataforma SaaS modular.
- Permitir gerenciamento de licenças de software.
- Suportar múltiplos clientes (multi-tenant).
- Implementar controle granular de permissões.
- Possuir auditoria completa.
- Garantir alta segurança.
- Permitir escalabilidade futura.
- Manter independência de frameworks.

---

# ⭐ Principais Funcionalidades

- Multi-Tenant
- RBAC
- MFA
- Auditoria
- Outbox Pattern
- Gestão de Arquivos
- Gestão de Licenças
- Gestão de Assinaturas
- Menus Dinâmicos
- Feature Flags

---

# 📌 Status Atual

| Item | Status |
|--------|--------|
| Arquitetura | ✅ Concluída |
| Modelagem Banco | ✅ Concluída |
| Infraestrutura JDBC | 🚧 Em Desenvolvimento |
| JavaFX | 🚧 Em Desenvolvimento |
| Licenciamento | 📋 Planejado |
| Financeiro | 📋 Planejado |
| API | 📋 Planejado |

Projeto arquitetado para evolução gradual até se tornar uma plataforma SaaS completa de gestão e licenciamento de software.

---

# 🏗 Arquitetura

```text
src/main/java/com/br/yat/ecosystemcore/

├── bootstrap/
│   └── App.java
│
├── config/
│   ├── DatabaseConfig.java
│   └── AppConfig.java
│
├── infrastructure/
│   ├── database/
│   │   ├── ConnectionFactory.java
│   │   ├── HikariProvider.java
│   │   └── TransactionManager.java
│   │
│   ├── security/
│   │   ├── BCryptService.java
│   │   ├── AESSecurity.java
│   │   └── SessionManager.java
│   │
│   └── outbox/
│       └── OutboxProcessor.java
│
├── domain/
│   ├── entity/
│   ├── enums/
│   └── valueobject/
│
├── application/
│   ├── empresa/
│   ├── usuario/
│   ├── licenca/
│   └── pagamento/
│
├── repository/
│   ├── base/
│   ├── empresa/
│   ├── usuario/
│   └── licenca/
│
├── service/
│   ├── validation/
│   ├── external/
│   └── mapper/
│
├── ui/
│   ├── core/
│   ├── common/
│   ├── modules/
│   └── menu/
│
├── security/
│
└── util/
```

---

# 🏛 Fluxo Arquitetural

```text
JavaFX UI
    │
    ▼
Application Layer
    │
    ▼
Domain Layer
    │
    ▼
Repository Layer
    │
    ▼
JDBC
    │
    ▼
MySQL
```

---

# 🏢 Recursos do Core

## Multi-Tenant

Cada cliente possui isolamento completo de:

- Usuários
- Empresas
- Configurações
- Recursos
- Permissões
- Menus

Tabela principal:

```sql
tenant
```

---

## Gestão de Usuários

Funcionalidades:

- Cadastro
- Alteração
- Bloqueio
- Controle de acesso
- MFA
- Sessões
- Histórico de senha

Tabelas:

```sql
usuario
usuario_mfa
usuario_historico_senha
sessao_usuario
```

---

## Gestão de Empresas

Funcionalidades:

- Multiempresa
- Empresa padrão
- Vínculo de usuários

Tabelas:

```sql
empresa
empresa_usuario
```

---

## Controle de Acesso (RBAC)

Estrutura:

```text
Usuário
 ↓
Perfil
 ↓
Permissões
 ↓
Menus
```

Tabelas:

```sql
perfil
permissao
perfil_permissao
usuario_permissao
permissao_menu
```

---

## Menus Dinâmicos

Controle completo de menus pelo banco.

Tabelas:

```sql
modulo_sistema
menu_sistema
tenant_modulo
tenant_menu
```

---

## Auditoria

Rastreamento completo de:

- Inclusões
- Alterações
- Exclusões
- Login
- Logout
- Alteração de permissões

Tabela:

```sql
log_auditoria
```

---

## Outbox Pattern

Preparado para:

- Eventos
- Integrações
- APIs
- Mensageria
- Microsserviços

Tabela:

```sql
outbox_event
```

---

## Gestão de Arquivos

Sistema polimórfico para anexos.

Tabelas:

```sql
arquivo
arquivo_vinculo
```

---

## Notificações

Sistema interno de notificações.

Tabela:

```sql
notificacao
```

---

# 🔒 Segurança

## BCrypt

Utilizado para armazenamento seguro de senhas.

---

## AES

Criptografia de informações sensíveis.

---

## MFA

Autenticação em dois fatores.

Tabela:

```sql
usuario_mfa
```

## Camadas de Proteção

- Hash BCrypt
- MFA (TOTP)
- Controle de Sessões
- Refresh Tokens
- Bloqueio por Tentativas de Login
- Controle de Dispositivos Confiáveis
- Auditoria Completa
- Soft Delete
- Controle de Permissões Granulares
- Criptografia AES para dados sensíveis

---

## Controle de Sessões

Recursos:

- Refresh Token
- Revogação
- Expiração
- Dispositivos confiáveis

Tabelas:

```sql
sessao_usuario
dispositivo_confiavel
```

---

## Anti Brute Force

Tabela:

```sql
tentativa_login_log
```

## Fluxo de Autenticação

Usuário
↓
Login
↓
Validação BCrypt
↓
Validação MFA
↓
Criação da Sessão
↓
Refresh Token
↓
Acesso ao Sistema

---

# 📋 LGPD

Implementações previstas:

- Consentimento de dados
- Histórico de termos
- Soft Delete
- Anonimização

Campos:

```sql
consentimento_dados
termo_aceito_em
versao_termo
anonimizado_em
```

---

# 🗄 Banco de Dados

Banco utilizado:

```text
MySQL 8+
```

Características:

- Multi-Tenant
- Soft Delete
- Auditoria
- Versionamento
- Outbox Pattern
- MFA
- Controle de Sessões
- RBAC

---	

# 🔄 Controle de Concorrência

Utilizando:

```text
Optimistic Locking
```

Campo:

```sql
version
```

Presente em diversas entidades.

---

# 📦 Versionamento

O projeto utiliza versionamento semântico.

Formato:

MAJOR.MINOR.PATCH

Exemplos:

1.0.0
1.1.0
1.2.0

Tabela responsável:

schema_version

---

# 🏗 Estrutura da Base de Dados

A modelagem foi dividida em módulos.

## Bloco 1
Infraestrutura e Versionamento

- schema_version
- sistema_config
- job_execucao

## Bloco 2
Multi-Tenant

- tenant
- tenant_config
- tenant_parametro
- tenant_feature

## Bloco 3
Cadastros

- empresa
- pessoa
- usuario

## Bloco 4
RBAC

- perfil
- permissao
- perfil_permissao
- usuario_permissao

## Bloco 5
Menus Dinâmicos

- modulo_sistema
- menu_sistema

## Bloco 6
Segurança

- sessao_usuario
- usuario_mfa
- tentativa_login_log

## Bloco 7
Serviços

- log_auditoria
- arquivo
- notificacao

---

# ⚙️ Configurações

Arquivo:

```text
application.properties
```

Exemplo:

```properties
db.host=localhost
db.port=3306
db.database=ecossistema_sistema
db.username=root
db.password=senha

hikari.maximumPoolSize=20
hikari.minimumIdle=5
```

## HikariCP

Pool de conexões otimizado.

Configuração inicial sugerida:

```java
config.setMaximumPoolSize(20);
config.setMinimumIdle(5);
config.setIdleTimeout(300000);
config.setMaxLifetime(1800000);
config.setConnectionTimeout(30000);
config.setLeakDetectionThreshold(60000);
config.setAutoCommit(false);
```

---

# 🚩 Feature Flags

O sistema suporta ativação e desativação de recursos por Tenant.

Exemplos:

- LICENCIAMENTO
- FINANCEIRO
- CRM
- API_PUBLICA
- RELATORIOS

Tabela:

tenant_feature

---

# 📦 Módulos do Sistema

Core
├── Multi-Tenant
├── Usuários
├── Empresas
├── RBAC
├── Auditoria
├── Arquivos
├── Notificações
└── Segurança

Comercial
├── Produtos
├── Licenças
├── Assinaturas
└── Pagamentos

Integrações
├── API REST
├── SMTP
├── WhatsApp
└── Webhooks

---

# 🚧 Roadmap

## Versão 1.0.0

### Infraestrutura

- [x] Arquitetura Multi-Tenant
- [x] Usuários
- [x] Empresas
- [x] RBAC
- [x] Menus Dinâmicos
- [x] Auditoria
- [x] MFA
- [x] Sessões
- [x] Outbox Pattern
- [x] Soft Delete
- [x] Versionamento
- [x] Arquivos
- [x] Notificações

---

## Versão 1.1.0

### Licenciamento de Software

- [ ] Cadastro de Produtos
- [ ] Cadastro de Versões
- [ ] Planos Comerciais
- [ ] Emissão de Licenças
- [ ] Renovação Automática
- [ ] Trial
- [ ] Licença Perpétua
- [ ] Licença Recorrente
- [ ] Bloqueio Automático

---

## Versão 1.2.0

### Financeiro

- [ ] Assinaturas
- [ ] Cobranças
- [ ] PIX
- [ ] Cartão
- [ ] Boletos
- [ ] Histórico Financeiro
- [ ] Integração Bancária

---

## Versão 1.3.0

### Integrações

- [ ] SMTP
- [ ] WhatsApp
- [ ] Webhooks
- [ ] API REST
- [ ] API Pública para Clientes

---

## Versão 1.4.0

### Monitoramento

- [ ] Dashboard
- [ ] Logs Centralizados
- [ ] Métricas
- [ ] Alertas
- [ ] Telemetria

---

## Versão 2.0.0

### Cloud Native

- [ ] Docker
- [ ] Docker Compose
- [ ] Redis
- [ ] PostgreSQL
- [ ] Kafka
- [ ] Kubernetes

---

# 🚀 Roadmap Técnico

## Infraestrutura

- [ ] Flyway
- [ ] Liquibase
- [ ] Docker
- [ ] Docker Compose
- [ ] Redis
- [ ] Kafka

## Observabilidade

- [ ] Métricas
- [ ] Health Checks
- [ ] Monitoramento
- [ ] Alertas

## Segurança

- [ ] Rotação de Chaves AES
- [ ] Assinatura JWT
- [ ] Device Fingerprint

---

# 💰 Licenciamento e Assinaturas

O principal objetivo do projeto é se tornar uma plataforma completa de licenciamento de software.

## Estrutura Prevista

```text
Cliente
 ↓
Produto
 ↓
Versão
 ↓
Plano Comercial
 ↓
Licença
 ↓
Assinatura
```

---

## Entidades Futuras

### produto

Representa o software comercializado.

Exemplos:

- ERP
- PDV
- CRM
- Sistema de Licenciamento

---

### produto_versao

Controle de versões:

```text
1.0.0
1.1.0
2.0.0
```

---

### plano_comercial

Tipos:

- Trial
- Starter
- Professional
- Enterprise

---

### licenca

Controle da licença emitida.

Campos previstos:

```text
Chave
Status
Data Expiração
Limite de Usuários
Limite de Dispositivos
```

---

### licenca_dispositivo

Controle de máquinas autorizadas.

Exemplos:

```text
MAC Address
UUID da Máquina
Fingerprint
```

---

### licenca_ativacao

Histórico completo de ativações.

---

### assinatura

Controle financeiro da licença.

---

### pagamento

Registro de cobranças.

---

### webhook_licenca

Eventos:

- Licença criada
- Licença renovada
- Licença expirada
- Licença bloqueada

---

# 🧠 Padrões de Projeto Utilizados

- SOLID
- Clean Architecture
- Repository Pattern
- Service Layer
- Outbox Pattern
- Unit Of Work
- Optimistic Locking
- Factory Pattern
- Builder Pattern
- Strategy Pattern
- Observer Pattern

---

# 🏛 Princípios Arquiteturais

O projeto segue os seguintes princípios:

- Baixo acoplamento
- Alta coesão
- Separação de responsabilidades
- Arquitetura orientada ao domínio
- Persistência desacoplada
- Independência de frameworks
- Testabilidade
- Escalabilidade horizontal futura
- Evolução incremental

---

# 📈 Escalabilidade

A arquitetura foi projetada para crescimento horizontal e evolução contínua.

O sistema suporta:

- Multi-tenancy isolado por cliente
- Processamento assíncrono via Outbox Pattern
- Evolução gradual para microsserviços
- Alto volume de usuários e transações
- Crescimento modular por feature flags
- Possibilidade de cache distribuído
- Separação clara de domínios e responsabilidades

---

A base do sistema permite evolução para:

- Arquitetura distribuída
- Mensageria (event-driven)
- Microservices
- Cloud-native deployment

---

# 🗃 Modelo de Domínio

```text
Tenant
├── Empresas
├── Usuários
├── Perfis
├── Permissões
├── Configurações
├── Menus
├── Features
└── Licenças

Empresa
├── Usuários
└── Perfis

Usuário
├── Sessões
├── MFA
├── Permissões
└── Auditoria
```

---

# 📋 Convenções do Projeto

## Banco de Dados

- snake_case
- chaves primárias = id
- UUID público em uuid_publico
- Soft Delete com deleted_at

---

## Java

- Classes em PascalCase
- Métodos em camelCase
- Interfaces iniciadas por I quando necessário
- DTOs separados das entidades

## SQL

- Índices nomeados
- Constraints nomeadas
- Foreign Keys explícitas

---

# 📐 Decisões Arquiteturais

Algumas decisões importantes adotadas no projeto:

| Decisão | Motivo |
|----------|---------|
| Java Puro | Controle total da aplicação |
| JDBC | Máxima performance e independência |
| JavaFX | Interface desktop moderna |
| MySQL | Estabilidade e ampla adoção |
| HikariCP | Melhor pool de conexões Java |
| UUID Público | Evitar exposição de IDs internos |
| Soft Delete | Preservação histórica |
| Outbox Pattern | Preparação para integrações |
| Multi-Tenant | Escalabilidade comercial |

---

# 🧪 Estratégia de Testes

Planejamento:

- Testes Unitários
- Testes de Integração
- Testes de Repositório JDBC
- Testes de Segurança
- Testes de Performance
- Testes de Concorrência

---

# 📊 Métricas Planejadas

- Total de Tenants
- Usuários Ativos
- Licenças Ativas
- Licenças Expiradas
- Receita Mensal
- Receita Anual
- Ativações por Produto
- Uso por Empresa

---

# 🔥 Diferenciais do Projeto

✔ Java Puro sem Frameworks

✔ Arquitetura Multi-Tenant

✔ RBAC Corporativo

✔ Auditoria Completa

✔ MFA

✔ Outbox Pattern

✔ Soft Delete Global

✔ Optimistic Locking

✔ HikariCP Otimizado

✔ Preparado para SaaS

✔ Preparado para Licenciamento de Software

✔ Estrutura preparada para Microsserviços

---

# 🎖 Requisitos Não Funcionais

O sistema foi projetado visando:

- Segurança
- Escalabilidade
- Manutenibilidade
- Auditabilidade
- Performance
- Extensibilidade
- Confiabilidade
- Disponibilidade

---

# 🛣 Visão de Longo Prazo

Transformar o Ecosystem Core em uma plataforma completa de:

- Gestão de Licenças
- ERP Modular
- CRM
- Plataforma SaaS
- Marketplace de Módulos
- Gestão Financeira
- Gestão de Assinaturas
- Portal do Cliente
- API Pública para Integrações

---

# ⚙️ Configuração do Ambiente

## Requisitos

- Java 21+
- Maven 3.9+
- MySQL 8+

## Clonar Projeto

```bash
git clone https://github.com/seu-usuario/ecosystem-core.git
```

## Criar Banco

```sql
CREATE DATABASE ecossistema_sistema;
```

## Executar Script

Execute o arquivo:

database/schema.sql

## Compilar

```bash
mvn clean install
```

## Executar

```bash
mvn javafx:run
```

---

# 🤝 Contribuição

Contribuições são bem-vindas.

Fluxo recomendado:

1. Fork do projeto
2. Criar branch feature
3. Commit das alterações
4. Pull Request

Padrões obrigatórios:

- Seguir arquitetura existente
- Respeitar SOLID
- Manter separação de camadas
- Não adicionar frameworks sem justificativa

---

# 📄 Licença

Este projeto está sob licença MIT.

Consulte o arquivo LICENSE para mais informações.

---

# 👨‍💻 Autor

YAT Ecosystem

Enterprise Software Platform