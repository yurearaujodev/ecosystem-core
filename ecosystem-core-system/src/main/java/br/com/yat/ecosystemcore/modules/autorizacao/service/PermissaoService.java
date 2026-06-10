package br.com.yat.ecosystemcore.modules.autorizacao.service;

import br.com.yat.ecosystemcore.modules.autorizacao.entity.Permissao;
import br.com.yat.ecosystemcore.modules.autorizacao.repository.PerfilPermissaoRepository;
import br.com.yat.ecosystemcore.modules.autorizacao.repository.PermissaoRepository;
import br.com.yat.ecosystemcore.modules.autorizacao.repository.UsuarioPermissaoRepository;
import br.com.yat.ecosystemcore.shared.database.TransactionManager;
import br.com.yat.ecosystemcore.shared.service.BaseService;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class PermissaoService extends BaseService {

	private final PermissaoRepository permissaoRepository;
	private final PerfilPermissaoRepository perfilPermissaoRepository;
	private final UsuarioPermissaoRepository usuarioPermissaoRepository;

	public PermissaoService(PermissaoRepository permissaoRepository,
			PerfilPermissaoRepository perfilPermissaoRepository,
			UsuarioPermissaoRepository usuarioPermissaoRepository) {
		this.permissaoRepository = permissaoRepository;
		this.perfilPermissaoRepository = perfilPermissaoRepository;
		this.usuarioPermissaoRepository = usuarioPermissaoRepository;

	}

	public List<Permissao> listarPermissoesDisponiveis() throws SQLException {
		requireSession();

		return TransactionManager
				.executeInTransaction(conn -> permissaoRepository.listarPorTenantOuGlobais(conn, tenant()));
	}

	public List<Long> obterIdsPermissoesDoPerfil(Long perfilId) throws SQLException {
		requireSession();

		if (perfilId == null) {
			throw new IllegalArgumentException("ID do perfil não pode ser nulo.");
		}

		return TransactionManager
				.executeInTransaction(conn -> perfilPermissaoRepository.listarIdsPermissoesPorPerfil(conn, perfilId));
	}

	public void salvarPermissoesDoPerfil(Long perfilId, List<Long> idsPermissoes) throws SQLException {
		requireSession();

		if (perfilId == null) {
			throw new IllegalArgumentException("ID do perfil é obrigatório.");
		}

		TransactionManager.executeVoidInTransaction(
				conn -> perfilPermissaoRepository.atualizarVinculos(conn, perfilId, idsPermissoes));
	}

	public void salvarPermissoesDoUsuario(Long usuarioId, List<Long> idsPermissoes) throws SQLException {
		requireSession();

		if (usuarioId == null) {
			throw new IllegalArgumentException("ID do usuário é obrigatório.");
		}

		TransactionManager.executeVoidInTransaction(
				conn -> usuarioPermissaoRepository.atualizarVinculosUsuario(conn, usuarioId, idsPermissoes));
	}

	public List<Long> obterIdsPermissoesDoUsuario(Long usuarioId) throws SQLException {

		requireSession();

		if (usuarioId == null) {
			throw new IllegalArgumentException("ID do usuário inválido.");
		}

		return TransactionManager.executeInTransaction(
				conn -> usuarioPermissaoRepository.listarIdsPermissoesPorUsuario(conn, usuarioId));
	}

	public void cadastrarNovaPermissao(Permissao permissao) throws SQLException {
		requireSession();

		if (permissao == null) {
			throw new IllegalArgumentException("Permissão não pode ser nula.");
		}

		if (permissao.getModulo() == null || permissao.getModulo().isBlank()) {
			throw new IllegalArgumentException("Módulo é obrigatório.");
		}

		if (permissao.getAcao() == null || permissao.getAcao().isBlank()) {
			throw new IllegalArgumentException("Ação é obrigatória.");
		}

		TransactionManager.executeVoidInTransaction(conn -> {

			String modulo = permissao.getModulo().trim().toUpperCase();
			String acao = permissao.getAcao().trim().toUpperCase();

			Optional<Permissao> existente = permissaoRepository.buscarPorChaveComposta(conn, tenant(), modulo, acao);

			if (existente.isPresent()) {
				throw new IllegalStateException("Já existe uma permissão cadastrada para este módulo e ação.");
			}

			permissao.setTenantId(tenant());
			permissao.setModulo(modulo);
			permissao.setAcao(acao);

			permissaoRepository.salvar(conn, permissao);
		});
	}

}