package br.com.yat.ecosystemcore.ui.modules.usuario;

import br.com.yat.ecosystemcore.application.usuario.BCryptPasswordEncoder;
import br.com.yat.ecosystemcore.application.usuario.PasswordEncoder;
import br.com.yat.ecosystemcore.domain.entity.*;
import br.com.yat.ecosystemcore.infrastructure.concurrent.AppExecutors;
import br.com.yat.ecosystemcore.infrastructure.database.TransactionManager;
import br.com.yat.ecosystemcore.infrastructure.security.Sessao;
import br.com.yat.ecosystemcore.service.external.*;
import br.com.yat.ecosystemcore.util.PasswordExtractor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class UsuarioCadastroDialogController {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioCadastroDialogController.class);

    @FXML private TextField txtPessoaNome;
    @FXML private ComboBox<Empresa> cmbEmpresa;
    @FXML private ComboBox<Perfil> cmbPerfil;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtSenha;
    @FXML private Button btnSalvar;
    @FXML private TextField txtMacAddress;
    @FXML private CheckBox chkConsentimento;

    private Stage stage;
    private Pessoa pessoaSelecionada; 
    private Usuario usuarioEmEdicao; 
    
    private final UsuarioService usuarioService = new UsuarioService();
    private final PerfilService perfilService = new PerfilService();
    private final EmpresaService empresaService = new EmpresaService();
    private final PessoaService pessoaService = new PessoaService();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void setStage(Stage stage) {
        this.stage = stage;
        inicializarCombosDinamicos();
    }

    public void setUsuarioParaEdicao(Usuario usuario, Long perfilIdAtual) {
        this.usuarioEmEdicao = usuario;
        
        Platform.runLater(() -> {
            btnSalvar.setText("💾 Salvar Alterações");
            txtEmail.setText(usuario.getEmail());
            txtMacAddress.setText(usuario.getMacAddressAutorizado());
            chkConsentimento.setSelected(usuario.isConsentimentoDados());
            txtSenha.setPromptText("(Deixe em branco para manter a atual)");
            
            AppExecutors.getDatabaseExecutor().execute(() -> {
                try {
                    // Apenas chame o serviço, ele já sabe abrir a conexão
                    List<Pessoa> todasPessoas = pessoaService.listarTodas();
                    
                    todasPessoas.stream()
                        .filter(p -> p.getId().equals(usuario.getPessoaId()))
                        .findFirst()
                        .ifPresent(p -> Platform.runLater(() -> treePessoaConverter(p)));

                    Platform.runLater(() -> {
                        cmbEmpresa.getItems().stream().filter(e -> e.getId().equals(usuario.getEmpresaPadraoId())).findFirst().ifPresent(cmbEmpresa.getSelectionModel()::select);
                        cmbPerfil.getItems().stream().filter(p -> p.getId().equals(perfilIdAtual)).findFirst().ifPresent(cmbPerfil.getSelectionModel()::select);
                    });
                } catch (Exception e) {
                    logger.error("Erro ao carregar dados para edição", e);
                }
            });
        });
    }

    private void inicializarCombosDinamicos() {
        cmbPerfil.setConverter(createConverter(Perfil::getNome));
        cmbEmpresa.setConverter(createConverter(Empresa::getRazaoSocial));

        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                // Executa em contexto transacional
                TransactionManager.executeVoidInTransaction(c -> {
                    List<Perfil> perfis = perfilService.listarPerfisPorTenant(Sessao.tenant().getId());
                    List<Empresa> empresas = empresaService.listarEmpresasDoTenantAtivo(); 
                    Platform.runLater(() -> {
                        cmbPerfil.setItems(FXCollections.observableArrayList(perfis));
                        cmbEmpresa.setItems(FXCollections.observableArrayList(empresas));
                    });
                });
            } catch (Exception e) {
                logger.error("Falha ao inicializar combos", e);
            }
        });
    }

    @FXML
    void onBuscarPessoa() {
        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                // Chamada direta, sem envolver em executeInTransaction
                List<Pessoa> pessoas = pessoaService.listarTodas();
                Platform.runLater(() -> {
                    if (pessoas.isEmpty()) return;
                    ChoiceDialog<Pessoa> dialog = new ChoiceDialog<>(pessoas.get(0), pessoas);
                    dialog.showAndWait().ifPresent(this::treePessoaConverter);
                });
            } catch (Exception e) {
                logger.error("Erro ao buscar pessoas", e);
            }
        });
    }

    @FXML
    void onSalvar() {
        if (!Sessao.isActive()) {
            mostrarAlerta("Sessão Expirada", "Realize o login novamente.");
            return;
        }

        // Validações de UI (Thread JavaFX)
        if (pessoaSelecionada == null || cmbEmpresa.getSelectionModel().isEmpty()) {
            mostrarAlerta("Campos Obrigatórios", "Verifique os campos obrigatórios.");
            return;
        }

        btnSalvar.setDisable(true);
        boolean isModoCadastro = (usuarioEmEdicao == null);
        
        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                TransactionManager.executeVoidInTransaction(c -> {
                    Usuario user = isModoCadastro ? new Usuario() : usuarioEmEdicao;
                    user.setTenantId(Sessao.tenant().getId());
                    user.setPessoaId(pessoaSelecionada.getId());
                    user.setEmpresaPadraoId(cmbEmpresa.getSelectionModel().getSelectedItem().getId());
                    user.setEmail(txtEmail.getText().trim());
                    
                    if (isModoCadastro) user.setSenhaHash(passwordEncoder.encode(PasswordExtractor.extrair(txtSenha)));
                    
                    EmpresaUsuarioDetalheDTO vinculo = new EmpresaUsuarioDetalheDTO();
                    vinculo.setEmpresaId(user.getEmpresaPadraoId());
                    vinculo.setPerfilId(cmbPerfil.getSelectionModel().getSelectedItem().getId());
                    
                    usuarioService.salvarUsuarioCompleto(user, Collections.singletonList(vinculo));
                });
                Platform.runLater(() -> { stage.close(); });
            } catch (Exception e) {
                logger.error("Erro ao salvar", e);
                Platform.runLater(() -> btnSalvar.setDisable(false));
            }
        });
    }

    private <T> StringConverter<T> createConverter(java.util.function.Function<T, String> toString) {
        return new StringConverter<>() {
            public String toString(T object) { return object != null ? toString.apply(object) : ""; }
            public T fromString(String string) { return null; }
        };
    }

    private void treePessoaConverter(Pessoa p) { this.pessoaSelecionada = p; this.txtPessoaNome.setText(p.getNomeRazao()); }
    private void mostrarAlerta(String h, String c) { new Alert(Alert.AlertType.ERROR, c).showAndWait(); }
    @FXML void onCancelar() { stage.close(); }
}