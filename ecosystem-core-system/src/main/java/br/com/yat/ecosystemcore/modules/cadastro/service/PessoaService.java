package br.com.yat.ecosystemcore.modules.cadastro.service;

import br.com.yat.ecosystemcore.shared.context.Sessao;
import br.com.yat.ecosystemcore.shared.database.TransactionManager;
import br.com.yat.ecosystemcore.shared.service.BaseService;
import br.com.yat.ecosystemcore.modules.cadastro.entity.Pessoa;
import br.com.yat.ecosystemcore.modules.cadastro.repository.PessoaRepository;

import java.sql.SQLException;
import java.util.List;

public class PessoaService extends BaseService {

	private final PessoaRepository pessoaRepository;

	public PessoaService(PessoaRepository pessoaRepository) {
		this.pessoaRepository = pessoaRepository;
	}

	public void salvarPessoa(Pessoa pessoa) throws SQLException {
		// requirePermission("PESSOA_EDITAR");
		requireSession();

		validarPessoa(pessoa);

		String tenantId = Sessao.tenantId();
		Long usuarioLogadoId = Sessao.usuarioId();

		TransactionManager.executeVoidInTransaction(conn -> {
			if (pessoa.getId() == null) {
				pessoa.setTenantId(tenantId);
				pessoa.setCreatedBy(usuarioLogadoId);
				Long novoId = pessoaRepository.insert(conn, pessoa);
				pessoa.setId(novoId);
			} else {
				pessoa.setTenantId(tenantId);
				pessoa.setUpdatedBy(usuarioLogadoId);
				pessoaRepository.update(conn, pessoa);
			}
		});

	}

	public List<Pessoa> listarTodas() throws SQLException {
		// requirePermission("PESSOA_VISUALIZAR");
		requireSession();

		return TransactionManager.executeInTransaction(conn -> pessoaRepository.findAll(conn, tenant()));

	}

	public void deletarPessoa(Long id) throws SQLException {
//		requirePermission("PESSOA_EXCLUIR");
		requireSession();

		TransactionManager.executeVoidInTransaction(conn -> {

			boolean deletado = pessoaRepository.softDeleteComUsuario(conn, id, tenant(), userId());

			if (!deletado) {
				throw new SQLException(
						"Registro não encontrado, já excluído ou pertence a outra organização corporativa.");
			}
		});
	}

	private void validarPessoa(Pessoa pessoa) {
		if (pessoa == null) {
			throw new IllegalArgumentException("Pessoa não informada.");
		}
		if (pessoa.getNomeRazao() == null || pessoa.getNomeRazao().isBlank()) {
			throw new IllegalArgumentException("Nome/Razão Social é obrigatório.");
		}
		if (pessoa.getTipo() == null || (!"FISICA".equals(pessoa.getTipo()) && !"JURIDICA".equals(pessoa.getTipo()))) {
			throw new IllegalArgumentException("Tipo deve ser FISICA ou JURIDICA.");
		}
	}
}