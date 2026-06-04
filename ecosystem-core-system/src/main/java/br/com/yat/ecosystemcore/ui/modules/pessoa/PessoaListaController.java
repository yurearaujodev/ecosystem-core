package br.com.yat.ecosystemcore.ui.modules.pessoa;

import br.com.yat.ecosystemcore.domain.entity.Pessoa;
import br.com.yat.ecosystemcore.service.external.PessoaService;
import br.com.yat.ecosystemcore.infrastructure.security.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class PessoaListaController {

    @FXML private TextField txtPesquisa;
    @FXML private TableView<Pessoa> tblPessoas;
    @FXML private TableColumn<Pessoa, Long> colId;
    @FXML private TableColumn<Pessoa, String> colTipo;
    @FXML private TableColumn<Pessoa, String> colNome;
    @FXML private TableColumn<Pessoa, String> colCpfCnpj;
    @FXML private TableColumn<Pessoa, String> colTelefone;
    @FXML private TableColumn<Pessoa, String> colStatus;

    private final PessoaService pessoaService = new PessoaService();
    private final ObservableList<Pessoa> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarColunas();
        carregarDados();
    }

    private void configurarColunas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nomeRazao"));
        colCpfCnpj.setCellValueFactory(new PropertyValueFactory<>("cpfCnpj"));
        colTelefone.setCellValueFactory(new PropertyValueFactory<>("telefone"));
        
        colStatus.setCellValueFactory(cellData -> {
            boolean ativo = cellData.getValue().isAtivo();
            return new SimpleStringProperty(ativo ? "Ativo" : "Inativo");
        });
    }

    private void carregarDados() {
        try {
            masterData.clear();
            // 🛠️ MUDANÇA: Buscando o id do Tenant diretamente da Sessão ativa
            String tenantId = SessionManager.getTenantAtual().getId();
            List<Pessoa> bdPessoas = pessoaService.listarTodas(tenantId);
            masterData.addAll(bdPessoas);
            tblPessoas.setItems(masterData);
        } catch (SQLException e) {
            exibirAlerta("Erro", "Falha ao carregar pessoas do banco: " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (NullPointerException e) {
            exibirAlerta("Erro de Sessão", "Nenhum usuário autenticado no sistema.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onPesquisar() {
        String termo = txtPesquisa.getText();
        if (termo == null || termo.trim().isEmpty()) {
            tblPessoas.setItems(masterData);
            return;
        }
        
        String busca = termo.toLowerCase().trim();
        ObservableList<Pessoa> filtrado = masterData.filtered(p -> 
            (p.getNomeRazao() != null && p.getNomeRazao().toLowerCase().contains(busca)) ||
            (p.getCpfCnpj() != null && p.getCpfCnpj().contains(busca)) ||
            (p.getApelidoFantasia() != null && p.getApelidoFantasia().toLowerCase().contains(busca))
        );
        tblPessoas.setItems(filtrado);
    }

    @FXML
    private void onNovo() {
        abrirFormulario(null);
    }

    @FXML
    private void onAlterar() {
        Pessoa selecionada = tblPessoas.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            exibirAlerta("Aviso", "Selecione uma pessoa na tabela para poder alterar.", Alert.AlertType.WARNING);
            return;
        }
        abrirFormulario(selecionada);
    }

    private void abrirFormulario(Pessoa pessoa) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/ui/modules/pessoa-cadastro.fxml"));
            javafx.scene.Parent root = loader.load();
            
            PessoaCadastroController controller = loader.getController();
            
            if (pessoa == null) {
                controller.iniciarInclusao(this::carregarDados); 
            } else {
                controller.iniciarEdicao(pessoa, this::carregarDados);
            }
            
            Stage stage = new Stage();
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle(pessoa == null ? "Nova Pessoa" : "Alterar Pessoa");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            
        } catch (java.io.IOException e) {
            exibirAlerta("Erro", "Erro ao carregar layout do formulário: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onExcluir() {
        Pessoa selecionada = tblPessoas.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            exibirAlerta("Aviso", "Selecione uma pessoa na tabela para realizar a exclusão lógica.", Alert.AlertType.WARNING);
            return;
        }

        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION, "Deseja mesmo arquivar a pessoa " + selecionada.getNomeRazao() + "?", ButtonType.YES, ButtonType.NO);
        confirmacao.showAndWait().ifPresent(resposta -> {
            if (resposta == ButtonType.YES) {
                try {
                    // 🛠️ MUDANÇA: Passando dados extraídos do SessionManager em tempo de execução
                    String tenantId = SessionManager.getTenantAtual().getId();
                    Long usuarioId = SessionManager.getUsuarioLogado().getId();
                    
                    pessoaService.deletarPessoa(selecionada.getId(), tenantId, usuarioId);
                    carregarDados();
                } catch (SQLException e) {
                    exibirAlerta("Erro", "Erro ao executar exclusão: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void exibirAlerta(String titulo, String conteudo, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(conteudo);
        alert.showAndWait();
    }
}