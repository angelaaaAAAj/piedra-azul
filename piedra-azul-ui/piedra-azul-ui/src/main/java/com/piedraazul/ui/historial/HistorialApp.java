package com.piedraazul.ui.historial;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.application.Application;
import java.time.LocalDateTime;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class HistorialApp extends Application {

    private final Long   medicoId;
    private final String nombreMedico;

    private final HttpClient   http   = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final ObservableList<HistorialEntry>      registros    = FXCollections.observableArrayList();
    private final ObservableList<ReagendamientoEntry> reagendamientos = FXCollections.observableArrayList();

    // Campos del formulario de registro (se pre-llenan al seleccionar cita)
    private final Label       lblFeedback           = new Label();

    public HistorialApp(Long medicoId, String nombreMedico) {
        this.medicoId     = medicoId;
        this.nombreMedico = nombreMedico != null ? nombreMedico : "";
    }

    public HistorialApp() {
        this.medicoId     = null;
        this.nombreMedico = "";
    }

    @Override
    public void start(Stage stage) {

        // ── ENCABEZADO ──
        Label titulo = new Label("Historial Clínico - Piedra Azul");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 22));
        titulo.setTextFill(Color.web("#4C1D95"));

        Label subtitulo = new Label(nombreMedico.isBlank()
                ? "Consulta de historial"
                : "Dr/a. " + nombreMedico);
        subtitulo.setFont(Font.font("System", 13));
        subtitulo.setTextFill(Color.web("#6B7280"));

        // ── SECCIÓN: CITAS DEL MÉDICO ──
        DatePicker dpFecha = new DatePicker(LocalDate.now());
        dpFecha.setPrefHeight(36);

        Button btnFiltrar = boton("🔍 Ver citas", "#7B2FBE");

        HBox panelFiltro = new HBox(10,
                etiqueta("Fecha:"), dpFecha, btnFiltrar);
        panelFiltro.setAlignment(Pos.CENTER_LEFT);

        // Tabla de citas
        TableView<CitaEntry> tablaCitas = new TableView<>();
        tablaCitas.setPlaceholder(new Label("No hay citas para esta fecha"));
        tablaCitas.setPrefHeight(220);
        tablaCitas.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                cargarHistorialPorCita(newVal.getId());
            }
        });

        TableColumn<CitaEntry, Long>   colId      = colCita("ID",       "id",       60);
        TableColumn<CitaEntry, String> colHora    = colCita("Hora",     "hora",     80);
        TableColumn<CitaEntry, String> colPac     = colCita("Paciente", "nombrePaciente", 200);
        TableColumn<CitaEntry, String> colMotivo  = colCita("Motivo",   "motivo",   160);
        TableColumn<CitaEntry, String> colEstado  = new TableColumn<>("Estado");
        colEstado.setPrefWidth(110);
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                badge.setFont(Font.font("System", FontWeight.BOLD, 11));
                badge.setPadding(new Insets(3, 10, 3, 10));
                badge.setStyle(badgeEstado(item));
                setGraphic(badge);
                setText(null);
            }
        });

        // Columna de acciones
        TableColumn<CitaEntry, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setPrefWidth(240);
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnHistorial = new Button("📋 Ver historial");
            private final Button btnAtendida  = new Button("✓ Marcar atendida");
            private final HBox   caja         = new HBox(6, btnHistorial, btnAtendida);

            {
                btnHistorial.setStyle(estiloBotonTabla("#7B2FBE"));
                btnAtendida.setStyle(estiloBotonTabla("#059669"));
                caja.setAlignment(Pos.CENTER);

                btnHistorial.setOnAction(e -> {
                    CitaEntry cita = getTableView().getItems().get(getIndex());
                    abrirVentanaHistorial(cita, tablaCitas, dpFecha);
                });

                btnAtendida.setOnAction(e -> {
                    CitaEntry cita = getTableView().getItems().get(getIndex());
                    marcarAtendida(cita.getId(), tablaCitas, dpFecha.getValue());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                CitaEntry cita = getTableView().getItems().get(getIndex());
                boolean completada = "COMPLETADA".equals(cita.getEstado());
                btnAtendida.setDisable(completada);
                btnAtendida.setText(completada ? "✓ Atendida" : "✓ Marcar atendida");
                setGraphic(caja);
            }
        });

        tablaCitas.getColumns().addAll(colId, colHora, colPac,
                colMotivo, colEstado, colAcciones);

        btnFiltrar.setOnAction(e ->
                cargarCitasMedico(tablaCitas, dpFecha.getValue()));

        // Cargar hoy al abrir
        if (medicoId != null) {
            cargarCitasMedico(tablaCitas, LocalDate.now());
        }

        VBox seccionCitas = seccion("📅  Mis citas",
                new VBox(10, panelFiltro, tablaCitas));
        seccionCitas.setVisible(medicoId != null);
        seccionCitas.setManaged(medicoId != null);

        // ── SECCIÓN: HISTORIAL DE LA CITA SELECCIONADA ──
        TableView<HistorialEntry> tablaHistorial = new TableView<>();
        tablaHistorial.setPlaceholder(
                new Label("Selecciona una cita para ver su historial"));
        tablaHistorial.setPrefHeight(160);

        TableColumn<HistorialEntry, Number> colHId   = new TableColumn<>("ID");
        colHId.setCellValueFactory(c -> c.getValue().idProperty());
        colHId.setPrefWidth(50);

        TableColumn<HistorialEntry, String> colHFecha = new TableColumn<>("Fecha");
        colHFecha.setCellValueFactory(c -> c.getValue().fechaRegistroProperty());
        colHFecha.setPrefWidth(140);

        TableColumn<HistorialEntry, String> colHTipo  = new TableColumn<>("Tipo");
        colHTipo.setCellValueFactory(c -> c.getValue().tipoRegistroProperty());
        colHTipo.setPrefWidth(110);

        TableColumn<HistorialEntry, String> colHDesc  = new TableColumn<>("Descripción");
        colHDesc.setCellValueFactory(c -> c.getValue().descripcionProperty());
        colHDesc.setPrefWidth(300);

        TableColumn<HistorialEntry, String> colHProf  = new TableColumn<>("Profesional");
        colHProf.setCellValueFactory(c -> c.getValue().registradoPorProperty());
        colHProf.setPrefWidth(140);

        tablaHistorial.getColumns().addAll(colHId, colHFecha, colHTipo, colHDesc, colHProf);
        tablaHistorial.setItems(registros);

        // ── SECCIÓN: REAGENDAMIENTOS ──
        TableView<ReagendamientoEntry> tablaReag = new TableView<>();
        tablaReag.setPlaceholder(new Label("Sin reagendamientos para esta cita"));
        tablaReag.setPrefHeight(120);
        tablaReag.setItems(reagendamientos);

        TableColumn<ReagendamientoEntry, String> colRAnt  = new TableColumn<>("Fecha anterior");
        colRAnt.setCellValueFactory(c -> c.getValue().fechaAnteriorProperty());
        colRAnt.setPrefWidth(160);

        TableColumn<ReagendamientoEntry, String> colRNueva = new TableColumn<>("Fecha nueva");
        colRNueva.setCellValueFactory(c -> c.getValue().fechaNuevaProperty());
        colRNueva.setPrefWidth(160);

        TableColumn<ReagendamientoEntry, String> colRMotivo = new TableColumn<>("Motivo");
        colRMotivo.setCellValueFactory(c -> c.getValue().motivoProperty());
        colRMotivo.setPrefWidth(180);

        TableColumn<ReagendamientoEntry, String> colRResp = new TableColumn<>("Responsable");
        colRResp.setCellValueFactory(c -> c.getValue().responsableProperty());
        colRResp.setPrefWidth(130);

        tablaReag.getColumns().addAll(colRAnt, colRNueva, colRMotivo, colRResp);


        lblFeedback.setFont(Font.font("System", 12));
        lblFeedback.setWrapText(true);

        // ── LAYOUT ──
        VBox root = new VBox(14,
                new VBox(4, titulo, subtitulo),
                seccionCitas,
                seccion("📋  Registros clínicos de la cita seleccionada", tablaHistorial),
                seccion("🔄  Reagendamientos de la cita seleccionada", tablaReag),
                lblFeedback);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #F5F3FF;");

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F5F3FF; -fx-background-color: #F5F3FF;");

        stage.setTitle("Historial Clínico - Piedra Azul");
        stage.setScene(new Scene(scroll, 1050, 750));
        stage.show();
    }

    private void abrirVentanaHistorial(CitaEntry cita,
                                       TableView<CitaEntry> tablaCitas,
                                       DatePicker dpFecha) {
        Stage ventana = new Stage();
        ventana.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        ventana.setTitle("Historial - Cita #" + cita.getId()
                + "  ·  " + cita.getNombrePaciente());

        // ── Encabezado ──
        Label lblInfo = new Label("Cita #" + cita.getId()
                + "  ·  " + cita.getHora()
                + "  ·  " + cita.getNombrePaciente());
        lblInfo.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblInfo.setTextFill(Color.web("#4C1D95"));

        // ── Tabla historial ──
        ObservableList<HistorialEntry> regsVentana =
                FXCollections.observableArrayList();
        TableView<HistorialEntry> tablaH = new TableView<>();
        tablaH.setItems(regsVentana);
        tablaH.setPlaceholder(new Label("Sin registros clínicos para esta cita"));
        tablaH.setPrefHeight(180);

        TableColumn<HistorialEntry, Number> cId = new TableColumn<>("ID");
        cId.setCellValueFactory(c -> c.getValue().idProperty());
        cId.setPrefWidth(50);
        TableColumn<HistorialEntry, String> cFecha = new TableColumn<>("Fecha");
        cFecha.setCellValueFactory(c -> c.getValue().fechaRegistroProperty());
        cFecha.setPrefWidth(130);
        TableColumn<HistorialEntry, String> cTipo = new TableColumn<>("Tipo");
        cTipo.setCellValueFactory(c -> c.getValue().tipoRegistroProperty());
        cTipo.setPrefWidth(100);
        TableColumn<HistorialEntry, String> cDesc = new TableColumn<>("Descripción");
        cDesc.setCellValueFactory(c -> c.getValue().descripcionProperty());
        cDesc.setPrefWidth(280);
        TableColumn<HistorialEntry, String> cProf = new TableColumn<>("Profesional");
        cProf.setCellValueFactory(c -> c.getValue().registradoPorProperty());
        cProf.setPrefWidth(130);
        tablaH.getColumns().addAll(cId, cFecha, cTipo, cDesc, cProf);

        // ── Formulario registro ──
        TextField fCitaId = new TextField(String.valueOf(cita.getId()));
        fCitaId.setEditable(false);
        fCitaId.setStyle(campoEstilo() + "-fx-background-color: #F3F4F6;");

        TextField fPacId = new TextField(String.valueOf(cita.getPacienteId()));
        fPacId.setEditable(false);
        fPacId.setStyle(campoEstilo() + "-fx-background-color: #F3F4F6;");

        ComboBox<String> cbTipoV = new ComboBox<>();
        cbTipoV.getItems().addAll("CONSULTA", "CONTROL", "PROCEDIMIENTO");
        cbTipoV.setPromptText("Tipo de registro *");
        cbTipoV.setMaxWidth(Double.MAX_VALUE);

        TextArea txtDescV = new TextArea();
        txtDescV.setPromptText("Descripción del procedimiento o control *");
        txtDescV.setPrefHeight(80);
        txtDescV.setWrapText(true);
        txtDescV.setStyle(campoEstilo());

        Label lblFbV = new Label();
        lblFbV.setFont(Font.font("System", 12));
        lblFbV.setWrapText(true);

        Button btnReg = boton("💾 Registrar y marcar como atendida", "#2E7D32");
        btnReg.setPrefWidth(Double.MAX_VALUE);

        // Deshabilitar si ya está completada
        if ("COMPLETADA".equals(cita.getEstado())) {
            btnReg.setDisable(true);
            btnReg.setText("✓ Cita ya atendida");
        }

        btnReg.setOnAction(ev -> {
            String desc = txtDescV.getText().trim();
            String tipo = cbTipoV.getValue();
            if (desc.isBlank() || tipo == null) {
                lblFbV.setText("✗ Completa todos los campos");
                lblFbV.setTextFill(Color.web("#DC2626"));
                return;
            }

            HistorialController controller = new HistorialController();
            controller.registrarEntrada(
                    cita.getPacienteId(),
                    medicoId,
                    cita.getId(),
                    tipo, desc, nombreMedico,
                    lblFbV);

            // Marcar como atendida automáticamente
            new Thread(() -> {
                try {
                    HttpRequest req = requestAuth(
                            "http://localhost:8080/api/citas/"
                                    + cita.getId() + "/completar")
                            .method("PATCH",
                                    HttpRequest.BodyPublishers.noBody())
                            .build();

                    // CAMBIO: leer la respuesta
                    HttpResponse<String> respCompletar = http.send(req,
                            HttpResponse.BodyHandlers.ofString());

                    // AGREGAR este print:
                    System.out.println("COMPLETAR status: " + respCompletar.statusCode());
                    System.out.println("COMPLETAR body: " + respCompletar.body());

                    if (respCompletar.statusCode() != 200) {
                        Platform.runLater(() -> {
                            lblFbV.setText("✗ Error al completar: " + respCompletar.body());
                            lblFbV.setTextFill(Color.web("#DC2626"));
                        });
                        return; // ← detener si falló
                    }

                    // Recargar historial dentro de la ventana
                    new Thread(() -> {
                        try { Thread.sleep(400); } catch (Exception ignored) {}
                        cargarHistorialEnVentana(cita.getId(), regsVentana);
                    }).start();

                    Platform.runLater(() -> {
                        feedback(lblFeedback,
                                "✓ Cita #" + cita.getId() + " marcada como atendida",
                                true);
                        cargarCitasMedico(tablaCitas, dpFecha.getValue());
                        txtDescV.clear();
                        cbTipoV.setValue(null);
                        btnReg.setDisable(true);
                        btnReg.setText("✓ Cita ya atendida");
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        lblFbV.setText("✗ Error al marcar como atendida");
                        lblFbV.setTextFill(Color.web("#DC2626"));
                    });
                }
            }).start();
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.add(etiqueta("Cita ID:"),     0, 0); form.add(fCitaId, 1, 0);
        form.add(etiqueta("Paciente ID:"), 2, 0); form.add(fPacId,  3, 0);
        form.add(etiqueta("Tipo *:"),      0, 1); form.add(cbTipoV, 1, 1, 3, 1);
        form.add(etiqueta("Descripción *:"), 0, 2); form.add(txtDescV, 1, 2, 3, 1);
        form.add(lblFbV, 0, 3, 3, 1);
        form.add(btnReg, 0, 4, 4, 1);

        VBox layout = new VBox(14,
                lblInfo,
                seccion("📋  Registros clínicos", tablaH),
                seccion("📝  Nueva entrada clínica", form));
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #F5F3FF;");

        ScrollPane scroll = new ScrollPane(layout);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F5F3FF; -fx-background-color: #F5F3FF;");

        ventana.setScene(new Scene(scroll, 780, 600));
        ventana.show();

        // Cargar historial al abrir
        cargarHistorialEnVentana(cita.getId(), regsVentana);
    }

    // Carga el historial de una cita en la lista dada (para la ventana modal)
    private void cargarHistorialEnVentana(Long citaId,
                                          ObservableList<HistorialEntry> lista) {
        new Thread(() -> {
            try {
                HttpRequest req = requestAuth(
                        "http://localhost:8080/api/historial/cita/" + citaId)
                        .GET().build();
                HttpResponse<String> resp =
                        http.send(req, HttpResponse.BodyHandlers.ofString());
                System.out.println("Respuesta /completar: " + resp.statusCode() + " - " + resp.body());
                List<Map<String, Object>> data = mapper.readValue(
                        resp.body(), new TypeReference<>() {});

                List<HistorialEntry> entries = data.stream().map(m -> {
                    long hId    = Long.parseLong(m.get("id").toString());
                    long hPacId = m.get("pacienteId") != null
                            ? Long.parseLong(m.get("pacienteId").toString()) : 0L;
                    long hMedId = m.get("medicoId") != null
                            ? Long.parseLong(m.get("medicoId").toString()) : 0L;
                    long hCitId = m.get("citaId") != null
                            ? Long.parseLong(m.get("citaId").toString()) : 0L;
                    String tipo    = m.getOrDefault("tipoRegistro", "").toString();
                    String desc    = m.getOrDefault("descripcion", "").toString();
                    String regPor  = m.getOrDefault("registradoPor", "").toString();
                    String fechaStr = m.getOrDefault("fecha", "").toString();
                    LocalDateTime fechaDt = null;
                    try {
                        if (!fechaStr.isBlank())
                            fechaDt = LocalDateTime.parse(fechaStr.substring(0, 19));
                    } catch (Exception ignored) {}
                    return new HistorialEntry(
                            hId, hPacId, hMedId, hCitId,
                            tipo, desc, fechaDt, regPor);
                }).toList();

                Platform.runLater(() -> lista.setAll(entries));
            } catch (Exception e) {
                Platform.runLater(() -> lista.clear());
            }
        }).start();
    }

    // ── Carga las citas del médico para una fecha ─────────────────────────
    private void cargarCitasMedico(TableView<CitaEntry> tabla, LocalDate fecha) {
        if (medicoId == null) return;
        new Thread(() -> {
            try {
                HttpRequest req = requestAuth(
                        "http://localhost:8080/api/citas/medico/" + medicoId)
                        .GET().build();
                System.out.println("URL llamada: " + req.uri());
                HttpResponse<String> resp =
                        http.send(req, HttpResponse.BodyHandlers.ofString());
                List<Map<String, Object>> lista = mapper.readValue(
                        resp.body(), new TypeReference<>() {});

                // Filtrar por fecha
                String fechaStr = fecha != null ? fecha.toString() : LocalDate.now().toString();
                List<CitaEntry> entries = lista.stream()
                        .filter(m -> {
                            String fh = m.getOrDefault("fechaHora", "").toString();
                            return fh.startsWith(fechaStr);
                        })
                        .map(this::mapToCitaEntry)
                        .toList();

                Platform.runLater(() -> tabla.setItems(
                        FXCollections.observableArrayList(entries)));
            } catch (Exception e) {
                Platform.runLater(() ->
                        feedback(lblFeedback, "✗ Error al cargar citas", false));
            }
        }).start();
    }

    // ── Marcar cita como atendida (COMPLETADA) ────────────────────────────
    private void marcarAtendida(Long citaId, TableView<CitaEntry> tabla,
                                LocalDate fecha) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Marcar la cita #" + citaId + " como atendida?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmar");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.YES) return;
            new Thread(() -> {
                try {
                    // Reutilizar el endpoint PATCH /cancelar pero con estado COMPLETADA
                    // El backend tiene el endpoint /completar o usamos un body con estado
                    HttpRequest req = requestAuth(
                            "http://localhost:8080/api/citas/" + citaId + "/completar")
                            .method("PATCH", HttpRequest.BodyPublishers.noBody())
                            .build();
                    HttpResponse<String> resp =
                            http.send(req, HttpResponse.BodyHandlers.ofString());

                    Platform.runLater(() -> {
                        if (resp.statusCode() == 200) {
                            feedback(lblFeedback,
                                    "✓ Cita #" + citaId + " marcada como atendida", true);
                            cargarCitasMedico(tabla, fecha);
                        } else {
                            feedback(lblFeedback,
                                    "✗ Error al marcar cita: " + resp.body(), false);
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() ->
                            feedback(lblFeedback, "✗ Error de conexión", false));
                }
            }).start();
        });
    }

    // ── Carga el historial de una cita específica ─────────────────────────
    private void cargarHistorialPorCita(Long citaId) {
        new Thread(() -> {
            try {
                HttpRequest req = requestAuth(
                        "http://localhost:8080/api/historial/cita/" + citaId)
                        .GET().build();
                HttpResponse<String> resp =
                        http.send(req, HttpResponse.BodyHandlers.ofString());
                List<Map<String, Object>> lista = mapper.readValue(
                        resp.body(), new TypeReference<>() {});

                List<HistorialEntry> entries = lista.stream()
                        .map(m -> {
                            long hId       = Long.parseLong(m.get("id").toString());
                            long hPacId    = m.get("pacienteId") != null
                                    ? Long.parseLong(m.get("pacienteId").toString()) : 0L;
                            long hMedId    = m.get("medicoId") != null
                                    ? Long.parseLong(m.get("medicoId").toString()) : 0L;
                            long hCitaId   = m.get("citaId") != null
                                    ? Long.parseLong(m.get("citaId").toString()) : 0L;
                            String tipo    = m.getOrDefault("tipoRegistro", "").toString();
                            String desc    = m.getOrDefault("descripcion", "").toString();
                            String regPor  = m.getOrDefault("registradoPor", "").toString();
                            String fechaStr = m.getOrDefault("fecha", "").toString();
                            java.time.LocalDateTime fechaDt = null;
                            try {
                                if (!fechaStr.isBlank())
                                    fechaDt = java.time.LocalDateTime.parse(
                                            fechaStr.substring(0, 19));
                            } catch (Exception ignored) {}
                            return new HistorialEntry(
                                    hId, hPacId, hMedId, hCitaId,
                                    tipo, desc, fechaDt, regPor);
                        })
                        .toList();

                // Cargar también reagendamientos
                HttpRequest reqR = requestAuth(
                        "http://localhost:8080/api/historial/cambios/cita/" + citaId)
                        .GET().build();
                HttpResponse<String> respR =
                        http.send(reqR, HttpResponse.BodyHandlers.ofString());
                List<Map<String, Object>> listaR = mapper.readValue(
                        respR.body(), new TypeReference<>() {});

                List<ReagendamientoEntry> reagEntries = listaR.stream()
                        .map(m -> new ReagendamientoEntry(
                                m.getOrDefault("fechaHoraAnterior", "").toString(),
                                m.getOrDefault("fechaHoraNueva", "").toString(),
                                m.getOrDefault("motivoCambio", "").toString(),
                                m.getOrDefault("cambiadoPor", "").toString()))
                        .toList();

                Platform.runLater(() -> {
                    registros.setAll(entries);
                    reagendamientos.setAll(reagEntries);
                    feedback(lblFeedback,
                            "✓ Historial de cita #" + citaId + " cargado", true);
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        feedback(lblFeedback, "✗ Error al cargar historial", false));
            }
        }).start();
    }

    // ── Mapear respuesta a CitaEntry ──────────────────────────────────────
    private CitaEntry mapToCitaEntry(Map<String, Object> m) {
        CitaEntry e = new CitaEntry();
        e.setId(Long.parseLong(m.get("id").toString()));
        e.setPacienteId(m.get("pacienteId") != null
                ? Long.parseLong(m.get("pacienteId").toString()) : 0L);
        String fh = m.getOrDefault("fechaHora", "").toString();
        e.setHora(fh.length() >= 16 ? fh.substring(11, 16) : fh);
        e.setMotivo(m.getOrDefault("motivo", "").toString());
        e.setEstado(m.getOrDefault("estado", "").toString());

        // Resolver nombre del paciente
        try {
            HttpRequest req = requestAuth(
                    "http://localhost:8080/api/pacientes/" + e.getPacienteId())
                    .GET().build();
            HttpResponse<String> resp =
                    http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                Map<String, Object> pac = mapper.readValue(
                        resp.body(), new TypeReference<>() {});
                e.setNombrePaciente(pac.get("nombre") + " " + pac.get("apellido"));
            }
        } catch (Exception ex) {
            e.setNombrePaciente("ID: " + e.getPacienteId());
        }
        return e;
    }

    private HttpRequest.Builder requestAuth(String url) {
        String token = com.piedraazul.ui.app.PiedraAzulApp.getToken();
        HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(url));
        if (token != null) b.header("Authorization", "Bearer " + token);
        return b;
    }

    private void feedback(Label lbl, String msg, boolean ok) {
        lbl.setText(msg);
        lbl.setTextFill(ok ? Color.web("#059669") : Color.web("#DC2626"));
    }

    // ── Helpers de UI ─────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private <T> TableColumn<CitaEntry, T> colCita(String titulo,
                                                  String prop, double ancho) {
        TableColumn<CitaEntry, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(ancho);
        return c;
    }

    private String badgeEstado(String estado) {
        return switch (estado) {
            case "PROGRAMADA"  ->
                    "-fx-background-color:#EDE9FE; -fx-text-fill:#4C1D95; -fx-background-radius:999;";
            case "COMPLETADA"  ->
                    "-fx-background-color:#D1FAE5; -fx-text-fill:#065F46; -fx-background-radius:999;";
            case "CANCELADA"   ->
                    "-fx-background-color:#FEE2E2; -fx-text-fill:#991B1B; -fx-background-radius:999;";
            default ->
                    "-fx-background-color:#F3F4F6; -fx-text-fill:#374151; -fx-background-radius:999;";
        };
    }

    private String estiloBotonTabla(String color) {
        return "-fx-background-color: " + color + ";"
                + "-fx-text-fill: white; -fx-font-size: 11px;"
                + "-fx-font-weight: bold; -fx-background-radius: 5;"
                + "-fx-cursor: hand; -fx-padding: 4 8;";
    }

    private VBox seccion(String titulo, javafx.scene.Node contenido) {
        Label lbl = new Label(titulo);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 13));
        lbl.setTextFill(Color.web("#4C1D95"));
        VBox caja = new VBox(8, lbl, contenido);
        caja.setPadding(new Insets(12));
        caja.setBackground(new Background(new BackgroundFill(
                Color.WHITE, new CornerRadii(8), Insets.EMPTY)));
        caja.setStyle("-fx-border-color: #C084FC; -fx-border-radius: 8;");
        return caja;
    }

    private Button boton(String texto, String color) {
        Button btn = new Button(texto);
        btn.setStyle("-fx-background-color: " + color + ";"
                + "-fx-text-fill: white; -fx-font-weight: bold;"
                + "-fx-background-radius: 6; -fx-cursor: hand;");
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
                -fx-font-size: 12px;
                """;
    }

    public static void main(String[] args) { launch(); }
}