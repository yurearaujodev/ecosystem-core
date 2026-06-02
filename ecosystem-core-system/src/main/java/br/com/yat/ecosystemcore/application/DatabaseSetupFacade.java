package br.com.yat.ecosystemcore.application;

import java.util.function.Consumer;

import br.com.yat.ecosystemcore.application.usecase.SalvarConfiguracaoUseCase;
import br.com.yat.ecosystemcore.application.usecase.TestarConexaoUseCase;
import br.com.yat.ecosystemcore.domain.dto.DatabaseForm;
import br.com.yat.ecosystemcore.domain.entity.DatabaseConfig;
import br.com.yat.ecosystemcore.infrastructure.database.DatabaseStatus;
import br.com.yat.ecosystemcore.service.external.DatabaseSetupService;

public final class DatabaseSetupFacade {
    
    private final TestarConexaoUseCase testarUseCase = new TestarConexaoUseCase();
    private final SalvarConfiguracaoUseCase salvarUseCase = new SalvarConfiguracaoUseCase();
    private final DatabaseSetupService setupService = new DatabaseSetupService();

    public void testarConexao(DatabaseForm form, char[] senha, 
                              Consumer<DatabaseStatus> onSuccess, Consumer<Throwable> onFailed) {
        testarUseCase.executar(form, senha, onSuccess, onFailed);
    }

    public void salvarConfiguracao(DatabaseForm form, char[] senha, 
                                   Runnable onSuccess, Consumer<Throwable> onFailed) {
        salvarUseCase.executar(form, senha, onSuccess, onFailed);
    }

    public DatabaseConfig carregarConfiguracaoExistente() {
        return setupService.carregarConfiguracao();
    }
}
