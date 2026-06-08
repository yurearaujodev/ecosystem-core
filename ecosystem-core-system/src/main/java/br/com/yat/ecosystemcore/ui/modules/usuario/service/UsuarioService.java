package br.com.yat.ecosystemcore.ui.modules.usuario.service;

import br.com.yat.ecosystemcore.domain.entity.EmpresaUsuarioDetalheDTO;
import br.com.yat.ecosystemcore.repository.perfil.EmpresaUsuarioRepository;
import br.com.yat.ecosystemcore.repository.usuario.UsuarioSegurancaConfigRepository;
import br.com.yat.ecosystemcore.service.external.EmpresaUsuarioService;
import br.com.yat.ecosystemcore.shared.database.TransactionManager;
import br.com.yat.ecosystemcore.shared.service.BaseService;
import br.com.yat.ecosystemcore.ui.modules.usuario.entity.Usuario;
import br.com.yat.ecosystemcore.ui.modules.usuario.repository.UsuarioRepository;

import java.sql.SQLException;
import java.util.List;

public class UsuarioService extends BaseService {

	private final UsuarioRepository usuarioRepository;
	private final EmpresaUsuarioRepository empresaUsuarioRepository;
	private final UsuarioSegurancaConfigRepository segurancaConfigRepository;
	private final EmpresaUsuarioService empresaUsuarioService;

	public UsuarioService(UsuarioRepository usuarioRepository, EmpresaUsuarioRepository empresaUsuarioRepository,
			UsuarioSegurancaConfigRepository segurancaConfigRepository, EmpresaUsuarioService empresaUsuarioService) {
		this.usuarioRepository = usuarioRepository;
		this.empresaUsuarioRepository = empresaUsuarioRepository;
		this.segurancaConfigRepository = segurancaConfigRepository;
		this.empresaUsuarioService = empresaUsuarioService;
	}

	public void salvarUsuario(Usuario usuario, List<EmpresaUsuarioDetalheDTO> vinculos) throws SQLException {
		// requirePermission("usuario:salvar");
		requireSession();

		if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
			throw new IllegalArgumentException("O e-mail corporativo é obrigatório.");
		}
		String tenantId = tenant();
		Long usuarioLogadoId = userId();

		usuario.setTenantId(tenantId);

		TransactionManager.executeVoidInTransaction(conn -> {

			boolean novo = usuario.getId() == null;

			if (novo) {
				Long id = usuarioRepository.insert(conn, usuario, usuarioLogadoId);
				usuario.setId(id);

				segurancaConfigRepository.inserirPadrao(conn, id, tenantId);
			} else {
				usuarioRepository.update(conn, usuario, usuarioLogadoId);
			}
			empresaUsuarioService.sincronizarVinculos(conn, usuario.getId(), vinculos);
		});
	}

	public List<Usuario> listarTodos() throws SQLException {
		// requirePermission("usuario:visualizar");
		requireSession();

		return TransactionManager.executeInTransaction(conn -> usuarioRepository.findAll(conn, tenant()));
	}

	public void deletarUsuario(Long id) throws SQLException {
		// requirePermission("usuario:deletar");
		requireSession();

		TransactionManager.executeVoidInTransaction(conn -> {

			boolean ok = usuarioRepository.softDeleteComUsuario(conn, id, tenant(), userId());

			if (!ok) {
				throw new SQLException("Usuário não encontrado ou inválido.");
			}
		});
	}

	public List<EmpresaUsuarioDetalheDTO> listarVinculosEmpresa(Long usuarioId) throws SQLException {
		// requirePermission("usuario:visualizar");
		requireSession();

		return TransactionManager.executeInTransaction(
				conn -> empresaUsuarioRepository.findVinculosPorUsuario(conn, usuarioId, tenant()));
	}

}