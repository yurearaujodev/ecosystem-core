package br.com.yat.ecosystemcore.modules.auditoria.entity;

import java.time.LocalDateTime;

public class TentativaLoginLog {
    private Long id;
    private String tenantIdDetectado;
    private String emailTentativa;
    private String ipOrigem;
    private String dispositivoInfo;
    private boolean sucesso;
    private String motivoFalha;
    private LocalDateTime dataHora;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantIdDetectado() { return tenantIdDetectado; }
    public void setTenantIdDetectado(String tenantIdDetectado) { this.tenantIdDetectado = tenantIdDetectado; }
    public String getEmailTentativa() { return emailTentativa; }
    public void setEmailTentativa(String emailTentativa) { this.emailTentativa = emailTentativa; }
    public String getIpOrigem() { return ipOrigem; }
    public void setIpOrigem(String ipOrigem) { this.ipOrigem = ipOrigem; }
    public String getDispositivoInfo() { return dispositivoInfo; }
    public void setDispositivoInfo(String dispositivoInfo) { this.dispositivoInfo = dispositivoInfo; }
    public boolean isSucesso() { return sucesso; }
    public void setSucesso(boolean sucesso) { this.sucesso = sucesso; }
    public String getMotivoFalha() { return motivoFalha; }
    public void setMotivoFalha(String motivoFalha) { this.motivoFalha = motivoFalha; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}