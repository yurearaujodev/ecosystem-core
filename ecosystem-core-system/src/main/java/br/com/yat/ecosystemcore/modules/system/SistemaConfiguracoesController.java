package br.com.yat.ecosystemcore.ui.modules.system;

import br.com.yat.ecosystemcore.application.system.dto.SistemaConfigDTO;
import br.com.yat.ecosystemcore.modules.sistema.service.GestaoSistemaUseCase;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SistemaConfiguracoesController {
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final GestaoSistemaUseCase useCase = new GestaoSistemaUseCase();

    @FXML private TableView<SistemaConfigDTO> tblConfig;
    @FXML private TableColumn<SistemaConfigDTO, String> colChave;
    @FXML private TableColumn<SistemaConfigDTO, String> colValor;
    @FXML private TableColumn<SistemaConfigDTO, String> colDesc;

    @FXML
    public void initialize() {
        colChave.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().chave()));
        colValor.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().valorConfig()));
        colDesc.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().descricao()));
        carregarDados();
    }

    @FXML
    public void carregarDados() {
        dbExecutor.submit(() -> {
            try {
                List<SistemaConfigDTO> dados = useCase.obterConfiguracoesGlobais();
                Platform.runLater(() -> tblConfig.setItems(FXCollections.observableArrayList(dados)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
