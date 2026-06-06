package br.com.yat.ecosystemcore.ui.modules.system;

import br.com.yat.ecosystemcore.domain.dto.SessaoAtivaProjecaoDTO;
import br.com.yat.ecosystemcore.domain.entity.TentativaLoginLog;
import br.com.yat.ecosystemcore.infrastructure.concurrent.AppExecutors;
import br.com.yat.ecosystemcore.ui.core.ScreenLifecycle;
import br.com.yat.ecosystemcore.service.external.AuditoriaSessaoService;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MonitoramentoSessoesController implements ScreenLifecycle {

    @FXML private TableView<SessaoAtivaProjecaoDTO> tableSessoes;
    @FXML private TableColumn<SessaoAtivaProjecaoDTO, String> colUsuario, colEmail, colEmpresa, colIp, colDispositivo;
    @FXML private TableColumn<SessaoAtivaProjecaoDTO, LocalDateTime> colCriadoEm;

    @FXML private TableView<TentativaLoginLog> tableTentativas;
    @FXML private TableColumn<TentativaLoginLog, String> colTentativaEmail, colTentativaIp, colTentativaMotivo, colTentativaDispositivo;
    @FXML private TableColumn<TentativaLoginLog, Boolean> colTentativaStatus;
    @FXML private TableColumn<TentativaLoginLog, LocalDateTime> colTentativaData;

    @FXML private Button btnDerrubar;

    private final AuditoriaSessaoService auditoriaService = new AuditoriaSessaoService();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML
    public void initialize() {
        configurarColunasTabelas();

        // Listener reativo para controle do estado do botão de revogação
        tableSessoes.getSelectionModel().selectedItemProperty().addListener((obs, antigo, selecionado) -> 
            btnDerrubar.setDisable(selecionado == null)
        );
    }

    @Override
    public void onShow() {
        carregarDadosPainel();
    }

    @Override
    public void onHide() {
        // Gancho para limpeza de listeners globais se necessário
    }

    @Override
    public void onDestroy() {
        tableSessoes.getItems().clear();
        tableTentativas.getItems().clear();
    }

    @FXML
    private void carregarDadosPainel() {
        // Bloqueia interações temporariamente para evitar concorrência visual (Double Click)
        tableSessoes.setDisable(true);
        tableTentativas.setDisable(true);

        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                List<SessaoAtivaProjecaoDTO> sessoesAtivas = auditoriaService.obterSessoesAtivas();
                List<TentativaLoginLog> historicoLogins = auditoriaService.obterHistoricoTentativas();

                // Atualiza de forma segura os componentes na Thread Gráfica (UI Thread)
                Platform.runLater(() -> {
                    tableSessoes.setItems(FXCollections.observableArrayList(sessoesAtivas));
                    tableTentativas.setItems(FXCollections.observableArrayList(historicoLogins));
                    destravarTabelas();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    destravarTabelas();
                    exibirAlerta("Erro Crítico", "Falha ao ler dados de auditoria: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        });
    }

    @FXML
    private void handleDerrubarSessao() {
        SessaoAtivaProjecaoDTO sessaoSelecionada = tableSessoes.getSelectionModel().getSelectedItem();
        if (sessaoSelecionada == null) return;

        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION, 
                "Deseja encerrar forçadamente a sessão do usuário " + sessaoSelecionada.nomeUsuario() + "?\nO terminal será desconectado.", 
                ButtonType.YES, ButtonType.NO);
        confirmacao.setHeaderText(null);
        
        confirmacao.showAndWait().ifPresent(resposta -> {
            if (resposta == ButtonType.YES) {
                AppExecutors.getDatabaseExecutor().execute(() -> {
                    try {
                        auditoriaService.derrubarSessao(sessaoSelecionada.sessaoId());
                        Platform.runLater(() -> {
                            exibirAlerta("Sessão Encerrada", "A sessão foi revogada com sucesso no banco de dados.", Alert.AlertType.INFORMATION);
                            carregarDadosPainel();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> 
                            exibirAlerta("Erro", "Não foi possível derrubar a sessão: " + e.getMessage(), Alert.AlertType.ERROR)
                        );
                    }
                });
            }
        });
    }

    private void configurarColunasTabelas() {
        // 🔒 SOLUÇÃO: Expressões Lambda nativas impedem erros de reflexão caso o DTO seja um Record
        colUsuario.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().nomeUsuario()));
        colEmail.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().emailUsuario()));
        colEmpresa.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().nomeEmpresa()));
        colIp.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().ipOrigem()));
        colDispositivo.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().dispositivoInfo()));
        
        colCriadoEm.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().criadoEm()));
        colCriadoEm.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatter.format(item));
            }
        });

        // Mapeamento explícito da Tabela de Logs de Tentativas
        colTentativaEmail.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmailTentativa()));
        colTentativaIp.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getIpOrigem()));
        colTentativaMotivo.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMotivoFalha()));
        colTentativaDispositivo.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDispositivoInfo()));
        
        colTentativaData.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDataHora()));
        colTentativaData.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatter.format(item));
            }
        });

        // Renderização customizada de Sucesso/Falha do Log de Acessos
        colTentativaStatus.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().isSucesso()));
        colTentativaStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean sucesso, boolean empty) {
                super.updateItem(sucesso, empty);
                if (empty || sucesso == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(""); 
                } else {
                    if (sucesso) {
                        setText("SUCESSO");
                        setTextFill(Color.GREEN);
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setText("FALHA");
                        setTextFill(Color.RED);
                        setStyle("-fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void destravarTabelas() {
        tableSessoes.setDisable(false);
        tableTentativas.setDisable(false);
    }

    private void exibirAlerta(String titulo, String msg, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}