package br.com.yat.ecosystemcore.domain.entity;

public class Usuario {
    private Long id;
    private String uuidPublico;
    private String tenantId;
    private Long pessoaId;
    private Long empresaPadraoId;
    private String email;
    private String senhaHash;
    private String status;

    public Usuario() {}

    // Getters e Setters
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}