package com.piedraazul.ui.pacientes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class PacienteApp extends Application {

    private final TableView<Paciente> tabla = new TableView<>();
    private final ObservableList<Paciente> pacientes = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final TextField txtNombre = new TextField();
    private final TextField txtApellido = new TextField();
    private final TextField txtDocumento = new TextField();
    private final TextField txtTelefono = new TextField();
    private final ComboBox<String> cbGenero = new ComboBox<>();
    private final DatePicker dpFechaNacimiento = new DatePicker();
    private final TextField txtEmail = new TextField();
    private final TextField txtEps = new TextField();
    private final TextField txtBuscarDocumento = new TextField();
    private final ComboBox<String> cbNuevoEstado = new ComboBox<>();
    private Long pacienteEditandoId = null;
    private Label lblEstadoFormulario = new Label();

    @Override
    public void start(Stage stage) {

        // -- TÍTULO --
        Label titulo = new Label("Gestión de Pacientes");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 22));
        titulo.setTextFill(Color.web("#4C1D95"));

        Label subtitulo = new Label("Clínica Piedra Azul");
        subtitulo.setFont(Font.font("System", 13));
        subtitulo.setTextFill(Color.web("#6B7280"));

        VBox encabezado = new VBox(4, titulo, subtitulo);

        // -- TABLA --
        TableColumn<Paciente, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);

        TableColumn<Paciente, String> colNombre = new TableColumn<>("Nombre completo");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colNombre.setPrefWidth(180);

        TableColumn<Paciente, String> colDocumento = new TableColumn<>("Documento");
        colDocumento.setCellValueFactory(new PropertyValueFactory<>("numeroDocumento"));
        colDocumento.setPrefWidth(120);

        TableColumn<Paciente, String> colTelefono = new TableColumn<>("Celular");
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colTelefono.setPrefWidth(110);

        TableColumn<Paciente, String> colGenero = new TableColumn<>("Género");
        colGenero.setCellValueFactory(new PropertyValueFactory<>("genero"));
        colGenero.setPrefWidth(90);

        TableColumn<Paciente, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setPrefWidth(160);

        TableColumn<Paciente, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setPrefWidth(110);

        tabla.getColumns().addAll(colId, colNombre, colDocumento,
                colTelefono, colGenero, colEmail, colEstado);
        tabla.setItems(pacientes);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Al seleccionar fila carga el paciente para editar
        tabla.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, seleccionado) -> {
                    if (seleccionado != null) {
                        cargarPacienteParaEditar(seleccionado);
                    }
                });

        // -- BUSCADOR --
        txtBuscarDocumento.setPromptText("Buscar por número de documento...");
        txtBuscarDocumento.setStyle(campoEstilo());
        txtBuscarDocumento.setPrefWidth(280);

        Button btnBuscar = new Button("🔍 Buscar");
        btnBuscar.setStyle(estiloBoton("#7B2FBE"));
        btnBuscar.setOnAction(e -> buscarPorDocumento());

        Button btnMostrarTodos = new Button("Ver todos");
        btnMostrarTodos.setStyle(estiloBotonSecundario());
        btnMostrarTodos.setOnAction(e -> cargarPacientes());

        HBox buscador = new HBox(10, txtBuscarDocumento, btnBuscar, btnMostrarTodos);
        buscador.setAlignment(Pos.CENTER_LEFT);

        // -- PANEL FORMULARIO --
        lblEstadoFormulario.setFont(Font.font("System", FontWeight.BOLD, 12));
        lblEstadoFormulario.setTextFill(Color.web("#7B2FBE"));
        lblEstadoFormulario.setText("Nuevo paciente");

        txtNombre.setPromptText("Nombre *");
        txtNombre.setStyle(campoEstilo());
        txtApellido.setPromptText("Apellido *");
        txtApellido.setStyle(campoEstilo());
        txtDocumento.setPromptText("Documento *");
        txtDocumento.setStyle(campoEstilo());
        txtTelefono.setPromptText("Celular *");
        txtTelefono.setStyle(campoEstilo());
        txtEmail.setPromptText("Email *");
        txtEmail.setStyle(campoEstilo());
        txtEps.setPromptText("EPS (opcional)");
        txtEps.setStyle(campoEstilo());
        cbGenero.getItems().addAll("HOMBRE", "MUJER", "OTRO");
        cbGenero.setPromptText("Género *");
        cbGenero.setMaxWidth(Double.MAX_VALUE);
        dpFechaNacimiento.setPromptText("Fecha de nacimiento *");
        dpFechaNacimiento.setMaxWidth(Double.MAX_VALUE);

        GridPane formulario = new GridPane();
        formulario.setHgap(12);
        formulario.setVgap(10);
        formulario.add(etiqueta("Nombre *"), 0, 0);
        formulario.add(txtNombre, 0, 1);
        formulario.add(etiqueta("Apellido *"), 1, 0);
        formulario.add(txtApellido, 1, 1);
        formulario.add(etiqueta("Documento *"), 0, 2);
        formulario.add(txtDocumento, 0, 3);
        formulario.add(etiqueta("Celular *"), 1, 2);
        formulario.add(txtTelefono, 1, 3);
        formulario.add(etiqueta("Género *"), 0, 4);
        formulario.add(cbGenero, 0, 5);
        formulario.add(etiqueta("Fecha de nacimiento *"), 1, 4);
        formulario.add(dpFechaNacimiento, 1, 5);
        formulario.add(etiqueta("Email *"), 0, 6);
        formulario.add(txtEmail, 0, 7);
        formulario.add(etiqueta("EPS (opcional)"), 1, 6);
        formulario.add(txtEps, 1, 7);

        Button btnGuardar = new Button("💾 Guardar paciente");
        btnGuardar.setStyle(estiloBoton("#7B2FBE"));
        btnGuardar.setPrefWidth(200);
        btnGuardar.setOnAction(e -> registrarPaciente());

        Button btnLimpiar = new Button("✕ Cancelar");
        btnLimpiar.setStyle(estiloBotonSecundario());
        btnLimpiar.setOnAction(e -> {
            limpiarFormulario();
            lblEstadoFormulario.setText("Nuevo paciente");
        });

        HBox botonesFormulario = new HBox(10, btnGuardar, btnLimpiar);

        // -- CAMBIAR ESTADO --
        cbNuevoEstado.getItems().addAll(
                "ACTIVO", "INACTIVO", "EN_TRATAMIENTO", "DADO_DE_ALTA");
        cbNuevoEstado.setPromptText("Nuevo estado");
        cbNuevoEstado.setMaxWidth(Double.MAX_VALUE);

        Button btnCambiarEstado = new Button("Cambiar estado");
        btnCambiarEstado.setStyle(estiloBoton("#2E7D32"));
        btnCambiarEstado.setOnAction(e -> cambiarEstado());

        Label lblEstadoHint = new Label(
                "Seleccione un paciente de la tabla para cambiar su estado");
        lblEstadoHint.setFont(Font.font("System", 11));
        lblEstadoHint.setTextFill(Color.web("#6B7280"));

        HBox panelEstado = new HBox(10, cbNuevoEstado, btnCambiarEstado);
        panelEstado.setAlignment(Pos.CENTER_LEFT);

        // -- PANEL IZQUIERDO (formulario) --
        VBox panelFormulario = new VBox(10,
                lblEstadoFormulario,
                formulario,
                botonesFormulario,
                new Separator(),
                etiqueta("Cambiar estado del paciente seleccionado:"),
                lblEstadoHint,
                panelEstado);
        panelFormulario.setPadding(new Insets(15));
        panelFormulario.setPrefWidth(380);
        panelFormulario.setStyle("""
                -fx-background-color: white;
                -fx-border-color: #E5E7EB;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                """);

        // -- PANEL DERECHO (tabla) --
        Label lblTabla = new Label("Pacientes registrados");
        lblTabla.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblTabla.setTextFill(Color.web("#4C1D95"));

        Label lblHint = new Label(
                "Haga clic en un paciente para editarlo");
        lblHint.setFont(Font.font("System", 11));
        lblHint.setTextFill(Color.web("#6B7280"));

        VBox panelTabla = new VBox(8, buscador, lblTabla, lblHint, tabla);
        panelTabla.setPadding(new Insets(15));
        panelTabla.setStyle("""
                -fx-background-color: white;
                -fx-border-color: #E5E7EB;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                """);
        HBox.setHgrow(panelTabla, Priority.ALWAYS);

        HBox contenido = new HBox(15, panelFormulario, panelTabla);
        HBox.setHgrow(panelTabla, Priority.ALWAYS);

        Label lblFeedback = new Label();
        lblFeedback.setFont(Font.font("System", 12));
        lblFeedback.setWrapText(true);

        VBox root = new VBox(15, encabezado, contenido, lblFeedback);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #F5F3FF;");

        stage.setTitle("Gestión de Pacientes - Piedra Azul");
        stage.setScene(new Scene(root, 1100, 680));
        stage.show();

        cargarPacientes();
    }

    private void cargarPacientes() {
        try {
            HttpRequest request = requestAutenticado("http://localhost:8080/api/pacientes")
                    .GET().build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            List<Paciente> lista = mapper.readValue(response.body(),
                    new TypeReference<List<Paciente>>() {});
            pacientes.setAll(lista);
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo conectar con el servidor");
        }
    }

    private void registrarPaciente() {
        if (txtNombre.getText().isBlank() || txtApellido.getText().isBlank()
                || txtDocumento.getText().isBlank() || txtTelefono.getText().isBlank()
                || cbGenero.getValue() == null
                || dpFechaNacimiento.getValue() == null) {
            mostrarAlerta("Error",
                    "Complete los campos obligatorios (*)\nIncluyendo fecha de nacimiento y género");
            return;
        }

        try {
            String json = """
                    {
                      "nombre": "%s",
                      "apellido": "%s",
                      "numeroDocumento": "%s",
                      "telefono": "%s",
                      "genero": "%s",
                      "fechaNacimiento": "%s",
                      "email": %s,
                      "eps": %s
                    }
                    """.formatted(
                    txtNombre.getText().trim(),
                    txtApellido.getText().trim(),
                    txtDocumento.getText().trim(),
                    txtTelefono.getText().trim(),
                    cbGenero.getValue(),
                    dpFechaNacimiento.getValue().toString(),
                    txtEmail.getText().isBlank()
                            ? "null" : "\"" + txtEmail.getText().trim() + "\"",
                    txtEps.getText().isBlank()
                            ? "null" : "\"" + txtEps.getText().trim() + "\""
            );

            HttpRequest.Builder builder = requestAutenticado(
                    pacienteEditandoId == null
                            ? "http://localhost:8080/api/pacientes/registro"
                            : "http://localhost:8080/api/pacientes/" + pacienteEditandoId)
                    .header("Content-Type", "application/json");

            if (pacienteEditandoId == null) {
                builder.POST(HttpRequest.BodyPublishers.ofString(json));
            } else {
                builder.PUT(HttpRequest.BodyPublishers.ofString(json));
            }

            HttpResponse<String> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201 || response.statusCode() == 200) {
                mostrarAlerta("Éxito", pacienteEditandoId == null
                        ? "Paciente registrado correctamente"
                        : "Paciente actualizado correctamente");
                pacienteEditandoId = null;
                lblEstadoFormulario.setText("Nuevo paciente");
                limpiarFormulario();
                cargarPacientes();
            } else {
                mostrarAlerta("Error", response.body());
            }
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo guardar el paciente");
        }
    }

    private void buscarPorDocumento() {
        String documento = txtBuscarDocumento.getText().trim();
        if (documento.isBlank()) {
            mostrarAlerta("Error", "Ingrese un número de documento");
            return;
        }
        try {
            HttpRequest request = requestAutenticado(
                    "http://localhost:8080/api/pacientes/documento/" + documento)
                    .GET().build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Paciente paciente = mapper.readValue(
                        response.body(), Paciente.class);
                pacientes.setAll(paciente);
            } else {
                pacientes.clear();
                mostrarAlerta("No encontrado",
                        "No existe un paciente con ese documento");
            }
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo buscar el paciente");
        }
    }

    private void cambiarEstado() {
        if (pacienteEditandoId == null) {
            mostrarAlerta("Error",
                    "Seleccione un paciente de la tabla primero");
            return;
        }
        String estado = cbNuevoEstado.getValue();
        if (estado == null) {
            mostrarAlerta("Error", "Seleccione un estado");
            return;
        }
        try {
            String json = "{\"estado\": \"" + estado + "\"}";
            HttpRequest request = requestAutenticado(
                    "http://localhost:8080/api/pacientes/" + pacienteEditandoId + "/estado")
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                mostrarAlerta("Éxito", "Estado actualizado correctamente");
                cbNuevoEstado.setValue(null);
                cargarPacientes();
            } else {
                mostrarAlerta("Error", response.body());
            }
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo cambiar el estado");
        }
    }

    private void cargarPacienteParaEditar(Paciente paciente) {
        pacienteEditandoId = paciente.getId();
        txtNombre.setText(paciente.getNombre() != null ? paciente.getNombre() : "");
        txtApellido.setText(paciente.getApellido() != null ? paciente.getApellido() : "");
        txtDocumento.setText(paciente.getNumeroDocumento() != null
                ? paciente.getNumeroDocumento() : "");
        txtTelefono.setText(paciente.getTelefono() != null ? paciente.getTelefono() : "");
        cbGenero.setValue(paciente.getGenero());
        txtEmail.setText(paciente.getEmail() != null ? paciente.getEmail() : "");
        lblEstadoFormulario.setText("Editando: " + paciente.getNombreCompleto());
    }

    private void limpiarFormulario() {
        pacienteEditandoId = null;
        txtNombre.clear();
        txtApellido.clear();
        txtDocumento.clear();
        txtTelefono.clear();
        cbGenero.setValue(null);
        dpFechaNacimiento.setValue(null);
        txtEmail.clear();
        txtEps.clear();
        cbNuevoEstado.setValue(null);
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private Label etiqueta(String texto) {
        Label lbl = new Label(texto);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 11));
        lbl.setTextFill(Color.web("#4C1D95"));
        return lbl;
    }

    private String campoEstilo() {
        return """
                -fx-background-color: white;
                -fx-border-color: #C084FC;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                -fx-padding: 6 10;
                -fx-font-size: 12px;
                """;
    }

    private String estiloBoton(String color) {
        return "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 6;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 8 16;";
    }

    private String estiloBotonSecundario() {
        return """
                -fx-background-color: transparent;
                -fx-text-fill: #7B2FBE;
                -fx-border-color: #7B2FBE;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                -fx-cursor: hand;
                -fx-padding: 8 16;
                """;
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