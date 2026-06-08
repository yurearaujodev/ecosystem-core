package br.com.yat.ecosystemcore.application.usecase;

import javafx.concurrent.Task;
import java.util.function.Consumer;

import br.com.yat.ecosystemcore.domain.dto.DatabaseForm;
import br.com.yat.ecosystemcore.domain.entity.DatabaseConfig;
import br.com.yat.ecosystemcore.domain.entity.DatabaseCredentials;
import br.com.yat.ecosystemcore.service.external.DatabaseSetupService;
import br.com.yat.ecosystemcore.shared.current.AppExecutors;

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

