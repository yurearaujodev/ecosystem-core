package br.com.yat.ecosystemcore.modules.sistema.service;

import br.com.yat.ecosystemcore.application.system.dto.*;
import br.com.yat.ecosystemcore.modules.sistema.repository.SistemaMonitoramentoRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class GestaoSistemaUseCase {

    private final SistemaMonitoramentoRepository repository = new SistemaMonitoramentoRepository();

    public List<SistemaConfigDTO> obterConfiguracoesGlobais() throws SQLException {
        return repository.listarConfiguracoes();
    }

    public void salvarConfiguracao(String chave, String novoValor) throws SQLException {
        if (chave == null || chave.isBlank()) throw new IllegalArgumentException("Chave inválida.");
        repository.atualizarConfiguracao(chave, novoValor);
    }

    public List<JobExecucaoDTO> obterJobsAgendados() throws SQLException {
        return repository.listarJobsAgendados();
    }

    public List<OutboxEventDTO> obterEventosOutbox(Integer status) throws SQLException {
        return repository.listarEventosOutbox(status);
    }

    public List<SchemaVersionDTO> obterHistoricoDoSchema() throws SQLException {
        return repository.listarVersoesSchema();
    }

    // ⚡ ROTINA: Forçar reprocessamento de evento Outbox que falhou
    public void refazerDisparoOutbox(UUID eventoId) throws SQLException {
        // Reseta o contador de tentativas e devolve o status para PENDING
        String sql = "UPDATE outbox_events SET status = 'PENDING', tentativas = 0, processado_em = NULL WHERE id = ?";
        try (var conn = br.com.yat.ecosystemcore.shared.database.ConnectionFactory.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, eventoId.toString());
            stmt.executeUpdate();
        }
    }
    
    // ⚡ ROTINA: Trigger manual para executar um Job de Background imediatamente
    public void dispararJobAgora(String nomeJob, String grupo) throws SQLException {
        // Atualiza o status na tabela de controle para sinalizar ao Executor que rode a rotina imediatamente
        String sql = "UPDATE sistema_jobs_controle SET proxima_execucao = CURRENT_TIMESTAMP, status = 'RUNNING' WHERE job_name = ? AND job_group = ?";
        try (var conn = br.com.yat.ecosystemcore.shared.database.ConnectionFactory.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nomeJob);
            stmt.setString(2, grupo);
            stmt.executeUpdate();
            
            // Dica: Se você usa o Quartz Scheduler, aqui você chamaria: scheduler.triggerJob(JobKey.jobKey(nomeJob, grupo));
        }
    }
}
