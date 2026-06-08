package br.com.yat.ecosystemcore.service.external;

import br.com.yat.ecosystemcore.application.system.dto.MfaConfigDTO;
import br.com.yat.ecosystemcore.repository.perfil.UsuarioMfaRepository;
import br.com.yat.ecosystemcore.shared.context.SessionScope;
import br.com.yat.ecosystemcore.shared.database.TransactionManager;

import java.sql.Connection;
import java.sql.SQLException;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

public class MfaService {

    private final UsuarioMfaRepository repository = new UsuarioMfaRepository();
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    // Nome que vai aparecer no aplicativo do celular do usuário (Ex: YatEcosystem)
    private static final String ISSUER = "YatEcosystem";

    /**
     * Passo 1: Inicia a configuração do MFA gerando chaves oficiais da biblioteca GoogleAuth.
     * ⚡ ATUALIZADO: Agora usa SessionScope e garante atomicidade transacional na geração de chaves.
     */
    public MfaConfigDTO iniciarConfiguracaoMfa(Long usuarioId, String emailUsuario) throws SQLException {
        // 🔒 ATUALIZADO: Validação e captura do escopo de segurança novo
        if (SessionScope.tenant() == null) {
            throw new IllegalStateException("Nenhum tenant ativo na sessão para configurar o MFA.");
        }
        
        String tenantId = SessionScope.tenant().getId();
        
        // 1. Gera as chaves usando o componente nativo do com.warrenstrange
        GoogleAuthenticatorKey credentials = gAuth.createCredentials();
        String novoSegredo = credentials.getKey();

        // 🛡️ Sanitiza o email e monta a URL encodada manualmente.
        String emailSanitizado = emailUsuario != null ? emailUsuario.trim() : "usuario";
        
        String labelEncodado = java.net.URLEncoder.encode(ISSUER + ":" + emailSanitizado, java.nio.charset.StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20"); // Garante que espaços virem %20 e não +
                
        String issuerEncodado = java.net.URLEncoder.encode(ISSUER, java.nio.charset.StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        // Formato padrão mundial aceito por qualquer leitor:
        String qrCodeUrl = String.format(
            "otpauth://totp/%s?secret=%s&issuer=%s",
            labelEncodado,
            novoSegredo,
            issuerEncodado
        );

        // ⚡ INTEGRADO: Executa a gravação do novo segredo garantindo o commit da transação
        TransactionManager.executeVoidInTransaction(conn -> 
            repository.salvarNovoSegredo(conn, usuarioId, tenantId, novoSegredo)
        );

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
     * Desativa e remove completamente o registro de MFA do usuário.
     */
    public void desativarMfa(Long usuarioId) throws SQLException {
        try (Connection conn = TransactionManager.getConnection()) {
            repository.deletarMfa(conn, usuarioId);
        }
    }

    /**
     * Verifica se o MFA está ativo (Muito útil para interceptar o fluxo de login posteriormente).
     */
    public boolean isMfaAtivo(Long usuarioId) throws SQLException {
        try (Connection conn = TransactionManager.getConnection()) {
            return repository.buscarPorUsuario(conn, usuarioId)
                    .map(MfaConfigDTO::ativo)
                    .orElse(false);
        }
    }
}