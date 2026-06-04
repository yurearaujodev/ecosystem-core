package br.com.yat.ecosystemcore.ui.modules.usuario;

import br.com.yat.ecosystemcore.application.usuario.BCryptPasswordEncoder;
import br.com.yat.ecosystemcore.application.usuario.PasswordEncoder;
import br.com.yat.ecosystemcore.domain.entity.Usuario;
import br.com.yat.ecosystemcore.infrastructure.concurrent.AppExecutors;
import br.com.yat.ecosystemcore.infrastructure.security.SessionManager;
import br.com.yat.ecosystemcore.service.external.UsuarioService;
import br.com.yat.ecosystemcore.util.PasswordExtractor;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class UsuarioCadastroDialogController {

    @FXML private TextField txtPessoaId;
    @FXML private TextField txtEmpresaId;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtSenha;

    private Stage stage;
    private final UsuarioService usuarioService = new UsuarioService();
    
    // 🔐 Injeção da sua estratégia de criptografia baseada em BCrypt
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void setStage(Stage stage) { 
        this.stage = stage; 
    }

    @FXML
    void onCancelar() {
        stage.close();
    }

    @FXML
    void onSalvar() {
        try {
            // Validações básicas
            if (txtPessoaId.getText().isBlank() || txtEmail.getText().isBlank() || txtSenha.getText().isBlank()) {
                mostrarAlerta("Campos Obrigatórios", "Por favor, preencha todos os campos (Pessoa ID, E-mail e Senha).");
                return;
            }

            Long pessoaId = Long.parseLong(txtPessoaId.getText().trim());
            // Se o empresaId for nulo ou vazio, tratamos como erro para evitar inconsistência de acesso
            if (txtEmpresaId.getText().isBlank()) {
                mostrarAlerta("Vinculação Necessária", "O usuário precisa estar vinculado a uma Empresa (Empresa ID).");
                return;
            }
            Long empresaId = Long.parseLong(txtEmpresaId.getText().trim());
            String email = txtEmail.getText().trim();

            // Extração de senha e Hash
            char[] senhaExtraida = PasswordExtractor.extrair(txtSenha);
            String senhaCriptografada = passwordEncoder.encode(new String(senhaExtraida));

            String tenantAtivo = SessionManager.getTenantAtual().getId();
            Long usuarioLogadoId = SessionManager.getUsuarioLogado().getId();

            Usuario novoUsuario = new Usuario();
            novoUsuario.setTenantId(tenantAtivo);
            novoUsuario.setPessoaId(pessoaId);
            novoUsuario.setEmpresaPadraoId(empresaId); // Define como padrão
            novoUsuario.setEmail(email);
            novoUsuario.setSenhaHash(senhaCriptografada);
            novoUsuario.setStatus("ACTIVE");

            AppExecutors.getDatabaseExecutor().execute(() -> {
                try {
                    // O Service fará a transação: Insert Usuário -> Insert Segurança -> Insert Vínculo
                    usuarioService.salvarUsuarioCompleto(novoUsuario, empresaId, usuarioLogadoId);
                    
                    Platform.runLater(() -> {
                        new Alert(Alert.AlertType.INFORMATION, "Usuário criado e vinculado com sucesso!").show();
                        stage.close();
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> mostrarAlerta("Erro de Persistência", "Falha ao salvar: " + ex.getMessage()));
                }
            });
        } catch (NumberFormatException ex) {
            mostrarAlerta("Formato Inválido", "IDs devem ser números inteiros.");
        }
    }

    private void mostrarAlerta(String header, String conteudo) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro Operacional");
        alert.setHeaderText(header);
        alert.setContentText(conteudo);
        alert.showAndWait();
    }
}
