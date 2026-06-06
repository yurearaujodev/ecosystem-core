package br.com.yat.ecosystemcore.service.external;

import br.com.yat.ecosystemcore.domain.entity.Permissao;
import br.com.yat.ecosystemcore.infrastructure.database.TransactionManager;
import br.com.yat.ecosystemcore.infrastructure.security.Sessao;
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
     * Retorna todas as permissões disponíveis (Globais + específicas da Organização logada).
     * ⚡ ATUALIZADO: Captura automática do Tenant via Sessão corporativa ativa.
     */
    public List<Permissao> listarPermissoesDisponiveis() throws SQLException {
        if (!Sessao.isActive()) {
            throw new IllegalStateException("Nenhum contexto de sessão ativa detectado para listar permissões.");
        }
        
        String tenantId = Sessao.tenant().getId();

        try (Connection conn = TransactionManager.getConnection()) {
            return permissaoRepository.listarPorTenantOuGlobais(conn, tenantId);
        }
    }

    /**
     * Coleta todos os IDs de permissões associados a um determinado perfil.
     * ⚡ OTIMIZADO: Uso de conexão direta para leitura rápida na Thread do JavaFX.
     */
    public List<Long> obterIdsPermissoesDoPerfil(Long perfilId) throws SQLException {
        if (perfilId == null) {
            throw new IllegalArgumentException("ID do Perfil não pode ser nulo.");
        }
        try (Connection conn = TransactionManager.getConnection()) {
            return perfilPermissaoRepository.listarIdsPermissoesPorPerfil(conn, perfilId);
        }
    }

    /**
     * Vincula um lote de permissões a um Perfil corporativo de forma atômica.
     */
    public void salvarPermissoesDoPerfil(Long perfilId, List<Long> idsPermissoes) throws SQLException {
        if (perfilId == null) {
            throw new IllegalArgumentException("ID do Perfil é obrigatório para salvar o vínculo.");
        }
        if (!Sessao.isActive()) {
            throw new IllegalStateException("Sessão inválida para alteração de privilégios.");
        }
        
        TransactionManager.executeVoidInTransaction(conn -> {
            perfilPermissaoRepository.atualizarVinculos(conn, perfilId, idsPermissoes);
        });
    }

    /**
     * Salva as permissões customizadas (Direct ACL) na conta de um usuário específico.
     */
    public void salvarPermissoesDoUsuario(Long usuarioId, List<Long> idsPermissoes) throws SQLException {
        if (usuarioId == null) {
            throw new IllegalArgumentException("ID do Usuário é obrigatório.");
        }
        if (!Sessao.isActive()) {
            throw new IllegalStateException("Operação rejeitada: Sem credenciais de sessão válidas.");
        }
        
        TransactionManager.executeVoidInTransaction(conn -> {
            usuarioPermissaoRepository.atualizarVinculosUsuario(conn, usuarioId, idsPermissoes);
        });
    }

    /**
     * Coleta as permissões customizadas diretas do usuário (Overridden Permissions).
     */
    public List<Long> obterIdsPermissoesDoUsuario(Long usuarioId) throws SQLException {
        if (usuarioId == null) {
            throw new IllegalArgumentException("ID do Usuário inválido.");
        }
        try (Connection conn = TransactionManager.getConnection()) {
            return usuarioPermissaoRepository.listarIdsPermissoesPorUsuario(conn, usuarioId);
        }
    }

    /**
     * Cria uma nova permissão customizada dentro do ecossistema, isolada por Tenant.
     * ⚡ ATUALIZADO: Injeta e valida automaticamente o Tenant da sessão para evitar vazamento.
     */
    public void cadastrarNovaPermissao(Permissao permissao) throws SQLException {
        if (!Sessao.isActive()) {
            throw new IllegalStateException("Operação negada: Sessão expirada ou inválida.");
        }
        
        if (permissao.getModulo() == null || permissao.getModulo().isBlank() ||
            permissao.getAcao() == null || permissao.getAcao().isBlank()) {
            throw new IllegalArgumentException("Módulo e Ação são obrigatórios para registrar uma permissão.");
        }

        String tenantId = Sessao.tenant().getId();

        TransactionManager.executeVoidInTransaction(conn -> {
            // Garante que o módulo/ação fique em caixa alta para padronização de segurança
            String modulo = permissao.getModulo().toUpperCase().trim();
            String acao = permissao.getAcao().toUpperCase().trim();

            Optional<Permissao> existente = permissaoRepository.buscarPorChaveComposta(
                conn, tenantId, modulo, acao
            );
            
            if (existente.isPresent()) {
                throw new IllegalStateException("Já existe essa ação mapeada para este módulo neste Tenant.");
            }

            // Injeta os dados limpos baseados na sessão do usuário logado
            permissao.setTenantId(tenantId);
            permissao.setModulo(modulo);
            permissao.setAcao(acao);

            permissaoRepository.salvar(conn, permissao);
        });
    }
}