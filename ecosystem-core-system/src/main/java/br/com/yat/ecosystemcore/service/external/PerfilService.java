package br.com.yat.ecosystemcore.service.external;

import br.com.yat.ecosystemcore.domain.entity.Perfil;
import br.com.yat.ecosystemcore.infrastructure.database.ConnectionFactory;
import br.com.yat.ecosystemcore.repository.perfil.PerfilRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PerfilService {

    private final PerfilRepository perfilRepository = new PerfilRepository();

    /**
     * Cria um novo perfil no sistema validando duplicidade de chave dentro do mesmo Tenant
     */
    public void cadastrarPerfil(Perfil perfil, Long usuarioCriadorId) throws SQLException {
        if (perfil.getNome() == null || perfil.getNome().isBlank()) {
            throw new IllegalArgumentException("O nome do perfil não pode estar vazio.");
        }
        if (perfil.getChaveIdentificadora() == null || perfil.getChaveIdentificadora().isBlank()) {
            throw new IllegalArgumentException("A chave identificadora é obrigatória.");
        }

        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Valida a Constraint de unicidade composta por Tenant
                Optional<Perfil> existente = perfilRepository.buscarPorChave(conn, perfil.getTenantId(), perfil.getChaveIdentificadora().toUpperCase());
                if (existente.isPresent()) {
                    throw new IllegalStateException("Já existe um perfil ativo com esta chave identificadora neste Tenant.");
                }

                perfil.setUuidPublico(UUID.randomUUID().toString());
                perfil.setChaveIdentificadora(perfil.getChaveIdentificadora().toUpperCase().trim());
                perfil.setCreatedBy(usuarioCriadorId);

                perfilRepository.salvar(conn, perfil);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Altera dados de um perfil existente respeitando o isolamento do Multi-Tenant
     */
    public void atualizarPerfil(Perfil perfil, Long usuarioModificadorId) throws SQLException {
        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                perfil.setUpdatedBy(usuarioModificadorId);
                perfilRepository.atualizar(conn, perfil);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Retorna todos os perfis ativos de uma determinada organização
     */
    public List<Perfil> listarPerfisPorTenant(String tenantId) throws SQLException {
        try (Connection conn = ConnectionFactory.getConnection()) {
            return perfilRepository.listarPorTenant(conn, tenantId);
        }
    }

    /**
     * Executa a remoção lógica (Soft Delete) salvando qual usuário realizou a ação
     */
    public void excluirPerfil(Long id, String tenantId, Long usuarioLogadoId) throws SQLException {
        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean deletado = perfilRepository.softDeleteComAuditoria(conn, id, tenantId, usuarioLogadoId);
                if (!deletado) {
                    throw new SQLException("Perfil não encontrado ou já excluído anteriormente.");
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
