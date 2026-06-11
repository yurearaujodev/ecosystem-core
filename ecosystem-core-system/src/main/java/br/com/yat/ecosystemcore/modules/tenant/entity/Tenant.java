package br.com.yat.ecosystemcore.modules.tenant.entity;

import java.time.LocalDateTime;

public class Tenant {

    private String id;

    private String nomeConta;

    private String plano;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public Tenant() {
    }

    public boolean isAtivo() {
        return "ATIVO".equalsIgnoreCase(status);
    }

    public boolean isInativo() {
        return !isAtivo();
    }

    public boolean isExcluido() {
        return deletedAt != null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNomeConta() {
        return nomeConta;
    }

    public void setNomeConta(String nomeConta) {
        this.nomeConta = nomeConta;
    }

    public String getPlano() {
        return plano;
    }

    public void setPlano(String plano) {
        this.plano = plano;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
//package br.com.yat.ecosystemcore.domain.entity;
//
//import java.time.LocalDateTime;
//
//public class Tenant {
//    private String id; // CHAR(36) -> UUID
//    private String nomeConta;
//    private String plano;
//    private String status;
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
//    private LocalDateTime deletedAt;
//
//    public Tenant() {}
//
//    public Tenant(String id, String nomeConta, String plano, String status) {
//        this.id = id;
//        this.nomeConta = nomeConta;
//        this.plano = plano;
//        this.status = status;
//    }
//
//    // Getters e Setters Reais
//    public String getId() { return id; }
//    public void setId(String id) { this.id = id; }
//    public String getNomeConta() { return nomeConta; }
//    public void setNomeConta(String nomeConta) { this.nomeConta = nomeConta; }
//    public String getPlano() { return plano; }
//    public void setPlano(String plano) { this.plano = plano; }
//    public String getStatus() { return status; }
//    public void setStatus(String status) { this.status = status; }
//    public LocalDateTime getCreatedAt() { return createdAt; }
//    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
//    public LocalDateTime getUpdatedAt() { return updatedAt; }
//    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
//    public LocalDateTime getDeletedAt() { return deletedAt; }
//    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
//}