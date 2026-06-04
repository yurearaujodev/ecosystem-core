package br.com.yat.ecosystemcore.domain.entity;

import java.time.LocalDateTime;

public class Usuario {
    private Long id;
    private String uuidPublico;
    private String tenantId;
    private Long  pessoaId;
    private Long empresaPadraoId;
    private String email;
    private String senhaHash;
    private int tentativasLogin;
    private LocalDateTime bloqueadoAte;
    private LocalDateTime ultimoAcesso;
    private String status;
    private int version;

    public Usuario() {}

    // Getters e Setters mantidos...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUuidPublico() { return uuidPublico; }
    public void setUuidPublico(String uuidPublico) { this.uuidPublico = uuidPublico; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Long getPessoaId() { return pessoaId; }
    public void setPessoaId(Long pessoaId) { this.pessoaId = pessoaId; }
    public Long getEmpresaPadraoId() { return empresaPadraoId; }
    public void setEmpresaPadraoId(Long empresaPadraoId) { this.empresaPadraoId = empresaPadraoId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public int getTentativasLogin() { return tentativasLogin; }
    public void setTentativasLogin(int tentativasLogin) { this.tentativasLogin = tentativasLogin; }
    public LocalDateTime getBloqueadoAte() { return bloqueadoAte; }
    public void setBloqueadoAte(LocalDateTime bloqueadoAte) { this.bloqueadoAte = bloqueadoAte; }
    public LocalDateTime getUltimoAcesso() { return ultimoAcesso; }
    public void setUltimoAcesso(LocalDateTime ultimoAcesso) { this.ultimoAcesso = ultimoAcesso; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}