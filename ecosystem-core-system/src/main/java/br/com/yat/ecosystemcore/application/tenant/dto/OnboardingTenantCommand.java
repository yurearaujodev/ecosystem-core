package br.com.yat.ecosystemcore.application.tenant.dto;

import java.util.List;

public record OnboardingTenantCommand(
    String nomeConta,
    String plano,
    String timezone,
    int limiteUsuarios,
    String razaoSocial,
    String nomeFantasia,
    String cnpj,
    String inscricaoEstadual,
    String telefoneEmpresa,
    String logradouro,
    String cidade,
    String estado,
    String cep,
    String nomeAdmin,
    String cpfAdmin,
    String telefoneAdmin,
    String emailAdmin,
    String senhaAdmin,
    boolean permitirMultiplasSessoes,
    boolean aceitaAcessoForaEmpresa,
    List<Long> modulosIds // ⬅️ O novo parâmetro que carrega as escolhas da tela
) {}