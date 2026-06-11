package br.com.yat.ecosystemcore.modules.sistema.service;

import javafx.concurrent.Task;
import java.util.function.Consumer;

import br.com.yat.ecosystemcore.modules.sistema.dto.DatabaseForm;
import br.com.yat.ecosystemcore.shared.current.AppExecutors;
import br.com.yat.ecosystemcore.shared.database.DatabaseConfig;
import br.com.yat.ecosystemcore.shared.database.DatabaseCredentials;
import br.com.yat.ecosystemcore.shared.database.DatabaseSetupService;

public final class SalvarConfiguracaoUseCase {
    private final DatabaseSetupService setupService = new DatabaseSetupService();

    public void executar(DatabaseForm form, char[] senhaOrigemUI, 
                         Runnable onSuccess, Consumer<Throwable> onFailed) {
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
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
                    setupService.saveConfiguration(config, credentials);
                    return null;
                }
            }
        };

        task.setOnSucceeded(e -> onSuccess.run());
        task.setOnFailed(e -> onFailed.accept(task.getException()));
        AppExecutors.getDatabaseExecutor().submit(task);
    }
}

