package br.com.yat.ecosystemcore.application.usecase;

import javafx.concurrent.Task;
import java.util.function.Consumer;

import br.com.yat.ecosystemcore.domain.dto.DatabaseForm;
import br.com.yat.ecosystemcore.domain.entity.DatabaseConfig;
import br.com.yat.ecosystemcore.domain.entity.DatabaseCredentials;
import br.com.yat.ecosystemcore.service.external.DatabaseConnectionService;
import br.com.yat.ecosystemcore.service.external.DatabaseSetupService;
import br.com.yat.ecosystemcore.shared.current.AppExecutors;
import br.com.yat.ecosystemcore.shared.database.DatabaseStatus;

public final class TestarConexaoUseCase {
    private final DatabaseConnectionService connectionService = new DatabaseConnectionService();
    private final DatabaseSetupService setupService = new DatabaseSetupService();

    public void executar(DatabaseForm form, char[] senhaOrigemUI, 
                         Consumer<DatabaseStatus> onSuccess, Consumer<Throwable> onFailed) {
        
        Task<DatabaseStatus> task = new Task<>() {
            @Override
            protected DatabaseStatus call() throws Exception {
                DatabaseCredentials credentials;
                if (senhaOrigemUI != null && senhaOrigemUI.length > 0) {
                    credentials = new DatabaseCredentials(senhaOrigemUI);
                } else {
                    credentials = setupService.carregarCredenciaisExistentes();
                    if (credentials == null) {
                        throw new IllegalArgumentException("A senha do banco de dados não pode estar vazia.");
                    }
                }

                try (credentials) {
                    DatabaseConfig config = DatabaseConfig.criarAPartirDeFormulario(
                        form.ip(), form.porta(), form.banco(), form.usuario()
                    );
                    return connectionService.testarConexaoDiretaSemSalvar(config, credentials);
                }
            }
        };

        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onFailed.accept(task.getException()));
        AppExecutors.getDatabaseExecutor().submit(task);
    }
}

