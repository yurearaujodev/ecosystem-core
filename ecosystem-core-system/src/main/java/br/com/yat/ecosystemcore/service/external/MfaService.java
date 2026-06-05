package br.com.yat.ecosystemcore.service.external;

import br.com.yat.ecosystemcore.application.system.dto.MfaConfigDTO;
import br.com.yat.ecosystemcore.infrastructure.database.TransactionManager;
import br.com.yat.ecosystemcore.infrastructure.security.SessionManager;
import br.com.yat.ecosystemcore.repository.perfil.UsuarioMfaRepository;

import java.sql.Connection;
import java.sql.SQLException;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;

public class MfaService {

    private final UsuarioMfaRepository repository = new UsuarioMfaRepository();
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    // Nome que vai aparecer no aplicativo do celular do usuário (Ex: YatEcosystem)
    private static final String ISSUER = "YatEcosystem";

    /**
     * Passo 1: Inicia a configuração do MFA gerando chaves oficiais da biblioteca GoogleAuth
     */
    public MfaConfigDTO iniciarConfiguracaoMfa(Long usuarioId, String emailUsuario) throws SQLException {
        String tenantId = SessionManager.getTenantAtual().getId();
        
        // 1. Gera as chaves usando o componente nativo do com.warrenstrange
        GoogleAuthenticatorKey credentials = gAuth.createCredentials();
        String novoSegredo = credentials.getKey();

        // 2. O próprio GoogleAuth cria o link oficial otpauth:// mapeado e seguro
        String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(ISSUER, emailUsuario, credentials);

        // 3. Salva no banco de dados participando da conexão atual do TransactionManager
        try (Connection conn = TransactionManager.getConnection()) {
            repository.salvarNovoSegredo(conn, usuarioId, tenantId, novoSegredo);
        }

        return new MfaConfigDTO(usuarioId, novoSegredo, qrCodeUrl, false);
    }

    /**
     * Passo 2: O usuário digita o token numérico de 6 dígitos exibido no celular.
     * Se for válido, mudamos ativo para 1.
     */
    public boolean verificarEAtivarMfa(Long usuarioId, int codigoDigitado) throws SQLException {
        try (Connection conn = TransactionManager.getConnection()) {
            var mfaOpt = repository.buscarPorUsuario(conn, usuarioId);
            
            if (mfaOpt.isEmpty()) {
                throw new IllegalStateException("Nenhuma configuração de MFA inicializada para este usuário.");
            }

            MfaConfigDTO config = mfaOpt.get();
            
            // 🌟 Validação oficial em tempo real usando a biblioteca do Google
            boolean codigoValido = gAuth.authorize(config.secretBase32(), codigoDigitado);

            if (codigoValido) {
                repository.confirmarAtivacao(conn, usuarioId);
                return true;
            }

            return false;
        }
    }

    /**
     * Desativa e remove completamente o registro de MFA do usuário
     */
    public void desativarMfa(Long usuarioId) throws SQLException {
        try (Connection conn = TransactionManager.getConnection()) {
            repository.deletarMfa(conn, usuarioId);
        }
    }

    /**
     * Verifica se o MFA está ativo (Muito útil para interceptar o fluxo de login posteriormente)
     */
    public boolean isMfaAtivo(Long usuarioId) throws SQLException {
        try (Connection conn = TransactionManager.getConnection()) {
            return repository.buscarPorUsuario(conn, usuarioId)
                    .map(MfaConfigDTO::ativo)
                    .orElse(false);
        }
    }
}