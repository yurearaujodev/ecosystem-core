package br.com.yat.ecosystemcore.modules.autenticacao.dto;

import br.com.yat.ecosystemcore.modules.cadastro.entity.Empresa;
import br.com.yat.ecosystemcore.modules.tenant.entity.Tenant;
import br.com.yat.ecosystemcore.modules.usuario.entity.Usuario;

import java.time.LocalDateTime;

public class SessaoDTO {

    private Usuario usuario;
    private Tenant tenant;
    private Empresa empresa;
    
    // 🌟 NOVOS CAMPOS ADICIONADOS PARA A NOVA INFRAESTRUTURA DE SEGURANÇA
    private String sessionId;
    private LocalDateTime expiraEm;
    private String refreshToken;

    public SessaoDTO() {}

    // Construtor completo expandido
    public SessaoDTO(Usuario usuario, Tenant tenant, Empresa empresa, String sessionId, LocalDateTime expiraEm, String refreshToken) {
        this.usuario = usuario;
        this.tenant = tenant;
        this.empresa = empresa;
        this.sessionId = sessionId;
        this.expiraEm = expiraEm;
        this.refreshToken = refreshToken;
    }

    // Getters originais
    public Usuario getUsuario() { return usuario; }
    public Tenant getTenant() { return tenant; }
    public Empresa getEmpresa() { return empresa; }

    // ⚡ NOVOS GETTERS E SETTERS
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public LocalDateTime getExpiraEm() { return expiraEm; }
    public void setExpiraEm(LocalDateTime expiraEm) { this.expiraEm = expiraEm; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    // Setters para os objetos base (caso precise popular manualmente)
//    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
//    public void setTenant(Tenant tenant) { this.tenant = tenant; }
//    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
}