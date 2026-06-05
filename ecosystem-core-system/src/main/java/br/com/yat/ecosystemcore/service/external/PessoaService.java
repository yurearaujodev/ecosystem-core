package br.com.yat.ecosystemcore.service.external;

import br.com.yat.ecosystemcore.domain.entity.Pessoa;
import br.com.yat.ecosystemcore.infrastructure.database.TransactionManager;
import br.com.yat.ecosystemcore.repository.usuario.PessoaRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class PessoaService {

    private final PessoaRepository pessoaRepository = new PessoaRepository();

    public void salvarPessoa(Pessoa pessoa, Long usuarioLogadoId) throws SQLException {
        if (pessoa.getNomeRazao() == null || pessoa.getNomeRazao().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome ou razão social é obrigatório.");
        }
        if (pessoa.getTipo() == null || (!pessoa.getTipo().equals("FISICA") && !pessoa.getTipo().equals("JURIDICA"))) {
            throw new IllegalArgumentException("O tipo de pessoa deve ser FISICA ou JURIDICA.");
        }

        TransactionManager.executeInTransaction(conn -> {
            if (pessoa.getId() == null) {
                if (pessoa.getUuidPublico() == null || pessoa.getUuidPublico().isEmpty()) {
                    pessoa.setUuidPublico(UUID.randomUUID().toString());
                }
                pessoa.setCreatedBy(usuarioLogadoId);
                Long novoId = pessoaRepository.insert(conn, pessoa);
                pessoa.setId(novoId);
            } else {
                pessoa.setUpdatedBy(usuarioLogadoId);
                pessoaRepository.update(conn, pessoa);
            }
            return null; // 💡 Adicionado para satisfazer o TransactionalSupplier<Void>
        });
    }

    public List<Pessoa> listarTodas(String tenantId) throws SQLException {
        // 💡 Veja como ficou muito mais limpo sem o AtomicReference:
        return TransactionManager.executeInTransaction(conn -> {
            return pessoaRepository.findAll(conn, tenantId);
        });
    }
    
    public void deletarPessoa(Long id, String tenantId, Long usuarioLogadoId) throws SQLException {
        TransactionManager.executeInTransaction(conn -> {
            boolean deletado = pessoaRepository.softDeleteComUsuario(conn, id, tenantId, usuarioLogadoId);
            if (!deletado) {
                throw new SQLException("Não foi possível excluir o registro solicitado.");
            }
            return null; // 💡 Adicionado para satisfazer o TransactionalSupplier<Void>
        });
    }
}
//package br.com.yat.ecosystemcore.service.external;
//
//import br.com.yat.ecosystemcore.domain.entity.Pessoa;
//import br.com.yat.ecosystemcore.infrastructure.database.TransactionManager;
//import br.com.yat.ecosystemcore.repository.usuario.PessoaRepository;
//
//import java.sql.SQLException;
//import java.util.List;
//import java.util.UUID;
//import java.util.concurrent.atomic.AtomicReference;
//
//public class PessoaService {
//
//    private final PessoaRepository pessoaRepository = new PessoaRepository();
//
//    public void salvarPessoa(Pessoa pessoa, Long usuarioLogadoId) throws SQLException {
//        if (pessoa.getNomeRazao() == null || pessoa.getNomeRazao().trim().isEmpty()) {
//            throw new IllegalArgumentException("O nome ou razão social é obrigatório.");
//        }
//        if (pessoa.getTipo() == null || (!pessoa.getTipo().equals("FISICA") && !pessoa.getTipo().equals("JURIDICA"))) {
//            throw new IllegalArgumentException("O tipo de pessoa deve ser FISICA ou JURIDICA.");
//        }
//
//        TransactionManager.executeInTransaction(conn -> {
//            if (pessoa.getId() == null) {
//                if (pessoa.getUuidPublico() == null || pessoa.getUuidPublico().isEmpty()) {
//                    pessoa.setUuidPublico(UUID.randomUUID().toString());
//                }
//                pessoa.setCreatedBy(usuarioLogadoId);
//                Long novoId = pessoaRepository.insert(conn, pessoa);
//                pessoa.setId(novoId);
//            } else {
//                pessoa.setUpdatedBy(usuarioLogadoId);
//                pessoaRepository.update(conn, pessoa);
//            }
//        });
//    }
//
//    public List<Pessoa> listarTodas(String tenantId) throws SQLException {
//        AtomicReference<List<Pessoa>> resultado = new AtomicReference<>();
//        TransactionManager.executeInTransaction(conn -> {
//            resultado.set(pessoaRepository.findAll(conn, tenantId));
//        });
//        return resultado.get();
//    }
//    
//    public void deletarPessoa(Long id, String tenantId, Long usuarioLogadoId) throws SQLException {
//        TransactionManager.executeInTransaction(conn -> {
//            boolean deletado = pessoaRepository.softDeleteComUsuario(conn, id, tenantId, usuarioLogadoId);
//            if (!deletado) {
//                throw new SQLException("Não foi possível excluir o registro solicitado.");
//            }
//        });
//    }
//}