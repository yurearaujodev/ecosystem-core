package br.com.yat.ecosystemcore.domain.entity;

import java.time.LocalDateTime;

public class Pessoa {
    private Long id;
    private String uuidPublico;
    private String tenantId;
    private String tipo; // FISICA / JURIDICA
    private String nomeRazao;
    private String apelidoFantasia;
    private String cpfCnpj;
    private String telefone;
    private boolean ativo;
    
    // CAMPOS QUE ESTAVAM FALTANDO (Auditoria e Versão):
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long createdBy;
    private Long updatedBy;
    private Long deletedBy;

    public Pessoa() {}

    // Getters e Setters existentes
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUuidPublico() { return uuidPublico; }
    public void setUuidPublico(String uuidPublico) { this.uuidPublico = uuidPublico; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getNomeRazao() { return nomeRazao; }
    public void setNomeRazao(String nomeRazao) { this.nomeRazao = nomeRazao; }
    public String getApelidoFantasia() { return apelidoFantasia; }
    public void setApelidoFantasia(String apelidoFantasia) { this.apelidoFantasia = apelidoFantasia; }
    public String getCpfCnpj() { return cpfCnpj; }
    public void setCpfCnpj(String cpfCnpj) { this.cpfCnpj = cpfCnpj; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    // Getters e Setters dos novos campos
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public Long getDeletedBy() { return deletedBy; }
    public void setDeletedBy(Long deletedBy) { this.deletedBy = deletedBy; }
}