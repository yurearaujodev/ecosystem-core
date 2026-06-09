package br.com.yat.ecosystemcore.modules.empresa.service;

import br.com.yat.ecosystemcore.shared.database.TransactionManager;
import br.com.yat.ecosystemcore.shared.service.BaseService;
import br.com.yat.ecosystemcore.modules.empresa.entity.Empresa;
import br.com.yat.ecosystemcore.modules.empresa.repository.EmpresaRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.ConcurrentModificationException;

public class EmpresaService extends BaseService {

	private final EmpresaRepository repository;

	public EmpresaService(EmpresaRepository repository) {
		this.repository = repository;
	}

	public List<Empresa> listarEmpresasDoTenantAtivo() throws SQLException {
		// requirePermission("EMPRESA_VISUALIZAR");
		requireSession();

		return TransactionManager.executeInTransaction(conn -> repository.listarPorTenant(conn, tenant()));
	}

	public void salvarEmpresa(Empresa empresa) {
		// requirePermission("EMPRESA_EDITAR");
		requireSession();

		empresa.setTenantId(tenant());
		if (empresa.getCnpj() == null || empresa.getCnpj().trim().length() != 14) {
			throw new IllegalArgumentException("O CNPJ deve conter exatamente 14 dígitos numéricos.");
		}
		if (empresa.getRazaoSocial() == null || empresa.getRazaoSocial().trim().isEmpty()) {
			throw new IllegalArgumentException("A Razão Social é obrigatória.");
		}
		try {
			TransactionManager.executeVoidInTransaction(conn -> {
				if (empresa.getId() == null) {
					empresa.setCreatedBy(userId());
					Long id = repository.insert(conn, empresa);
					empresa.setId(id);
				} else {
					empresa.setUpdatedBy(userId());
					if (!repository.update(conn, empresa)) {
						throw new ConcurrentModificationException(
								"Empresa não encontrada, sem acesso ou alterada por outro usuário.");
					}
				}

			});
		} catch (SQLException e) {
			if (e.getErrorCode() == 1062) {
				throw new RuntimeException("CNPJ já cadastrado.");
			}
			throw new RuntimeException("Erro ao salvar dados da empresa.", e);
		}
	}

	public void excluirEmpresa(Long id) throws SQLException {
		// requirePermission("EMPRESA_EXCLUIR");
		requireSession();
		TransactionManager.executeVoidInTransaction(conn -> {

			boolean deletado = repository.softDelete(conn, id, tenant(), userId());

			if (!deletado) {
				throw new IllegalStateException("O registro já foi removido ou não pertence ao tenant ativo.");
			}

		});

	}

	public Empresa buscarPorId(Long id) throws SQLException {
		// requirePermission("EMPRESA_VISUALIZAR");
		requireSession();

		return TransactionManager.executeInTransaction(conn -> repository.findByIdAndTenant(conn, id, tenant())
				.orElseThrow(() -> new IllegalStateException("Empresa não encontrada.")));

	}
	
	public List<Empresa> listarParaLogin() throws SQLException {

	    return TransactionManager.executeInTransaction(repository::listarTodasAtivas);
	}
}