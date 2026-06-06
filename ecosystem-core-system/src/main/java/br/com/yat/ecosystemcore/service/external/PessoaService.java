package br.com.yat.ecosystemcore.service.external;

import br.com.yat.ecosystemcore.domain.entity.Pessoa;
import br.com.yat.ecosystemcore.infrastructure.database.TransactionManager;
import br.com.yat.ecosystemcore.infrastructure.security.Sessao;
import br.com.yat.ecosystemcore.repository.usuario.PessoaRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class PessoaService {

    private final PessoaRepository pessoaRepository = new PessoaRepository();

    /**
     * Salva ou atualiza uma pessoa no sistema injetando e validando automaticamente 
     * o contexto de segurança e isolamento por Tenant da sessão ativa.
     */
    public void salvarPessoa(Pessoa pessoa) throws SQLException {
        // 🔒 VALIDAÇÃO DE SEGURANÇA: Garante uma sessão ativa corporativa
        if (!Sessao.isActive()) {
            throw new IllegalStateException("Operação negada: Nenhuma sessão ativa detectada.");
        }

        if (pessoa.getNomeRazao() == null || pessoa.getNomeRazao().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome ou razão social é obrigatório.");
        }
        if (pessoa.getTipo() == null || (!pessoa.getTipo().equals("FISICA") && !pessoa.getTipo().equals("JURIDICA"))) {
            throw new IllegalArgumentException("O tipo de pessoa deve ser FISICA ou JURIDICA.");
        }

        // Captura automática de auditoria e contexto corporativo
        String tenantId = Sessao.tenant().getId();
        Long usuarioLogadoId = Sessao.usuario().getId();

        // ⚡ CORRIGIDO: Utiliza o executeVoidInTransaction eliminando retornos "null" manuais
        TransactionManager.executeVoidInTransaction(conn -> {
            if (pessoa.getId() == null) {
                if (pessoa.getUuidPublico() == null || Math.max(0, pessoa.getUuidPublico().length()) == 0) {
                    pessoa.setUuidPublico(UUID.randomUUID().toString());
                }
                
                // Força a injeção do Tenant correto da sessão para evitar cross-tenant injection
                pessoa.setTenantId(tenantId);
                pessoa.setCreatedBy(usuarioLogadoId);
                
                Long novoId = pessoaRepository.insert(conn, pessoa);
                pessoa.setId(novoId);
            } else {
                // 🔒 Regra de integridade: Garante que o usuário não está tentando atualizar uma entidade de outro Tenant
                pessoa.setTenantId(tenantId);
                pessoa.setUpdatedBy(usuarioLogadoId);
                pessoaRepository.update(conn, pessoa);
            }
        });
    }

    /**
     * Lista todas as pessoas ativas pertencentes à organização logada.
     * ⚡ OTIMIZADO: Removido o bloco transacional de gravação por uma leitura direta e limpa.
     */
    public List<Pessoa> listarTodas() throws SQLException {
        if (!Sessao.isActive()) {
            throw new IllegalStateException("Contexto de organização inválido na sessão.");
        }

        String tenantId = Sessao.tenant().getId();
        
        // Operações de listagem pura usam a conexão transacional limpa sem overhead de transaction logs
        try (Connection conn = TransactionManager.getConnection()) {
            return pessoaRepository.findAll(conn, tenantId);
        }
    }
    
    /**
     * Executa a exclusão lógica (Soft Delete) de uma pessoa blindada por Tenant.
     */
    public void deletarPessoa(Long id) throws SQLException {
        if (!Sessao.isActive()) {
            throw new IllegalStateException("Sessão inválida para realizar a exclusão.");
        }

        String tenantId = Sessao.tenant().getId();
        Long usuarioLogadoId = Sessao.usuario().getId();

        // ⚡ CORRIGIDO: Redirecionado para o método Void apropriado
        TransactionManager.executeVoidInTransaction(conn -> {
            boolean deletado = pessoaRepository.softDeleteComUsuario(conn, id, tenantId, usuarioLogadoId);
            if (!deletado) {
                throw new SQLException("Registro não encontrado, já excluído ou pertence a outra organização corporativa.");
            }
        });
    }
}