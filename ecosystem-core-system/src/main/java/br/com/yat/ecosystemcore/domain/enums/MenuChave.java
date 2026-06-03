package br.com.yat.ecosystemcore.domain.enums;

public enum MenuChave {
    // MÓDULO: HOME / DASHBOARD
    HOME,

    // MÓDULO: CADASTROS
    CADASTROS_EMPRESA,
    CADASTROS_PESSOA,
    CADASTROS_USUARIO,
    
    // MÓDULO: SEGURANÇA (RBAC)
    SEGURANCA_PERFIL,
    SEGURANCA_PERMISSAO,
    
    // MÓDULO: ADMINISTRAÇÃO / CONFIGURAÇÕES
    ADMIN_TENANT_CONFIG,
    ADMIN_PARAMETROS,
    CONFIGURACAO_BANCO,
    
    // MÓDULO: AUDITORIA
    AUDITORIA_LOGS,
    AUDITORIA_JOBS,
    
    //MÓDULO: FINANCEIRO
    FINANCEIRO_FLUXO;
}