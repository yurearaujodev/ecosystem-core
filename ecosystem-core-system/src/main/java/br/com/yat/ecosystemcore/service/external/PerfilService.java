package br.com.yat.ecosystemcore.service.external;

import br.com.yat.ecosystemcore.domain.entity.Perfil;
import br.com.yat.ecosystemcore.repository.perfil.PerfilRepository;
import br.com.yat.ecosystemcore.shared.database.TransactionManager;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PerfilService {

    private final PerfilRepository perfilRepository = new PerfilRepository();

    /**
     * Cria um novo perfil no sistema validando duplicidade de chave dentro do mesmo Tenant.
     * ⚡ ATUALIZADO: Agora utiliza o executeVoidInTransaction eliminando rollbacks manuais.
     */
    public void cadastrarPerfil(Perfil perfil, Long usuarioCriadorId) throws SQLException {
        if (perfil.getNome() == null || perfil.getNome().isBlank()) {
            throw new IllegalArgumentException("O nome do perfil não pode estar vazio.");
        }
        if (perfil.getChaveIdentificadora() == null || perfil.getChaveIdentificadora().isBlank()) {
            throw new IllegalArgumentException("A chave identificadora é obrigatória.");
        }

        // 🌟 Gerenciado de forma centralizada e segura pelo TransactionManager da Thread
        TransactionManager.executeVoidInTransaction(conn -> {
            
            // Valida a Constraint de unicidade composta por Tenant
            Optional<Perfil> existente = perfilRepository.buscarPorChave(
                conn, perfil.getTenantId(), perfil.getChaveIdentificadora().toUpperCase()
            );
            
            if (existente.isPresent()) {
                throw new IllegalStateException("Já existe um perfil ativo com esta chave identificadora neste Tenant.");
            }

            perfil.setUuidPublico(UUID.randomUUID().toString());
            perfil.setChaveIdentificadora(perfil.getChaveIdentificadora().toUpperCase().trim());
            perfil.setCreatedBy(usuarioCriadorId);

            perfilRepository.salvar(conn, perfil);
        });
    }

    /**
     * Altera dados de um perfil existente respeitando o isolamento do Multi-Tenant.
     * ⚡ ATUALIZADO: Transação limpa via ThreadLocal.
     */
    public void atualizarPerfil(Perfil perfil, Long usuarioModificadorId) throws SQLException {
        TransactionManager.executeVoidInTransaction(conn -> {
            perfil.setUpdatedBy(usuarioModificadorId);
            perfilRepository.atualizar(conn, perfil);
        });
    }

    /**
     * Retorna todos os perfis ativos de uma determinada organização.
     * ⚡ ATUALIZADO: Coleta a conexão contextual da Thread sem abrir conexões fantasmas.
     */
    public List<Perfil> listarPerfisPorTenant(String tenantId) throws SQLException {
        try (var conn = TransactionManager.getConnection()) {
            return perfilRepository.listarPorTenant(conn, tenantId);
        }
    }

    /**
     * Executa a remoção lógica (Soft Delete) salvando qual usuário realizou a ação.
     * ⚡ ATUALIZADO: Proteção atômica se o perfil não for encontrado.
     */
    public void excluirPerfil(Long id, String tenantId, Long usuarioLogadoId) throws SQLException {
        TransactionManager.executeVoidInTransaction(conn -> {
            boolean deletado = perfilRepository.softDeleteComAuditoria(conn, id, tenantId, usuarioLogadoId);
            if (!deletado) {
                throw new SQLException("Perfil não encontrado ou já excluído anteriormente.");
            }
        });
    }
}