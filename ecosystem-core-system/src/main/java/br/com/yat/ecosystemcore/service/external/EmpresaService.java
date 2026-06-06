package br.com.yat.ecosystemcore.service.external;

import br.com.yat.ecosystemcore.domain.entity.Empresa;
import br.com.yat.ecosystemcore.repository.empresa.EmpresaRepository;
import br.com.yat.ecosystemcore.infrastructure.database.TransactionManager;
import br.com.yat.ecosystemcore.infrastructure.security.SessionScope;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.ConcurrentModificationException;

public class EmpresaService {

    private final EmpresaRepository repository = new EmpresaRepository();

    public List<Empresa> listarEmpresasDoTenantAtivo() {
        // 🔒 ATUALIZADO: Uso do SessionScope estável da nova arquitetura
        if (SessionScope.tenant() == null) {
            throw new IllegalStateException("Nenhum tenant ativo na sessão para listar empresas.");
        }
        
        String tenantId = SessionScope.tenant().getId();
        
        // ⚡ INTEGRADO: Usa o TransactionManager para obter a conexão contextual da Thread
        try (Connection conn = TransactionManager.getConnection()) {
            return repository.listarPorTenant(conn, tenantId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao carregar lista de empresas.", e);
        }
    }

    public void salvarEmpresa(Empresa empresa) {
        // 🔒 ATUALIZADO: Validação e captura do escopo de segurança novo
        if (SessionScope.usuario() == null || SessionScope.tenant() == null) {
            throw new IllegalStateException("Usuário ou Tenant não identificados na sessão ativa.");
        }

        Long userId = SessionScope.usuario().getId();
        empresa.setTenantId(SessionScope.tenant().getId());
        
        // Validações Básicas de Regra de Negócio
        if (empresa.getCnpj() == null || empresa.getCnpj().trim().length() != 14) {
            throw new IllegalArgumentException("O CNPJ deve conter exatamente 14 dígitos numéricos.");
        }
        if (empresa.getRazaoSocial() == null || empresa.getRazaoSocial().trim().isEmpty()) {
            throw new IllegalArgumentException("A Razão Social é obrigatória.");
        }

        // ⚡ INTEGRADO: Transacional nativo acoplado à conexão gerenciada da thread
        try (Connection conn = TransactionManager.getConnection()) {
            if (empresa.getId() == null) {
                empresa.setCreatedBy(userId);
                Long id = repository.insert(conn, empresa);
                empresa.setId(id);
            } else {
                empresa.setUpdatedBy(userId);
                if (!repository.update(conn, empresa)) {
                    throw new ConcurrentModificationException("Erro de concorrência ao atualizar dados da empresa.");
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) throw new RuntimeException("CNPJ já cadastrado.");
            throw new RuntimeException("Erro ao salvar dados da empresa.", e);
        }
    }

    public void excluirEmpresa(Long id) {
        // 🔒 ATUALIZADO: Uso do SessionScope estável
        if (SessionScope.usuario() == null || SessionScope.tenant() == null) {
            throw new IllegalStateException("Sessão inválida para processar a exclusão.");
        }

        String tenantId = SessionScope.tenant().getId();
        Long usuarioId = SessionScope.usuario().getId();

        // ⚡ INTEGRADO: Contexto de conexão limpo e gerenciado
        try (Connection conn = TransactionManager.getConnection()) {
            boolean deletado = repository.softDelete(conn, id, tenantId, usuarioId);
            if (!deletado) {
                throw new IllegalStateException("O registro já foi removido ou não pertence ao escopo da conta ativa.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao remover a empresa de forma lógica.", e);
        }
    }
    
    public Empresa buscarPorId(Long id) {
        // ⚡ INTEGRADO: Conexão segura gerenciada
        try (Connection conn = TransactionManager.getConnection()) {
            return repository.findById(conn, id)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada com o ID: " + id));
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar detalhes da empresa.", e);
        }
    }
}