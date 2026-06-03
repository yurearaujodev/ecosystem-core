package br.com.yat.ecosystemcore.application.usuario;

import br.com.yat.ecosystemcore.application.usuario.dto.SessaoDTO;
import br.com.yat.ecosystemcore.domain.entity.*;
import br.com.yat.ecosystemcore.infrastructure.database.TransactionManager;
import br.com.yat.ecosystemcore.repository.empresa.EmpresaRepository;
import br.com.yat.ecosystemcore.repository.tenant.TenantRepository;
import br.com.yat.ecosystemcore.repository.usuario.SessaoUsuarioRepository;
import br.com.yat.ecosystemcore.repository.usuario.UsuarioRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class AutenticacaoUseCase {

    private final UsuarioRepository usuarioRepository = new UsuarioRepository();
    private final TenantRepository tenantRepository = new TenantRepository();
    private final EmpresaRepository empresaRepository = new EmpresaRepository();
    private final SessaoUsuarioRepository sessaoUsuarioRepository = new SessaoUsuarioRepository();

    private final PasswordEncoder passwordEncoder;

    public AutenticacaoUseCase(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public SessaoDTO autenticar(String email, String senhaPura) throws SQLException {

        final SessaoDTO[] resposta = new SessaoDTO[1];

        TransactionManager.executeInTransaction(conn -> {

            Usuario usuario = usuarioRepository.findByEmail(conn, email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário ou senha incorretos."));

            if (!passwordEncoder.matches(senhaPura, usuario.getSenhaHash())) {
                throw new IllegalArgumentException("Usuário ou senha incorretos.");
            }

            Tenant tenant = tenantRepository.findTenantPorIdSemTenantId(conn, usuario.getTenantId())
                .orElseThrow(() -> new SQLException("Tenant não encontrado."));

            Empresa empresa = empresaRepository.findEmpresaPorIdSemTenantId(conn, usuario.getEmpresaPadraoId())
                .orElseThrow(() -> new SQLException("Empresa não encontrada."));

            SessaoUsuario sessao = new SessaoUsuario();

            String token64 =
                    (UUID.randomUUID().toString() + UUID.randomUUID().toString())
                            .replace("-", "")
                            .substring(0, 64);

            sessao.setId(token64);
            sessao.setTenantId(usuario.getTenantId());
            sessao.setUsuarioId(usuario.getId());
            sessao.setEmpresaAtivaId(usuario.getEmpresaPadraoId());
            sessao.setExpiraEm(LocalDateTime.now().plusHours(8));

            sessaoUsuarioRepository.insert(conn, sessao);

            resposta[0] = new SessaoDTO(usuario, tenant, empresa);
        });

        return resposta[0];
    }
}