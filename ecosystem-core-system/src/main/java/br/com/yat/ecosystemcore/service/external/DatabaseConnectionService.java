package br.com.yat.ecosystemcore.service.external;

import br.com.yat.ecosystemcore.domain.entity.DatabaseConfig;
import br.com.yat.ecosystemcore.domain.entity.DatabaseCredentials;
import br.com.yat.ecosystemcore.shared.database.ConnectionFactory;
import br.com.yat.ecosystemcore.shared.database.DatabaseStatus;

public class DatabaseConnectionService {

    public DatabaseStatus testarConexaoDiretaSemSalvar(DatabaseConfig config, DatabaseCredentials credentials) {
        if (config == null || credentials == null) return DatabaseStatus.error("Parâmetros inválidos.");
        
        try {
            return ConnectionFactory.testRawConnection(config, credentials); 
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return DatabaseStatus.error("Erro de comunicação: " + msg);
        }
    }
}
