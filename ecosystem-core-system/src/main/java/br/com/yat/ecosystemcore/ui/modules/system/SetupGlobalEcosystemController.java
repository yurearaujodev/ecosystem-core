package br.com.yat.ecosystemcore.ui.modules.system;

import br.com.yat.ecosystemcore.infrastructure.concurrent.AppExecutors;
import br.com.yat.ecosystemcore.infrastructure.database.ConnectionFactory;
import br.com.yat.ecosystemcore.infrastructure.database.DatabaseMenuSeeder;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.UUID;

public class SetupGlobalEcosystemController {

    @FXML private TextField txtNomePlataforma;
    @FXML private TextField txtAmbiente;
    @FXML private TextField txtVersaoInicial;
    
    // Campos do Administrador / Dono do sistema
    @FXML private TextField txtNomeAdmin;
    @FXML private TextField txtEmailAdmin;
    @FXML private PasswordField txtSenhaAdmin;
    
    @FXML private VBox vboxModulosCatalogo;
    @FXML private Button btnInicializar;

    @FXML
    public void initialize() {
        // Carrega a pré-visualização visual dos módulos que o seeder vai criar
        carregarPrevisualizacaoCatalogo();
    }

    private void carregarPrevisualizacaoCatalogo() {
        Set<String> modulosEsquematizados = new LinkedHashSet<>();
        modulosEsquematizados.add("🏠 HOME");
        modulosEsquematizados.add("👥 CADASTROS");
        modulosEsquematizados.add("🔐 SEGURANÇA");
        modulosEsquematizados.add("🏢 ADMINISTRAÇÃO");
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
        // Validações de segurança de preenchimento na UI
        if (txtNomePlataforma.getText().isBlank() || txtAmbiente.getText().isBlank() ||
            txtNomeAdmin.getText().isBlank() || txtEmailAdmin.getText().isBlank() || txtSenhaAdmin.getText().isBlank()) {
            mostrarAlerta("Campos Vazios", "Erro de Parâmetros", "Todos os campos do sistema e do administrador são obrigatórios.", AlertType.WARNING);
            return;
        }

        if (!txtEmailAdmin.getText().contains("@")) {
            mostrarAlerta("E-mail Inválido", "Erro de Validação", "Por favor, insira um e-mail válido para o administrador.", AlertType.WARNING);
            return;
        }

        setLoading(true);

        String nomePlat = txtNomePlataforma.getText().trim();
        String ambiente = txtAmbiente.getText().trim();
        String nomeAdmin = txtNomeAdmin.getText().trim();
        String emailAdmin = txtEmailAdmin.getText().trim();
        String senhaPura = txtSenhaAdmin.getText();

        AppExecutors.getDatabaseExecutor().execute(() -> {
            try (Connection conn = ConnectionFactory.getConnection()) {
                conn.setAutoCommit(false); // Transação Atômica Global
                
                try {
                    // 1. Salva a configuração mestre do sistema
                    String sqlConfig = "INSERT INTO sistema_config (chave, valor_config, descricao) VALUES (?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlConfig)) {
                        String jsonPayload = String.format("{\"nome_plataforma\": \"%s\", \"ambiente\": \"%s\"}", nomePlat, ambiente);
                        
                        stmt.setString(1, "CONFIG_GLOBAL_SISTEMA");
                        stmt.setString(2, jsonPayload);
                        stmt.setString(3, "Configurações primárias do ecossistema definidas no Onboarding do Dono");
                        stmt.executeUpdate();
                    }

                    // 2. Executa a carga estrutural de Menus, Módulos e o Tenant Global de Sistema
                    DatabaseMenuSeeder.inicializarCargaEstrutural(conn);
                    
                    // ID fixo do escopo global usado pelo seeder para isolamento de dados (Multi-tenant ready)
                    String tenantGlobalUuid = "00000000-0000-0000-0000-000000000000";

                    // 3. Cadastra o Dono na tabela 'pessoa'
                    String sqlPessoa = "INSERT INTO pessoa (uuid_publico, tenant_id, tipo, nome_razao, ativo) VALUES (?, ?, ?, ?, ?)";
                    long pessoaId = 0;
                    try (PreparedStatement stmt = conn.prepareStatement(sqlPessoa, Statement.RETURN_GENERATED_KEYS)) {
                        stmt.setString(1, UUID.randomUUID().toString());
                        stmt.setString(2, tenantGlobalUuid);
                        stmt.setString(3, "FISICA");
                        stmt.setString(4, nomeAdmin);
                        stmt.setInt(5, 1);
                        stmt.executeUpdate();
                        
                        try (ResultSet rs = stmt.getGeneratedKeys()) {
                            if (rs.next()) {
                                pessoaId = rs.getLong(1);
                            }
                        }
                    }

                    // 4. Aplica hash seguro na senha e insere o registro de login na tabela 'usuario'
                    String senhaHash = BCrypt.hashpw(senhaPura, BCrypt.gensalt(12));
                    String sqlUsuario = "INSERT INTO usuario (uuid_publico, tenant_id, pessoa_id, email, senha_hash, status) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlUsuario)) {
                        stmt.setString(1, UUID.randomUUID().toString());
                        stmt.setString(2, tenantGlobalUuid);
                        stmt.setLong(3, pessoaId);
                        stmt.setString(4, emailAdmin);
                        stmt.setString(5, senhaHash);
                        stmt.setString(6, "ACTIVE");
                        stmt.executeUpdate();
                    }

                    // Se NENHUMA linha de código falhou até aqui, persistimos tudo permanentemente
                    conn.commit();

                    // Retorna para a Thread principal do JavaFX para trocar de tela
                    Platform.runLater(() -> {
                        setLoading(false);
                        
                        Alert alerta = new Alert(AlertType.INFORMATION);
                        alerta.setTitle("Sucesso Corporativo");
                        alerta.setHeaderText("Ambiente Configurado com Sucesso!");
                        alerta.setContentText("O ecossistema foi estabelecido, os módulos foram semeados e o usuário administrador master foi criado.");
                        alerta.showAndWait();
                        
                        try {
                            // Redireciona para o painel principal
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/menu/menu-view.fxml"));
                            Parent root = loader.load();
                            
                            javafx.stage.Stage stage = (javafx.stage.Stage) btnInicializar.getScene().getWindow();
                            stage.getScene().setRoot(root);
                            stage.setTitle("YAT Ecosystem Core - Painel do Administrador");
                            stage.setMaximized(true);
                            
                        } catch (Exception e) {
                            mostrarAlerta("Erro de Navegação", "Não foi possível abrir o menu pós-setup", e.getMessage(), AlertType.ERROR);
                        }
                    });

                } catch (Exception ex) {
                    conn.rollback(); // Desfaz absolutamente tudo caso ocorra qualquer erro de chave ou constraint
                    throw ex;
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    mostrarAlerta("Erro Crítico", "Falha catastrófica ao persistir o setup", e.getMessage(), AlertType.ERROR);
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