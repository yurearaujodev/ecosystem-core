package br.com.yat.ecosystemcore.infrastructure.security;

import br.com.yat.ecosystemcore.domain.entity.Usuario;
import br.com.yat.ecosystemcore.domain.entity.Tenant;
import br.com.yat.ecosystemcore.domain.entity.Empresa;

/**
 * Contexto global da sessão do usuário autenticado no ecossistema JavaFX.
 */
public final class SessionManager {
    private static Usuario usuarioLogado;
    private static Tenant tenantAtual;
    private static Empresa empresaFilial;

    private SessionManager() {} 
    
    public static void setEmpresaFilial(Empresa empresa) {
        empresaFilial = empresa;
    }

    public static void iniciarSessao(Usuario usuario, Tenant tenant, Empresa empresa) {
        usuarioLogado = usuario;
        tenantAtual = tenant;
        empresaFilial = empresa;
    }

    public static void encerrarSessao() {
        usuarioLogado = null;
        tenantAtual = null;
        empresaFilial = null;
    }

    public static Usuario getUsuarioLogado() { return usuarioLogado; }
    public static Tenant getTenantAtual() { return tenantAtual; }
    public static Empresa getEmpresaFilial() { return empresaFilial; }
    
    public static boolean temEmpresaVinculada() { return empresaFilial != null; }
}