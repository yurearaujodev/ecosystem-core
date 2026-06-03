package br.com.yat.ecosystemcore.domain.entity;

public class UsuarioSegurancaConfig {

	private Long usuarioId;
	private String tenantId;
	private boolean requerNovaSenha;
	private boolean aceitaAcessoForaEmpresa;
	private String ipEstaticoObrigatorio;
	private boolean permitirMultiplasSessoes;

	public UsuarioSegurancaConfig() {
	}

	public Long getUsuarioId() {
		return usuarioId;
	}

	public void setUsuarioId(Long usuarioId) {
		this.usuarioId = usuarioId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public boolean isRequerNovaSenha() {
		return requerNovaSenha;
	}

	public void setRequerNovaSenha(boolean requerNovaSenha) {
		this.requerNovaSenha = requerNovaSenha;
	}

	public boolean isAceitaAcessoForaEmpresa() {
		return aceitaAcessoForaEmpresa;
	}

	public void setAceitaAcessoForaEmpresa(boolean aceitaAcessoForaEmpresa) {
		this.aceitaAcessoForaEmpresa = aceitaAcessoForaEmpresa;
	}

	public String getIpEstaticoObrigatorio() {
		return ipEstaticoObrigatorio;
	}

	public void setIpEstaticoObrigatorio(String ipEstaticoObrigatorio) {
		this.ipEstaticoObrigatorio = ipEstaticoObrigatorio;
	}

	public boolean isPermitirMultiplasSessoes() {
		return permitirMultiplasSessoes;
	}

	public void setPermitirMultiplasSessoes(boolean permitirMultiplasSessoes) {
		this.permitirMultiplasSessoes = permitirMultiplasSessoes;
	}

}