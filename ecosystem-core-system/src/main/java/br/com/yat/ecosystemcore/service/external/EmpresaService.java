package br.com.yat.ecosystemcore.service.external;

import br.com.yat.ecosystemcore.domain.entity.Empresa;
import br.com.yat.ecosystemcore.repository.empresa.EmpresaRepository;
import br.com.yat.ecosystemcore.infrastructure.database.ConnectionFactory; // Ajuste para o seu pacote de conexão
import br.com.yat.ecosystemcore.infrastructure.security.SessionManager;     // Ajuste para o seu gerenciador de sessão

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.ConcurrentModificationException;

public class EmpresaService {

    private final EmpresaRepository repository = new EmpresaRepository();

    public List<Empresa> listarEmpresasDoTenantAtivo() {
        String tenantId = SessionManager.getTenantAtual().getId();
        try (Connection conn = ConnectionFactory.getConnection()) {
            return repository.listarPorTenant(conn, tenantId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao carregar lista de empresas.", e);
        }
    }

    public void salvarEmpresa(Empresa empresa) {
        String tenantId = SessionManager.getTenantAtual().getId();
        empresa.setTenantId(tenantId);
        
        // Validações Básicas de Regra de Negócio
        if (empresa.getCnpj() == null || empresa.getCnpj().trim().length() != 14) {
            throw new IllegalArgumentException("O CNPJ deve conter exatamente 14 dígitos numéricos.");
        }
        if (empresa.getRazaoSocial() == null || empresa.getRazaoSocial().trim().isEmpty()) {
            throw new IllegalArgumentException("A Razão Social é obrigatória.");
        }

        try (Connection conn = ConnectionFactory.getConnection()) {
            if (empresa.getId() == null) {
                Long idGerado = repository.insert(conn, empresa);
                empresa.setId(idGerado);
            } else {
                boolean atualizado = repository.update(conn, empresa);
                if (!atualizado) {
                    throw new ConcurrentModificationException("Não foi possível atualizar a empresa. Verifique se os dados foram modificados por outro usuário.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha operacional ao persistir os dados da empresa.", e);
        }
    }

    public void excluirEmpresa(Long id) {
        String tenantId = SessionManager.getTenantAtual().getId();
        Long usuarioId = SessionManager.getUsuarioLogado().getId();

        try (Connection conn = ConnectionFactory.getConnection()) {
            boolean deletado = repository.softDelete(conn, id, tenantId, usuarioId);
            if (!deletado) {
                throw new IllegalStateException("O registro já foi removido ou não pertence ao escopo da conta ativa.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao remover a empresa de forma lógica.", e);
        }
    }
    
    public Empresa buscarPorId(Long id) {
        try (Connection conn = ConnectionFactory.getConnection()) {
            return repository.findById(conn, id)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada com o ID: " + id));
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar detalhes da empresa.", e);
        }
    }
}
