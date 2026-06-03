package br.com.yat.ecosystemcore.domain.entity;

import java.time.LocalDateTime;

public class SessaoUsuario {
    private String id; // CHAR(64)
    private String tenantId; // CHAR(36)
    private Long usuarioId;
    private Long empresaAtivaId;
    private String tokenAtualizacao; // refresh_token CHAR(64)
    private LocalDateTime refreshExpiraEm;
    private String ipOrigem;
    private String dispositivoInfo;
    private LocalDateTime criadoEm;
    private LocalDateTime expiraEm;
    private LocalDateTime revogadoEm;

    public SessaoUsuario() {}

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public Long getEmpresaAtivaId() { return empresaAtivaId; }
    public void setEmpresaAtivaId(Long empresaAtivaId) { this.empresaAtivaId = empresaAtivaId; }
    public String getTokenAtualizacao() { return tokenAtualizacao; }
    public void setTokenAtualizacao(String tokenAtualizacao) { this.tokenAtualizacao = tokenAtualizacao; }
    public LocalDateTime getRefreshExpiraEm() { return refreshExpiraEm; }
    public void setRefreshExpiraEm(LocalDateTime refreshExpiraEm) { this.refreshExpiraEm = refreshExpiraEm; }
    public String getIpOrigem() { return ipOrigem; }
    public void setIpOrigem(String ipOrigem) { this.ipOrigem = ipOrigem; }
    public String getDispositivoInfo() { return dispositivoInfo; }
    public void setDispositivoInfo(String dispositivoInfo) { this.dispositivoInfo = dispositivoInfo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public void setExpiraEm(LocalDateTime expiraEm) { this.expiraEm = expiraEm; }
    public LocalDateTime getRevogadoEm() { return revogadoEm; }
    public void setRevogadoEm(LocalDateTime revogadoEm) { this.revogadoEm = revogadoEm; }
}