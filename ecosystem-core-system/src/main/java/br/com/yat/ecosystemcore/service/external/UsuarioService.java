package br.com.yat.ecosystemcore.service.external;

import br.com.yat.ecosystemcore.domain.entity.EmpresaUsuarioDetalheDTO;
import br.com.yat.ecosystemcore.domain.entity.Usuario;
import br.com.yat.ecosystemcore.infrastructure.database.TransactionManager;
import br.com.yat.ecosystemcore.repository.perfil.EmpresaUsuarioRepository;
import br.com.yat.ecosystemcore.repository.usuario.UsuarioRepository;
import br.com.yat.ecosystemcore.repository.usuario.UsuarioSegurancaConfigRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class UsuarioService {

    private final UsuarioRepository usuarioRepository = new UsuarioRepository();
    private final EmpresaUsuarioRepository empresaUsuarioRepository = new EmpresaUsuarioRepository();
    private final UsuarioSegurancaConfigRepository segurancaConfigRepository = new UsuarioSegurancaConfigRepository();
    private final EmpresaUsuarioService empresaUsuarioService = new EmpresaUsuarioService();

    public void salvarUsuarioCompleto(Usuario usuario, List<EmpresaUsuarioDetalheDTO> vinculos, Long usuarioLogadoId) throws SQLException {
        if (usuario.getEmail() == null || usuario.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("O e-mail corporativo é obrigatório.");
        }

        TransactionManager.executeInTransaction(conn -> {
            boolean ehNovo = (usuario.getId() == null);
            
            if (ehNovo) {
                if (usuario.getUuidPublico() == null) {
                    usuario.setUuidPublico(UUID.randomUUID().toString());
                }
                Long novoId = usuarioRepository.insert(conn, usuario, usuarioLogadoId);
                usuario.setId(novoId);
                segurancaConfigRepository.inserirPadrao(conn, novoId, usuario.getTenantId());
            } else {
                usuarioRepository.update(conn, usuario, usuarioLogadoId);
            }

            empresaUsuarioService.sincronizarVinculos(usuario.getId(), vinculos);

            return null;
        });
    }

    /**
     * ⚡ OTIMIZADO: Apenas busca a conexão contextual sem forçar abertura de blocos transacionais pesados
     */
    public List<Usuario> listarTodos(String tenantId) throws SQLException {
        try (Connection conn = TransactionManager.getConnection()) {
            return usuarioRepository.findAll(conn, tenantId);
        }
    }

    public void deletarUsuario(Long id, String tenantId, Long usuarioLogadoId) throws SQLException {
        TransactionManager.executeInTransaction(conn -> {
            boolean deletado = usuarioRepository.softDeleteComUsuario(conn, id, tenantId, usuarioLogadoId);
            if (!deletado) {
                throw new SQLException("Não foi possível excluir o usuário solicitado.");
            }
            return null;
        });
    }
    
    /**
     * ⚡ OTIMIZADO: Ajustado para o padrão limpo de consulta
     */
    public List<EmpresaUsuarioDetalheDTO> listarVinculosEmpresa(Long usuarioId, String tenantId) throws SQLException {
        try (Connection conn = TransactionManager.getConnection()) {
            return empresaUsuarioRepository.findVinculosPorUsuario(conn, usuarioId, tenantId);
        }
    }
}