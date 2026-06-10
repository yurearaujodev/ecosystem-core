package br.com.yat.ecosystemcore.modules.seguranca.service;

import br.com.yat.ecosystemcore.modules.seguranca.dto.MfaConfigDTO;
import br.com.yat.ecosystemcore.modules.seguranca.repository.MfaRepository;
import br.com.yat.ecosystemcore.shared.database.TransactionManager;
import br.com.yat.ecosystemcore.shared.service.BaseService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

public class MfaService extends BaseService {

	private static final String ISSUER = "YatEcosystem";

	private final MfaRepository repository;
	private final GoogleAuthenticator gAuth;

	public MfaService(MfaRepository repository, GoogleAuthenticator gAuth) {
		this.repository = repository;
		this.gAuth = gAuth;
	}

	public MfaConfigDTO iniciarConfiguracaoMfa(Long usuarioId, String emailUsuario) throws SQLException {

		requireSession();

		String tenantId = tenant();

		GoogleAuthenticatorKey credentials = gAuth.createCredentials();

		String segredo = credentials.getKey();

		String emailSanitizado = emailUsuario != null ? emailUsuario.trim() : "usuario";

		String labelEncodado = URLEncoder.encode(ISSUER + ":" + emailSanitizado, StandardCharsets.UTF_8).replace("+",
				"%20");

		String issuerEncodado = URLEncoder.encode(ISSUER, StandardCharsets.UTF_8).replace("+", "%20");

		String qrCodeUrl = String.format("otpauth://totp/%s?secret=%s&issuer=%s", labelEncodado, segredo,
				issuerEncodado);

		TransactionManager
				.executeVoidInTransaction(conn -> repository.salvarNovoSegredo(conn, usuarioId, tenantId, segredo));

		return new MfaConfigDTO(usuarioId, segredo, qrCodeUrl, false);
	}

	public boolean verificarEAtivarMfa(Long usuarioId, int codigoDigitado) throws SQLException {

		requireSession();

		return TransactionManager.executeInTransaction(conn -> {
			var mfaOpt = repository.buscarPorUsuario(conn, usuarioId);
			if (mfaOpt.isEmpty()) {
				throw new IllegalStateException("Nenhuma configuração MFA encontrada para o usuário.");
			}
			MfaConfigDTO config = mfaOpt.get();
			boolean codigoValido = gAuth.authorize(config.secretBase32(), codigoDigitado);
			if (!codigoValido) {
				return false;
			}
			repository.confirmarAtivacao(conn, usuarioId);
			return true;
		});
	}

	public void desativarMfa(Long usuarioId) throws SQLException {

		requireSession();
		TransactionManager.executeVoidInTransaction(conn -> repository.deletarMfa(conn, usuarioId));
	}

	public boolean isMfaAtivo(Long usuarioId) throws SQLException {

		requireSession();

		return TransactionManager.executeInTransaction(
				conn -> repository.buscarPorUsuario(conn, usuarioId).map(MfaConfigDTO::ativo).orElse(false));
	}
}