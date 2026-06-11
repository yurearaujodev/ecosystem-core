package br.com.yat.ecosystemcore.modules.autenticacao.service;

import br.com.yat.ecosystemcore.shared.util.TokenGenerator;
import br.com.yat.ecosystemcore.modules.autenticacao.entity.SessaoUsuario;
import br.com.yat.ecosystemcore.modules.autenticacao.repository.SessaoUsuarioRepository;
import br.com.yat.ecosystemcore.modules.cadastro.entity.Empresa;
import br.com.yat.ecosystemcore.modules.usuario.entity.Usuario;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class SessaoUsuarioService {

	private final SessaoUsuarioRepository repository;

	public SessaoUsuarioService(SessaoUsuarioRepository repository) {
		this.repository = repository;
	}

	public SessaoUsuario criarSessao(Usuario usuario, Empresa empresa) {

		SessaoUsuario sessao = new SessaoUsuario();

		sessao.setId(TokenGenerator.generateSessionId());
		sessao.setRefreshToken(TokenGenerator.generateRefreshToken());

		sessao.setTenantId(usuario.getTenantId());
		sessao.setUsuarioId(usuario.getId());

		sessao.setEmpresaAtivaId(empresa != null ? empresa.getId() : null);

		sessao.setExpiraEm(LocalDateTime.now().plusHours(8));
		sessao.setRefreshExpiraEm(LocalDateTime.now().plusDays(7));

		sessao.setIpOrigem("127.0.0.1");
		sessao.setDispositivoInfo("Desktop JavaFX");

		return sessao;
	}

	public SessaoUsuario salvarSessao(Connection conn, SessaoUsuario sessao) throws SQLException {
		repository.insert(conn, sessao);
		return sessao;
	}

	public void revogar(Connection conn, String sessionId) throws SQLException {
		repository.revoke(conn, sessionId);
	}
}