package br.com.yat.ecosystemcore.application.system.usecase;

import br.com.yat.ecosystemcore.application.system.dto.AtualizarDetalhesUsuarioCommand;
import br.com.yat.ecosystemcore.infrastructure.database.TransactionManager;
import br.com.yat.ecosystemcore.repository.perfil.UsuarioPermissaoRepository;
import br.com.yat.ecosystemcore.repository.perfil.UsuarioSegurancaConfigRepository;
import br.com.yat.ecosystemcore.service.external.EmpresaUsuarioService;

import java.sql.SQLException;

public class SalvarDetalhesSegurancaUsuarioUseCase {

    private final EmpresaUsuarioService empresaUsuarioService = new EmpresaUsuarioService();
    private final UsuarioPermissaoRepository usuarioPermissaoRepo = new UsuarioPermissaoRepository();
    private final UsuarioSegurancaConfigRepository segurancaConfigRepo = new UsuarioSegurancaConfigRepository();

    public void execute(AtualizarDetalhesUsuarioCommand command) throws SQLException {
        
        TransactionManager.executeInTransaction(conn -> {
            
            empresaUsuarioService.sincronizarVinculos(command.usuarioId(), command.vinculosEmpresas());

            usuarioPermissaoRepo.atualizarVinculosUsuario(conn, command.usuarioId(), command.idsPermissoesExtras());

            segurancaConfigRepo.salvarOuAtualizar(conn, command.usuarioId(), conn.getSchema(), command.segurancaConfig());
 
        });
    }
}

