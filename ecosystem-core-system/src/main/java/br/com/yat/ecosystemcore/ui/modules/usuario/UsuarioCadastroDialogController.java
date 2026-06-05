package br.com.yat.ecosystemcore.ui.modules.usuario;

import br.com.yat.ecosystemcore.application.usuario.BCryptPasswordEncoder;
import br.com.yat.ecosystemcore.application.usuario.PasswordEncoder;
import br.com.yat.ecosystemcore.domain.entity.Empresa;
import br.com.yat.ecosystemcore.domain.entity.Perfil;
import br.com.yat.ecosystemcore.domain.entity.Pessoa;
import br.com.yat.ecosystemcore.domain.entity.EmpresaUsuarioDetalheDTO;
import br.com.yat.ecosystemcore.domain.entity.Usuario;
import br.com.yat.ecosystemcore.infrastructure.concurrent.AppExecutors;
import br.com.yat.ecosystemcore.infrastructure.security.SessionManager;
import br.com.yat.ecosystemcore.service.external.PerfilService;
import br.com.yat.ecosystemcore.service.external.UsuarioService;
import br.com.yat.ecosystemcore.service.external.EmpresaService; 
import br.com.yat.ecosystemcore.service.external.PessoaService;

import br.com.yat.ecosystemcore.util.PasswordExtractor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            
            String tenantId = SessionManager.getTenantAtual().getId();
            AppExecutors.getDatabaseExecutor().execute(() -> {
                try {
                    List<Pessoa> todasPessoas = pessoaService.listarTodas(tenantId);
                    todasPessoas.stream()
                        .filter(p -> p.getId().equals(usuario.getPessoaId()))
                        .findFirst()
                        .ifPresent(p -> Platform.runLater(() -> treePessoaConverter(p)));

                    Platform.runLater(() -> {
                        cmbEmpresa.getItems().stream()
                            .filter(e -> e.getId().equals(usuario.getEmpresaPadraoId()))
                            .findFirst()
                            .ifPresent(e -> cmbEmpresa.getSelectionModel().select(e));

                        cmbPerfil.getItems().stream()
                            .filter(p -> p.getId().equals(perfilIdAtual))
                            .findFirst()
                            .ifPresent(p -> cmbPerfil.getSelectionModel().select(p));
                    });
                } catch (Exception e) {
                    logger.error("Erro ao carregar dados complementares para edição", e);
                }
            });
        });
    }

    private void inicializarCombosDinamicos() {
        String tenantId = SessionManager.getTenantAtual().getId();

        cmbPerfil.setConverter(new StringConverter<>() {
            @Override public String toString(Perfil p) { return p != null ? p.getNome() : ""; }
            @Override public Perfil fromString(String string) { return null; }
        });

        cmbEmpresa.setConverter(new StringConverter<>() {
            @Override public String toString(Empresa e) { return e != null ? e.getRazaoSocial() : ""; }
            @Override public Empresa fromString(String string) { return null; }
        });

        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                List<Perfil> perfis = perfilService.listarPerfisPorTenant(tenantId);
                List<Empresa> empresas = empresaService.listarEmpresasDoTenantAtivo(); 

                Platform.runLater(() -> {
                    cmbPerfil.setItems(FXCollections.observableArrayList(perfis));
                    cmbEmpresa.setItems(FXCollections.observableArrayList(empresas));
                });
            } catch (Exception e) {
                logger.error("Falha ao inicializar componentes de seleção de segurança", e);
            }
        });
    }

    @FXML
    void onBuscarPessoa() {
        String tenantId = SessionManager.getTenantAtual().getId();

        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                List<Pessoa> pessoasDisponiveis = pessoaService.listarTodas(tenantId);

                Platform.runLater(() -> {
                    if (pessoasDisponiveis.isEmpty()) {
                        mostrarAlertaInformativo("Nenhum Registro", "Não há pessoas cadastradas para este Tenant.");
                        return;
                    }

                    // 🔥 COMPILAÇÃO CORRIGIDA: Sem .setConverter(), usando o toString() nativo e limpo de Pessoa.java
                    ChoiceDialog<Pessoa> dialog = new ChoiceDialog<>(pessoasDisponiveis.get(0), pessoasDisponiveis);
                    dialog.setTitle("Selecionar Pessoa");
                    dialog.setHeaderText("Vinculação de Credencial de Usuário");
                    dialog.setContentText("Escolha a pessoa:");
                    
                    var optionalResult = dialog.showAndWait();
                    optionalResult.ifPresent(pessoa -> {
                        this.treePessoaConverter(pessoa);
                    });
                });

            } catch (Exception e) {
                logger.error("Erro ao listar pessoas para o componente Lookup", e);
                Platform.runLater(() -> mostrarAlerta("Erro de Busca", "Falha ao consultar a tabela de pessoas."));
            }
        });
    }

    private Pessoa treePessoaConverter(Pessoa pessoa) {
        this.pessoaSelecionada = pessoa;
        this.txtPessoaNome.setText(pessoa.getNomeRazao());
        return pessoa;
    }

    @FXML
    void onCancelar() {
        stage.close();
    }

    @FXML
    void onSalvar() {
        if (SessionManager.getTenantAtual() == null || SessionManager.getUsuarioLogado() == null) {
            mostrarAlerta("Sessão Expirada", "Impossível processar operação sem credenciais ativas.");
            stage.close();
            return;
        }

        if (pessoaSelecionada == null || cmbEmpresa.getSelectionModel().isEmpty() || cmbPerfil.getSelectionModel().isEmpty()) {
            mostrarAlerta("Campos Obrigatórios", "Selecione uma Pessoa, Empresa e Perfil de acesso.");
            return;
        }
        if (txtEmail.getText().isBlank()) {
            mostrarAlerta("Campos Obrigatórios", "O e-mail corporativo é obrigatório.");
            return;
        }

        boolean isModoCadastro = (usuarioEmEdicao == null);
        if (isModoCadastro && txtSenha.getText().isBlank()) {
            mostrarAlerta("Campos Obrigatórios", "A senha é obrigatória para novos cadastros.");
            return;
        }
        if (!chkConsentimento.isSelected()) {
            mostrarAlerta("Consentimento Obrigatório", "Você deve aceitar o tratamento de dados para prosseguir.");
            return;
        }

        try {
            btnSalvar.setDisable(true);
            
            String tenantAtivo = SessionManager.getTenantAtual().getId();
            Long usuarioLogadoId = SessionManager.getUsuarioLogado().getId();
            Long empresaId = cmbEmpresa.getSelectionModel().getSelectedItem().getId();
            Long perfilId = cmbPerfil.getSelectionModel().getSelectedItem().getId();

            Usuario usuarioAlvo = isModoCadastro ? new Usuario() : usuarioEmEdicao;
            
            usuarioAlvo.setTenantId(tenantAtivo);
            usuarioAlvo.setPessoaId(pessoaSelecionada.getId());
            usuarioAlvo.setEmpresaPadraoId(empresaId);
            usuarioAlvo.setEmail(txtEmail.getText().trim());
            usuarioAlvo.setConsentimentoDados(true);
            usuarioAlvo.setVersaoTermo("1.0");
            
            if (isModoCadastro) {
                usuarioAlvo.setStatus("ACTIVE");
            }

            char[] senhaExtraida = PasswordExtractor.extrair(txtSenha);
            if (senhaExtraida != null && senhaExtraida.length > 0) {
                String senhaCriptografada = passwordEncoder.encode(senhaExtraida);
                usuarioAlvo.setSenhaHash(senhaCriptografada);
            }

            if (txtMacAddress.getText() != null && !txtMacAddress.getText().isBlank()) {
                usuarioAlvo.setMacAddressAutorizado(txtMacAddress.getText().trim());
            } else {
                usuarioAlvo.setMacAddressAutorizado(null);
            }

            AppExecutors.getDatabaseExecutor().execute(() -> {
                try {
                    EmpresaUsuarioDetalheDTO vinculo = new EmpresaUsuarioDetalheDTO();
                    vinculo.setEmpresaId(empresaId);
                    vinculo.setPerfilId(perfilId); 
                    
                    List<EmpresaUsuarioDetalheDTO> listaVinculos = Collections.singletonList(vinculo);

                    usuarioService.salvarUsuarioCompleto(usuarioAlvo, listaVinculos, usuarioLogadoId);

                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Sucesso");
                        alert.setContentText(isModoCadastro ? "Usuário criado com sucesso!" : "Alterações salvas com sucesso!");
                        alert.showAndWait();
                        stage.close();
                    });

                } catch (Exception ex) {
                    logger.error("Falha ao salvar dados do operador", ex);
                    Platform.runLater(() -> {
                        btnSalvar.setDisable(false);
                        mostrarAlerta("Erro de Operação", "Não foi possível persistir os dados no banco.");
                    });
                }
            });

        } catch (Exception ex) {
            logger.error("Erro inesperado na Thread de interface", ex);
            btnSalvar.setDisable(false);
            mostrarAlerta("Erro Crítico", "Ocorreu um erro ao processar os dados.");
        }
    }

    private void mostrarAlerta(String header, String conteudo) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro Operacional");
        alert.setHeaderText(header);
        alert.setContentText(conteudo);
        alert.showAndWait();
    }

    private void mostrarAlertaInformativo(String titulo, String conteudo) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setContentText(conteudo);
        alert.showAndWait();
    }
    
    public void setPessoaSelecionada(Pessoa pessoa) {
        this.pessoaSelecionada = pessoa;
        this.txtPessoaNome.setText(pessoa.getNomeRazao());
    }
}