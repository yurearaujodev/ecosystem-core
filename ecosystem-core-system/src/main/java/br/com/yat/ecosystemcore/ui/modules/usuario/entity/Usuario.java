package br.com.yat.ecosystemcore.ui.modules.usuario.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public class Usuario {
	private Long id;
	private String uuidPublico;
	private String tenantId;
	private Long pessoaId;
	private Long empresaPadraoId;
	private String email;
	private String senhaHash;
	private int tentativasLogin;
	private LocalDateTime bloqueadoAte;
	private LocalDateTime ultimoAcesso;
	private String status;
	private int version;
	private String macAddressAutorizado;
	// --- Campos de Auditoria ---
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;
	private Long createdBy;
	private Long updatedBy;
	private Long deletedBy;

	// --- Campos de Consentimento (LGPD) ---
	private boolean consentimentoDados;
	private LocalDateTime termoAceitoEm;
	private String versaoTermo;
	private LocalDateTime anonimizadoEm;

	public Usuario() {
		this.uuidPublico = UUID.randomUUID().toString();

		this.status = "ATIVO";
		this.tentativasLogin = 0;
		this.version = 1;
	}

	// Getters e Setters mantidos...
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

	public Long getPessoaId() {
		return pessoaId;
	}

	public void setPessoaId(Long pessoaId) {
		this.pessoaId = pessoaId;
	}

	public Long getEmpresaPadraoId() {
		return empresaPadraoId;
	}

	public void setEmpresaPadraoId(Long empresaPadraoId) {
		this.empresaPadraoId = empresaPadraoId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSenhaHash() {
		return senhaHash;
	}

	public void setSenhaHash(String senhaHash) {
		this.senhaHash = senhaHash;
	}

	public int getTentativasLogin() {
		return tentativasLogin;
	}

	public void setTentativasLogin(int tentativasLogin) {
		this.tentativasLogin = tentativasLogin;
	}

	public LocalDateTime getBloqueadoAte() {
		return bloqueadoAte;
	}

	public void setBloqueadoAte(LocalDateTime bloqueadoAte) {
		this.bloqueadoAte = bloqueadoAte;
	}

	public LocalDateTime getUltimoAcesso() {
		return ultimoAcesso;
	}

	public void setUltimoAcesso(LocalDateTime ultimoAcesso) {
		this.ultimoAcesso = ultimoAcesso;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getMacAddressAutorizado() {
		return macAddressAutorizado;
	}

	public void setMacAddressAutorizado(String macAddressAutorizado) {
		this.macAddressAutorizado = macAddressAutorizado;
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

	public boolean isConsentimentoDados() {
		return consentimentoDados;
	}

	public void setConsentimentoDados(boolean consentimentoDados) {
		this.consentimentoDados = consentimentoDados;
	}

	public LocalDateTime getTermoAceitoEm() {
		return termoAceitoEm;
	}

	public void setTermoAceitoEm(LocalDateTime termoAceitoEm) {
		this.termoAceitoEm = termoAceitoEm;
	}

	public String getVersaoTermo() {
		return versaoTermo;
	}

	public void setVersaoTermo(String versaoTermo) {
		this.versaoTermo = versaoTermo;
	}

	public LocalDateTime getAnonimizadoEm() {
		return anonimizadoEm;
	}

	public void setAnonimizadoEm(LocalDateTime anonimizadoEm) {
		this.anonimizadoEm = anonimizadoEm;
	}

}