package br.com.yat.ecosystemcore.domain.entity;

public class Pessoa {
    private Long id;
    private String uuidPublico;
    private String tenantId;
    private String tipo; // FISICA / JURIDICA
    private String nomeRazao;
    private String apelidoFantasia;
    private String cpfCnpj;
    private String telefone;

    public Pessoa() {}

    // Getters e Setters
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
}