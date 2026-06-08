package br.com.yat.ecosystemcore.app;

import br.com.yat.ecosystemcore.application.usuario.AutenticacaoUseCase;
import br.com.yat.ecosystemcore.application.usuario.BCryptPasswordEncoder;
import br.com.yat.ecosystemcore.application.usuario.PasswordEncoder;
import br.com.yat.ecosystemcore.repository.empresa.EmpresaRepository;
import br.com.yat.ecosystemcore.repository.perfil.EmpresaUsuarioRepository;
import br.com.yat.ecosystemcore.repository.tenant.TenantRepository;
import br.com.yat.ecosystemcore.repository.usuario.SessaoUsuarioRepository;
import br.com.yat.ecosystemcore.repository.usuario.UsuarioSegurancaConfigRepository;
import br.com.yat.ecosystemcore.service.external.EmpresaUsuarioService;
import br.com.yat.ecosystemcore.service.external.SessionService;
import br.com.yat.ecosystemcore.shared.context.Sessao;
import br.com.yat.ecosystemcore.shared.context.SessionScope;
import br.com.yat.ecosystemcore.shared.security.CachedSessionSecurityService;
import br.com.yat.ecosystemcore.shared.security.UserContextProvider;
import br.com.yat.ecosystemcore.ui.modules.usuario.repository.UsuarioRepository;
import br.com.yat.ecosystemcore.ui.modules.usuario.service.UsuarioService;

public final class ApplicationContext {

	private static boolean initialized = false;
	private static SessionService sessionService;
	private static UsuarioService usuarioService; // 🌟 1. Atributo estático para o serviço

	public static void init() {
		if (initialized)
			return;

		// ================= REPOSITORIES =================
		UsuarioRepository usuarioRepository = new UsuarioRepository();
		TenantRepository tenantRepository = new TenantRepository();
		EmpresaRepository empresaRepository = new EmpresaRepository();
		SessaoUsuarioRepository sessaoUsuarioRepository = new SessaoUsuarioRepository();
		
		// 🌟 2. Novos repositórios necessários para o UsuarioService
		EmpresaUsuarioRepository empresaUsuarioRepository = new EmpresaUsuarioRepository();
		UsuarioSegurancaConfigRepository segurancaConfigRepository = new UsuarioSegurancaConfigRepository();

		// ================= SERVICES CORE & SECURITY =================
		sessionService = new SessionService(sessaoUsuarioRepository);
		CachedSessionSecurityService sessionSecurityService = new CachedSessionSecurityService(sessaoUsuarioRepository);

		UserContextProvider.init(usuarioRepository);
		SessionScope.init(sessionSecurityService);
		Sessao.init(sessionSecurityService);

		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

		// ================= SERVICES EXTERNAL (MÓDULOS) =================
		// 🌟 3. Instancia os serviços respeitando a árvore de dependências
		EmpresaUsuarioService empresaUsuarioService = new EmpresaUsuarioService(); // Ajuste o construtor dele se ele também pedir repositories!
		
		usuarioService = new UsuarioService(
				usuarioRepository, 
				empresaUsuarioRepository, 
				segurancaConfigRepository, 
				empresaUsuarioService
		);

		// ================= USE CASES =================
		AutenticacaoUseCase autenticacaoUseCase = new AutenticacaoUseCase(
				usuarioRepository, tenantRepository, empresaRepository, sessionService, passwordEncoder);

		// Registra no Holder
		Holder.autenticacaoUseCase = autenticacaoUseCase;
		Holder.sessionSecurityService = sessionSecurityService;

		initialized = true;
	}

	// 🌟 4. Getter global para os Controllers utilizarem
	public static UsuarioService getUsuarioService() {
		return usuarioService;
	}

	public static AutenticacaoUseCase getAutenticacaoUseCase() {
		return Holder.autenticacaoUseCase;
	}

	public static CachedSessionSecurityService getSecurityService() {
		return Holder.sessionSecurityService;
	}

	public static SessionService getSessionService() {
		return sessionService;
	}

	private static final class Holder {
		private static AutenticacaoUseCase autenticacaoUseCase;
		private static CachedSessionSecurityService sessionSecurityService;
	}
}
//package br.com.yat.ecosystemcore.app;
//
//import br.com.yat.ecosystemcore.application.usuario.AutenticacaoUseCase;
//import br.com.yat.ecosystemcore.application.usuario.BCryptPasswordEncoder;
//import br.com.yat.ecosystemcore.application.usuario.PasswordEncoder;
//import br.com.yat.ecosystemcore.repository.empresa.EmpresaRepository;
//import br.com.yat.ecosystemcore.repository.tenant.TenantRepository;
//import br.com.yat.ecosystemcore.repository.usuario.SessaoUsuarioRepository;
//import br.com.yat.ecosystemcore.repository.usuario.UsuarioRepository;
//import br.com.yat.ecosystemcore.service.external.SessionService;
//import br.com.yat.ecosystemcore.shared.context.Sessao;
//import br.com.yat.ecosystemcore.shared.context.SessionScope;
//import br.com.yat.ecosystemcore.shared.security.CachedSessionSecurityService;
//import br.com.yat.ecosystemcore.shared.security.UserContextProvider;
//
//public final class ApplicationContext {
//
//	private static boolean initialized = false;
//	private static SessionService sessionService;
//
//	public static void init() {
//		if (initialized)
//			return;
//
//		// Repositories
//		UsuarioRepository usuarioRepository = new UsuarioRepository();
//		TenantRepository tenantRepository = new TenantRepository();
//		EmpresaRepository empresaRepository = new EmpresaRepository();
//		SessaoUsuarioRepository sessaoUsuarioRepository = new SessaoUsuarioRepository();
//
//		// Services
//		sessionService = new SessionService(sessaoUsuarioRepository);
//
//		CachedSessionSecurityService sessionSecurityService = new CachedSessionSecurityService(sessaoUsuarioRepository);
//
//		UserContextProvider.init(usuarioRepository);
//		SessionScope.init(sessionSecurityService);
//		Sessao.init(sessionSecurityService);
//
//		// Security
//		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
//
//		// UseCases
//		AutenticacaoUseCase autenticacaoUseCase = new AutenticacaoUseCase(usuarioRepository, tenantRepository,
//				empresaRepository, sessionService, passwordEncoder);
//
//		// 👇 expõe globalmente (ou getters depois)
//		Holder.autenticacaoUseCase = autenticacaoUseCase;
//		Holder.sessionSecurityService = sessionSecurityService;
//
//		initialized = true;
//	}
//
//	public static AutenticacaoUseCase getAutenticacaoUseCase() {
//		return Holder.autenticacaoUseCase;
//	}
//
//	public static CachedSessionSecurityService getSecurityService() {
//		return Holder.sessionSecurityService;
//	}
//
//	public static SessionService getSessionService() {
//		return sessionService;
//	}
//
//	private static final class Holder {
//		private static AutenticacaoUseCase autenticacaoUseCase;
//		private static CachedSessionSecurityService sessionSecurityService;
//	}
//}