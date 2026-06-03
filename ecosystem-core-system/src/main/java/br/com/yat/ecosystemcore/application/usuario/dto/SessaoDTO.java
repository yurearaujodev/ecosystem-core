package br.com.yat.ecosystemcore.application.usuario.dto;

import br.com.yat.ecosystemcore.domain.entity.Empresa;
import br.com.yat.ecosystemcore.domain.entity.Tenant;
import br.com.yat.ecosystemcore.domain.entity.Usuario;

public class SessaoDTO {

    private Usuario usuario;
    private Tenant tenant;
    private Empresa empresa;

    public SessaoDTO() {}

    public SessaoDTO(Usuario usuario, Tenant tenant, Empresa empresa) {
        this.usuario = usuario;
        this.tenant = tenant;
        this.empresa = empresa;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Empresa getEmpresa() {
        return empresa;
    }
}