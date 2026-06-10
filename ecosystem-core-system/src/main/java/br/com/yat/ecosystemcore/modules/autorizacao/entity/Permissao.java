package br.com.yat.ecosystemcore.modules.autorizacao.entity;

public class Permissao {
    private Long id;
    private String uuidPublico;
    private String tenantId;
    private String modulo;
    private String acao;
    private String chaveComposta; // Campo gerado no banco (modulo:acao)
    private String descricao;
    private Integer version;

    // Construtores
    public Permissao() {}

    public Permissao(Long id, String uuidPublico, String tenantId, String modulo, String acao, String chaveComposta, String descricao, Integer version) {
        this.id = id;
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.modulo = modulo;
        this.acao = acao;
        this.chaveComposta = chaveComposta;
        this.descricao = descricao;
        this.version = version;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUuidPublico() { return uuidPublico; }
    public void setUuidPublico(String uuidPublico) { this.uuidPublico = uuidPublico; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }

    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }

    public String getChaveComposta() { return chaveComposta; }
    public void setChaveComposta(String chaveComposta) { this.chaveComposta = chaveComposta; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}