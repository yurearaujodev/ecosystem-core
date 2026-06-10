package br.com.yat.ecosystemcore.modules.seguranca.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public class Perfil {

    private Long id;
    private String uuidPublico;
    private String tenantId;
    private String nome;
    private String chaveIdentificadora;
    private String descricao;
    
    // Novos campos adicionados para conformidade com o banco de dados
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private Long createdBy;
    private Long updatedBy;
    private Long deletedBy;

    public Perfil() {
    	this.uuidPublico = UUID.randomUUID().toString();
    	this.version = 1;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuidPublico() {
        return uuidPublico;
    }

    public void setUuidPublico(String uuidPublico) {
        this.uuidPublico = uuidPublico;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getChaveIdentificadora() {
        return chaveIdentificadora;
    }

    public void setChaveIdentificadora(String chaveIdentificadora) {
        this.chaveIdentificadora = chaveIdentificadora;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Long getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(Long deletedBy) {
        this.deletedBy = deletedBy;
    }
}