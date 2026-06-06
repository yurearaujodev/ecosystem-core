package br.com.yat.ecosystemcore.domain.enums;

public enum MenuChave {
    // 🏠 HOME
    DASHBOARD,

    // 👥 CADASTROS (Padrão: Consulta + Novo)
    CADASTROS_EMPRESAS,
    CADASTROS_PESSOAS,
    CADASTROS_USUARIOS,
    
    // 🔐 SEGURANÇA 
    SEGURANCA_PERFIS,
    SEGURANCA_PERMISSOES,
    SEGURANCA_ABA_USUARIO_DETALHE, // Agrupa: Usuário x Empresas, Permissões Extras
    SEGURANCA_MFA,
    SEGURANCA_ABA_SESSÕES_TENTATIVAS, // Agrupa: Sessões Ativas, Tentativas de Login, Dispositivos (Somente Consulta)

    // 🏢 ADMINISTRAÇÃO
    ADMIN_TENANTS, // Consulta + Novo
    ADMIN_ABA_CONFIG_TENANT, // Agrupa: Configurações Tenant, Parâmetros e Features (Abas)
    ADMIN_MODULOS, // Consulta + Novo
    ADMIN_MENUS,   // Consulta + Novo
    ADMIN_TENANT_MENUS,

    // 🔑 LICENCIAMENTO (Alinhado estritamente com o modelo de 5 tabelas)
    LICENCAS_APLICATIVOS,         // Gerencia `licenca_aplicativo` (Catálogo de softwares/módulos comercializados)
    LICENCAS_GERENCIAMENTO,       // Gerencia `licenca` e `licenca_payload` (Central de emissão e geração de tokens RSA)
    LICENCAS_DISPOSITIVOS,        // Gerencia `ativacao_dispositivo` (Controle físico e liberação de hardwares/máquinas)
    LICENCAS_REVOGACOES,          // Gerencia `revogacao_licenca` (Histórico e logs de chaves bloqueadas ou canceladas)
    
    // 🔔 COMUNICAÇÃO
    COMUNICACAO_NOTIFICACOES,

    // 📁 DOCUMENTOS
    DOCUMENTOS_ARQUIVOS,

    // 📜 AUDITORIA (Padrão: Abas / Somente Consulta)
    AUDITORIA_ABA_PRINCIPAL, // Agrupa: Logs e Configuração Auditoria

    // ⚙ SISTEMA (Somente Consulta / Rotinas)
    SISTEMA_CONFIGURACOES,
    SISTEMA_JOBS,
    SISTEMA_OUTBOX_EVENTS,
    SISTEMA_SCHEMA_VERSAO,

    // MÓDULO AUXILIAR
    CONFIGURACAO_BANCO,
    FINANCEIRO_FLUXO;
}