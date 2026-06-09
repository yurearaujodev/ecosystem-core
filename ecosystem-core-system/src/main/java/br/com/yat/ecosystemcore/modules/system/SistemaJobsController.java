package br.com.yat.ecosystemcore.ui.modules.system;

import br.com.yat.ecosystemcore.application.system.dto.JobExecucaoDTO;
import br.com.yat.ecosystemcore.application.system.usecase.GestaoSistemaUseCase;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SistemaJobsController {
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final GestaoSistemaUseCase useCase = new GestaoSistemaUseCase();

    @FXML private TableView<JobExecucaoDTO> tblJobs;
    @FXML private TableColumn<JobExecucaoDTO, Long> colId;
    @FXML private TableColumn<JobExecucaoDTO, String> colTipo;
    @FXML private TableColumn<JobExecucaoDTO, String> colStatus;
    @FXML private TableColumn<JobExecucaoDTO, LocalDateTime> colInicio;
    @FXML private TableColumn<JobExecucaoDTO, LocalDateTime> colFim;
    @FXML private TableColumn<JobExecucaoDTO, String> colErro;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().id()));
        colTipo.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().tipoJob()));
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().status()));
        colInicio.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().inicio()));
        colFim.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().fim()));
        colErro.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().erroMensagem()));
        carregarDados();
    }

    @FXML
    public void carregarDados() {
        dbExecutor.submit(() -> {
            try {
                // CORRIGIDO: O nome correto definido no UseCase é obterJobsAgendados()
                List<JobExecucaoDTO> dados = useCase.obterJobsAgendados(); 
                Platform.runLater(() -> tblJobs.setItems(FXCollections.observableArrayList(dados)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
