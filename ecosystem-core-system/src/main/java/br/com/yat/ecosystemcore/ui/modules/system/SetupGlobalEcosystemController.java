package br.com.yat.ecosystemcore.ui.modules.system;

import br.com.yat.ecosystemcore.application.system.dto.SetupEcosystemCommand;
import br.com.yat.ecosystemcore.application.system.usecase.SetupGlobalEcosystemUseCase;
import br.com.yat.ecosystemcore.infrastructure.concurrent.AppExecutors;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Cursor;
import javafx.scene.Parent;

import java.util.Set;
import java.util.LinkedHashSet;

public class SetupGlobalEcosystemController {

    @FXML private TextField txtNomePlataforma;
    @FXML private TextField txtAmbiente;
    @FXML private TextField txtVersaoInicial;
    
    // Novos campos adicionados para conformidade estrita com as constraints do banco
    @FXML private TextField txtRazaoSocial;
    @FXML private TextField txtNomeFantasia;
    @FXML private TextField txtCnpjEmpresa;
    
    // Dados do Administrador e Governança
    @FXML private TextField txtNomeAdmin;
    @FXML private TextField txtEmailAdmin;
    @FXML private PasswordField txtSenhaAdmin;
    @FXML private TextField txtChavePerfil;
    
    @FXML private VBox vboxModulosCatalogo;
    @FXML private Button btnInicializar;

    private final SetupGlobalEcosystemUseCase useCase = new SetupGlobalEcosystemUseCase();

    @FXML
    public void initialize() {
        carregarPrevisualizacaoCatalogo();
    }

    private void carregarPrevisualizacaoCatalogo() {
        Set<String> modulosEsquematizados = new LinkedHashSet<>();
        modulosEsquematizados.add("🏠 HOME");
        modulosEsquematizados.add("👥 CADASTROS");
        modulosEsquematizados.add("🔐 SEGURANÇA");
        modulosEsquematizados.add("🏢 ADMINISTRAÇÃO");
        modulosEsquematizados.add("🔑 LICENCIAMENTO");
        modulosEsquematizados.add("🔔 COMUNICAÇÃO");
        modulosEsquematizados.add("📁 DOCUMENTOS");
        modulosEsquematizados.add("📜 AUDITORIA");
        modulosEsquematizados.add("⚙ SISTEMA");
        modulosEsquematizados.add("💰 FINANCEIRO");

        for (String modulo : modulosEsquematizados) {
            Label lbl = new Label("• " + modulo);
            lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #495057;");
            vboxModulosCatalogo.getChildren().add(lbl);
        }
    }

    @FXML
    public void handleInicializar() {
        // Validação estrita na interface para prevenir falhas de campos nulos no banco
        if (txtNomePlataforma.getText().isBlank() || txtAmbiente.getText().isBlank() ||
            txtRazaoSocial.getText().isBlank() || txtNomeFantasia.getText().isBlank() || txtCnpjEmpresa.getText().isBlank() ||
            txtNomeAdmin.getText().isBlank() || txtEmailAdmin.getText().isBlank() || txtSenhaAdmin.getText().isBlank() || 
            txtChavePerfil.getText().isBlank()) {
            
            mostrarAlerta("Campos Vazios", "Erro de Entrada", "Todos os parâmetros do formulário são obrigatórios.", AlertType.WARNING);
            return;
        }

        String cnpjLimpo = txtCnpjEmpresa.getText().replaceAll("\\D", "");
        if (cnpjLimpo.length() != 14) {
            mostrarAlerta("CNPJ Inválido", "Erro de Validação", "O campo CNPJ precisa possuir exatamente 14 numerais.", AlertType.WARNING);
            return;
        }

        if (!txtEmailAdmin.getText().contains("@")) {
            mostrarAlerta("E-mail Inválido", "Erro de Validação", "Forneça um e-mail com formato válido para o login do administrador.", AlertType.WARNING);
            return;
        }

        setLoading(true);

        // Instanciação do comando com tratamento de strings higienizadas
        SetupEcosystemCommand command = new SetupEcosystemCommand(
            txtNomePlataforma.getText().trim(),
            txtAmbiente.getText().trim(),
            txtVersaoInicial.getText().trim(),
            txtRazaoSocial.getText().trim(),
            txtNomeFantasia.getText().trim(),
            cnpjLimpo,
            txtNomeAdmin.getText().trim(),
            txtEmailAdmin.getText().trim().toLowerCase(),
            txtSenhaAdmin.getText(),
            txtChavePerfil.getText().trim()
        );

        // Execução em background thread para evitar o congelamento da interface visual (UI Thread)
        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                useCase.execute(command);

                // Execução bem-sucedida: Processamento de interface na Thread principal do JavaFX
                Platform.runLater(() -> {
                    setLoading(false);
                    
                    Alert alerta = new Alert(AlertType.INFORMATION);
                    alerta.setTitle("Sucesso Corporativo");
                    alerta.setHeaderText("Ambiente Configurado com Sucesso!");
                    alerta.setContentText("O ecossistema foi estabelecido, a infraestrutura foi semeada e a empresa master foi vinculada ao administrador.");
                    alerta.showAndWait();
                    
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/modules/login-view.fxml"));
                        Parent root = loader.load();
                        
                        Stage stage = (Stage) btnInicializar.getScene().getWindow();
                        
                        // Garante o estado normal antes de redimensionar e injetar o root de login
                        stage.setMaximized(false); 
                        stage.setResizable(false);
                        stage.setWidth(450);  // Largura ideal para uma tela de login elegante
                        stage.setHeight(550); // Altura ideal para uma tela de login elegante
                        
                        stage.getScene().setRoot(root);
                        stage.setTitle("YAT Ecosystem Core - Autenticação");
                        stage.centerOnScreen();
                        
                    } catch (Exception e) {
                        mostrarAlerta("Erro de Navegação", "Não foi possível carregar a janela principal pós-setup", e.getMessage(), AlertType.ERROR);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    mostrarAlerta("Erro Crítico", "O banco de dados rejeitou a persistência do setup", e.getMessage(), AlertType.ERROR);
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        btnInicializar.setDisable(loading);
        if (btnInicializar.getScene() != null) {
            btnInicializar.getScene().setCursor(loading ? Cursor.WAIT : Cursor.DEFAULT);
        }
    }

    private void mostrarAlerta(String t, String c, String m, AlertType type) {
        Alert a = new Alert(type); a.setTitle(t); a.setHeaderText(c); a.setContentText(m); a.showAndWait();
    }
}