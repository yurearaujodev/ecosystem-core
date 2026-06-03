package br.com.yat.ecosystemcore.infrastructure.security;

import br.com.yat.ecosystemcore.domain.entity.Usuario;
import br.com.yat.ecosystemcore.domain.entity.Tenant;
import br.com.yat.ecosystemcore.domain.entity.Empresa;

/**
 * Contexto global thread-safe da sessão do usuário logado.
 * Modificado para usar ThreadLocal, garantindo isolamento total entre Threads.
 */
public final class SessionManager {
    private static final ThreadLocal<Usuario> usuarioLogado = new ThreadLocal<>();
    private static final ThreadLocal<Tenant> tenantAtual = new ThreadLocal<>();
    private static final ThreadLocal<Empresa> empresaFilial = new ThreadLocal<>();

    private SessionManager() {} 

    public static void iniciarSessao(Usuario usuario, Tenant tenant, Empresa empresa) {
        usuarioLogado.set(usuario);
        tenantAtual.set(tenant);
        empresaFilial.set(empresa);
    }

    public static void encerrarSessao() {
        usuarioLogado.remove();
        tenantAtual.remove();
        empresaFilial.remove();
    }

    public static Usuario getUsuarioLogado() { return usuarioLogado.get(); }
    public static Tenant getTenantAtual() { return tenantAtual.get(); }
    public static Empresa getEmpresaFilial() { return empresaFilial.get(); }
}