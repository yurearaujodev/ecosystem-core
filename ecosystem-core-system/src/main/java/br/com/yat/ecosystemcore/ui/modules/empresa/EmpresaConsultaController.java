package br.com.yat.ecosystemcore.ui.modules.empresa;

import br.com.yat.ecosystemcore.domain.entity.Empresa;
import br.com.yat.ecosystemcore.service.external.EmpresaService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.util.List;

public class EmpresaConsultaController {

    @FXML private TextField txtPesquisa;
    @FXML private Button btnNovaEmpresa;
    @FXML private TableView<Empresa> tblEmpresas;
    @FXML private TableColumn<Empresa, Long> colId;
    @FXML private TableColumn<Empresa, String> colCnpj;
    @FXML private TableColumn<Empresa, String> colRazaoSocial;
    @FXML private TableColumn<Empresa, String> colNomeFantasia;
    @FXML private TableColumn<Empresa, String> colCidade;
    @FXML private TableColumn<Empresa, Void> colAcoes;

    private final EmpresaService empresaService = new EmpresaService();
    private final ObservableList<Empresa> masterData = FXCollections.observableArrayList();

//    @FXML
//    public void initialize() {
//        configurarColunas();
//     // MELHORIA: Mensagem amigável quando a tabela não tiver registros
//        tblEmpresas.setPlaceholder(new Label("Nenhuma empresa cadastrada para esta conta corporativa."));
//        carregarDados();
//        configurarFiltroPesquisa();
//    }
    
    @FXML
    public void initialize() {
        // 1. Criamos o label com estilo de texto visível e definimos na tabela
        Label placeholderLabel = new Label("Nenhuma empresa cadastrada para esta conta corporativa.");
        placeholderLabel.setStyle("-fx-text-fill: #6C757D; -fx-font-size: 14px;");
        tblEmpresas.setPlaceholder(placeholderLabel);
        
        // 2. Configuramos as colunas normalmente
        configurarColunas();
        
        // 3. Buscamos do banco (retornando as 0 empresas do log)
        carregarDados();
        
        // 4. Montamos o filtro
        configurarFiltroPesquisa();
        
        // 5. Força o redesenho completo dos nós visuais da tabela no boot
        tblEmpresas.requestLayout();
    }

    private void configurarColunas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCnpj.setCellValueFactory(new PropertyValueFactory<>("cnpj"));
        colRazaoSocial.setCellValueFactory(new PropertyValueFactory<>("razaoSocial"));
        colNomeFantasia.setCellValueFactory(new PropertyValueFactory<>("nomeFantasia"));
        
        // CORREÇÃO AQUI: Montamos a string final antes de passar para a Lambda do Binding
        colCidade.setCellValueFactory(cellData -> {
            Empresa emp = cellData.getValue();
            
            String cidade = emp.getCidade() != null ? emp.getCidade() : "";
            String estado = emp.getEstado() != null ? emp.getEstado() : "";
            
            // Cria a string final que não será mais alterada
            final String resultadoCidadeUf = (cidade.isEmpty() || estado.isEmpty()) 
                    ? cidade + estado 
                    : cidade + "/" + estado;

            // Agora passamos a variável final segura para a Lambda interna
            return javafx.beans.binding.Bindings.createStringBinding(() -> resultadoCidadeUf);
        });

        adicionarBotoesAcao();
    }
    private void carregarDados() {
        try {
            masterData.clear();
            
            List<Empresa> empresas = empresaService.listarEmpresasDoTenantAtivo();
            
            // Log temporário no console para você ver se o banco está respondendo
            System.out.println("DEBUG: Quantidade de empresas retornadas do banco: " + (empresas != null ? empresas.size() : 0));
            
            if (empresas != null && !empresas.isEmpty()) {
                masterData.addAll(empresas);
            }
            
        } catch (Exception e) {
            // Imprime o erro completo no console da IDE para descobrirmos o culpado real
            System.err.println("❌ ERRO GRAVE NO CARREGAMENTO:");
            e.printStackTrace(); 
            
            exibirAlertaError("Erro de Carregamento", "Não foi possível carregar a listagem.", e.getMessage());
        }
    }

//    private void carregarDados() {
//        try {
//            masterData.clear();
//            List<Empresa> empresas = empresaService.listarEmpresasDoTenantAtivo();
//            masterData.addAll(empresas);
//            tblEmpresas.setItems(masterData);
//        } catch (Exception e) {
//            exibirAlertaError("Erro de Carregamento", "Não foi possível carregar a listagem de empresas.", e.getMessage());
//        }
//    }

