package com.piedraazul.ui.agenda;

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
    private final DatePicker dpFechaBuscar = new DatePicker();
    private final TextField txtPacienteId = new TextField();
    private final TextField txtMotivo = new TextField();
    private final ComboBox<String> cbMedicoAgendar = new ComboBox<>();
    private final DatePicker dpFechaAgendar = new DatePicker();
    private final ComboBox<String> cbHoraAgendar = new ComboBox<>();
    private final TextField txtCitaIdAccion = new TextField();
    private final DatePicker dpNuevaFecha = new DatePicker();
    private final ComboBox<String> cbNuevaHora = new ComboBox<>();
    private final Label lblFeedback = new Label();

    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        // ── TÍTULO ──
        Label titulo = new Label("Gestión de Agenda - Piedra Azul");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 22));
        titulo.setTextFill(Color.web("#4C1D95"));

        // ── SECCIÓN BUSCAR CITAS ──
        Label lblBuscar = etiqueta("🔍  Buscar citas por médico y fecha");
        lblBuscar.setFont(Font.font("System", FontWeight.BOLD, 14));

        txtMedicoId.setPromptText("ID del médico");
        txtMedicoId.setStyle(campoEstilo());
        txtMedicoId.setPrefHeight(36);

        dpFechaBuscar.setPromptText("Seleccione fecha");
        dpFechaBuscar.setPrefHeight(36);

        Button btnBuscar = boton("Buscar", "#7B2FBE");
        btnBuscar.setOnAction(e -> buscarCitas());

        Button btnCargarTodas = boton("Ver todas", "#4C1D95");
        btnCargarTodas.setOnAction(e -> cargarTodasLasCitas());

        Button btnExportarCSV = boton("Exportar CSV", "#2E7D32");
        btnExportarCSV.setOnAction(e -> exportarCSV());

        HBox panelBuscar = new HBox(10,
                etiqueta("Médico ID:"), txtMedicoId,
                etiqueta("Fecha:"), dpFechaBuscar,
                btnBuscar, btnCargarTodas, btnExportarCSV);
        panelBuscar.setAlignment(Pos.CENTER_LEFT);

        VBox seccionBuscar = seccion("🔍  Buscar citas", panelBuscar);

        // ── TABLA ──
        TableColumn<Agenda, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);

        TableColumn<Agenda, Long> colPaciente = new TableColumn<>("Paciente ID");
        colPaciente.setCellValueFactory(new PropertyValueFactory<>("pacienteId"));
        colPaciente.setPrefWidth(100);

        TableColumn<Agenda, String> colMedico = new TableColumn<>("Médico");
        colMedico.setCellValueFactory(new PropertyValueFactory<>("medico"));
        colMedico.setPrefWidth(150);

        TableColumn<Agenda, String> colFecha = new TableColumn<>("Fecha y Hora");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        colFecha.setPrefWidth(150);

        TableColumn<Agenda, String> colMotivo = new TableColumn<>("Motivo");
        colMotivo.setCellValueFactory(new PropertyValueFactory<>("motivo"));
        colMotivo.setPrefWidth(150);

        TableColumn<Agenda, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setPrefWidth(100);

        tabla.getColumns().addAll(colId, colPaciente, colMedico,
                colFecha, colMotivo, colEstado);
        tabla.setItems(citas);
        tabla.setPrefHeight(250);

        // ── SECCIÓN AGENDAR CITA ──
        txtPacienteId.setPromptText("ID del paciente");
        txtPacienteId.setStyle(campoEstilo());
        txtPacienteId.setPrefHeight(36);

        cbMedicoAgendar.setPromptText("Seleccione médico");
        cbMedicoAgendar.setPrefHeight(36);
        cbMedicoAgendar.setPrefWidth(180);

        txtMotivo.setPromptText("Motivo de la cita");
        txtMotivo.setStyle(campoEstilo());
        txtMotivo.setPrefHeight(36);

        dpFechaAgendar.setPromptText("Fecha de la cita");
        dpFechaAgendar.setPrefHeight(36);

        // Horas disponibles
        for (int h = 8; h <= 16; h++) {
            cbHoraAgendar.getItems().addAll(
                    String.format("%02d:00", h),
                    String.format("%02d:30", h));
        }
        cbHoraAgendar.setPromptText("Hora");
        cbHoraAgendar.setPrefHeight(36);

        Button btnAgendar = boton("✓  Agendar cita", "#7B2FBE");
        btnAgendar.setOnAction(e -> agendarCita());

        GridPane formularioAgendar = new GridPane();
        formularioAgendar.setHgap(10);
        formularioAgendar.setVgap(10);
        formularioAgendar.add(etiqueta("Paciente ID:"), 0, 0);
        formularioAgendar.add(txtPacienteId, 1, 0);
        formularioAgendar.add(etiqueta("Médico:"), 2, 0);
        formularioAgendar.add(cbMedicoAgendar, 3, 0);
        formularioAgendar.add(etiqueta("Motivo:"), 0, 1);
        formularioAgendar.add(txtMotivo, 1, 1);
        formularioAgendar.add(etiqueta("Fecha:"), 2, 1);
        formularioAgendar.add(dpFechaAgendar, 3, 1);
        formularioAgendar.add(etiqueta("Hora:"), 4, 1);
        formularioAgendar.add(cbHoraAgendar, 5, 1);
        formularioAgendar.add(btnAgendar, 5, 2);

        VBox seccionAgendar = seccion("📅  Agendar nueva cita", formularioAgendar);

        // ── SECCIÓN CANCELAR / REAGENDAR ──
        txtCitaIdAccion.setPromptText("ID de la cita");
        txtCitaIdAccion.setStyle(campoEstilo());
        txtCitaIdAccion.setPrefHeight(36);
        txtCitaIdAccion.setPrefWidth(120);

        dpNuevaFecha.setPromptText("Nueva fecha");
        dpNuevaFecha.setPrefHeight(36);

        for (int h = 8; h <= 16; h++) {
            cbNuevaHora.getItems().addAll(
                    String.format("%02d:00", h),
                    String.format("%02d:30", h));
        }
        cbNuevaHora.setPromptText("Nueva hora");
        cbNuevaHora.setPrefHeight(36);

        Button btnCancelar = boton("✗  Cancelar cita", "#C62828");
        btnCancelar.setOnAction(e -> cancelarCita(txtCitaIdAccion.getText()));

        Button btnReagendar = boton("↺  Reagendar cita", "#7B2FBE");
        btnReagendar.setOnAction(e -> reagendarCita(
                txtCitaIdAccion.getText(),
                dpNuevaFecha.getValue(),
                cbNuevaHora.getValue()));

        GridPane formularioAccion = new GridPane();
        formularioAccion.setHgap(10);
        formularioAccion.setVgap(10);
        formularioAccion.add(etiqueta("Cita ID:"), 0, 0);
        formularioAccion.add(txtCitaIdAccion, 1, 0);
        formularioAccion.add(btnCancelar, 2, 0);
        formularioAccion.add(etiqueta("Nueva fecha:"), 0, 1);
        formularioAccion.add(dpNuevaFecha, 1, 1);
        formularioAccion.add(etiqueta("Nueva hora:"), 2, 1);
        formularioAccion.add(cbNuevaHora, 3, 1);
        formularioAccion.add(btnReagendar, 4, 1);

        VBox seccionAccion = seccion("✏️  Cancelar / Reagendar cita", formularioAccion);

        // ── FEEDBACK ──
        lblFeedback.setFont(Font.font("System", 13));
        lblFeedback.setWrapText(true);

        // ── LAYOUT PRINCIPAL ──
        VBox root = new VBox(12,
                titulo,
                seccionBuscar,
                tabla,
                seccionAgendar,
                seccionAccion,
                lblFeedback);
        root.setPadding(new Insets(20));
        root.setBackground(new Background(new BackgroundFill(
                Color.web("#F5F3FF"), CornerRadii.EMPTY, Insets.EMPTY)));

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F5F3FF; -fx-background-color: #F5F3FF;");

        cargarMedicos();
        cargarTodasLasCitas();

        stage.setTitle("Agenda - Piedra Azul");
        stage.setScene(new Scene(scroll, 1000, 750));
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
            feedback("✓ " + citas.size() + " citas cargadas", false);
        } catch (Exception e) {
            feedback("✗ No se pudo conectar con el servidor", true);
        }
    }

    private void buscarCitas() {
        try {
            String medicoId = txtMedicoId.getText().trim();
            if (medicoId.isBlank()) {
                feedback("✗ Ingrese el ID del médico", true);
                return;
            }
            String url = "http://localhost:8080/api/citas/medico/" + medicoId;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            List<Map<String, Object>> lista = mapper.readValue(
                    response.body(), new TypeReference<>() {});

            if (dpFechaBuscar.getValue() != null) {
                String fecha = dpFechaBuscar.getValue().toString();
                lista = lista.stream()
                        .filter(m -> {
                            String fh = (String) m.get("fechaHora");
                            return fh != null && fh.startsWith(fecha);
                        }).toList();
            }
            citas.setAll(lista.stream().map(this::mapToCita).toList());
            feedback("✓ " + citas.size() + " citas encontradas", false);
        } catch (Exception e) {
            feedback("✗ No se pudo buscar las citas", true);
        }
    }

    private void agendarCita() {
        String pacienteId = txtPacienteId.getText().trim();
        String medicoId = cbMedicoAgendar.getValue();

        if (pacienteId.isBlank() || medicoId == null
                || dpFechaAgendar.getValue() == null
                || cbHoraAgendar.getValue() == null) {
            feedback("✗ Paciente, médico, fecha y hora son obligatorios", true);
            return;
        }

        try {
            String fechaHora = dpFechaAgendar.getValue().toString()
                    + "T" + cbHoraAgendar.getValue();

            String json = """
                {
                  "pacienteId": %s,
                  "medicoId": %s,
                  "motivo": "%s",
                  "fechaHoraManual": "%s"
                }
                """.formatted(pacienteId, medicoId,
                    txtMotivo.getText().trim(), fechaHora);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/citas"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                feedback("✓ Cita agendada correctamente", false);
                limpiarFormulario();
                cargarTodasLasCitas();
            } else {
                feedback("✗ Error: " + response.body(), true);
            }
        } catch (Exception e) {
            feedback("✗ No se pudo agendar la cita: " + e.getMessage(), true);
        }
    }

    private void cancelarCita(String citaId) {
        if (citaId.isBlank()) {
            feedback("✗ Ingrese el ID de la cita", true);
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/citas/"
                            + citaId + "/cancelar"))
                    .method("PATCH", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                feedback("✓ Cita cancelada correctamente", false);
                cargarTodasLasCitas();
            } else {
                feedback("✗ Error: " + response.body(), true);
            }
        } catch (Exception e) {
            feedback("✗ No se pudo cancelar la cita", true);
        }
    }

    private void reagendarCita(String citaId, javafx.util.converter.LocalDateStringConverter conv,
                               String nuevaHora) {
        feedback("✗ Ingrese el ID, nueva fecha y hora", true);
    }

    private void reagendarCita(String citaId,
                               java.time.LocalDate nuevaFecha,
                               String nuevaHora) {
        if (citaId.isBlank() || nuevaFecha == null || nuevaHora == null) {
            feedback("✗ Ingrese el ID, nueva fecha y hora", true);
            return;
        }
        try {
            String fechaHora = nuevaFecha.toString() + "T" + nuevaHora;
            String json = "{\"fechaHora\": \"" + fechaHora + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/citas/"
                            + citaId + "/reagendar"))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                feedback("✓ Cita reagendada correctamente", false);
                cargarTodasLasCitas();
            } else {
                feedback("✗ Error: " + response.body(), true);
            }
        } catch (Exception e) {
            feedback("✗ No se pudo reagendar la cita", true);
        }
    }

    private void exportarCSV() {
        String medicoId = txtMedicoId.getText().trim();
        String fecha = dpFechaBuscar.getValue() != null
                ? dpFechaBuscar.getValue().toString() : "";

        if (medicoId.isBlank() || fecha.isBlank()) {
            feedback("✗ Busque citas por médico y fecha antes de exportar", true);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar CSV");
        fileChooser.setInitialFileName("citas_" + fecha + ".csv");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File archivo = fileChooser.showSaveDialog(primaryStage);
        if (archivo == null) return;

        try (FileWriter writer = new FileWriter(archivo)) {
            writer.write("ID,PacienteID,Medico,FechaHora,Motivo,Estado\n");
            for (Agenda a : citas) {
                writer.write(String.format("%s,%s,%s,%s,%s,%s\n",
                        a.getId(), a.getPacienteId(), a.getMedico(),
                        a.getFechaHora(), a.getMotivo(), a.getEstado()));
            }
            feedback("✓ CSV exportado en: " + archivo.getAbsolutePath(), false);
        } catch (Exception e) {
            feedback("✗ No se pudo exportar el CSV", true);
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
        a.setId(m.get("id") != null
                ? Long.parseLong(m.get("id").toString()) : 0L);
        a.setPacienteId(m.get("pacienteId") != null
                ? Long.parseLong(m.get("pacienteId").toString()) : 0L);
        if (m.get("medico") instanceof Map<?, ?> medico) {
            a.setMedico(medico.get("nombre") + " " + medico.get("apellido"));
        }
        a.setFechaHora(m.get("fechaHora") != null
                ? m.get("fechaHora").toString() : "");
        a.setMotivo(m.get("motivo") != null
                ? m.get("motivo").toString() : "");
        a.setEstado(m.get("estado") != null
                ? m.get("estado").toString() : "");
        return a;
    }

    private void limpiarFormulario() {
        txtPacienteId.clear();
        txtMotivo.clear();
        dpFechaAgendar.setValue(null);
        cbHoraAgendar.setValue(null);
        cbMedicoAgendar.setValue(null);
    }

    private void feedback(String mensaje, boolean error) {
        lblFeedback.setText(mensaje);
        lblFeedback.setTextFill(error
                ? Color.web("#DC2626") : Color.web("#059669"));
    }

    // ── UTILIDADES DE ESTILO ──
    private VBox seccion(String titulo, javafx.scene.Node contenido) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblTitulo.setTextFill(Color.web("#4C1D95"));

        VBox caja = new VBox(8, lblTitulo, contenido);
        caja.setPadding(new Insets(12));
        caja.setBackground(new Background(new BackgroundFill(
                Color.WHITE, new CornerRadii(8), Insets.EMPTY)));
        caja.setStyle("-fx-border-color: #C084FC; -fx-border-radius: 8;");
        return caja;
    }

    private Button boton(String texto, String color) {
        Button btn = new Button(texto);
        btn.setStyle("-fx-background-color: " + color + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 6;"
                + "-fx-cursor: hand;");
        btn.setPrefHeight(36);
        return btn;
    }

    private Label etiqueta(String texto) {
        Label lbl = new Label(texto);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 12));
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
                -fx-font-size: 13px;
                """;
    }

    public static void main(String[] args) {
        launch();
    }
}