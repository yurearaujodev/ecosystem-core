package br.com.yat.ecosystemcore.ui.modules.tenant;

import br.com.yat.ecosystemcore.application.tenant.OnboardingTenantUseCase;
import br.com.yat.ecosystemcore.application.tenant.OnboardingTenantUseCaseFactory;
import br.com.yat.ecosystemcore.application.tenant.dto.OnboardingTenantCommand;
import br.com.yat.ecosystemcore.shared.current.AppExecutors;
import br.com.yat.ecosystemcore.shared.database.ConnectionFactory;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;
import javafx.scene.Cursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class OnboardingTenantController {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingTenantController.class);
    private final OnboardingTenantUseCase onboardingUseCase = OnboardingTenantUseCaseFactory.create();

    @FXML private TextField txtNomeConta;
    @FXML private ComboBox<String> cbPlano;
    @FXML private TextField txtTimezone;
    @FXML private TextField txtLimiteUsuarios;
    @FXML private TextField txtRazaoSocial;
    @FXML private TextField txtNomeFantasia;
    @FXML private TextField txtCnpj;
    @FXML private TextField txtInscricaoEstadual;
    @FXML private TextField txtTelefoneEmpresa;
    @FXML private TextField txtLogradouro;
    @FXML private TextField txtCidade;
    @FXML private TextField txtEstado;
    @FXML private TextField txtCep;

    @FXML private TextField txtNomeAdmin;
    @FXML private TextField txtCpfAdmin;
    @FXML private TextField txtTelefoneAdmin;
    @FXML private TextField txtEmailAdmin;
    @FXML private PasswordField txtSenhaAdmin;
    @FXML private PasswordField txtConfirmarSenhaAdmin;
    @FXML private ComboBox<String> cbMultiplasSessoes;
    @FXML private ComboBox<String> cbAcessoForaEmpresa;

    @FXML private Button btnRegistrar;
    
    // ATRIBUTOS INTEGRADOS PARA LISTAGEM DE MÓDULOS
    @FXML private VBox vboxModulosDisponiveis;
    private final List<CheckBox> checkBoxesModulos = new ArrayList<>();

    @FXML
    public void initialize() {
        cbPlano.getItems().addAll("STANDARD", "PREMIUM", "ENTERPRISE");
        cbPlano.setValue("ENTERPRISE");

        cbMultiplasSessoes.getItems().addAll("Sim", "Não");
        cbMultiplasSessoes.setValue("Não");

        cbAcessoForaEmpresa.getItems().addAll("Sim", "Não");
        cbAcessoForaEmpresa.setValue("Sim");

        // CARREGA DO BANCO OS MÓDULOS DISPONÍVEIS INSERIDOS PELO SEEDER
        carregarModulosParaSelecao();
    }

    private void carregarModulosParaSelecao() {
        String sql = "SELECT id, nome FROM modulo_sistema WHERE ativo = 1 ORDER BY ordem";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
             
            while (rs.next()) {
                long id = rs.getLong("id");
                String nome = rs.getString("nome");
                
                CheckBox cb = new CheckBox(nome);
                cb.setSelected(true); // Deixa todos marcados por padrão
                cb.setUserData(id);   // Armazena a Primary Key do módulo no nó gráfico
                
                checkBoxesModulos.add(cb);
                vboxModulosDisponiveis.getChildren().add(cb);
            }
        } catch (Exception e) {
            logger.error("Erro ao carregar catálogo de módulos para o onboarding", e);
        }
    }

    @FXML
    public void handleRegistrar() {
        if (!validarFormulario()) return;

        setLoading(true);
        OnboardingTenantCommand command = montarCommand();

        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                onboardingUseCase.executar(command);
                Platform.runLater(() -> {
                    setLoading(false);
                    mostrarAlerta("Sucesso", "Ecossistema Inicializado com Sucesso!", "Todas as tabelas foram povoadas e o usuário mestre foi criado.", AlertType.INFORMATION);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    mostrarAlerta("Erro de Banco", "Falha ao gravar os dados estruturais", e.getMessage(), AlertType.ERROR);
                });
            }
        });
    }

    private OnboardingTenantCommand montarCommand() {
        int limite = 5;
        try { limite = Integer.parseInt(txtLimiteUsuarios.getText()); } catch(NumberFormatException ignored){}

        // DESCOBRE E MAPEIA OS MÓDULOS QUE O DONO MARCOU NA TELA
        List<Long> modulosSelecionados = checkBoxesModulos.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> (Long) cb.getUserData())
                .toList();

        // ENVIADO DIRETAMENTE PARA O CONSTRUTOR DO COMMAND RECORD
        return new OnboardingTenantCommand(
                txtNomeConta.getText().trim(),
                cbPlano.getValue(),
                txtTimezone.getText().trim(),
                limite,
                txtRazaoSocial.getText().trim(),
                txtNomeFantasia.getText().trim(),
                txtCnpj.getText().trim(),
                txtInscricaoEstadual.getText().trim(),
                txtTelefoneEmpresa.getText().trim(),
                txtLogradouro.getText().trim(),
                txtCidade.getText().trim(),
                txtEstado.getText().trim(),
                txtCep.getText().trim(),
                txtNomeAdmin.getText().trim(),
                txtCpfAdmin.getText().trim(),
                txtTelefoneAdmin.getText().trim(),
                txtEmailAdmin.getText().trim(),
                txtSenhaAdmin.getText(),
                "Sim".equals(cbMultiplasSessoes.getValue()),
                "Sim".equals(cbAcessoForaEmpresa.getValue()),
                modulosSelecionados // Adicionado como último parâmetro do Command
        );
    }

    private boolean validarFormulario() {
        if (txtNomeConta.getText().isBlank() || txtRazaoSocial.getText().isBlank() || 
            txtCnpj.getText().isBlank() || txtEmailAdmin.getText().isBlank() || txtSenhaAdmin.getText().isBlank()) {
            mostrarAlerta("Campos Vazios", "Validação", "Por favor, preencha os campos estruturais obrigatórios do banco.", AlertType.WARNING);
            return false;
        }
        if (!txtSenhaAdmin.getText().equals(txtConfirmarSenhaAdmin.getText())) {
            mostrarAlerta("Senhas Diferentes", "Validação", "A senha e a confirmação não conferem.", AlertType.WARNING);
            return false;
        }
        return true;
    }

    private void setLoading(boolean loading) {
        btnRegistrar.setDisable(loading);
        if (btnRegistrar.getScene() != null) {
            btnRegistrar.getScene().setCursor(loading ? Cursor.WAIT : Cursor.DEFAULT);
        }
    }

    private void mostrarAlerta(String t, String c, String m, AlertType type) {
        Alert a = new Alert(type); a.setTitle(t); a.setHeaderText(c); a.setContentText(m); a.showAndWait();
    }
}