//    private void configurarFiltroPesquisa() {
//        FilteredList<Empresa> filteredData = new FilteredList<>(masterData, p -> true);
//        txtPesquisa.textProperty().addListener((observable, oldValue, newValue) -> {
//            filteredData.setPredicate(empresa -> {
//                if (newValue == null || newValue.trim().isEmpty()) return true;
//                
//                String lowerCaseFilter = newValue.toLowerCase().trim();
//                if (empresa.getRazaoSocial().toLowerCase().contains(lowerCaseFilter)) return true;
//                if (empresa.getCnpj().contains(lowerCaseFilter)) return true;
//                if (empresa.getUuidPublico().contains(lowerCaseFilter)) return true;
//                return false;
//            });
//        });
//        tblEmpresas.setItems(filteredData);
//    }
    
    private void configurarFiltroPesquisa() {
        // Cria a lista filtrada baseada no nosso masterData
        FilteredList<Empresa> filteredData = new FilteredList<>(masterData, p -> true);
        
        // Ouvinte para mudanças no campo de texto de pesquisa
        txtPesquisa.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(empresa -> {
                if (newValue == null || newValue.trim().isEmpty()) {
                    return true;
                }
                
                String lowerCaseFilter = newValue.toLowerCase().trim();
                if (empresa.getRazaoSocial().toLowerCase().contains(lowerCaseFilter)) return true;
                if (empresa.getCnpj().contains(lowerCaseFilter)) return true;
                if (empresa.getUuidPublico().contains(lowerCaseFilter)) return true;
                return false;
            });
        });
        
        // Vincula a lista filtrada à tabela
        tblEmpresas.setItems(filteredData);
        
        // Força a tabela a atualizar e reconhecer que está vazia no momento do boot
        tblEmpresas.refresh();
    }

    private void adicionarBotoesAcao() {
        colAcoes.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Empresa, Void> call(final TableColumn<Empresa, Void> param) {
                return new TableCell<>() {
                    private final Button btnEditar = new Button("✏️");
                    private final Button btnExcluir = new Button("🗑️");
                    private final HBox container = new HBox(8, btnEditar, btnExcluir);

                    {
                        btnEditar.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 4 8 4 8;");
                        btnExcluir.setStyle("-fx-background-color: #DC3545; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 4 8 4 8;");
                        
                        btnEditar.setOnAction(event -> {
                            Empresa empresa = getTableView().getItems().get(getIndex());
                            onEditarEmpresa(empresa);
                        });

                        btnExcluir.setOnAction(event -> {
                            Empresa empresa = getTableView().getItems().get(getIndex());
                            onExcluirEmpresa(empresa);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : container);
                    }
                };
            }
        });
    }
    
    @FXML
    void onNovaEmpresa(ActionEvent event) {
        abrirDialogCadastro(new Empresa());
    }

    private void onEditarEmpresa(Empresa empresa) {
        abrirDialogCadastro(empresa);
    }

    private void abrirDialogCadastro(Empresa empresa) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/ui/modules/empresa-cadastro-dialog.fxml"));
            javafx.scene.Parent root = loader.load();
            
            EmpresaCadastroController controller = loader.getController();
            controller.setEmpresaAlvo(empresa);

            Stage dialogStage = new Stage();
            dialogStage.setTitle(empresa.getId() == null ? "Cadastrar Empresa" : "Modificar Empresa");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initOwner(tblEmpresas.getScene().getWindow());
            dialogStage.setScene(new javafx.scene.Scene(root));
            
            // Trava o redimensionamento para manter o layout intocado e limpo
            dialogStage.setResizable(false); 
            dialogStage.showAndWait();

            // Se o usuário clicou em salvar com sucesso, atualiza a grade de dados imediatamente
            if (controller.isSalvoComSucesso()) {
                carregarDados();
            }
        } catch (java.io.IOException e) {
            exibirAlertaError("Erro de Infraestrutura", "Não foi possível carregar o arquivo visual do formulário.", e.getMessage());
        }
    }

    private void onExcluirEmpresa(Empresa empresa) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmação de Exclusão");
        alert.setHeaderText("Deseja realmente arquivar esta empresa?");
        alert.setContentText("Empresa: " + empresa.getRazaoSocial() + "\nEsta ação poderá ser revertida por administradores.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                empresaService.excluirEmpresa(empresa.getId());
                carregarDados(); // Recarrega a grade aplicando o Soft Delete imediato
            } catch (Exception e) {
                exibirAlertaError("Falha na Operação", "Não foi possível remover logicamente o registro.", e.getMessage());
            }
        }
    }

    private void exibirAlertaError(String titulo, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}