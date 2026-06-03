package br.com.yat.ecosystemcore.domain.entity;

public class Perfil {

	private Long id;
	private String uuidPublico;
	private String tenantId;
	private String nome;
	private String chaveIdentificadora;
	private String descricao;

	public Perfil() {
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

}