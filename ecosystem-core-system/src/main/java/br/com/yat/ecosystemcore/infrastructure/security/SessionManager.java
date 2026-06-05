package br.com.yat.ecosystemcore.infrastructure.security;

import br.com.yat.ecosystemcore.domain.entity.Empresa;
import br.com.yat.ecosystemcore.domain.entity.Tenant;
import br.com.yat.ecosystemcore.domain.entity.Usuario;

public final class SessionManager {

    private static Usuario usuarioLogado;
    private static Tenant tenantAtual;
    private static Empresa empresaFilial;

    private SessionManager() {
    }

    // 🛡️ CORREÇÃO DE THREAD-SAFETY: Toda leitura e escrita passa a usar o mesmo monitor de exclusão mútua
    public static synchronized void iniciarSessao(Usuario usuario, Tenant tenant, Empresa empresa) {
        usuarioLogado = usuario;
        tenantAtual = tenant;
        empresaFilial = empresa;
    }

    public static synchronized void encerrarSessao() {
        usuarioLogado = null;
        tenantAtual = null;
        empresaFilial = null;
    }

    public static synchronized void setEmpresaFilial(Empresa empresa) {
        empresaFilial = empresa;
    }

    public static synchronized Usuario getUsuarioLogado() {
        return usuarioLogado;
    }

    public static synchronized Tenant getTenantAtual() {
        return tenantAtual;
    }

    public static synchronized Empresa getEmpresaFilial() {
        return empresaFilial;
    }

    public static synchronized boolean temEmpresaVinculada() {
        return empresaFilial != null;
    }
}