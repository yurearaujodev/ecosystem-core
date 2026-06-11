package br.com.yat.ecosystemcore.modules.system;

import br.com.yat.ecosystemcore.application.system.dto.OutboxEventDTO;
import br.com.yat.ecosystemcore.modules.sistema.service.GestaoSistemaUseCase;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SistemaOutboxEventsController {
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final GestaoSistemaUseCase useCase = new GestaoSistemaUseCase();

    @FXML private TableView<OutboxEventDTO> tblOutbox;
    @FXML private TableColumn<OutboxEventDTO, Long> colId;
    @FXML private TableColumn<OutboxEventDTO, String> colTenant;
    @FXML private TableColumn<OutboxEventDTO, String> colTipo;
    @FXML private TableColumn<OutboxEventDTO, String> colPayload;
    @FXML private TableColumn<OutboxEventDTO, Integer> colStatus;
    @FXML private TableColumn<OutboxEventDTO, LocalDateTime> colCriado;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().id()));
        colTenant.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().tenantId()));
        colTipo.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().eventoTipo()));
        colPayload.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().payload()));
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().processado()).asObject());
        colCriado.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().criadoEm()));
        carregarDados();
    }

    @FXML
    public void carregarDados() {
        dbExecutor.submit(() -> {
            try {
                List<OutboxEventDTO> dados = useCase.obterEventosOutbox(0); 
                Platform.runLater(() -> tblOutbox.setItems(FXCollections.observableArrayList(dados)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
