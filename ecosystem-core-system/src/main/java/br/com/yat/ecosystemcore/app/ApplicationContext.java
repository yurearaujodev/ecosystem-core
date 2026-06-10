package br.com.yat.ecosystemcore.app;

import br.com.yat.ecosystemcore.repository.tenant.TenantRepository;
import br.com.yat.ecosystemcore.service.external.SessionService;
import br.com.yat.ecosystemcore.shared.context.Sessao;
import br.com.yat.ecosystemcore.shared.context.SessionScope;
import br.com.yat.ecosystemcore.shared.security.CachedSessionSecurityService;
import br.com.yat.ecosystemcore.shared.security.UserContextProvider;

import com.warrenstrange.googleauth.GoogleAuthenticator;

import br.com.yat.ecosystemcore.modules.autenticacao.repository.SessaoUsuarioRepository;
import br.com.yat.ecosystemcore.modules.autenticacao.service.AutenticacaoUseCase;
import br.com.yat.ecosystemcore.modules.autorizacao.repository.PerfilPermissaoRepository;
import br.com.yat.ecosystemcore.modules.autorizacao.repository.PermissaoRepository;
import br.com.yat.ecosystemcore.modules.autorizacao.repository.UsuarioPermissaoRepository;
import br.com.yat.ecosystemcore.modules.autorizacao.service.PermissaoService;
import br.com.yat.ecosystemcore.modules.cadastro.repository.EmpresaRepository;
import br.com.yat.ecosystemcore.modules.cadastro.repository.PessoaRepository;
import br.com.yat.ecosystemcore.modules.cadastro.service.EmpresaService;
import br.com.yat.ecosystemcore.modules.cadastro.service.PessoaService;
import br.com.yat.ecosystemcore.modules.seguranca.repository.MfaRepository;
import br.com.yat.ecosystemcore.modules.seguranca.repository.PerfilRepository;
import br.com.yat.ecosystemcore.modules.seguranca.service.MfaService;
import br.com.yat.ecosystemcore.modules.seguranca.service.PerfilService;
import br.com.yat.ecosystemcore.modules.usuario.repository.EmpresaUsuarioRepository;
import br.com.yat.ecosystemcore.modules.usuario.repository.UsuarioRepository;
import br.com.yat.ecosystemcore.modules.usuario.repository.UsuarioSegurancaConfigRepository;
import br.com.yat.ecosystemcore.modules.usuario.service.BCryptPasswordEncoder;
import br.com.yat.ecosystemcore.modules.usuario.service.EmpresaUsuarioService;
import br.com.yat.ecosystemcore.modules.usuario.service.PasswordEncoder;
import br.com.yat.ecosystemcore.modules.usuario.service.SalvarDetalhesSegurancaUsuarioUseCase;
import br.com.yat.ecosystemcore.modules.usuario.service.UsuarioService;

public final class ApplicationContext {

	private static boolean initialized = false;
	private static SessionService sessionService;
	private static UsuarioService usuarioService;
	private static EmpresaService empresaService;
	private static PessoaService pessoaService;
	private static MfaService mfaService;
	private static PerfilService perfilservice;
	private static EmpresaUsuarioService empresaUsuarioService;
	private static SalvarDetalhesSegurancaUsuarioUseCase salvarDetalhesSegurancaUsuarioUseCase;
	private static PermissaoService permissaoService;

	public static void init() {
		if (initialized)
			return;

		// ================= REPOSITORIES =================
		UsuarioRepository usuarioRepository = new UsuarioRepository();
		TenantRepository tenantRepository = new TenantRepository();
		EmpresaRepository empresaRepository = new EmpresaRepository();
		SessaoUsuarioRepository sessaoUsuarioRepository = new SessaoUsuarioRepository();
		PessoaRepository pessoaRepository = new PessoaRepository();

		// 🌟 2. Novos repositórios necessários para o UsuarioService
		EmpresaUsuarioRepository empresaUsuarioRepository = new EmpresaUsuarioRepository();
		UsuarioPermissaoRepository usuarioPermissaoRepository = new UsuarioPermissaoRepository();
		UsuarioSegurancaConfigRepository segurancaConfigRepository = new UsuarioSegurancaConfigRepository();
		MfaRepository mfaRepository = new MfaRepository();
		PerfilRepository perfilRepository = new PerfilRepository();
		PerfilPermissaoRepository perfilPermissaoRepository = new PerfilPermissaoRepository();
		PermissaoRepository permissaoRepository = new PermissaoRepository();

		// ================= SERVICES CORE & SECURITY =================
		sessionService = new SessionService(sessaoUsuarioRepository);
		CachedSessionSecurityService sessionSecurityService = new CachedSessionSecurityService(sessaoUsuarioRepository);

		UserContextProvider.init(usuarioRepository);
		SessionScope.init(sessionSecurityService);
		Sessao.init(sessionSecurityService);

		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

		// ================= SERVICES EXTERNAL (MÓDULOS) =================
		// 🌟 3. Instancia os serviços respeitando a árvore de dependências
		empresaUsuarioService = new EmpresaUsuarioService(empresaUsuarioRepository, usuarioPermissaoRepository,
				segurancaConfigRepository);

		usuarioService = new UsuarioService(usuarioRepository, empresaUsuarioRepository, segurancaConfigRepository,
				empresaUsuarioService);
		empresaService = new EmpresaService(empresaRepository);
		pessoaService = new PessoaService(pessoaRepository);
		GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
		mfaService = new MfaService(mfaRepository, googleAuthenticator);
		perfilservice = new PerfilService(perfilRepository);
		permissaoService = new PermissaoService(permissaoRepository, perfilPermissaoRepository, usuarioPermissaoRepository);

		// ================= USE CASES =================
		AutenticacaoUseCase autenticacaoUseCase = new AutenticacaoUseCase(usuarioRepository, tenantRepository,
				empresaRepository, sessionService, passwordEncoder);

		salvarDetalhesSegurancaUsuarioUseCase = new SalvarDetalhesSegurancaUsuarioUseCase(empresaUsuarioService,
				usuarioPermissaoRepository, segurancaConfigRepository);

		// Registra no Holder
		Holder.autenticacaoUseCase = autenticacaoUseCase;
		Holder.sessionSecurityService = sessionSecurityService;

		initialized = true;
	}

	// 🌟 4. Getter global para os Controllers utilizarem
	public static UsuarioService getUsuarioService() {
		return usuarioService;
	}

	public static EmpresaService getEmpresaService() {
		return empresaService;
	}

	public static PessoaService getPessoaService() {
		return pessoaService;
	}

	public static MfaService getMfaService() {
		return mfaService;
	}

	public static PerfilService getPerfilService() {
		return perfilservice;
	}

	public static EmpresaUsuarioService getEmpresaUsuarioService() {
		return empresaUsuarioService;
	}

	public static SalvarDetalhesSegurancaUsuarioUseCase getSalvarDetalhesSegurancaUsuarioUseCase() {
		return salvarDetalhesSegurancaUsuarioUseCase;
	}
	
	public static PermissaoService getPermissaoService() {
		return permissaoService;
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