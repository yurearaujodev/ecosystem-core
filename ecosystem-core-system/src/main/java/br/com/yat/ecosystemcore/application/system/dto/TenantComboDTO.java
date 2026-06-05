package br.com.yat.ecosystemcore.application.system.dto;

public record TenantComboDTO(String tenantId, String nomeExibicao) {
    // Força o ComboBox do JavaFX a exibir o nome amigável na interface
    @Override
    public String toString() {
        return this.nomeExibicao;
    }
}