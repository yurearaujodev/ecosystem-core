package br.com.yat.ecosystemcore.domain.entity;

import br.com.yat.ecosystemcore.domain.enums.TenantPlano;
import br.com.yat.ecosystemcore.domain.enums.TenantStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public class Tenant {
    private UUID id; // CHAR(36) no banco
    private String nomeConta;
    private TenantPlano plano;
    private TenantStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt; // Para o Soft Delete

    // Construtor padrão
    public Tenant() {}

    // Construtor completo para criação rápida
    public Tenant(UUID id, String nomeConta, TenantPlano plano, TenantStatus status) {
        this.id = id;
        this.nomeConta = nomeConta;
        this.plano = plano;
        this.status = status;
    }

    // Getters e Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNomeConta() { return nomeConta; }
    public void setNomeConta(String nomeConta) { this.nomeConta = nomeConta; }

    public TenantPlano getPlano() { return plano; }
    public void setPlano(TenantPlano plano) { this.plano = plano; }

    public TenantStatus getStatus() { return status; }
    public void setStatus(TenantStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}