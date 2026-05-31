package br.com.yat.ecosystemcore.infrastructure.security;

//Mocks temporários para não quebrar a compilação enquanto você cria o seu banco de dados
class Usuario {}
class Tenant {}
class Empresa {}

/**
* Contexto global thread-safe da sessão do usuário logado.
*/
public final class SessionManager {
 private static Usuario usuarioLogado;
 private static Tenant tenantAtual;
 private static Empresa empresaFilial;

 private SessionManager() {} 

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

 public static Usuario getUsuarioLogado() { return usuarioLogado; }
 public static Tenant getTenantAtual() { return tenantAtual; }
 public static Empresa getEmpresaFilial() { return empresaFilial; }
}
