package br.com.yat.ecosystemcore.application.system.usecase;

import br.com.yat.ecosystemcore.application.system.dto.AtualizarDetalhesUsuarioCommand;
import br.com.yat.ecosystemcore.repository.perfil.UsuarioPermissaoRepository;
import br.com.yat.ecosystemcore.repository.perfil.UsuarioSegurancaConfigRepository;
import br.com.yat.ecosystemcore.service.external.EmpresaUsuarioService;
import br.com.yat.ecosystemcore.shared.context.Sessao;
import br.com.yat.ecosystemcore.shared.database.TransactionManager;

import java.sql.SQLException;

public class SalvarDetalhesSegurancaUsuarioUseCase {

    private final EmpresaUsuarioService empresaUsuarioService = new EmpresaUsuarioService();
    private final UsuarioPermissaoRepository usuarioPermissaoRepo = new UsuarioPermissaoRepository();
    private final UsuarioSegurancaConfigRepository segurancaConfigRepo = new UsuarioSegurancaConfigRepository();

//    public void execute(AtualizarDetalhesUsuarioCommand command) throws SQLException {
//        
//        TransactionManager.executeVoidInTransaction(conn -> {
//            
//        	empresaUsuarioService.sincronizarVinculos(conn, command.usuarioId(), command.vinculosEmpresas());
//        	
//            usuarioPermissaoRepo.atualizarVinculosUsuario(conn, command.usuarioId(), command.idsPermissoesExtras());
//
//            segurancaConfigRepo.salvarOuAtualizar(conn, command.usuarioId(), conn.getSchema(), command.segurancaConfig());
// 
//        });
//    }
    
    public void execute(AtualizarDetalhesUsuarioCommand command) throws SQLException {
        // 1. Capture o tenant ANTES de entrar na transação
        // Isso garante que você não dependa do estado interno da conexão (schema)
        String tenantIdAtivo = Sessao.tenant().getId(); 

        if (tenantIdAtivo == null) {
            throw new SQLException("Sessão expirada ou tenant não identificado.");
        }
        
        TransactionManager.executeVoidInTransaction(conn -> {
            // 2. Use o tenantId capturado
            empresaUsuarioService.sincronizarVinculos(conn, command.usuarioId(), command.vinculosEmpresas());
            usuarioPermissaoRepo.atualizarVinculosUsuario(conn, command.usuarioId(), command.idsPermissoesExtras());

            // 3. O repositório agora recebe o UUID real, não o schema do banco
            segurancaConfigRepo.salvarOuAtualizar(conn, command.usuarioId(), tenantIdAtivo, command.segurancaConfig());
        });
    }
}

