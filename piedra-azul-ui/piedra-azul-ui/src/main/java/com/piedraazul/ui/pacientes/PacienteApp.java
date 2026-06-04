package com.piedraazul.ui.pacientes;

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
    private final TextField txtEstadoId = new TextField();
    private final ComboBox<String> cbNuevoEstado = new ComboBox<>();
    private Long pacienteEditandoId = null;

    @Override
    public void start(Stage stage) {

        Label titulo = new Label("Gestión de Pacientes - Piedra Azul");
        titulo.setStyle("""
                -fx-font-size: 24px;
                -fx-font-weight: bold;
                -fx-text-fill: #1E3A5F;
                """);

        // ── TABLA ──
        TableColumn<Paciente, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Paciente, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));

        TableColumn<Paciente, String> colDocumento = new TableColumn<>("Documento");
        colDocumento.setCellValueFactory(new PropertyValueFactory<>("numeroDocumento"));

        TableColumn<Paciente, String> colTelefono = new TableColumn<>("Celular");
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));

        TableColumn<Paciente, String> colGenero = new TableColumn<>("Género");
        colGenero.setCellValueFactory(new PropertyValueFactory<>("genero"));

        TableColumn<Paciente, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<Paciente, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        tabla.getColumns().addAll(colId, colNombre, colDocumento,
                colTelefono, colGenero, colEmail, colEstado);
        tabla.setItems(pacientes);
        tabla.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2
                    && tabla.getSelectionModel().getSelectedItem() != null) {
                cargarPacienteParaEditar(tabla.getSelectionModel().getSelectedItem());
            }
        });

        // ── FORMULARIO REGISTRO ──
        txtNombre.setPromptText("Nombre *");
        txtApellido.setPromptText("Apellido *");
        txtDocumento.setPromptText("Documento *");
        txtTelefono.setPromptText("Celular *");
        txtEmail.setPromptText("Email (opcional)");
        txtEps.setPromptText("EPS (opcional)");
        cbGenero.getItems().addAll("HOMBRE", "MUJER", "OTRO");
        cbGenero.setPromptText("Género *");
        dpFechaNacimiento.setPromptText("Fecha de nacimiento *");

        GridPane formulario = new GridPane();
        formulario.setHgap(10);
        formulario.setVgap(10);
        formulario.add(txtNombre, 0, 0);
        formulario.add(txtApellido, 1, 0);
        formulario.add(txtDocumento, 0, 1);
        formulario.add(txtTelefono, 1, 1);
        formulario.add(cbGenero, 0, 2);
        formulario.add(dpFechaNacimiento, 1, 2);
        formulario.add(txtEmail, 0, 3);
        formulario.add(txtEps, 1, 3);

        Button btnGuardar = new Button("Registrar paciente");
        btnGuardar.setStyle(estiloBoton());
        btnGuardar.setOnAction(e -> registrarPaciente());
        formulario.add(btnGuardar, 1, 4);

        // ── BUSCADOR ──
        txtBuscarDocumento.setPromptText("Buscar por documento");
        Button btnBuscar = new Button("Buscar");
        btnBuscar.setStyle(estiloBoton());
        btnBuscar.setOnAction(e -> buscarPorDocumento());

        Button btnMostrarTodos = new Button("Mostrar todos");
        btnMostrarTodos.setStyle(estiloBoton());
        btnMostrarTodos.setOnAction(e -> cargarPacientes());

        GridPane buscador = new GridPane();
        buscador.setHgap(10);
        buscador.add(txtBuscarDocumento, 0, 0);
        buscador.add(btnBuscar, 1, 0);
        buscador.add(btnMostrarTodos, 2, 0);

        // ── CAMBIAR ESTADO ──
        txtEstadoId.setPromptText("ID del paciente");
        cbNuevoEstado.getItems().addAll(
                "ACTIVO", "INACTIVO", "EN_TRATAMIENTO", "DADO_DE_ALTA");
        cbNuevoEstado.setPromptText("Nuevo estado");

        Button btnCambiarEstado = new Button("Cambiar estado");
        btnCambiarEstado.setStyle(estiloBoton());
        btnCambiarEstado.setOnAction(e -> cambiarEstado());

        GridPane panelEstado = new GridPane();
        panelEstado.setHgap(10);
        panelEstado.add(txtEstadoId, 0, 0);
        panelEstado.add(cbNuevoEstado, 1, 0);
        panelEstado.add(btnCambiarEstado, 2, 0);

        Button btnCargar = new Button("Cargar pacientes");
        btnCargar.setStyle(estiloBoton());
        btnCargar.setOnAction(e -> cargarPacientes());

        VBox root = new VBox(15, titulo, formulario, buscador,
                panelEstado, btnCargar, tabla);
        root.setStyle("""
                -fx-padding: 20;
                -fx-background-color: #F4F6F9;
                """);

        stage.setTitle("Gestión de Pacientes - Piedra Azul");
        stage.setScene(new Scene(root, 900, 600));
        stage.show();

        cargarPacientes();
    }

    private void cargarPacientes() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/pacientes"))
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
            mostrarAlerta("Error", "Complete los campos obligatorios (*)\nIncluyendo fecha de nacimiento");
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
                      "fechaNacimiento": %s,
                      "email": %s,
                      "eps": %s
                    }
                    """.formatted(
                    txtNombre.getText(),
                    txtApellido.getText(),
                    txtDocumento.getText(),
                    txtTelefono.getText(),
                    cbGenero.getValue(),
                    dpFechaNacimiento.getValue() != null
                            ? "\"" + dpFechaNacimiento.getValue() + "\""
                            : "null",
                    txtEmail.getText().isBlank()
                            ? "null"
                            : "\"" + txtEmail.getText() + "\"",
                    txtEps.getText().isBlank()
                            ? "null"
                            : "\"" + txtEps.getText() + "\""
            );

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .header("Content-Type", "application/json");

            if (pacienteEditandoId == null) {
                builder.uri(URI.create("http://localhost:8080/api/pacientes/registro"))
                        .POST(HttpRequest.BodyPublishers.ofString(json));
            } else {
                builder.uri(URI.create("http://localhost:8080/api/pacientes/" + pacienteEditandoId))
                        .PUT(HttpRequest.BodyPublishers.ofString(json));
            }

            HttpResponse<String> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201 || response.statusCode() == 200) {
                mostrarAlerta("Éxito", pacienteEditandoId == null
                        ? "Paciente registrado correctamente"
                        : "Paciente actualizado correctamente");
                pacienteEditandoId = null;
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
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/pacientes/documento/" + documento))
                    .GET().build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Paciente paciente = mapper.readValue(response.body(), Paciente.class);
                pacientes.setAll(paciente);
            } else {
                pacientes.clear();
                mostrarAlerta("No encontrado", "No existe un paciente con ese documento");
            }
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo buscar el paciente");
        }
    }

    private void cambiarEstado() {
        String id = txtEstadoId.getText().trim();
        String estado = cbNuevoEstado.getValue();
        if (id.isBlank() || estado == null) {
            mostrarAlerta("Error", "Ingrese el ID y seleccione un estado");
            return;
        }
        try {
            String json = "{\"estado\": \"" + estado + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/pacientes/" + id + "/estado"))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                mostrarAlerta("Éxito", "Estado actualizado correctamente");
                txtEstadoId.clear();
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
        txtNombre.setText(paciente.getNombre());
        txtApellido.setText(paciente.getApellido());
        txtDocumento.setText(paciente.getNumeroDocumento());
        txtTelefono.setText(paciente.getTelefono());
        cbGenero.setValue(paciente.getGenero());
        txtEmail.setText(paciente.getEmail() != null ? paciente.getEmail() : "");
        mostrarAlerta("Edición", "Paciente cargado. Modifique los datos y guarde.");
    }

    private void limpiarFormulario() {
        txtNombre.clear();
        txtApellido.clear();
        txtDocumento.clear();
        txtTelefono.clear();
        cbGenero.setValue(null);
        dpFechaNacimiento.setValue(null);
        txtEmail.clear();
        txtEps.clear();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private String estiloBoton() {
        return """
                -fx-background-color: #1E88E5;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                """;
    }

    public static void main(String[] args) {
        launch();
    }
}