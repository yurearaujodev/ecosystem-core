package br.com.yat.ecosystemcore.service.external;

import br.com.yat.ecosystemcore.domain.entity.EmpresaUsuarioDetalheDTO;
import br.com.yat.ecosystemcore.domain.entity.Usuario;
import br.com.yat.ecosystemcore.infrastructure.database.TransactionManager;
import br.com.yat.ecosystemcore.repository.perfil.EmpresaUsuarioRepository;
import br.com.yat.ecosystemcore.repository.perfil.PerfilRepository;
import br.com.yat.ecosystemcore.repository.usuario.UsuarioRepository;
import br.com.yat.ecosystemcore.repository.usuario.UsuarioSegurancaConfigRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class UsuarioService {

    private final UsuarioRepository usuarioRepository = new UsuarioRepository();
    private final PerfilRepository perfilRepository = new PerfilRepository();
    private final EmpresaUsuarioRepository empresaUsuarioRepository = new EmpresaUsuarioRepository();
    private final UsuarioSegurancaConfigRepository segurancaConfigRepository = new UsuarioSegurancaConfigRepository();

    public void salvarUsuarioCompleto(Usuario usuario, Long empresaId, Long usuarioLogadoId) throws SQLException {
        if (usuario.getEmail() == null || usuario.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("O e-mail corporativo é obrigatório.");
        }
        if (usuario.getPessoaId() == null) {
            throw new IllegalArgumentException("O usuário precisa estar vinculado a uma Pessoa válida.");
        }

        TransactionManager.executeInTransaction(conn -> {
            if (usuario.getId() == null) {
                if (usuario.getUuidPublico() == null || usuario.getUuidPublico().isEmpty()) {
                    usuario.setUuidPublico(UUID.randomUUID().toString());
                }
                
                // 1. Insere o Usuário
                Long novoId = usuarioRepository.insert(conn, usuario, usuarioLogadoId);
                usuario.setId(novoId);

                // 2. Cria configurações de segurança no Bloco 6
                segurancaConfigRepository.inserirPadrao(conn, novoId, usuario.getTenantId());

                // 3. Garante que o Perfil ADMIN do Tenant exista e recupera o ID
                Long perfilId = perfilRepository.criarPerfilAdminSeNecessario(conn, usuario.getTenantId());

                // 4. Cria o vínculo Multiempresa (empresa_usuario) obrigatório do Bloco 4
                if (empresaId != null) {
                    empresaUsuarioRepository.vincular(conn, usuario.getTenantId(), empresaId, novoId, perfilId);
                }
            } else {
                usuarioRepository.update(conn, usuario, usuarioLogadoId);
            }
        });
    }

    public List<Usuario> listarTodos(String tenantId) throws SQLException {
        AtomicReference<List<Usuario>> resultado = new AtomicReference<>();
        TransactionManager.executeInTransaction(conn -> {
            resultado.set(usuarioRepository.findAll(conn, tenantId));
        });
        return resultado.get();
    }

    public void deletarUsuario(Long id, String tenantId, Long usuarioLogadoId) throws SQLException {
        TransactionManager.executeInTransaction(conn -> {
            boolean deletado = usuarioRepository.softDeleteComUsuario(conn, id, tenantId, usuarioLogadoId);
            if (!deletado) {
                throw new SQLException("Não foi possível excluir o usuário solicitado.");
            }
        });
    }
    
    public List<EmpresaUsuarioDetalheDTO> listarVinculosEmpresa(Long usuarioId, String tenantId) throws SQLException {
        java.util.concurrent.atomic.AtomicReference<List<EmpresaUsuarioDetalheDTO>> resultado = new java.util.concurrent.atomic.AtomicReference<>();
        TransactionManager.executeInTransaction(conn -> {
            resultado.set(empresaUsuarioRepository.findVinculosPorUsuario(conn, usuarioId, tenantId));
        });
        return resultado.get();
    }
    
    public List<EmpresaUsuarioDetalheDTO> listarEmpresasParaUsuario(Long usuarioId, String tenantId) throws SQLException {
        return empresaUsuarioRepository.findVinculosPorUsuario(null, usuarioId, tenantId);
    }
}