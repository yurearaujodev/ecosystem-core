package br.com.yat.ecosystemcore.modules.seguranca.service;

import br.com.yat.ecosystemcore.modules.seguranca.entity.Perfil;
import br.com.yat.ecosystemcore.modules.seguranca.repository.PerfilRepository;
import br.com.yat.ecosystemcore.shared.database.TransactionManager;
import br.com.yat.ecosystemcore.shared.service.BaseService;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PerfilService extends BaseService {

	private final PerfilRepository perfilRepository;

	public PerfilService(PerfilRepository perfilRepository) {
		this.perfilRepository = perfilRepository;
	}

	public void cadastrarPerfil(Perfil perfil) throws SQLException {

		requireSession();

		if (perfil.getNome() == null || perfil.getNome().isBlank()) {
			throw new IllegalArgumentException("O nome do perfil não pode estar vazio.");
		}

		if (perfil.getChaveIdentificadora() == null || perfil.getChaveIdentificadora().isBlank()) {
			throw new IllegalArgumentException("A chave identificadora é obrigatória.");
		}

		TransactionManager.executeVoidInTransaction(conn -> {
			Optional<Perfil> existente = perfilRepository.buscarPorChave(conn, tenant(),
					perfil.getChaveIdentificadora().toUpperCase().trim());

			if (existente.isPresent()) {
				throw new IllegalStateException(
						"Já existe um perfil ativo com esta chave identificadora neste Tenant.");
			}
			perfil.setTenantId(tenant());
			perfil.setChaveIdentificadora(perfil.getChaveIdentificadora().toUpperCase().trim());
			perfil.setCreatedBy(userId());
			perfilRepository.salvar(conn, perfil);
		});
	}

	public void atualizarPerfil(Perfil perfil) throws SQLException {

		requireSession();

		TransactionManager.executeVoidInTransaction(conn -> {
			perfil.setUpdatedBy(userId());
			perfilRepository.atualizar(conn, perfil);
		});
	}

	public List<Perfil> listarPerfisPorTenant() throws SQLException {

		requireSession();

		return TransactionManager.executeInTransaction(conn -> perfilRepository.listarPorTenant(conn, tenant()));
	}

	public void excluirPerfil(Long id) throws SQLException {

		requireSession();

		TransactionManager.executeVoidInTransaction(conn -> {

			boolean deletado = perfilRepository.softDeleteComAuditoria(conn, id, tenant(), userId());

			if (!deletado) {
				throw new SQLException("Perfil não encontrado ou já excluído anteriormente.");
			}
		});
	}
}