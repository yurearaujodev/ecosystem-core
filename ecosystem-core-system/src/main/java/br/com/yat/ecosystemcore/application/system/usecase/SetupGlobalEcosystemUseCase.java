package br.com.yat.ecosystemcore.application.system.usecase;

import br.com.yat.ecosystemcore.application.system.dto.SetupEcosystemCommand;
import br.com.yat.ecosystemcore.infrastructure.database.ConnectionFactory;
import br.com.yat.ecosystemcore.infrastructure.database.DatabaseMenuSeeder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SetupGlobalEcosystemUseCase {

    public void execute(SetupEcosystemCommand command) throws SQLException {
        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false); // Garante atomicidade: ou faz tudo ou desfaz tudo
            
            try {
                // 1. Insere as chaves mestras na tabela sistema_config
                String sqlConfig = "INSERT INTO sistema_config (chave, valor_config, descricao) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sqlConfig)) {
                    // Monta o JSON esperado pelo campo valor_config
                    String jsonPayload = String.format(
                        "{\"nome_plataforma\": \"%s\", \"ambiente\": \"%s\", \"versao_core\": \"%s\"}",
                        command.nomePlataforma(),
                        command.ambiente(),
                        command.versaoCore()
                    );
                    
                    stmt.setString(1, "CONFIG_GLOBAL_SISTEMA");
                    stmt.setString(2, jsonPayload);
                    stmt.setString(3, "Configurações de inicialização da plataforma gravadas pelo Desenvolvedor.");
                    stmt.executeUpdate();
                }

                // 2. Dispara a carga do DatabaseMenuSeeder que você escreveu usando a mesma conexão
                DatabaseMenuSeeder.inicializarCargaEstrutural(conn);

                // Se tudo rodar perfeitamente (Config + Seeder), confirma no banco
                conn.commit();
                
            } catch (SQLException e) {
                conn.rollback(); // Se falhar o seeder ou o insert, desfaz a operação para não quebrar o banco
                throw e;
            }
        }
    }
}