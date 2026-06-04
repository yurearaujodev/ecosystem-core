//package br.com.yat.ecosystemcore.application.system.usecase;
//
//import br.com.yat.ecosystemcore.domain.entity.Usuario;
//import br.com.yat.ecosystemcore.infrastructure.database.ConnectionFactory;
//import br.com.yat.ecosystemcore.repository.perfil.EmpresaUsuarioRepository;
//import br.com.yat.ecosystemcore.repository.perfil.PerfilRepository;
//import br.com.yat.ecosystemcore.repository.usuario.UsuarioRepository;
//import br.com.yat.ecosystemcore.repository.usuario.UsuarioSegurancaConfigRepository;
//
//import java.sql.Connection;
//import java.sql.SQLException;
//import java.util.UUID;
//
//public class CadastrarUsuarioCredencialUseCase {
//
//    private final UsuarioRepository usuarioRepository = new UsuarioRepository();
//    private final PerfilRepository perfilRepository = new PerfilRepository();
//    private final EmpresaUsuarioRepository empresaUsuarioRepository = new EmpresaUsuarioRepository();
//    private final UsuarioSegurancaConfigRepository segurancaConfigRepository = new UsuarioSegurancaConfigRepository();
//
//    public void execute(String tenantId, Long pessoaId, Long empresaId, String email, String senhaHash) throws SQLException {
//        try (Connection conn = ConnectionFactory.getConnection()) {
//            conn.setAutoCommit(false); // Ativa controle transacional estrito
//
//            try {
//                // 1. Instancia e persiste a entidade de Usuário
//                Usuario usuario = new Usuario();
//                usuario.setUuidPublico(UUID.randomUUID().toString());
//                usuario.setTenantId(tenantId);
//                usuario.setPessoaId(pessoaId);
//                usuario.setEmpresaPadraoId(empresaId);
//                usuario.setEmail(email);
//                usuario.setSenhaHash(senhaHash);
//                usuario.setStatus("ACTIVE");
//
//                Long usuarioId = usuarioRepository.insert(conn, usuario);
//
//                // 2. Cria as configurações de segurança padrão do usuário (IP, múltiplas sessões...)
//                segurancaConfigRepository.inserirPadrao(conn, usuarioId, tenantId);
//
//                // 3. Garante o ID do Perfil Admin para o Tenant atual
//                Long perfilId = perfilRepository.criarPerfilAdminSeNecessario(conn, tenantId);
//
//                // 4. Cria o vínculo multiempresa para liberar os escopos de leitura de sessão
//                empresaUsuarioRepository.vincular(conn, tenantId, empresaId, usuarioId, perfilId);
//
//                conn.commit(); // Valida todas as operações juntas no banco de dados
//                
//            } catch (SQLException e) {
//                conn.rollback(); // Desfaz qualquer inserção parcial para evitar lixo no banco
//                throw e;
//            }
//        }
//    }
//}