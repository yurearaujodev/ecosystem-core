package br.com.yat.ecosystemcore.modules.autorizacao.entity;

import java.time.LocalDateTime;

public class DispositivoConfiavel {
    private Long id;
    private Long usuarioId;
    private String tenantId;
    private String dispositivoToken;
    private String nomeDispositivo;
    private LocalDateTime ultimoAcessoEm;
    private LocalDateTime validoAte;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getDispositivoToken() { return dispositivoToken; }
    public void setDispositivoToken(String dispositivoToken) { this.dispositivoToken = dispositivoToken; }
    public String getNomeDispositivo() { return nomeDispositivo; }
    public void setNomeDispositivo(String nomeDispositivo) { this.nomeDispositivo = nomeDispositivo; }
    public LocalDateTime getUltimoAcessoEm() { return ultimoAcessoEm; }
    public void setUltimoAcessoEm(LocalDateTime ultimoAcessoEm) { this.ultimoAcessoEm = ultimoAcessoEm; }
    public LocalDateTime getValidoAte() { return validoAte; }
    public void setValidoAte(LocalDateTime validoAte) { this.validoAte = validoAte; }
}