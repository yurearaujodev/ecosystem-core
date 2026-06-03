package br.com.yat.ecosystemcore.application.tenant.dto;

/**
 * Dados brutos coletados do formulário de onboarding (sem regra de negócio aplicada).
 */
public record OnboardingTenantCommand(
        String nomeConta,
        String plano,
        String razaoSocial,
        String nomeFantasia,
        String cnpj,
        String telefoneEmpresa,
        String cidade,
        String estado,
        String nomeAdmin,
        String cpfAdmin,
        String telefoneAdmin,
        String emailAdmin,
        String senhaAdmin
) {
}
