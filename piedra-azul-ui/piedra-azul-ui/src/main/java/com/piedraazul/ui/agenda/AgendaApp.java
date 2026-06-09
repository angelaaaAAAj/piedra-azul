package com.piedraazul.ui.agenda;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
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
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AgendaApp extends Application {

    private final TableView<Agenda> tabla = new TableView<>();
    private final ObservableList<Agenda> citas = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // Búsqueda por documento de paciente
    private final TextField txtDocumentoBuscar = new TextField();
    private final DatePicker dpFechaBuscar = new DatePicker();

    // Agendar
    private final TextField txtDocumentoPaciente = new TextField();
    private final TextField txtMotivo = new TextField();
    private final TextField txtBuscarMedico = new TextField();
    private final ObservableList<Agenda> todasLasCitas = FXCollections.observableArrayList();
    private final DatePicker dpFechaAgendar = new DatePicker();
    private final FlowPane panelFranjasAgendar = new FlowPane();
    private String horaSeleccionadaAgendar = null;

    // ComboBox de médicos con nombre+especialidad
    private final ComboBox<String> cbMedicoAgendar = new ComboBox<>();
    private final ComboBox<String> cbMedicoExportar = new ComboBox<>();
    private final List<Map<String, Object>> datosMedicos = new ArrayList<>();

    private final Label lblFeedback = new Label();
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        Label titulo = new Label("Gestión de Agenda - Piedra Azul");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 22));
        titulo.setTextFill(Color.web("#4C1D95"));

        // ── SECCIÓN BUSCAR POR DOCUMENTO ──
        txtDocumentoBuscar.setPromptText("Documento de identidad del paciente");
        txtDocumentoBuscar.setStyle(campoEstilo());
        txtDocumentoBuscar.setPrefHeight(36);
        txtDocumentoBuscar.setPrefWidth(260);

        dpFechaBuscar.setPromptText("Filtrar por fecha (opcional)");
        dpFechaBuscar.setPrefHeight(36);

        Button btnBuscar = boton("🔍 Buscar", "#7B2FBE");
        btnBuscar.setOnAction(e -> buscarCitasPorDocumento());

        Button btnCargarTodas = boton("Ver todas", "#4C1D95");
        Button btnActualizar = boton("🔄 Actualizar", "#2563EB");
        btnActualizar.setOnAction(e -> cargarTodasLasCitas());
        btnCargarTodas.setOnAction(e -> cargarTodasLasCitas());

        Button btnExportarCSV = boton("Exportar CSV", "#2E7D32");
        btnExportarCSV.setOnAction(e -> exportarCSV());

        cbMedicoExportar.setPromptText("Médico para exportar");
        cbMedicoExportar.setPrefHeight(36);
        cbMedicoExportar.setPrefWidth(220);

        HBox panelBuscar = new HBox(
                10,
                etiqueta("Documento paciente:"), txtDocumentoBuscar,
                etiqueta("Fecha:"), dpFechaBuscar,
                btnBuscar,
                btnCargarTodas,
                btnActualizar,
                cbMedicoExportar,
                btnExportarCSV
        );


        VBox seccionBuscar = seccion("🔍  Buscar citas", panelBuscar);

        // ── TABLA CON BOTONES POR FILA ──
        TableColumn<Agenda, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);

        TableColumn<Agenda, String> colPaciente = new TableColumn<>("Paciente");
        colPaciente.setCellValueFactory(new PropertyValueFactory<>("nombrePaciente"));
        colPaciente.setPrefWidth(150);

        TableColumn<Agenda, String> colMedico = new TableColumn<>("Médico");
        colMedico.setCellValueFactory(new PropertyValueFactory<>("medico"));
        colMedico.setPrefWidth(150);

        TableColumn<Agenda, String> colFecha = new TableColumn<>("Fecha y Hora");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        colFecha.setPrefWidth(140);

        TableColumn<Agenda, String> colMotivo = new TableColumn<>("Motivo");
        colMotivo.setCellValueFactory(new PropertyValueFactory<>("motivo"));
        colMotivo.setPrefWidth(130);

        TableColumn<Agenda, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setPrefWidth(100);

        // Columna de acciones con botones por fila
        TableColumn<Agenda, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setPrefWidth(200);
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnCancelar  = new Button("✗ Cancelar");
            private final Button btnReagendar = new Button("↺ Reagendar");
            private final HBox   caja         = new HBox(6, btnCancelar, btnReagendar);

            {
                btnCancelar.setStyle(estiloBotonTabla("#C62828"));
                btnReagendar.setStyle(estiloBotonTabla("#7B2FBE"));
                caja.setAlignment(Pos.CENTER);

                btnCancelar.setOnAction(e -> {
                    Agenda cita = getTableView().getItems().get(getIndex());
                    cancelarCita(cita.getId().toString());
                });

                btnReagendar.setOnAction(e -> {
                    Agenda cita = getTableView().getItems().get(getIndex());
                    abrirVentanaReagendar(cita);
                });
            }

            // REEMPLAZAR el updateItem completo:
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                Agenda cita = getTableView().getItems().get(getIndex());
                String estado = cita.getEstado();

                switch (estado) {
                    case "COMPLETADA" -> {
                        Label badge = new Label("✓ Atendida");
                        badge.setStyle(
                                "-fx-background-color: #D1FAE5;" +
                                        "-fx-text-fill: #065F46;" +
                                        "-fx-font-weight: bold;" +
                                        "-fx-font-size: 11px;" +
                                        "-fx-padding: 4 12;" +
                                        "-fx-background-radius: 999;"
                        );
                        setGraphic(badge);
                    }
                    case "CANCELADA" -> {
                        Label badge = new Label("✗ Cancelada");
                        badge.setStyle(
                                "-fx-background-color: #FEE2E2;" +
                                        "-fx-text-fill: #991B1B;" +
                                        "-fx-font-weight: bold;" +
                                        "-fx-font-size: 11px;" +
                                        "-fx-padding: 4 12;" +
                                        "-fx-background-radius: 999;"
                        );
                        setGraphic(badge);
                    }
                    // PROGRAMADA y REAGENDADA → botones activos
                    default -> {
                        btnCancelar.setDisable(false);
                        btnReagendar.setDisable(false);
                        setGraphic(caja);
                    }
                }
            }
        });

        tabla.getColumns().addAll(colId, colPaciente, colMedico,
                colFecha, colMotivo, colEstado, colAcciones);
        tabla.setItems(citas);
        tabla.setPrefHeight(260);
        tabla.setPlaceholder(new Label("No hay citas — use el buscador o 'Ver todas'"));

        // ── SECCIÓN AGENDAR ──
        txtDocumentoPaciente.setPromptText("Documento de identidad del paciente *");
        txtDocumentoPaciente.setStyle(campoEstilo());
        txtDocumentoPaciente.setPrefHeight(36);

        cbMedicoAgendar.setPromptText("Seleccione médico");
        cbMedicoAgendar.setPrefHeight(36);
        cbMedicoAgendar.setPrefWidth(260);

        txtMotivo.setPromptText("Motivo de la cita (opcional)");
        txtMotivo.setStyle(campoEstilo());
        txtMotivo.setPrefHeight(36);

        dpFechaAgendar.setPromptText("Fecha de la cita *");
        dpFechaAgendar.setPrefHeight(36);
        dpFechaAgendar.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
        dpFechaAgendar.valueProperty().addListener((obs, o, n) -> {
            horaSeleccionadaAgendar = null;
            cargarFranjasAgendar();
        });
        cbMedicoAgendar.valueProperty().addListener((obs, o, n) -> {
            horaSeleccionadaAgendar = null;
            cargarFranjasAgendar();
        });

        Label lblFranjas = etiqueta("3 · Selecciona una franja libre:");
        panelFranjasAgendar.setHgap(8);
        panelFranjasAgendar.setVgap(8);
        panelFranjasAgendar.setPrefHeight(90);
        panelFranjasAgendar.getChildren().add(hint("Seleccione médico y fecha primero"));

        HBox leyenda = new HBox(12,
                chip("libre", "#C084FC", "#EDE9FE"),
                chip("ocupado", "#9CA3AF", "#F3F4F6"),
                chip("seleccionado", "#7B2FBE", "#7B2FBE"));
        leyenda.setAlignment(Pos.CENTER_LEFT);

        Button btnAgendar = boton("✓  Agendar cita", "#7B2FBE");
        btnAgendar.setOnAction(e -> agendarCita());

        GridPane formularioAgendar = new GridPane();
        formularioAgendar.setHgap(12);
        formularioAgendar.setVgap(10);
        formularioAgendar.add(etiqueta("1 · Documento paciente *:"), 0, 0);
        formularioAgendar.add(txtDocumentoPaciente, 1, 0);
        formularioAgendar.add(etiqueta("2 · Médico *:"), 2, 0);
        formularioAgendar.add(cbMedicoAgendar, 3, 0);
        formularioAgendar.add(etiqueta("Motivo:"), 0, 1);
        formularioAgendar.add(txtMotivo, 1, 1);
        formularioAgendar.add(etiqueta("Fecha *:"), 2, 1);
        formularioAgendar.add(dpFechaAgendar, 3, 1);
        formularioAgendar.add(lblFranjas, 0, 2, 4, 1);
        formularioAgendar.add(panelFranjasAgendar, 0, 3, 4, 1);
        formularioAgendar.add(leyenda, 0, 4, 3, 1);
        formularioAgendar.add(btnAgendar, 3, 4);

        VBox seccionAgendar = seccion("📅  Agendar nueva cita", formularioAgendar);

        lblFeedback.setFont(Font.font("System", 13));
        lblFeedback.setWrapText(true);

        VBox root = new VBox(12,
                titulo,
                seccionBuscar,
                tabla,
                seccionAgendar,
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
        stage.setScene(new Scene(scroll, 1050, 780));
        stage.show();
    }

    // ── Cargar todas las citas ────────────────────────────────────────────
    private void cargarTodasLasCitas() {
        try {
            HttpRequest request = requestAutenticado("http://localhost:8080/api/citas")
                    .GET().build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            List<Map<String, Object>> lista = mapper.readValue(
                    response.body(), new TypeReference<>() {});
            List<Agenda> todas = lista.stream().map(this::mapToCita).toList();
            todasLasCitas.setAll(todas);  // ← NUEVO
            citas.setAll(todas);
            feedback("✓ " + citas.size() + " citas cargadas", false);
        } catch (Exception e) {
            feedback("✗ No se pudo conectar con el servidor", true);
        }
    }

    // ── Buscar citas por documento del paciente ───────────────────────────
    private void buscarCitasPorDocumento() {

        String documento = txtDocumentoBuscar.getText().trim();
        LocalDate fechaSeleccionada = dpFechaBuscar.getValue();

        if (documento.isBlank() && fechaSeleccionada == null) {
            feedback("✗ Ingrese documento o seleccione una fecha", true);
            return;
        }

        try {

            // CASO 1: SOLO FECHA
            if (documento.isBlank()) {

                List<Agenda> resultado = todasLasCitas.stream()
                        .filter(c -> c.getFechaHora() != null
                                && c.getFechaHora().startsWith(fechaSeleccionada.toString()))
                        .toList();

                citas.setAll(resultado);

                feedback("✓ " + resultado.size()
                        + " citas encontradas para "
                        + fechaSeleccionada, false);

                return;
            }

            // CASO 2 Y 3: DOCUMENTO O DOCUMENTO + FECHA

            HttpRequest reqPac = requestAutenticado(
                    "http://localhost:8080/api/pacientes/documento/" + documento)
                    .GET().build();

            HttpResponse<String> respPac = httpClient.send(
                    reqPac,
                    HttpResponse.BodyHandlers.ofString());

            if (respPac.statusCode() != 200) {
                feedback("✗ No se encontró paciente con ese documento", true);
                return;
            }

            Map<String, Object> paciente = mapper.readValue(
                    respPac.body(),
                    new TypeReference<>() {});

            Long pacienteId = Long.parseLong(
                    paciente.get("id").toString());

            HttpRequest reqCitas = requestAutenticado(
                    "http://localhost:8080/api/citas/paciente/" + pacienteId)
                    .GET().build();

            HttpResponse<String> respCitas = httpClient.send(
                    reqCitas,
                    HttpResponse.BodyHandlers.ofString());

            List<Map<String, Object>> lista = mapper.readValue(
                    respCitas.body(),
                    new TypeReference<>() {});

            // Si también seleccionó fecha
            if (fechaSeleccionada != null) {

                lista = lista.stream()
                        .filter(c -> {
                            String fh = c.get("fechaHora").toString();
                            return fh.startsWith(fechaSeleccionada.toString());
                        })
                        .toList();
            }

            citas.setAll(
                    lista.stream()
                            .map(this::mapToCita)
                            .toList());

            feedback("✓ " + citas.size()
                    + " citas encontradas", false);

        } catch (Exception e) {
            feedback("✗ Error al buscar: " + e.getMessage(), true);
        }
    }

    // ── Agendar cita ─────────────────────────────────────────────────────
    private void agendarCita() {
        String documento = txtDocumentoPaciente.getText().trim();
        String medicoLabel = cbMedicoAgendar.getValue();

        if (documento.isBlank() || medicoLabel == null
                || dpFechaAgendar.getValue() == null
                || horaSeleccionadaAgendar == null) {
            feedback("✗ Complete documento, médico, fecha y franja horaria", true);
            return;
        }

        try {
            // Resolver pacienteId desde documento
            HttpRequest reqPac = requestAutenticado(
                    "http://localhost:8080/api/pacientes/documento/" + documento)
                    .GET().build();
            HttpResponse<String> respPac = httpClient.send(reqPac,
                    HttpResponse.BodyHandlers.ofString());
            if (respPac.statusCode() != 200) {
                feedback("✗ No se encontró paciente con ese documento", true);
                return;
            }
            Map<String, Object> paciente = mapper.readValue(
                    respPac.body(), new TypeReference<>() {});
            Long pacienteId = Long.parseLong(paciente.get("id").toString());

            // Resolver medicoId desde el label seleccionado
            Long medicoId = getMedicoIdDesdeLabel(medicoLabel);
            if (medicoId == null) {
                feedback("✗ Médico no válido", true);
                return;
            }

            String fechaHora = dpFechaAgendar.getValue().toString()
                    + "T" + horaSeleccionadaAgendar;

            String json = """
                    {
                      "pacienteId": %d,
                      "medicoId": %d,
                      "motivo": "%s",
                      "fechaHoraManual": "%s"
                    }
                    """.formatted(pacienteId, medicoId,
                    txtMotivo.getText().trim(), fechaHora);

            HttpRequest request = requestAutenticado("http://localhost:8080/api/citas")
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

    // ── Cancelar cita ────────────────────────────────────────────────────
    private void cancelarCita(String citaId) {
        if (citaId.isBlank()) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Seguro que desea cancelar la cita #" + citaId + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmar cancelación");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    HttpRequest request = requestAutenticado(
                            "http://localhost:8080/api/citas/" + citaId + "/cancelar")
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
        });
    }

    // ── Ventana de reagendamiento con franjas ─────────────────────────────
    private void abrirVentanaReagendar(Agenda cita) {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Reagendar cita #" + cita.getId());

        Label lblInfo = new Label("Cita actual:  " + cita.getMedico()
                + "  ·  " + cita.getFechaHora());
        lblInfo.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblInfo.setTextFill(Color.web("#4C1D95"));

        Label lblFecha = etiqueta("Selecciona la nueva fecha:");
        DatePicker dpNuevaFecha = new DatePicker();
        dpNuevaFecha.setMaxWidth(Double.MAX_VALUE);
        dpNuevaFecha.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now().plusDays(1)));
            }
        });

        Label lblFranjas = etiqueta("Selecciona una franja disponible:");
        FlowPane franjas = new FlowPane();
        franjas.setHgap(8);
        franjas.setVgap(8);
        franjas.setPrefHeight(130);
        franjas.getChildren().add(hint("Selecciona una fecha para ver las franjas"));

        HBox leyenda = new HBox(12,
                chip("libre", "#C084FC", "#EDE9FE"),
                chip("ocupado", "#9CA3AF", "#F3F4F6"),
                chip("seleccionado", "#7B2FBE", "#7B2FBE"));
        leyenda.setAlignment(Pos.CENTER_LEFT);

        final String[] horaSeleccionada = {null};

        dpNuevaFecha.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            horaSeleccionada[0] = null;
            franjas.getChildren().setAll(hint("Cargando franjas..."));

            new Thread(() -> {
                try {
                    // Datos del médico
                    HttpRequest reqMedico = requestAutenticado(
                            "http://localhost:8080/api/medicos/" + cita.getMedicoId())
                            .GET().build();
                    HttpResponse<String> respMedico = httpClient.send(
                            reqMedico, HttpResponse.BodyHandlers.ofString());
                    Map<String, Object> medico = mapper.readValue(
                            respMedico.body(), new TypeReference<>() {});

                    String inicioStr = medico.getOrDefault("franjaInicio", "08:00").toString();
                    String finStr    = medico.getOrDefault("franjaFin",    "17:00").toString();
                    int intervalo    = Integer.parseInt(
                            medico.getOrDefault("intervaloCitas", "30").toString());

                    // Citas ocupadas ese día (excluyendo la que se reagenda)
                    HttpRequest reqCitas = requestAutenticado(
                            "http://localhost:8080/api/citas/medico/" + cita.getMedicoId())
                            .GET().build();
                    HttpResponse<String> respCitas = httpClient.send(
                            reqCitas, HttpResponse.BodyHandlers.ofString());
                    List<Map<String, Object>> citasMedico = mapper.readValue(
                            respCitas.body(), new TypeReference<>() {});

                    Set<String> ocupadas = citasMedico.stream()
                            .filter(c -> {
                                String fh = c.getOrDefault("fechaHora", "").toString();
                                return fh.startsWith(newVal.toString())
                                        && !c.get("id").toString()
                                        .equals(cita.getId().toString());
                            })
                            .map(c -> c.get("fechaHora").toString().substring(11, 16))
                            .collect(Collectors.toSet());

                    // Generar slots
                    LocalTime ini = LocalTime.parse(inicioStr);
                    LocalTime fin = LocalTime.parse(finStr);
                    List<LocalTime> slots = new ArrayList<>();
                    LocalTime cursor = ini;
                    while (!cursor.isAfter(fin.minusMinutes(intervalo))) {
                        slots.add(cursor);
                        cursor = cursor.plusMinutes(intervalo);
                    }

                    Platform.runLater(() -> {
                        franjas.getChildren().clear();
                        DateTimeFormatter hf = DateTimeFormatter.ofPattern("HH:mm");
                        for (LocalTime slot : slots) {
                            String hora  = slot.format(hf);
                            boolean libre = !ocupadas.contains(hora);
                            Button btn = new Button(hora);
                            btn.setPrefWidth(72);
                            btn.setPrefHeight(36);
                            btn.setFont(Font.font("System", 13));
                            if (!libre) {
                                btn.setStyle("""
                                        -fx-background-color: #F3F4F6;
                                        -fx-text-fill: #9CA3AF;
                                        -fx-background-radius: 6;
                                        """);
                                btn.setDisable(true);
                                btn.setTooltip(new Tooltip("Horario ocupado"));
                            } else {
                                btn.setStyle(estiloSlotLibre());
                                btn.setOnAction(ev -> {
                                    franjas.getChildren().stream()
                                            .filter(n -> n instanceof Button)
                                            .map(n -> (Button) n)
                                            .filter(b -> !b.isDisabled())
                                            .forEach(b -> b.setStyle(estiloSlotLibre()));
                                    btn.setStyle(estiloSlotSeleccionado());
                                    horaSeleccionada[0] = hora;
                                });
                            }
                            franjas.getChildren().add(btn);
                        }
                        if (slots.isEmpty()) {
                            franjas.getChildren().add(hint("Sin franjas para este médico"));
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() ->
                            franjas.getChildren().setAll(hint("Error al cargar franjas")));
                }
            }).start();
        });

        Label lblFeedbackVentana = new Label();
        lblFeedbackVentana.setFont(Font.font("System", 12));
        lblFeedbackVentana.setWrapText(true);

        Button btnConfirmar = boton("✓  Confirmar reagendamiento", "#7B2FBE");
        btnConfirmar.setPrefHeight(40);
        btnConfirmar.setOnAction(e -> {
            if (dpNuevaFecha.getValue() == null || horaSeleccionada[0] == null) {
                lblFeedbackVentana.setText("✗ Selecciona fecha y franja horaria");
                lblFeedbackVentana.setTextFill(Color.web("#DC2626"));
                return;
            }
            String nuevaFechaHora = dpNuevaFecha.getValue() + "T" + horaSeleccionada[0];
            String json = "{\"fechaHora\": \"" + nuevaFechaHora + "\"}";
            try {
                HttpRequest req = requestAutenticado(
                        "http://localhost:8080/api/citas/" + cita.getId() + "/reagendar")
                        .header("Content-Type", "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                        .build();
                HttpResponse<String> resp = httpClient.send(req,
                        HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    feedback("✓ Cita reagendada correctamente", false);
                    cargarTodasLasCitas();
                    ventana.close();
                } else {
                    lblFeedbackVentana.setText("✗ Error: " + resp.body());
                    lblFeedbackVentana.setTextFill(Color.web("#DC2626"));
                }
            } catch (Exception ex) {
                lblFeedbackVentana.setText("✗ Error de conexión");
                lblFeedbackVentana.setTextFill(Color.web("#DC2626"));
            }
        });

        Button btnCerrar = new Button("Cancelar");
        btnCerrar.setStyle("""
                -fx-background-color: transparent;
                -fx-text-fill: #7B2FBE;
                -fx-border-color: #7B2FBE;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                -fx-cursor: hand;
                -fx-padding: 8 16;
                """);
        btnCerrar.setOnAction(e -> ventana.close());

        HBox botones = new HBox(10, btnConfirmar, btnCerrar);
        botones.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(12,
                lblInfo, lblFecha, dpNuevaFecha,
                lblFranjas, franjas, leyenda,
                botones, lblFeedbackVentana);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #F5F3FF;");

        ventana.setScene(new Scene(layout, 700, 430));
        ventana.show();
    }

    // ── Cargar franjas en el formulario de agendar ────────────────────────
    private void cargarFranjasAgendar() {
        String medicoLabel = cbMedicoAgendar.getValue();
        LocalDate fecha = dpFechaAgendar.getValue();
        if (medicoLabel == null || fecha == null) return;

        Long medicoId = getMedicoIdDesdeLabel(medicoLabel);
        if (medicoId == null) return;

        Map<String, Object> medicoData = datosMedicos.stream()
                .filter(m -> medicoId.equals(Long.parseLong(m.get("id").toString())))
                .findFirst().orElse(null);
        if (medicoData == null) return;

        String inicioStr = medicoData.getOrDefault("franjaInicio", "08:00").toString();
        String finStr    = medicoData.getOrDefault("franjaFin",    "17:00").toString();
        int intervalo    = Integer.parseInt(
                medicoData.getOrDefault("intervaloCitas", "30").toString());

        panelFranjasAgendar.getChildren().setAll(hint("Cargando franjas..."));

        new Thread(() -> {
            try {
                HttpRequest req = requestAutenticado(
                        "http://localhost:8080/api/citas/medico/" + medicoId)
                        .GET().build();
                HttpResponse<String> resp = httpClient.send(req,
                        HttpResponse.BodyHandlers.ofString());
                List<Map<String, Object>> citasMedico = mapper.readValue(
                        resp.body(), new TypeReference<>() {});

                Set<String> ocupadas = citasMedico.stream()
                        .filter(c -> {
                            String fh = c.getOrDefault("fechaHora", "").toString();
                            return fh.startsWith(fecha.toString());
                        })
                        .map(c -> c.get("fechaHora").toString().substring(11, 16))
                        .collect(Collectors.toSet());

                LocalTime ini = LocalTime.parse(inicioStr);
                LocalTime fin = LocalTime.parse(finStr);
                List<LocalTime> slots = new ArrayList<>();
                LocalTime cursor = ini;
                while (!cursor.isAfter(fin.minusMinutes(intervalo))) {
                    slots.add(cursor);
                    cursor = cursor.plusMinutes(intervalo);
                }

                Platform.runLater(() -> {
                    panelFranjasAgendar.getChildren().clear();
                    horaSeleccionadaAgendar = null;
                    DateTimeFormatter hf = DateTimeFormatter.ofPattern("HH:mm");
                    for (LocalTime slot : slots) {
                        String hora  = slot.format(hf);
                        boolean libre = !ocupadas.contains(hora);
                        Button btn = new Button(hora);
                        btn.setPrefWidth(72);
                        btn.setPrefHeight(36);
                        btn.setFont(Font.font("System", 13));
                        if (!libre) {
                            btn.setStyle("""
                                    -fx-background-color: #F3F4F6;
                                    -fx-text-fill: #9CA3AF;
                                    -fx-background-radius: 6;
                                    """);
                            btn.setDisable(true);
                            btn.setTooltip(new Tooltip("Horario ocupado"));
                        } else {
                            btn.setStyle(estiloSlotLibre());
                            btn.setOnAction(ev -> {
                                panelFranjasAgendar.getChildren().stream()
                                        .filter(n -> n instanceof Button)
                                        .map(n -> (Button) n)
                                        .filter(b -> !b.isDisabled())
                                        .forEach(b -> b.setStyle(estiloSlotLibre()));
                                btn.setStyle(estiloSlotSeleccionado());
                                horaSeleccionadaAgendar = hora;
                            });
                        }
                        panelFranjasAgendar.getChildren().add(btn);
                    }
                    if (slots.isEmpty()) {
                        panelFranjasAgendar.getChildren().add(
                                hint("Sin franjas configuradas para este médico"));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        panelFranjasAgendar.getChildren().setAll(
                                hint("Error al cargar franjas")));
            }
        }).start();
    }

    // ── Exportar CSV ──────────────────────────────────────────────────────
    private void exportarCSV() {
        String medicoLabel = cbMedicoExportar.getValue();
        if (medicoLabel == null) {
            feedback("✗ Seleccione un médico para exportar", true);
            return;
        }
        if (dpFechaBuscar.getValue() == null) {
            feedback("✗ Seleccione una fecha para exportar", true);
            return;
        }

        Long medicoId = getMedicoIdDesdeLabel(medicoLabel);
        if (medicoId == null) {
            feedback("✗ Médico no válido", true);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Excel");
        fileChooser.setInitialFileName("citas_" + dpFechaBuscar.getValue() + ".xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        File archivo = fileChooser.showSaveDialog(primaryStage);
        if (archivo == null) return;

        try {
            String url = "http://localhost:8080/api/citas/export"
                    + "?medicoId=" + medicoId
                    + "&fecha=" + dpFechaBuscar.getValue();

            HttpRequest request = requestAutenticado(url).GET().build();
            HttpResponse<byte[]> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                feedback("✗ Error del servidor: "
                        + new String(response.body(), StandardCharsets.UTF_8), true);
                return;
            }
            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                fos.write(response.body());
            }
            feedback("✓ Excel exportado en: " + archivo.getAbsolutePath(), false);
        } catch (Exception e) {
            feedback("✗ No se pudo exportar: " + e.getMessage(), true);
        }
    }

    // ── Cargar médicos con nombre + especialidad ──────────────────────────
    private void cargarMedicos() {
        try {
            HttpRequest request = requestAutenticado(
                    "http://localhost:8080/api/medicos/disponibles")
                    .GET().build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            List<Map<String, Object>> lista = mapper.readValue(
                    response.body(), new TypeReference<>() {});
            datosMedicos.clear();
            datosMedicos.addAll(lista);
            List<String> labels = lista.stream()
                    .map(m -> m.get("nombre") + " " + m.get("apellido")
                            + " · " + m.getOrDefault("especialidad", ""))
                    .toList();
            cbMedicoAgendar.getItems().setAll(labels);
            cbMedicoExportar.getItems().setAll(labels);
        } catch (Exception e) {
        }
    }

    // ── Obtener medicoId a partir del label del ComboBox ─────────────────
    private Long getMedicoIdDesdeLabel(String label) {
        return datosMedicos.stream()
                .filter(m -> {
                    String lbl = m.get("nombre") + " " + m.get("apellido")
                            + " · " + m.getOrDefault("especialidad", "");
                    return lbl.equals(label);
                })
                .map(m -> Long.parseLong(m.get("id").toString()))
                .findFirst().orElse(null);
    }

    // ── Mapear respuesta a objeto Agenda ──────────────────────────────────
    private Agenda mapToCita(Map<String, Object> m) {
        Agenda a = new Agenda();
        a.setId(m.get("id") != null
                ? Long.parseLong(m.get("id").toString()) : 0L);
        a.setPacienteId(m.get("pacienteId") != null
                ? Long.parseLong(m.get("pacienteId").toString()) : 0L);
        if (m.get("medico") instanceof Map<?, ?> medico) {
            a.setMedico(medico.get("nombre") + " " + medico.get("apellido"));
            a.setMedicoId(Long.parseLong(medico.get("id").toString()));
        }
        a.setFechaHora(m.get("fechaHora") != null
                ? m.get("fechaHora").toString() : "");
        a.setMotivo(m.get("motivo") != null
                ? m.get("motivo").toString() : "");
        a.setEstado(m.get("estado") != null
                ? m.get("estado").toString() : "");

        // Resolver nombre y documento del paciente
        try {
            HttpRequest reqPac = requestAutenticado(
                    "http://localhost:8080/api/pacientes/" + a.getPacienteId())
                    .GET().build();
            HttpResponse<String> respPac = httpClient.send(reqPac,
                    HttpResponse.BodyHandlers.ofString());
            if (respPac.statusCode() == 200) {
                Map<String, Object> paciente = mapper.readValue(
                        respPac.body(), new TypeReference<>() {});
                String nombre = paciente.get("nombre") + " " + paciente.get("apellido");
                String doc    = paciente.getOrDefault("numeroDocumento", "").toString();
                a.setNombrePaciente(nombre + " · " + doc);
            } else {
                a.setNombrePaciente("ID: " + a.getPacienteId());
            }
        } catch (Exception e) {
            a.setNombrePaciente("ID: " + a.getPacienteId());
        }

        return a;
    }

    private void limpiarFormulario() {
        txtDocumentoPaciente.clear();
        txtMotivo.clear();
        dpFechaAgendar.setValue(null);
        cbMedicoAgendar.setValue(null);
        horaSeleccionadaAgendar = null;
        panelFranjasAgendar.getChildren().setAll(hint("Seleccione médico y fecha primero"));
    }

    private void feedback(String mensaje, boolean error) {
        lblFeedback.setText(mensaje);
        lblFeedback.setTextFill(error
                ? Color.web("#DC2626") : Color.web("#059669"));
    }

    // ── Helper JWT ────────────────────────────────────────────────────────
    private HttpRequest.Builder requestAutenticado(String url) {
        String token = com.piedraazul.ui.app.PiedraAzulApp.getToken();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return builder;
    }

    // ── Estilos ───────────────────────────────────────────────────────────
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

    private String estiloBotonTabla(String color) {
        return "-fx-background-color: " + color + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 11px;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 5;"
                + "-fx-cursor: hand;"
                + "-fx-padding: 4 8;";
    }

    private Label etiqueta(String texto) {
        Label lbl = new Label(texto);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 12));
        lbl.setTextFill(Color.web("#4C1D95"));
        return lbl;
    }

    private Label hint(String texto) {
        Label l = new Label(texto);
        l.setFont(Font.font("System", 12));
        l.setTextFill(Color.web("#6B7280"));
        return l;
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

    private String estiloSlotLibre() {
        return """
                -fx-background-color: #EDE9FE;
                -fx-text-fill: #4C1D95;
                -fx-border-color: #C084FC;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                -fx-cursor: hand;
                """;
    }

    private String estiloSlotSeleccionado() {
        return """
                -fx-background-color: #7B2FBE;
                -fx-text-fill: white;
                -fx-border-color: #6D28D9;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                -fx-cursor: hand;
                -fx-font-weight: bold;
                """;
    }

    private HBox chip(String texto, String colorBorde, String colorFondo) {
        Region dot = new Region();
        dot.setPrefSize(14, 14);
        dot.setStyle("-fx-background-color: " + colorFondo + ";"
                + "-fx-border-color: " + colorBorde + ";"
                + "-fx-border-radius: 3;"
                + "-fx-background-radius: 3;");
        Label lbl = new Label(texto);
        lbl.setFont(Font.font("System", 11));
        lbl.setTextFill(Color.web("#6B7280"));
        HBox box = new HBox(5, dot, lbl);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void filtrarPorMedico() {
        String busq = txtBuscarMedico.getText().trim().toLowerCase();
        if (busq.isBlank()) {
            citas.setAll(todasLasCitas);
            feedback("✓ " + citas.size() + " citas cargadas", false);
            return;
        }
        List<Agenda> filtradas = todasLasCitas.stream()
                .filter(a -> a.getMedico() != null
                        && a.getMedico().toLowerCase().contains(busq))
                .toList();
        citas.setAll(filtradas);
        feedback("✓ " + filtradas.size() + " citas encontradas para médico: "
                + txtBuscarMedico.getText().trim(), false);
    }

    public static void main(String[] args) {
        launch();
    }
}