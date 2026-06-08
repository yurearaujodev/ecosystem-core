package br.com.yat.ecosystemcore.ui.modules.pessoa;

import br.com.yat.ecosystemcore.domain.entity.Pessoa;
import br.com.yat.ecosystemcore.service.external.PessoaService;
import br.com.yat.ecosystemcore.shared.context.Sessao;
import br.com.yat.ecosystemcore.shared.current.AppExecutors;
import br.com.yat.ecosystemcore.ui.core.ContextAware;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class PessoaListaController implements ContextAware {

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
    
    @Override
    public void onContextChanged() {
        // Quando o contexto organizacional mudar, esta tela se auto-atualiza de forma segura
        carregarDados();
    }

    private void configurarColunas() {
        // Usando expressões lambda explícitas para evitar reflexão pesada e erros em tempo de execução
        colId.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getId()));
        colTipo.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTipo()));
        colNome.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNomeRazao()));
        colCpfCnpj.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCpfCnpj()));
        colTelefone.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTelefone()));
        
        colStatus.setCellValueFactory(cellData -> {
            boolean ativo = cellData.getValue().isAtivo();
            return new SimpleStringProperty(ativo ? "Ativo" : "Inativo");
        });
    }

    private void carregarDados() {
        // Reduz o risco de travamento na interface despachando a consulta para as Threads de Banco
        AppExecutors.getDatabaseExecutor().submit(() -> {
            try {
                // 🔒 VALIDAÇÃO: Verifica de antemão se a sessão corporativa está de fato ativa
                if (!Sessao.isActive()) {
                    Platform.runLater(() -> exibirAlerta("Erro de Sessão", "Nenhum usuário autenticado ou sessão expirada.", Alert.AlertType.ERROR));
                    return;
                }

                // O método real não precisa de argumentos, ele se auto-blinda por Tenant internamente
                List<Pessoa> bdPessoas = pessoaService.listarTodas();

                // Devolve a atualização dos dados para a thread gráfica do JavaFX
                Platform.runLater(() -> {
                    masterData.clear();
                    masterData.addAll(bdPessoas);
                    tblPessoas.setItems(masterData);
                    
                    // Se houver algum texto residual no campo de busca, reaplica o filtro
                    if (txtPesquisa.getText() != null && !txtPesquisa.getText().trim().isEmpty()) {
                        onPesquisar();
                    }
                });

            } catch (SQLException e) {
                Platform.runLater(() -> exibirAlerta("Erro", "Falha ao carregar pessoas do banco: " + e.getMessage(), Alert.AlertType.ERROR));
            } catch (Exception e) {
                Platform.runLater(() -> exibirAlerta("Erro Crítico", "Falha interna inesperada: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        });
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
            boxAvisoSelecao();
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
            boxAvisoSelecao();
            return;
        }

        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION, "Deseja mesmo arquivar a pessoa " + selecionada.getNomeRazao() + "?", ButtonType.YES, ButtonType.NO);
        confirmacao.showAndWait().ifPresent(resposta -> {
            if (resposta == ButtonType.YES) {
                // Executa a exclusão lógica em background para não congelar o sistema
                AppExecutors.getDatabaseExecutor().submit(() -> {
                    try {
                        // 🛠️ CORRIGIDO: O método real do seu serviço exige apenas o ID da Pessoa
                        pessoaService.deletarPessoa(selecionada.getId());
                        
                        // Atualiza a tabela chamando novamente o método assíncrono
                        carregarDados();
                    } catch (SQLException e) {
                        Platform.runLater(() -> exibirAlerta("Erro", "Erro ao executar exclusão: " + e.getMessage(), Alert.AlertType.ERROR));
                    }
                });
            }
        });
    }

    private void boxAvisoSelecao() {
        exibirAlerta("Aviso", "Selecione uma pessoa na tabela para realizar esta ação.", Alert.AlertType.WARNING);
    }

    private void exibirAlerta(String titulo, String conteudo, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(conteudo);
        alert.showAndWait();
    }
}