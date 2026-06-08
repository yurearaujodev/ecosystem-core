package br.com.yat.ecosystemcore.service.external;

import br.com.yat.ecosystemcore.domain.dto.SessaoAtivaProjecaoDTO;
import br.com.yat.ecosystemcore.domain.entity.TentativaLoginLog;
import br.com.yat.ecosystemcore.repository.usuario.SessaoUsuarioRepository;
import br.com.yat.ecosystemcore.shared.context.SessionScope;
import br.com.yat.ecosystemcore.shared.database.TransactionManager;
import br.com.yat.ecosystemcore.repository.usuario.SegurancaAuditoriaRepository;

import java.sql.SQLException;
import java.util.List;

public class AuditoriaSessaoService {

    private final SessaoUsuarioRepository sessaoRepository = new SessaoUsuarioRepository();
    private final SegurancaAuditoriaRepository auditoriaRepository = new SegurancaAuditoriaRepository();

    /**
     * Obtém todas as sessões abertas no momento filtradas pelo Tenant do Admin logado.
     */
    public List<SessaoAtivaProjecaoDTO> obterSessoesAtivas() throws SQLException {
        String tenantId = SessionScope.tenant().getId();
        return TransactionManager.executeInTransaction(conn -> 
            sessaoRepository.listarSessoesAtivasPorTenant(conn, tenantId)
        );
    }

    /**
     * Resgata as últimas tentativas de login para avaliar anomalias visuais ou brute-force.
     */
    public List<TentativaLoginLog> obterHistoricoTentativas() throws SQLException {
        String tenantId = SessionScope.tenant().getId();
        return TransactionManager.executeInTransaction(conn -> 
            auditoriaRepository.buscarLogsRecentes(conn, tenantId, 50)
        );
    }

    /**
     * Executa o comando "Derrubar Sessão" matando o registro via banco de dados.
     */
    public void derrubarSessao(String sessaoId) throws SQLException {
        TransactionManager.executeVoidInTransaction(conn -> {
            sessaoRepository.revoke(conn, sessaoId);
        });
    }

    /**
     * Remove a confiança atribuída a uma máquina ou terminal Desktop específico.
     */
    public void revogarDispositivo(Long dispositivoId) throws SQLException {
        String tenantId = SessionScope.tenant().getId();
        TransactionManager.executeVoidInTransaction(conn -> {
            auditoriaRepository.revogarDispositivoConfiavel(conn, dispositivoId, tenantId);
        });
    }
}