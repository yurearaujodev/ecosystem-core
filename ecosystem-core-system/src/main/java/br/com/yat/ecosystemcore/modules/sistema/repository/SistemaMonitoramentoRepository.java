package br.com.yat.ecosystemcore.modules.sistema.repository;

import br.com.yat.ecosystemcore.application.system.dto.*;
import br.com.yat.ecosystemcore.shared.database.ConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SistemaMonitoramentoRepository {

	public List<SistemaConfigDTO> listarConfiguracoes() throws SQLException {
		String sql = "SELECT chave, valor_config, descricao FROM sistema_config";
		List<SistemaConfigDTO> lista = new ArrayList<>();

		try (Connection conn = ConnectionFactory.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql);
				ResultSet rs = stmt.executeQuery()) {
			while (rs.next()) {
				lista.add(new SistemaConfigDTO(rs.getString("chave"), rs.getString("valor_config"),
						rs.getString("descricao")));
			}
		}
		return lista;
	}

	public void atualizarConfiguracao(String chave, String novoValor) throws SQLException {
		String sql = "UPDATE sistema_config SET valor_config = ? WHERE chave = ?";
		try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, novoValor);
			stmt.setString(2, chave);
			stmt.executeUpdate();
		}
	}

	public List<JobExecucaoDTO> listarJobsAgendados() throws SQLException {
		String sql = "SELECT id, tipo_job, status, inicio, fim, erro_mensagem FROM job_execucao ORDER BY inicio DESC LIMIT 50";
		List<JobExecucaoDTO> lista = new ArrayList<>();

		try (Connection conn = ConnectionFactory.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql);
				ResultSet rs = stmt.executeQuery()) {
			while (rs.next()) {
				lista.add(new JobExecucaoDTO(rs.getLong("id"), rs.getString("tipo_job"), rs.getString("status"),
						rs.getTimestamp("inicio") != null ? rs.getTimestamp("inicio").toLocalDateTime() : null,
						rs.getTimestamp("fim") != null ? rs.getTimestamp("fim").toLocalDateTime() : null,
						rs.getString("erro_mensagem")));
			}
		}
		return lista;
	}

	public List<OutboxEventDTO> listarEventosOutbox(Integer status) throws SQLException {
		String sql = "SELECT id, tenant_id, evento_tipo, payload, processado, criado_em FROM outbox_event WHERE processado = ?";
		List<OutboxEventDTO> lista = new ArrayList<>();

		try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setInt(1, status);

			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					lista.add(new OutboxEventDTO(rs.getLong("id"), rs.getString("tenant_id"),
							rs.getString("evento_tipo"), rs.getString("payload"), rs.getInt("processado"),
							rs.getTimestamp("criado_em").toLocalDateTime()));
				}
			}
		}
		return lista;
	}

	public List<SchemaVersionDTO> listarVersoesSchema() throws SQLException {
		String sql = "SELECT id, versao, descricao, executado_por, created_at FROM schema_version ORDER BY created_at DESC";
		List<SchemaVersionDTO> lista = new ArrayList<>();

		try (Connection conn = ConnectionFactory.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql);
				ResultSet rs = stmt.executeQuery()) {
			while (rs.next()) {
				lista.add(new SchemaVersionDTO(rs.getLong("id"), rs.getString("versao"), rs.getString("descricao"),
						rs.getString("executado_por"), rs.getTimestamp("created_at").toLocalDateTime()));
			}
		}
		return lista;
	}
}