package com.piedraazul.ui.agenda;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class AgendaApp extends Application {

    private final TableView<Agenda> tabla = new TableView<>();
    private final ObservableList<Agenda> citas = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final TextField txtMedicoId = new TextField();
    private final DatePicker dpFecha = new DatePicker();
    private final TextField txtPacienteId = new TextField();
    private final TextField txtMotivo = new TextField();
    private final ComboBox<String> cbMedicoAgendar = new ComboBox<>();
    private final TextField txtFechaHora = new TextField();

    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        Label titulo = new Label("Gestión de Agenda - Piedra Azul");
        titulo.setStyle("""
                -fx-font-size: 24px;
                -fx-font-weight: bold;
                -fx-text-fill: #1E3A5F;
                """);

        // ── TABLA ──
        TableColumn<Agenda, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Agenda, Long> colPaciente = new TableColumn<>("Paciente ID");
        colPaciente.setCellValueFactory(new PropertyValueFactory<>("pacienteId"));

        TableColumn<Agenda, String> colMedico = new TableColumn<>("Médico");
        colMedico.setCellValueFactory(new PropertyValueFactory<>("medico"));

        TableColumn<Agenda, String> colFecha = new TableColumn<>("Fecha y Hora");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));

        TableColumn<Agenda, String> colMotivo = new TableColumn<>("Motivo");
        colMotivo.setCellValueFactory(new PropertyValueFactory<>("motivo"));

        TableColumn<Agenda, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        tabla.getColumns().addAll(colId, colPaciente, colMedico, colFecha, colMotivo, colEstado);
        tabla.setItems(citas);

        // ── BÚSQUEDA POR MÉDICO Y FECHA ──
        txtMedicoId.setPromptText("ID del médico");
        dpFecha.setPromptText("Fecha");

        Button btnBuscar = new Button("Buscar citas");
        btnBuscar.setStyle(estiloBoton());
        btnBuscar.setOnAction(e -> buscarCitas());

        Button btnCargarTodas = new Button("Cargar todas");
        btnCargarTodas.setStyle(estiloBoton());
        btnCargarTodas.setOnAction(e -> cargarTodasLasCitas());

        Button btnExportarCSV = new Button("Exportar CSV");
        btnExportarCSV.setStyle("""
                -fx-background-color: #2E7D32;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                """);
        btnExportarCSV.setOnAction(e -> exportarCSV());

        GridPane busqueda = new GridPane();
        busqueda.setHgap(10);
        busqueda.setVgap(10);
        busqueda.add(new Label("Médico ID:"), 0, 0);
        busqueda.add(txtMedicoId, 1, 0);
        busqueda.add(new Label("Fecha:"), 2, 0);
        busqueda.add(dpFecha, 3, 0);
        busqueda.add(btnBuscar, 4, 0);
        busqueda.add(btnCargarTodas, 5, 0);
        busqueda.add(btnExportarCSV, 6, 0);

        // ── AGENDAR CITA ──
        txtPacienteId.setPromptText("ID del paciente");
        txtMotivo.setPromptText("Motivo");
        txtFechaHora.setPromptText("Fecha y hora (ej: 2026-06-10T10:00)");

        Button btnAgendar = new Button("Agendar cita");
        btnAgendar.setStyle(estiloBoton());
        btnAgendar.setOnAction(e -> agendarCita());

        GridPane formularioAgendar = new GridPane();
        formularioAgendar.setHgap(10);
        formularioAgendar.setVgap(10);
        formularioAgendar.add(new Label("Paciente ID:"), 0, 0);
        formularioAgendar.add(txtPacienteId, 1, 0);
        formularioAgendar.add(new Label("Médico ID:"), 2, 0);
        formularioAgendar.add(cbMedicoAgendar, 3, 0);
        formularioAgendar.add(new Label("Motivo:"), 0, 1);
        formularioAgendar.add(txtMotivo, 1, 1);
        formularioAgendar.add(new Label("Fecha y hora:"), 2, 1);
        formularioAgendar.add(txtFechaHora, 3, 1);
        formularioAgendar.add(btnAgendar, 3, 2);

        // ── CANCELAR Y REAGENDAR ──
        TextField txtCitaId = new TextField();
        txtCitaId.setPromptText("ID de la cita");
        TextField txtNuevaFecha = new TextField();
        txtNuevaFecha.setPromptText("Nueva fecha (ej: 2026-06-10T10:00)");

        Button btnCancelar = new Button("Cancelar cita");
        btnCancelar.setStyle("""
                -fx-background-color: #C62828;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                """);
        btnCancelar.setOnAction(e -> cancelarCita(txtCitaId.getText()));

        Button btnReagendar = new Button("Reagendar cita");
        btnReagendar.setStyle(estiloBoton());
        btnReagendar.setOnAction(e -> reagendarCita(txtCitaId.getText(), txtNuevaFecha.getText()));

        HBox accionesCita = new HBox(10,
                new Label("Cita ID:"), txtCitaId,
                new Label("Nueva fecha:"), txtNuevaFecha,
                btnCancelar, btnReagendar);

        Label lblFeedback = new Label();

        VBox root = new VBox(15, titulo, busqueda,
                new Separator(),
                new Label("Agendar nueva cita:"), formularioAgendar,
                new Separator(),
                new Label("Cancelar / Reagendar:"), accionesCita,
                lblFeedback, tabla);
        root.setStyle("""
                -fx-padding: 20;
                -fx-background-color: #F4F6F9;
                """);

        cargarMedicos();
        cargarTodasLasCitas();

        stage.setTitle("Agenda - Piedra Azul");
        stage.setScene(new Scene(root, 1100, 700));
        stage.show();
    }

    private void cargarTodasLasCitas() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/citas"))
                    .GET().build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            List<Map<String, Object>> lista = mapper.readValue(
                    response.body(), new TypeReference<>() {});
            citas.setAll(lista.stream().map(this::mapToCita).toList());
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo conectar con el servidor: " + e.getMessage());
        }
    }

    private void buscarCitas() {
        try {
            String medicoId = txtMedicoId.getText().trim();
            if (medicoId.isBlank()) {
                mostrarAlerta("Error", "Ingrese el ID del médico");
                return;
            }
            String url = "http://localhost:8080/api/citas/medico/" + medicoId;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            List<Map<String, Object>> lista = mapper.readValue(
                    response.body(), new TypeReference<>() {});

            // Filtrar por fecha si se seleccionó
            if (dpFecha.getValue() != null) {
                String fecha = dpFecha.getValue().toString();
                lista = lista.stream()
                        .filter(m -> {
                            String fh = (String) m.get("fechaHora");
                            return fh != null && fh.startsWith(fecha);
                        }).toList();
            }
            citas.setAll(lista.stream().map(this::mapToCita).toList());
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo buscar las citas: " + e.getMessage());
        }
    }

    private void agendarCita() {
        try {
            String pacienteId = txtPacienteId.getText().trim();
            String medicoId = cbMedicoAgendar.getValue();
            String motivo = txtMotivo.getText().trim();
            String fechaHora = txtFechaHora.getText().trim();

            if (pacienteId.isBlank() || medicoId == null) {
                mostrarAlerta("Error", "Paciente ID y Médico son obligatorios");
                return;
            }

            String json = """
                {
                  "pacienteId": %s,
                  "medicoId": %s,
                  "motivo": "%s",
                  "fechaHoraManual": %s
                }
                """.formatted(
                    pacienteId,
                    medicoId,
                    motivo,
                    fechaHora.isBlank() ? "null" : "\"" + fechaHora + "\""
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/citas"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                mostrarAlerta("Éxito", "Cita agendada correctamente");
                limpiarFormulario();
                cargarTodasLasCitas();
            } else {
                mostrarAlerta("Error", response.body());
            }
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo agendar la cita: " + e.getMessage());
        }
    }

    private void cancelarCita(String citaId) {
        if (citaId.isBlank()) {
            mostrarAlerta("Error", "Ingrese el ID de la cita");
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/citas/" + citaId + "/cancelar"))
                    .method("PATCH", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                mostrarAlerta("Éxito", "Cita cancelada correctamente");
                cargarTodasLasCitas();
            } else {
                mostrarAlerta("Error", response.body());
            }
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo cancelar la cita: " + e.getMessage());
        }
    }

    private void reagendarCita(String citaId, String nuevaFecha) {
        if (citaId.isBlank() || nuevaFecha.isBlank()) {
            mostrarAlerta("Error", "Ingrese el ID de la cita y la nueva fecha");
            return;
        }
        try {
            String json = "{\"fechaHora\": \"" + nuevaFecha + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/citas/" + citaId + "/reagendar"))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                mostrarAlerta("Éxito", "Cita reagendada correctamente");
                cargarTodasLasCitas();
            } else {
                mostrarAlerta("Error", response.body());
            }
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo reagendar la cita: " + e.getMessage());
        }
    }

    private void exportarCSV() {
        try {
            String medicoId = txtMedicoId.getText().trim();
            String fecha = dpFecha.getValue() != null
                    ? dpFecha.getValue().toString() : "";

            if (medicoId.isBlank() || fecha.isBlank()) {
                mostrarAlerta("Error", "Seleccione médico y fecha para exportar");
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar CSV");
            fileChooser.setInitialFileName("citas_" + fecha + ".csv");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("CSV", "*.csv"));
            File archivo = fileChooser.showSaveDialog(primaryStage);

            if (archivo == null) return;

            // Filtrar citas de la tabla por médico y fecha
            List<Agenda> citasFiltradas = citas.stream()
                    .filter(a -> a.getFechaHora() != null
                            && a.getFechaHora().startsWith(fecha))
                    .toList();

            try (FileWriter writer = new FileWriter(archivo)) {
                writer.write("ID,PacienteID,Medico,FechaHora,Motivo,Estado\n");
                for (Agenda a : citasFiltradas) {
                    writer.write(String.format("%s,%s,%s,%s,%s,%s\n",
                            a.getId(),
                            a.getPacienteId(),
                            a.getMedico(),
                            a.getFechaHora(),
                            a.getMotivo(),
                            a.getEstado()));
                }
            }
            mostrarAlerta("Éxito", "CSV exportado correctamente en:\n" + archivo.getAbsolutePath());

        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo exportar el CSV: " + e.getMessage());
        }
    }

    private void cargarMedicos() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/medicos"))
                    .GET().build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            List<Map<String, Object>> lista = mapper.readValue(
                    response.body(), new TypeReference<>() {});
            List<String> ids = lista.stream()
                    .map(m -> m.get("id").toString())
                    .toList();
            cbMedicoAgendar.getItems().setAll(ids);
        } catch (Exception e) {
            // Si no hay conexión se deja vacío
        }
    }

    private Agenda mapToCita(Map<String, Object> m) {
        Agenda a = new Agenda();
        a.setId(m.get("id") != null ? Long.parseLong(m.get("id").toString()) : 0L);
        a.setPacienteId(m.get("pacienteId") != null
                ? Long.parseLong(m.get("pacienteId").toString()) : 0L);
        if (m.get("medico") instanceof Map<?, ?> medico) {
            a.setMedico(medico.get("nombre") + " " + medico.get("apellido"));
        }
        a.setFechaHora(m.get("fechaHora") != null ? m.get("fechaHora").toString() : "");
        a.setMotivo(m.get("motivo") != null ? m.get("motivo").toString() : "");
        a.setEstado(m.get("estado") != null ? m.get("estado").toString() : "");
        return a;
    }

    private void limpiarFormulario() {
        txtPacienteId.clear();
        txtMotivo.clear();
        txtFechaHora.clear();
        cbMedicoAgendar.setValue(null);
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