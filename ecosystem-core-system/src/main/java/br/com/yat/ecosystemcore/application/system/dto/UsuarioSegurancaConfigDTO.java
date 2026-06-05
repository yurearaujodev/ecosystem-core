package br.com.yat.ecosystemcore.application.system.dto;

public record UsuarioSegurancaConfigDTO(
    boolean requerNovaSenha,
    boolean aceitaAcessoForaEmpresa,
    String ipEstaticoObrigatorio,
    boolean permitirMultiplasSessoes
) {}