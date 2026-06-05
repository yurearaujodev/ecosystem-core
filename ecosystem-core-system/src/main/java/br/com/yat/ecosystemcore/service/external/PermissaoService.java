package br.com.yat.ecosystemcore.service.external;

import br.com.yat.ecosystemcore.domain.entity.Permissao;
import br.com.yat.ecosystemcore.infrastructure.database.TransactionManager;
import br.com.yat.ecosystemcore.repository.perfil.PerfilPermissaoRepository;
import br.com.yat.ecosystemcore.repository.perfil.PermissaoRepository;
import br.com.yat.ecosystemcore.repository.perfil.UsuarioPermissaoRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class PermissaoService {

    private final PermissaoRepository permissaoRepository = new PermissaoRepository();
    private final PerfilPermissaoRepository perfilPermissaoRepository = new PerfilPermissaoRepository();
    private final UsuarioPermissaoRepository usuarioPermissaoRepository = new UsuarioPermissaoRepository();

    /**
     * ⚡ OTIMIZADO: Apenas busca a conexão sem abrir bloco transacional pesado
     */
    public List<Permissao> listarPermissoesDisponiveis(String tenantId) throws SQLException {
        try (Connection conn = TransactionManager.getConnection()) {
            return permissaoRepository.listarPorTenantOuGlobais(conn, tenantId);
        }
    }

    /**
     * ⚡ OTIMIZADO: Uso de conexão direta para leitura
     */
    public List<Long> obterIdsPermissoesDoPerfil(Long perfilId) throws SQLException {
        if (perfilId == null) throw new IllegalArgumentException("ID do Perfil não pode ser nulo.");
        try (Connection conn = TransactionManager.getConnection()) {
            return perfilPermissaoRepository.listarIdsPermissoesPorPerfil(conn, perfilId);
        }
    }

    /**
     * Vincula um lote de permissões a um Perfil.
     * 🌟 Usando o novo método void do TransactionManager (sem return null)
     */
    public void salvarPermissoesDoPerfil(Long perfilId, List<Long> idsPermissoes) throws SQLException {
        if (perfilId == null) throw new IllegalArgumentException("ID do Perfil é obrigatório para salvar o vínculo.");
        
        TransactionManager.executeInTransaction(conn -> {
            perfilPermissaoRepository.atualizarVinculos(conn, perfilId, idsPermissoes);
        });
    }

    /**
     * Salva as permissões customizadas diretas na conta de um usuário.
     * 🌟 Usando o novo método void do TransactionManager (sem return null)
     */
    public void salvarPermissoesDoUsuario(Long usuarioId, List<Long> idsPermissoes) throws SQLException {
        if (usuarioId == null) throw new IllegalArgumentException("ID do Usuário é obrigatório.");
        
        TransactionManager.executeInTransaction(conn -> {
            usuarioPermissaoRepository.atualizarVinculosUsuario(conn, usuarioId, idsPermissoes);
        });
    }

    /**
     * ⚡ OTIMIZADO: Uso de conexão direta para leitura
     */
    public List<Long> obterIdsPermissoesDoUsuario(Long usuarioId) throws SQLException {
        if (usuarioId == null) throw new IllegalArgumentException("ID do Usuário inválido.");
        try (Connection conn = TransactionManager.getConnection()) {
            return usuarioPermissaoRepository.listarIdsPermissoesPorUsuario(conn, usuarioId);
        }
    }

    /**
     * Cria uma nova permissão customizada.
     * 🌟 Usando o novo método void do TransactionManager (sem return null)
     */
    public void cadastrarNovaPermissao(Permissao permissao) throws SQLException {
        if (permissao.getModulo() == null || permissao.getModulo().isBlank() ||
            permissao.getAcao() == null || permissao.getAcao().isBlank()) {
            throw new IllegalArgumentException("Módulo e Ação são obrigatórios para registrar uma permissão.");
        }

        TransactionManager.executeInTransaction(conn -> {
            Optional<Permissao> existente = permissaoRepository.buscarPorChaveComposta(
                conn, permissao.getTenantId(), permissao.getModulo(), permissao.getAcao()
            );
            
            if (existente.isPresent()) {
                throw new IllegalStateException("Já existe essa ação mapeada para este módulo neste Tenant.");
            }

            permissaoRepository.salvar(conn, permissao);
        });
    }
}