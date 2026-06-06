package br.com.yat.ecosystemcore.service.external;

import br.com.yat.ecosystemcore.domain.entity.EmpresaUsuarioDetalheDTO;
import br.com.yat.ecosystemcore.domain.entity.Usuario;
import br.com.yat.ecosystemcore.infrastructure.database.TransactionManager;
import br.com.yat.ecosystemcore.infrastructure.security.Sessao;
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

    /**
     * Salva ou atualiza um usuário de forma atômica no banco, configurando as diretrizes
     * padrões de segurança e sincronizando os vínculos com as filiais da corporação.
     */
    public void salvarUsuarioCompleto(Usuario usuario, List<EmpresaUsuarioDetalheDTO> vinculos) throws SQLException {
        // 🔒 VALIDAÇÃO DE SEGURANÇA: Garante uma sessão ativa corporativa
        if (!Sessao.isActive()) {
            throw new IllegalStateException("Operação negada: Nenhuma sessão ativa detectada.");
        }

        if (usuario.getEmail() == null || usuario.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("O e-mail corporativo é obrigatório.");
        }

        // Captura automática do contexto logado na Thread
        String tenantId = Sessao.tenant().getId();
        Long usuarioLogadoId = Sessao.usuario().getId();

        // Força o isolamento por Tenant para o usuário alvo
        usuario.setTenantId(tenantId);

        // ⚡ OTIMIZADO: Utiliza executeVoidInTransaction para remover retornos manuais de 'null'
        TransactionManager.executeVoidInTransaction(conn -> {
            boolean ehNovo = (usuario.getId() == null);
            
            if (ehNovo) {
                if (usuario.getUuidPublico() == null || usuario.getUuidPublico().trim().isEmpty()) {
                    usuario.setUuidPublico(UUID.randomUUID().toString());
                }
                
                // Realiza a inserção do usuário principal
                Long novoId = usuarioRepository.insert(conn, usuario, usuarioLogadoId);
                usuario.setId(novoId);
                
                // Configurações automáticas de segurança do novo usuário (Injeta o tenantId coletado)
                segurancaConfigRepository.inserirPadrao(conn, novoId, tenantId);
            } else {
                // Atualização blindada por histórico de auditoria
                usuarioRepository.update(conn, usuario, usuarioLogadoId);
            }

            // Sincroniza as tabelas de junção n:n (Empresa <-> Usuário)
            // 💡 IMPORTANTE: Certifique-se de que a assinatura de 'sincronizarVinculos' aceita (Connection conn, ...)
            // para propagar a MESMA transação atômica. Se não aceitar, ajuste-a para receber o parâmetro 'conn'.
            empresaUsuarioService.sincronizarVinculos(conn, usuario.getId(), vinculos);
        });
    }

    /**
     * Lista todos os usuários ativos associados à organização atual.
     * ⚡ EXCELENTE: Consulta pura e limpa sem o overhead de blocos transacionais pesados.
     */
    public List<Usuario> listarTodos() throws SQLException {
        if (!Sessao.isActive()) {
            throw new IllegalStateException("Contexto corporativo inválido na sessão.");
        }

        String tenantId = Sessao.tenant().getId();

        try (Connection conn = TransactionManager.getConnection()) {
            return usuarioRepository.findAll(conn, tenantId);
        }
    }

    /**
     * Realiza a exclusão lógica (Soft Delete) do usuário mitigando acessos cross-tenant.
     */
    public void deletarUsuario(Long id) throws SQLException {
        if (!Sessao.isActive()) {
            throw new IllegalStateException("Sessão inválida para realizar a exclusão.");
        }

        String tenantId = Sessao.tenant().getId();
        Long usuarioLogadoId = Sessao.usuario().getId();

        // ⚡ CORRIGIDO: Redirecionado para o executor exclusivo Void
        TransactionManager.executeVoidInTransaction(conn -> {
            boolean deletado = usuarioRepository.softDeleteComUsuario(conn, id, tenantId, usuarioLogadoId);
            if (!deletado) {
                throw new SQLException("Não foi possível excluir o usuário: Registro inexistente ou de outra corporação.");
            }
        });
    }
    
    /**
     * Retorna a listagem detalhada de permissões e vínculos de filiais de um usuário específico.
     */
    public List<EmpresaUsuarioDetalheDTO> listarVinculosEmpresa(Long usuarioId) throws SQLException {
        if (!Sessao.isActive()) {
            throw new IllegalStateException("Impossível recuperar dados sem autenticação válida.");
        }

        String tenantId = Sessao.tenant().getId();

        try (Connection conn = TransactionManager.getConnection()) {
            return empresaUsuarioRepository.findVinculosPorUsuario(conn, usuarioId, tenantId);
        }
    }
}