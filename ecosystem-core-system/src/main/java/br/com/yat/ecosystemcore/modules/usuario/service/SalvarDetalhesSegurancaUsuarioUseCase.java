package br.com.yat.ecosystemcore.modules.usuario.service;

import br.com.yat.ecosystemcore.application.system.dto.AtualizarDetalhesUsuarioCommand;
import br.com.yat.ecosystemcore.modules.autorizacao.repository.UsuarioPermissaoRepository;
import br.com.yat.ecosystemcore.modules.usuario.repository.UsuarioSegurancaConfigRepository;
import br.com.yat.ecosystemcore.shared.context.Sessao;
import br.com.yat.ecosystemcore.shared.database.TransactionManager;

import java.sql.SQLException;

public class SalvarDetalhesSegurancaUsuarioUseCase {
//
//    private final EmpresaUsuarioService empresaUsuarioService = new EmpresaUsuarioService();
//    private final UsuarioPermissaoRepository usuarioPermissaoRepo = new UsuarioPermissaoRepository();
//    private final UsuarioSegurancaConfigRepository segurancaConfigRepo = new UsuarioSegurancaConfigRepository();

	private final EmpresaUsuarioService empresaUsuarioService;

	private final UsuarioPermissaoRepository usuarioPermissaoRepo;

	private final UsuarioSegurancaConfigRepository segurancaConfigRepo;

	public SalvarDetalhesSegurancaUsuarioUseCase(EmpresaUsuarioService empresaUsuarioService,
			UsuarioPermissaoRepository usuarioPermissaoRepo, UsuarioSegurancaConfigRepository segurancaConfigRepo) {

		this.empresaUsuarioService = empresaUsuarioService;
		this.usuarioPermissaoRepo = usuarioPermissaoRepo;
		this.segurancaConfigRepo = segurancaConfigRepo;
	}

//    public void execute(AtualizarDetalhesUsuarioCommand command) throws SQLException {
//        
//        TransactionManager.executeVoidInTransaction(conn -> {
//            
//        	empresaUsuarioService.sincronizarVinculos(conn, command.usuarioId(), command.vinculosEmpresas());
//        	
//            usuarioPermissaoRepo.atualizarVinculosUsuario(conn, command.usuarioId(), command.idsPermissoesExtras());
//
//            segurancaConfigRepo.salvarOuAtualizar(conn, command.usuarioId(), conn.getSchema(), command.segurancaConfig());
// 
//        });
//    }

	public void execute(AtualizarDetalhesUsuarioCommand command) throws SQLException {
		// 1. Capture o tenant ANTES de entrar na transação
		// Isso garante que você não dependa do estado interno da conexão (schema)
		String tenantIdAtivo = Sessao.tenantId();

		if (tenantIdAtivo == null) {
			throw new IllegalStateException("Sessão expirada ou tenant não identificado.");
		}

		TransactionManager.executeVoidInTransaction(conn -> {
			// 2. Use o tenantId capturado
			empresaUsuarioService.sincronizarVinculos(conn, command.usuarioId(), command.vinculosEmpresas());
			usuarioPermissaoRepo.atualizarVinculosUsuario(conn, command.usuarioId(), command.idsPermissoesExtras());

			// 3. O repositório agora recebe o UUID real, não o schema do banco
			// segurancaConfigRepo.salvarOuAtualizar(conn, command.usuarioId(),
			// tenantIdAtivo, command.segurancaConfig());
			if (segurancaConfigRepo.existeConfiguracao(conn, command.usuarioId())) {
				segurancaConfigRepo.atualizar(conn, command.usuarioId(), command.segurancaConfig());
			} else {
				segurancaConfigRepo.inserir(conn, command.usuarioId(), tenantIdAtivo, command.segurancaConfig());
			}
		});
	}
}
