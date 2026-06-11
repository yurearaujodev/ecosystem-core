package br.com.yat.ecosystemcore.modules.system;

import br.com.yat.ecosystemcore.application.system.dto.SchemaVersionDTO;
import br.com.yat.ecosystemcore.modules.sistema.service.GestaoSistemaUseCase;
import br.com.yat.ecosystemcore.shared.database.ConnectionFactory;
import br.com.yat.ecosystemcore.shared.database.DatabaseStatus;
import br.com.yat.ecosystemcore.shared.util.DatabaseConfigLoader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SistemaSchemaVersaoController {

    private final ExecutorService asyncExecutor = Executors.newSingleThreadExecutor();
    private final GestaoSistemaUseCase useCase = new GestaoSistemaUseCase();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // Substitua pelas variáveis da sua organização/projeto real
  //  private static final String GIT_USER = "SEU_USUARIO_OU_ORGANIZACAO";
  //  private static final String GIT_REPO = "SEU_REPOSITORIO";
private static final String GIT_USER = "yurearaujodev";
private static final String GIT_REPO = "ecosystem-core";

    // Componentes Visuais dos Cards
    @FXML private Label lblGitBranch;
    @FXML private Label lblGitCommitLocal;
    @FXML private Label lblGitStatus;
    @FXML private Label lblDbUrl;
    @FXML private Label lblDbUser;
    @FXML private Label lblDbStatus;

    // Tabela do Schema
    @FXML private TableView<SchemaVersionDTO> tblSchema;
    @FXML private TableColumn<SchemaVersionDTO, Long> colId;
    @FXML private TableColumn<SchemaVersionDTO, String> colVersao;
    @FXML private TableColumn<SchemaVersionDTO, String> colDesc;
    @FXML private TableColumn<SchemaVersionDTO, String> colAutor;
    @FXML private TableColumn<SchemaVersionDTO, LocalDateTime> colData;

    @FXML
    public void initialize() {
        // Mapeamento das colunas da tabela
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colVersao.setCellValueFactory(new PropertyValueFactory<>("versao"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colAutor.setCellValueFactory(new PropertyValueFactory<>("executadoPor"));
        colData.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        atualizarTudo();
    }

    @FXML
    public void atualizarTudo() {
        carregarHistoricoSchema();
        carregarDadosInfraBanco();
        verificarStatusGit();
    }

    private void carregarHistoricoSchema() {
        asyncExecutor.submit(() -> {
            try {
                List<SchemaVersionDTO> dados = useCase.obterHistoricoDoSchema();
                Platform.runLater(() -> tblSchema.setItems(FXCollections.observableArrayList(dados)));
            } catch (Exception e) {
                Platform.runLater(() -> tblSchema.setPlaceholder(new Label("Erro ao ler tabela schema_version: " + e.getMessage())));
            }
        });
    }

    private void carregarDadosInfraBanco() {
        asyncExecutor.submit(() -> {
            try {
                Properties props = DatabaseConfigLoader.loadConfig();
                String url = props.getProperty("db.url", "");
                String user = props.getProperty("db.user", "Desconhecido");

                // Lógica mais robusta para definir o ambiente
                String nomeAmbiente;
                String cor;

                if (url.contains("localhost") || url.contains("127.0.0.1")) {
                    nomeAmbiente = "LOCAL";
                    cor = "#3498db"; // Azul
                } else if (url.contains("homolog") || url.contains("teste")) {
                    nomeAmbiente = "HOMOLOGAÇÃO";
                    cor = "#f39c12"; // Laranja
                } else {
                    nomeAmbiente = "PRODUÇÃO";
                    cor = "#e74c3c"; // Vermelho
                }

                DatabaseStatus status = ConnectionFactory.reloadAndCheck();

                Platform.runLater(() -> {
                    // Exibe de forma limpa: Nome | URL
                    lblDbUrl.setText(nomeAmbiente + " | " + url.substring(0, Math.min(url.length(), 40)) + "...");
                    lblDbUser.setText(user);
                    
                    if (status.isAvailable()) {
                        lblDbStatus.setText("🟢 ONLINE");
                        lblDbStatus.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                    } else {
                        lblDbStatus.setText("🔴 OFFLINE");
                        lblDbStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblDbStatus.setText("🔴 ERRO DE INFRAESTRUTURA");
                    lblDbStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                });
            }
        });
    }

    private void verificarStatusGit() {
        asyncExecutor.submit(() -> {
            String branchLocal = "Desconhecida";
            String commitLocal = "Desconhecido";

            // 1. Tenta carregar o arquivo gerado pelo plugin do Maven
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("git.properties")) {
                if (is != null) {
                    Properties gitProps = new Properties();
                    // Use um Reader com UTF-8 para evitar caracteres estranhos
                    try (java.io.InputStreamReader reader = new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8)) {
                        gitProps.load(reader);
                    }
                    branchLocal = gitProps.getProperty("git.branch", "main");
                    // Pega o hash curto (7 caracteres) igual ao GitHub usa
                    commitLocal = gitProps.getProperty("git.commit.id.abbrev", 
                                  gitProps.getProperty("git.commit.id", "Desconhecido"));
                } else {
                    commitLocal = "Execução Local (Sem Build Maven)";
                    branchLocal = "main";
                }
            } catch (Exception e) {
                commitLocal = "Erro ao ler metadados Git";
            }

            final String finalBranch = branchLocal;
            final String finalCommit = commitLocal;

            Platform.runLater(() -> {
                lblGitBranch.setText(finalBranch);
                lblGitCommitLocal.setText(finalCommit);
            });

            // Se não houver dados válidos do Maven, cancela a chamada HTTP para evitar falsos positivos
            if (finalCommit.contains("Execução Local") || finalCommit.equals("Desconhecido")) {
                Platform.runLater(() -> {
                    lblGitStatus.setText("⚪ Não monitorado em tempo de desenvolvimento");
                    lblGitStatus.setStyle("-fx-text-fill: #7f8c8d;");
                });
                return;
            }

            // 2. Compara com a API pública do GitHub de forma assíncrona
            try {
                String urlApi = String.format("https://api.github.com/repos/%s/%s/branches/%s", GIT_USER, GIT_REPO, finalBranch);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlApi))
                        .header("Accept", "application/vnd.github.v3+json")
                        .header("User-Agent", "JavaFX-Ecosystem-Core")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String json = response.body();
                    // Captura o campo "sha" do JSON de forma manual e limpa para evitar dependências pesadas de Jackson/Gson
                    String shaRemoto = "";
                    if (json.contains("\"sha\"")) {
                        int idx = json.indexOf("\"sha\"");
                        shaRemoto = json.substring(idx + 7, idx + 14); // Pega os primeiros 7 caracteres do hash remoto
                    }

                    final String finalShaRemoto = shaRemoto;
                    Platform.runLater(() -> {
                        if (finalCommit.equalsIgnoreCase(finalShaRemoto)) {
                            lblGitStatus.setText("🟢 ATUALIZADO COM O GITHUB");
                            lblGitStatus.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                        } else {
                            lblGitStatus.setText("🟡 DESATUALIZADO (Existe nova versão pendente no GitHub)");
                            lblGitStatus.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        lblGitStatus.setText("⚪ Repositório Privado ou API Limitada (Status HTTP: " + response.statusCode() + ")");
                        lblGitStatus.setStyle("-fx-text-fill: #7f8c8d;");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblGitStatus.setText("❌ Falha ao conectar na API do GitHub");
                    lblGitStatus.setStyle("-fx-text-fill: #c0392b;");
                });
            }
        });
    }

    public void shutdown() {
        asyncExecutor.shutdown();
    }
}

