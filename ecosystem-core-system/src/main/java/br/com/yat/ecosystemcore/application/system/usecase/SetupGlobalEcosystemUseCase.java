package br.com.yat.ecosystemcore.application.system.usecase;

import br.com.yat.ecosystemcore.application.system.dto.SetupEcosystemCommand;
import br.com.yat.ecosystemcore.infrastructure.database.DatabaseMenuSeeder;
import br.com.yat.ecosystemcore.shared.database.ConnectionFactory;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class SetupGlobalEcosystemUseCase {

    public void execute(SetupEcosystemCommand command) throws SQLException {
        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false); // Transação Atômica Global (Garante o Tudo ou Nada)
            
            try {
                // 1. Gravação na tabela 'sistema_config'
                String sqlConfig = "INSERT INTO sistema_config (chave, valor_config, descricao) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sqlConfig)) {
                    String jsonPayload = String.format(
                        "{\"nome_plataforma\": \"%s\", \"ambiente\": \"%s\", \"versao_core\": \"%s\"}",
                        command.nomePlataforma(), command.ambiente(), command.versaoCore()
                    );
                    stmt.setString(1, "CONFIG_GLOBAL_SISTEMA");
                    stmt.setString(2, jsonPayload);
                    stmt.setString(3, "Configurações de inicialização da plataforma gravadas pelo Dono do Sistema.");
                    stmt.executeUpdate();
                }

                // 2. Carga estrutural de menus e permissões em lote (Nativo e Granular)
                DatabaseMenuSeeder.inicializarCargaEstrutural(conn);
                
                // UUID do Escopo Global fixado no banco de dados (Nil UUID)
                String tenantGlobalUuid = "00000000-0000-0000-0000-000000000000";

                // 3. Inserção na tabela 'empresa' (Garante campos obrigatórios)
                String sqlEmpresa = """
                    INSERT INTO empresa (uuid_publico, tenant_id, razao_social, nome_fantasia, cnpj, ativo) 
                    VALUES (?, ?, ?, ?, ?, 1)
                """;
                long empresaId = 0;
                try (PreparedStatement stmt = conn.prepareStatement(sqlEmpresa, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, tenantGlobalUuid);
                    stmt.setString(3, command.razaoSocial());
                    stmt.setString(4, command.nomeFantasia());
                    stmt.setString(5, command.cnpj());
                    stmt.executeUpdate();
                    
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            empresaId = rs.getLong(1);
                        }
                    }
                }

                // 4. Inserção na tabela 'pessoa'
                String sqlPessoa = "INSERT INTO pessoa (uuid_publico, tenant_id, tipo, nome_razao, ativo) VALUES (?, ?, 'FISICA', ?, 1)";
                long pessoaId = 0;
                try (PreparedStatement stmt = conn.prepareStatement(sqlPessoa, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, tenantGlobalUuid);
                    stmt.setString(3, command.nomeAdmin());
                    stmt.executeUpdate();
                    
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            pessoaId = rs.getLong(1);
                        }
                    }
                }

                // 5. Inserção na tabela 'usuario' (Atendendo às FKs compostas strict do seu DDL)
                String senhaHash = BCrypt.hashpw(command.senhaPura(), BCrypt.gensalt(12));
                String sqlUsuario = """
                    INSERT INTO usuario (uuid_publico, tenant_id, pessoa_id, empresa_padrao_id, email, senha_hash, status) 
                    VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')
                """;
                long usuarioId = 0;
                try (PreparedStatement stmt = conn.prepareStatement(sqlUsuario, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, tenantGlobalUuid);
                    stmt.setLong(3, pessoaId);
                    stmt.setLong(4, empresaId); // Vincula a empresa mestre como padrão do usuário
                    stmt.setString(5, command.emailAdmin());
                    stmt.setString(6, senhaHash);
                    stmt.executeUpdate();
                    
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            usuarioId = rs.getLong(1);
                        }
                    }
                }

                // 6. Inserção na tabela 'perfil' (Garante integridade da UNIQUE KEY de chave_identificadora)
                String sqlPerfil = """
                    INSERT INTO perfil (uuid_publico, tenant_id, nome, chave_identificadora, descricao) 
                    VALUES (?, ?, ?, ?, 'Perfil Mestre criado dinamicamente no Onboarding Inicial')
                """;
                long perfilId = 0;
                try (PreparedStatement stmt = conn.prepareStatement(sqlPerfil, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, tenantGlobalUuid);
                    stmt.setString(3, command.chavePerfil().toUpperCase());
                    stmt.setString(4, command.chavePerfil().toUpperCase());
                    stmt.executeUpdate();
                    
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            perfilId = rs.getLong(1);
                        }
                    }
                }

                // 💾 6.5. Vínculo automático de Segurança na tabela pivot 'perfil_permissao'
                // Este passo lê todas as linhas criadas no Passo 2 e as anexa ao perfil do Admin.
                String sqlVincularPermissoesMestre = """
                    INSERT INTO perfil_permissao (perfil_id, permissao_id)
                    SELECT ?, id FROM permissao WHERE tenant_id = ?
                    ON DUPLICATE KEY UPDATE perfil_id = perfil_id
                """;
                try (PreparedStatement stmtPerms = conn.prepareStatement(sqlVincularPermissoesMestre)) {
                    stmtPerms.setLong(1, perfilId);
                    stmtPerms.setString(2, tenantGlobalUuid);
                    stmtPerms.executeUpdate();
                }

                // 7. Gravação na tabela pivot 'empresa_usuario' (A consolidação definitiva do vínculo com escopo)
                String sqlVinculo = """
                    INSERT INTO empresa_usuario (tenant_id, empresa_id, usuario_id, perfil_id) 
                    VALUES (?, ?, ?, ?)
                """;
                try (PreparedStatement stmt = conn.prepareStatement(sqlVinculo)) {
                    stmt.setString(1, tenantGlobalUuid);
                    stmt.setLong(2, empresaId);
                    stmt.setLong(3, usuarioId);
                    stmt.setLong(4, perfilId);
                    stmt.executeUpdate();
                }

                // Se todas as operações foram bem-sucedidas, persistimos os dados de forma atômica
                conn.commit();
                
            } catch (SQLException e) {
                conn.rollback(); // Desfaz todas as inserções caso ocorra qualquer erro de integridade referencial
                throw e;
            }
        }
    }
}