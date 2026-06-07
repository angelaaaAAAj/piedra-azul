package com.piedraazul.ui.auditoria;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class AuditoriaApp extends Application {

    private final TableView<Auditoria> tabla = new TableView<>();
    private final ObservableList<Auditoria> auditorias = FXCollections.observableArrayList();

    private final ComboBox<String> cbTipoEvento = new ComboBox<>();
    private final TextField txtUsuario = new TextField();
    private final TextField txtMicroservicio = new TextField();

    private final String API_URL = "http://localhost:8080/api/auditoria";

    @Override
    public void start(Stage stage) {

        Label titulo = new Label("Auditoría del Sistema - Piedra Azul");
        titulo.setStyle("""
                -fx-font-size: 24px;
                -fx-font-weight: bold;
                -fx-text-fill: #1E3A5F;
        """);

        configurarTabla();

        cbTipoEvento.getItems().addAll(
                "PACIENTE_REGISTRADO",
                "CITA_AGENDADA",
                "CITA_CANCELADA",
                "CITA_REAGENDADA",
                "LOGIN_EXITOSO",
                "LOGIN_FALLIDO",
                "USUARIO_CREADO",
                "USUARIO_DESACTIVADO",
                "HISTORIAL_CREADO",
                "HISTORIAL_MODIFICADO"
        );
        cbTipoEvento.setPromptText("Tipo de evento");

        txtUsuario.setPromptText("Usuario");
        txtMicroservicio.setPromptText("Microservicio");

        Button btnCargar = new Button("Cargar auditorías");
        btnCargar.setOnAction(e -> cargarAuditorias());

        Button btnFiltrarTipo = new Button("Filtrar por tipo");
        btnFiltrarTipo.setOnAction(e -> filtrarPorTipo());

        Button btnFiltrarUsuario = new Button("Filtrar por usuario");
        btnFiltrarUsuario.setOnAction(e -> filtrarPorUsuario());

        Button btnFiltrarMicroservicio = new Button("Filtrar por microservicio");
        btnFiltrarMicroservicio.setOnAction(e -> filtrarPorMicroservicio());

        Button btnMostrarTodos = new Button("Mostrar todos");
        btnMostrarTodos.setOnAction(e -> cargarAuditorias());

        String estiloBoton = """
                -fx-background-color: #1565C0;
                -fx-text-fill: white;
                -fx-font-weight: bold;
        """;

        btnCargar.setStyle(estiloBoton);
        btnFiltrarTipo.setStyle(estiloBoton);
        btnFiltrarUsuario.setStyle(estiloBoton);
        btnFiltrarMicroservicio.setStyle(estiloBoton);
        btnMostrarTodos.setStyle(estiloBoton);

        GridPane filtros = new GridPane();
        filtros.setHgap(10);
        filtros.setVgap(10);
        filtros.add(cbTipoEvento, 0, 0);
        filtros.add(btnFiltrarTipo, 1, 0);
        filtros.add(txtUsuario, 0, 1);
        filtros.add(btnFiltrarUsuario, 1, 1);
        filtros.add(txtMicroservicio, 0, 2);
        filtros.add(btnFiltrarMicroservicio, 1, 2);
        filtros.add(btnCargar, 0, 3);
        filtros.add(btnMostrarTodos, 1, 3);

        VBox root = new VBox(15, titulo, filtros, tabla);
        root.setStyle("""
                -fx-padding: 20;
                -fx-background-color: #F4F6F9;
        """);

        Scene scene = new Scene(root, 1050, 550);

        stage.setTitle("Auditoría - Piedra Azul");
        stage.setScene(scene);
        stage.show();

        cargarAuditorias();
    }

    private void configurarTabla() {
        TableColumn<Auditoria, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Auditoria, String> colTipo = new TableColumn<>("Tipo evento");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoEvento"));

        TableColumn<Auditoria, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colDescripcion.setPrefWidth(280);

        TableColumn<Auditoria, Long> colEntidad = new TableColumn<>("Entidad ID");
        colEntidad.setCellValueFactory(new PropertyValueFactory<>("entidadId"));

        TableColumn<Auditoria, String> colUsuario = new TableColumn<>("Realizado por");
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("realizadoPor"));

        TableColumn<Auditoria, String> colMicroservicio = new TableColumn<>("Microservicio");
        colMicroservicio.setCellValueFactory(new PropertyValueFactory<>("microservicioOrigen"));

        TableColumn<Auditoria, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaEvento"));
        colFecha.setPrefWidth(180);

        tabla.getColumns().addAll(
                colId,
                colTipo,
                colDescripcion,
                colEntidad,
                colUsuario,
                colMicroservicio,
                colFecha
        );

        tabla.setItems(auditorias);
    }

    private void cargarAuditorias() {
        consumirLista(API_URL);
    }

    private void filtrarPorTipo() {
        if (cbTipoEvento.getValue() == null) {
            mostrarAlerta("Error", "Seleccione un tipo de evento");
            return;
        }

        consumirLista(API_URL + "/tipo/" + cbTipoEvento.getValue());
    }

    private void filtrarPorUsuario() {
        String usuario = txtUsuario.getText().trim();

        if (usuario.isBlank()) {
            mostrarAlerta("Error", "Ingrese un usuario");
            return;
        }

        consumirLista(API_URL + "/usuario/" + usuario);
    }

    private void filtrarPorMicroservicio() {
        String microservicio = txtMicroservicio.getText().trim();

        if (microservicio.isBlank()) {
            mostrarAlerta("Error", "Ingrese un microservicio");
            return;
        }

        consumirLista(API_URL + "/microservicio/" + microservicio);
    }

    private void consumirLista(String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = requestAutenticado(url)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                mostrarAlerta("Error", response.body());
                return;
            }

            ObjectMapper mapper = new ObjectMapper();

            List<Auditoria> lista = mapper.readValue(
                    response.body(),
                    new TypeReference<List<Auditoria>>() {}
            );

            auditorias.setAll(lista);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo conectar con ms-auditoria");
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private HttpRequest.Builder requestAutenticado(String url) {
        String token = com.piedraazul.ui.app.PiedraAzulApp.getToken();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    public static void main(String[] args) {
        launch();
    }
}
