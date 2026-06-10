package br.com.yat.ecosystemcore.modules.usuario.service;

import br.com.yat.ecosystemcore.application.system.dto.UsuarioSegurancaConfigDTO;
import br.com.yat.ecosystemcore.shared.context.Sessao;
import br.com.yat.ecosystemcore.shared.database.TransactionManager;
import br.com.yat.ecosystemcore.shared.service.BaseService;
import br.com.yat.ecosystemcore.modules.autorizacao.repository.UsuarioPermissaoRepository;
import br.com.yat.ecosystemcore.modules.usuario.dto.EmpresaUsuarioDetalheDTO;
import br.com.yat.ecosystemcore.modules.usuario.repository.EmpresaUsuarioRepository;
import br.com.yat.ecosystemcore.modules.usuario.repository.UsuarioSegurancaConfigRepository;
import br.com.yat.ecosystemcore.modules.usuario.ui.UsuarioGerenciamentoAbasController.CheckBoxListCellData;
import br.com.yat.ecosystemcore.modules.usuario.ui.UsuarioGerenciamentoAbasController.CustomItem;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class EmpresaUsuarioService extends BaseService {

	private final EmpresaUsuarioRepository repository;

	private final UsuarioPermissaoRepository usuarioPermissaoRepo;

	private final UsuarioSegurancaConfigRepository segurancaConfigRepo;

	public EmpresaUsuarioService(EmpresaUsuarioRepository repository, UsuarioPermissaoRepository usuarioPermissaoRepo,
			UsuarioSegurancaConfigRepository segurancaConfigRepo) {

		this.repository = repository;
		this.usuarioPermissaoRepo = usuarioPermissaoRepo;
		this.segurancaConfigRepo = segurancaConfigRepo;
	}

	public void sincronizarVinculos(Connection conn, Long usuarioId, List<EmpresaUsuarioDetalheDTO> novosVinculos)
			throws SQLException {

		requireSession();

		repository.removerTodosVinculosDoUsuario(conn, tenant(), usuarioId);

		if (novosVinculos == null) {
			return;
		}

		for (EmpresaUsuarioDetalheDTO dto : novosVinculos) {

			repository.vincular(conn, tenant(), dto.getEmpresaId(), usuarioId, dto.getPerfilId());
		}
	}

	public List<EmpresaUsuarioDetalheDTO> listarVinculosDoUsuario(Long usuarioId) throws SQLException {

		requireSession();

		return TransactionManager
				.executeInTransaction(conn -> repository.findVinculosPorUsuario(conn, usuarioId, tenant()));
	}

	public void removerVinculo(Long usuarioId, Long empresaId) throws SQLException {

		requireSession();

		TransactionManager
				.executeVoidInTransaction(conn -> repository.removerVinculo(conn, tenant(), empresaId, usuarioId));
	}

	/**
	 * Resgata chaves e IDs de permissões customizadas diretas do usuário.
	 */
	public List<Long> listarIdsPermissoesExtrasDoUsuario(Long usuarioId) {
		try (Connection conn = TransactionManager.getConnection()) {
			return usuarioPermissaoRepo.listarIdsPermissoesPorUsuario(conn, usuarioId);
		} catch (SQLException e) {
			throw new RuntimeException("Erro ao buscar permissões extras do usuário.", e);
		}
	}

	public UsuarioSegurancaConfigDTO buscarConfiguracoesDeSeguranca(Long usuarioId) throws SQLException {

		return TransactionManager.executeInTransaction(conn -> segurancaConfigRepo.buscarPorUsuario(conn, usuarioId));
	}

	public List<CustomItem> buscarTodosUsuariosAtivos() throws SQLException {
		String tenantId = Sessao.tenantId();
		try (Connection conn = TransactionManager.getConnection()) {
			// Supondo que você tenha um método no repositório ou um SQL simples
			return repository.findAllUsersAsCustomItems(conn, tenantId);
		}
	}

	public List<CustomItem> buscarEmpresasDoTenant() throws SQLException {
		String tenantId = Sessao.tenantId();
		try (Connection conn = TransactionManager.getConnection()) {
			return repository.findAllEmpresas(conn, tenantId);
		}
	}

	public List<CustomItem> buscarPerfisDisponiveis() throws SQLException {
		try (Connection conn = TransactionManager.getConnection()) {
			return repository.findAllPerfis(conn);
		}
	}

	public List<CheckBoxListCellData> buscarTodasPermissoes() throws SQLException {
		try (Connection conn = TransactionManager.getConnection()) {
			return repository.findAllPermissoes(conn);
		}
	}
}