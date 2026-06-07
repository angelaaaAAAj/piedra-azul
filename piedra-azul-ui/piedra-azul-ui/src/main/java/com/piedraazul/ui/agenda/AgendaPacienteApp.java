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
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Ventana de agendamiento autónomo para rol PACIENTE
// HU-10 — El paciente elige médico, franja horaria y confirma la cita
public class AgendaPacienteApp extends Application {

    // -- HTTP + JSON --
    private final HttpClient http     = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // -- Estado de selección --
    private Long pacienteId;
    private Map<String, Object> medicoSeleccionado = null;
    private LocalDateTime       franjaSeleccionada  = null;

    // -- Controles de UI --
    private final ListView<String>         listaMedicos = new ListView<>();
    private final ObservableList<String>   itemsMedicos = FXCollections.observableArrayList();
    private final List<Map<String,Object>> datosMedicos = new ArrayList<>();

    private final DatePicker dpFecha      = new DatePicker(LocalDate.now().plusDays(1));
    private final FlowPane   panelFranjas = new FlowPane();
    private final TextArea   txtMotivo    = new TextArea();
    private final Label      lblResumen   = new Label("Seleccione médico y franja");
    private final Label      lblFeedback  = new Label();

    // FIX 2: Constructor principal recibe pacienteId desde el login
    public AgendaPacienteApp(Long pacienteId) {
        this.pacienteId = pacienteId;
    }

    // Constructor sin args requerido por Application.launch()
    public AgendaPacienteApp() {}

    @Override
    public void start(Stage stage) {

        // ── ENCABEZADO ──
        Label titulo = new Label("Agendar mi cita");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 20));
        titulo.setTextFill(Color.web("#4C1D95"));

        Label subtitulo = new Label("Clínica Piedra Azul");
        subtitulo.setFont(Font.font("System", 13));
        subtitulo.setTextFill(Color.web("#6B7280"));

        VBox encabezado = new VBox(2, titulo, subtitulo);
        encabezado.setPadding(new Insets(0, 0, 12, 0));

        // ── PANEL IZQUIERDO: lista de médicos ──
        Label lblMedicos = etiqueta("1 · Elige un médico disponible");

        listaMedicos.setItems(itemsMedicos);
        listaMedicos.setPrefHeight(280);
        listaMedicos.setCellFactory(lv -> new MedicoCell());

        // Al seleccionar un médico se recalculan las franjas disponibles
        listaMedicos.getSelectionModel().selectedIndexProperty().addListener(
                (obs, oldIdx, newIdx) -> {
                    int i = newIdx.intValue();
                    if (i >= 0 && i < datosMedicos.size()) {
                        medicoSeleccionado = datosMedicos.get(i);
                        franjaSeleccionada = null;
                        actualizarResumen();
                        cargarFranjas();
                    }
                });

        Button btnRecargar = new Button("↺ Recargar");
        btnRecargar.setStyle(estiloBotonSecundario());
        btnRecargar.setOnAction(e -> cargarMedicos());

        VBox panelMedicos = panel(new VBox(8, lblMedicos, listaMedicos, btnRecargar));

        // ── PANEL DERECHO: fecha + franjas + motivo ──
        Label lblFecha = etiqueta("2 · Selecciona la fecha");

        dpFecha.setMaxWidth(Double.MAX_VALUE);
        // Solo se permiten fechas futuras (mínimo mañana)
        dpFecha.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now().plusDays(1)));
            }
        });
        // Al cambiar la fecha se recalculan las franjas
        dpFecha.valueProperty().addListener((obs, o, n) -> {
            franjaSeleccionada = null;
            actualizarResumen();
            cargarFranjas();
        });

        Label lblFranjas = etiqueta("3 · Selecciona una franja libre");

        panelFranjas.setHgap(8);
        panelFranjas.setVgap(8);
        panelFranjas.setPrefHeight(140);
        panelFranjas.getChildren().add(hintLabel("Seleccione un médico y una fecha"));

        // FIX 1: Leyenda corregida — el dot muestra el color del BOTÓN, no el fondo
        // libre → botón morado claro (#EDE9FE con borde #C084FC)
        // ocupado → botón gris (#9CA3AF)
        // seleccionado → botón morado oscuro (#7B2FBE)
        HBox leyenda = new HBox(12,
                chip("libre",        "#C084FC",  "#EDE9FE"),
                chip("ocupado",      "#9CA3AF",  "#F3F4F6"),
                chip("seleccionado", "#7B2FBE",  "#7B2FBE"));
        leyenda.setAlignment(Pos.CENTER_LEFT);

        txtMotivo.setPromptText("Motivo de la cita (opcional)");
        txtMotivo.setPrefHeight(70);
        txtMotivo.setMaxWidth(Double.MAX_VALUE);
        txtMotivo.setStyle(campoEstilo());

        VBox panelDerecho = panel(new VBox(10,
                lblFecha, dpFecha,
                lblFranjas, panelFranjas, leyenda,
                etiqueta("4 · Motivo (opcional)"), txtMotivo));

        // ── CONTENIDO PRINCIPAL ──
        HBox contenido = new HBox(16, panelMedicos, panelDerecho);
        HBox.setHgrow(panelDerecho, Priority.ALWAYS);

        // ── BARRA INFERIOR: resumen + confirmar ──
        lblResumen.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblResumen.setTextFill(Color.web("#4C1D95"));
        lblResumen.setWrapText(true);

        Button btnConfirmar = new Button("✓  Confirmar cita");
        btnConfirmar.setStyle(estiloBoton("#7B2FBE"));
        btnConfirmar.setPrefHeight(40);
        btnConfirmar.setOnAction(e -> confirmarCita());

        lblFeedback.setFont(Font.font("System", 13));
        lblFeedback.setWrapText(true);

        VBox bloqueResumen = new VBox(4, etiqueta("Resumen"), lblResumen);
        HBox barraInferior = new HBox(16, bloqueResumen, btnConfirmar);
        barraInferior.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(bloqueResumen, Priority.ALWAYS);
        barraInferior.setStyle("""
                -fx-background-color: white;
                -fx-border-color: #E5E7EB;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                """);
        barraInferior.setPadding(new Insets(14));

        // FIX 3: Sección "Mis citas programadas"
        // ── SECCIÓN MIS CITAS ──
        Label lblMisCitas = etiqueta("Mis citas programadas");
        lblMisCitas.setFont(Font.font("System", FontWeight.BOLD, 14));

        TableView<Agenda> tablaMisCitas = new TableView<>();

        TableColumn<Agenda, String> colMedico = new TableColumn<>("Médico");
        colMedico.setCellValueFactory(new PropertyValueFactory<>("medico"));
        colMedico.setPrefWidth(160);

        TableColumn<Agenda, String> colFechaHora = new TableColumn<>("Fecha y Hora");
        colFechaHora.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        colFechaHora.setPrefWidth(150);

        TableColumn<Agenda, String> colMotivo = new TableColumn<>("Motivo");
        colMotivo.setCellValueFactory(new PropertyValueFactory<>("motivo"));
        colMotivo.setPrefWidth(160);

        TableColumn<Agenda, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setPrefWidth(110);

        tablaMisCitas.getColumns().addAll(colMedico, colFechaHora, colMotivo, colEstado);
        tablaMisCitas.setPrefHeight(160);
        tablaMisCitas.setPlaceholder(new Label("No tienes citas programadas"));

// ── PANEL ACCIONES SOBRE CITA SELECCIONADA ──
        Label lblAcciones = etiqueta("Cita seleccionada:");
        lblAcciones.setVisible(false);

        Label lblCitaSeleccionada = new Label("");
        lblCitaSeleccionada.setFont(Font.font("System", 12));
        lblCitaSeleccionada.setTextFill(Color.web("#4C1D95"));
        lblCitaSeleccionada.setVisible(false);

        DatePicker dpNuevaFecha = new DatePicker();
        dpNuevaFecha.setPromptText("Nueva fecha");
        dpNuevaFecha.setVisible(false);
        dpNuevaFecha.setManaged(false);
        dpNuevaFecha.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now().plusDays(1)));
            }
        });

        ComboBox<String> cbNuevaHora = new ComboBox<>();
        for (int h = 8; h <= 16; h++) {
            cbNuevaHora.getItems().addAll(
                    String.format("%02d:00", h),
                    String.format("%02d:30", h));
        }
        cbNuevaHora.setPromptText("Nueva hora");
        cbNuevaHora.setVisible(false);
        cbNuevaHora.setManaged(false);

        Button btnCancelarCita = new Button("✗  Cancelar esta cita");
        btnCancelarCita.setStyle(estiloBoton("#C62828"));
        btnCancelarCita.setVisible(false);
        btnCancelarCita.setManaged(false);

        Button btnReagendarCita = new Button("↺  Reagendar esta cita");
        btnReagendarCita.setStyle(estiloBoton("#7B2FBE"));
        btnReagendarCita.setVisible(false);
        btnReagendarCita.setManaged(false);

        Label lblFeedbackCitas = new Label();
        lblFeedbackCitas.setFont(Font.font("System", 12));
        lblFeedbackCitas.setWrapText(true);

// Al seleccionar una cita mostrar las acciones
        tablaMisCitas.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, seleccionada) -> {
                    if (seleccionada != null) {
                        String estado = seleccionada.getEstado();
                        boolean cancelable = !estado.equals("CANCELADA")
                                && !estado.equals("COMPLETADA");

                        lblAcciones.setVisible(true);
                        lblCitaSeleccionada.setVisible(true);
                        lblCitaSeleccionada.setText(
                                seleccionada.getMedico() + "  ·  "
                                        + seleccionada.getFechaHora() + "  ·  "
                                        + estado);

                        btnCancelarCita.setVisible(cancelable);
                        btnCancelarCita.setManaged(cancelable);
                        btnReagendarCita.setVisible(cancelable);
                        btnReagendarCita.setManaged(cancelable);
                        dpNuevaFecha.setVisible(cancelable);
                        dpNuevaFecha.setManaged(cancelable);
                        cbNuevaHora.setVisible(cancelable);
                        cbNuevaHora.setManaged(cancelable);
                    }
                });

// Cancelar cita seleccionada
        btnCancelarCita.setOnAction(e -> {
            Agenda seleccionada = tablaMisCitas.getSelectionModel().getSelectedItem();
            if (seleccionada == null) return;

            new Thread(() -> {
                try {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/api/citas/"
                                    + seleccionada.getId() + "/cancelar"))
                            .method("PATCH", HttpRequest.BodyPublishers.noBody())
                            .build();
                    HttpResponse<String> resp =
                            http.send(req, HttpResponse.BodyHandlers.ofString());
                    Platform.runLater(() -> {
                        if (resp.statusCode() == 200) {
                            lblFeedbackCitas.setText("✓ Cita cancelada correctamente");
                            lblFeedbackCitas.setTextFill(Color.web("#059669"));
                            cargarMisCitas(tablaMisCitas);
                            ocultarAcciones(lblAcciones, lblCitaSeleccionada,
                                    btnCancelarCita, btnReagendarCita,
                                    dpNuevaFecha, cbNuevaHora);
                        } else {
                            lblFeedbackCitas.setText("✗ No se pudo cancelar la cita");
                            lblFeedbackCitas.setTextFill(Color.web("#DC2626"));
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        lblFeedbackCitas.setText("✗ Error de conexión");
                        lblFeedbackCitas.setTextFill(Color.web("#DC2626"));
                    });
                }
            }).start();
        });

// Reagendar cita seleccionada
        btnReagendarCita.setOnAction(e -> {
            Agenda seleccionada = tablaMisCitas.getSelectionModel().getSelectedItem();
            if (seleccionada == null) return;
            if (dpNuevaFecha.getValue() == null || cbNuevaHora.getValue() == null) {
                lblFeedbackCitas.setText("✗ Seleccione nueva fecha y hora");
                lblFeedbackCitas.setTextFill(Color.web("#DC2626"));
                return;
            }

            String nuevaFechaHora = dpNuevaFecha.getValue().toString()
                    + "T" + cbNuevaHora.getValue();

            new Thread(() -> {
                try {
                    String json = "{\"fechaHora\": \"" + nuevaFechaHora + "\"}";
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/api/citas/"
                                    + seleccionada.getId() + "/reagendar"))
                            .header("Content-Type", "application/json")
                            .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                            .build();
                    HttpResponse<String> resp =
                            http.send(req, HttpResponse.BodyHandlers.ofString());
                    Platform.runLater(() -> {
                        if (resp.statusCode() == 200) {
                            lblFeedbackCitas.setText("✓ Cita reagendada correctamente");
                            lblFeedbackCitas.setTextFill(Color.web("#059669"));
                            cargarMisCitas(tablaMisCitas);
                            ocultarAcciones(lblAcciones, lblCitaSeleccionada,
                                    btnCancelarCita, btnReagendarCita,
                                    dpNuevaFecha, cbNuevaHora);
                        } else {
                            lblFeedbackCitas.setText("✗ No se pudo reagendar: " + resp.body());
                            lblFeedbackCitas.setTextFill(Color.web("#DC2626"));
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        lblFeedbackCitas.setText("✗ Error de conexión");
                        lblFeedbackCitas.setTextFill(Color.web("#DC2626"));
                    });
                }
            }).start();
        });

        HBox botonesAccion = new HBox(10, btnCancelarCita, btnReagendarCita);

        Button btnVerMisCitas = new Button("↺ Actualizar mis citas");
        btnVerMisCitas.setStyle(estiloBotonSecundario());
        btnVerMisCitas.setOnAction(e -> cargarMisCitas(tablaMisCitas));

        VBox seccionMisCitas = panel(new VBox(10,
                lblMisCitas, tablaMisCitas, btnVerMisCitas,
                lblAcciones, lblCitaSeleccionada,
                new HBox(10, dpNuevaFecha, cbNuevaHora),
                botonesAccion, lblFeedbackCitas));

        // ── RAÍZ ──
        VBox root = new VBox(12,
                encabezado, contenido, barraInferior, lblFeedback, seccionMisCitas);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #F5F3FF;");

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F5F3FF; -fx-background-color: #F5F3FF;");

        stage.setTitle("Agendar cita - Piedra Azul");
        stage.setScene(new Scene(scroll, 900, 720));
        stage.show();

        cargarMedicos();
        cargarMisCitas(tablaMisCitas);
    }

    // -- Carga de médicos disponibles desde ms-agenda --
    private void cargarMedicos() {
        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/medicos/disponibles"))
                        .GET().build();
                HttpResponse<String> resp =
                        http.send(req, HttpResponse.BodyHandlers.ofString());

                List<Map<String, Object>> lista = mapper.readValue(
                        resp.body(), new TypeReference<>() {});

                Platform.runLater(() -> {
                    datosMedicos.clear();
                    itemsMedicos.clear();
                    datosMedicos.addAll(lista);
                    lista.forEach(m -> itemsMedicos.add(
                            m.get("nombre") + " " + m.get("apellido")
                                    + " · " + m.getOrDefault("especialidad", "")));
                    if (lista.isEmpty())
                        feedback("No hay médicos disponibles en este momento.", true);
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        feedback("No se pudo conectar con el servidor.", true));
            }
        }).start();
    }

    // FIX 3: Carga las citas del paciente actual desde ms-agenda
    private void cargarMisCitas(TableView<Agenda> tabla) {
        if (pacienteId == null) return;
        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/citas/paciente/"
                                + pacienteId))
                        .GET().build();
                HttpResponse<String> resp =
                        http.send(req, HttpResponse.BodyHandlers.ofString());

                List<Map<String, Object>> lista = mapper.readValue(
                        resp.body(), new TypeReference<>() {});

                Platform.runLater(() -> {
                    ObservableList<Agenda> items = FXCollections.observableArrayList(
                            lista.stream().map(m -> {
                                Agenda a = new Agenda();
                                a.setId(m.get("id") != null
                                        ? Long.parseLong(m.get("id").toString()) : 0L);
                                a.setPacienteId(pacienteId);
                                if (m.get("medico") instanceof Map<?, ?> med) {
                                    a.setMedico(med.get("nombre") + " " + med.get("apellido"));
                                    a.setMedicoId(Long.parseLong(med.get("id").toString()));
                                }
                                a.setFechaHora(m.get("fechaHora") != null
                                        ? m.get("fechaHora").toString() : "");
                                a.setMotivo(m.get("motivo") != null
                                        ? m.get("motivo").toString() : "");
                                a.setEstado(m.get("estado") != null
                                        ? m.get("estado").toString() : "");
                                return a;
                            }).toList());
                    tabla.setItems(items);
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        feedback("No se pudieron cargar tus citas.", true));
            }
        }).start();
    }

    // -- Calcula las franjas libres cruzando la franja del médico con sus citas --
    private void cargarFranjas() {
        if (medicoSeleccionado == null || dpFecha.getValue() == null) return;

        Long medicoId   = Long.parseLong(medicoSeleccionado.get("id").toString());
        LocalDate fecha = dpFecha.getValue();

        // Leer configuración del médico (con fallback a valores por defecto)
        String inicioStr = medicoSeleccionado.getOrDefault("franjaInicio", "08:00").toString();
        String finStr    = medicoSeleccionado.getOrDefault("franjaFin",    "17:00").toString();
        int intervalo    = Integer.parseInt(
                medicoSeleccionado.getOrDefault("intervaloCitas", "30").toString());

        panelFranjas.getChildren().setAll(hintLabel("Cargando franjas..."));

        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/citas/medico/" + medicoId))
                        .GET().build();
                HttpResponse<String> resp =
                        http.send(req, HttpResponse.BodyHandlers.ofString());

                List<Map<String, Object>> citas = mapper.readValue(
                        resp.body(), new TypeReference<>() {});

                // Horarios ya ocupados en la fecha seleccionada (formato HH:mm)
                Set<String> ocupadas = citas.stream()
                        .filter(c -> {
                            String fh = c.getOrDefault("fechaHora", "").toString();
                            return fh.startsWith(fecha.toString());
                        })
                        .map(c -> c.get("fechaHora").toString().substring(11, 16))
                        .collect(Collectors.toSet());

                // Generar todos los slots dentro de la franja del médico
                LocalTime ini = LocalTime.parse(inicioStr);
                LocalTime fin = LocalTime.parse(finStr);
                List<LocalTime> slots = new ArrayList<>();
                LocalTime cursor = ini;
                while (!cursor.isAfter(fin.minusMinutes(intervalo))) {
                    slots.add(cursor);
                    cursor = cursor.plusMinutes(intervalo);
                }

                Platform.runLater(() -> {
                    panelFranjas.getChildren().clear();
                    if (slots.isEmpty()) {
                        panelFranjas.getChildren().add(
                                hintLabel("Sin franjas configuradas para este médico."));
                        return;
                    }
                    DateTimeFormatter hf = DateTimeFormatter.ofPattern("HH:mm");
                    for (LocalTime slot : slots) {
                        String hora   = slot.format(hf);
                        boolean libre = !ocupadas.contains(hora);
                        panelFranjas.getChildren().add(buildSlotButton(hora, libre, fecha));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        panelFranjas.getChildren().setAll(
                                hintLabel("Error al cargar franjas.")));
            }
        }).start();
    }

    // -- Construye el botón de una franja horaria (libre u ocupado) --
    private Button buildSlotButton(String hora, boolean libre, LocalDate fecha) {
        Button btn = new Button(hora);
        btn.setPrefWidth(72);
        btn.setPrefHeight(36);
        btn.setFont(Font.font("System", 13));

        if (!libre) {
            // Slot ocupado: deshabilitado y tachado visualmente
            btn.setStyle("""
                    -fx-background-color: #F3F4F6;
                    -fx-text-fill: #9CA3AF;
                    -fx-background-radius: 6;
                    -fx-cursor: default;
                    """);
            btn.setDisable(true);
            btn.setTooltip(new Tooltip("Horario ocupado"));
        } else {
            btn.setStyle(estiloSlotLibre());
            btn.setOnAction(e -> {
                // Deseleccionar todos los slots libres
                panelFranjas.getChildren().stream()
                        .filter(n -> n instanceof Button)
                        .map(n -> (Button) n)
                        .filter(b -> !b.isDisabled())
                        .forEach(b -> b.setStyle(estiloSlotLibre()));

                // Marcar este slot como seleccionado
                btn.setStyle(estiloSlotSeleccionado());

                franjaSeleccionada = LocalDateTime.of(fecha, LocalTime.parse(hora));
                actualizarResumen();
            });
        }
        return btn;
    }

    // -- Confirmar cita: POST /api/citas con fechaHoraManual --
    private void confirmarCita() {
        if (medicoSeleccionado == null) {
            feedback("Seleccione un médico.", true); return;
        }
        if (franjaSeleccionada == null) {
            feedback("Seleccione una franja horaria.", true); return;
        }
        // FIX 2: pacienteId viene del constructor — ya no puede ser null en uso normal
        if (pacienteId == null) {
            feedback("Error interno: sesión sin pacienteId. Cierre y vuelva a iniciar sesión.", true);
            return;
        }

        Long   medicoId = Long.parseLong(medicoSeleccionado.get("id").toString());
        String motivo   = txtMotivo.getText().isBlank()
                ? "Consulta general" : txtMotivo.getText().trim();

        String json = """
                {
                  "pacienteId": %d,
                  "medicoId": %d,
                  "motivo": "%s",
                  "fechaHoraManual": "%s"
                }
                """.formatted(
                pacienteId,
                medicoId,
                motivo.replace("\"", "'"),
                franjaSeleccionada.toString());

        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/citas"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                HttpResponse<String> resp =
                        http.send(req, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (resp.statusCode() == 201) {
                        feedback("✓ Cita confirmada: " + lblResumen.getText(), false);
                        // Resetear selección para permitir agendar otra cita
                        franjaSeleccionada = null;
                        medicoSeleccionado = null;
                        listaMedicos.getSelectionModel().clearSelection();
                        panelFranjas.getChildren().setAll(
                                hintLabel("Seleccione un médico y una fecha"));
                        txtMotivo.clear();
                        actualizarResumen();
                    } else {
                        feedback("✗ Error al agendar: " + resp.body(), true);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        feedback("✗ No se pudo conectar: " + e.getMessage(), true));
            }
        }).start();
    }

    // -- Actualiza el texto de resumen con la selección actual --
    private void actualizarResumen() {
        if (medicoSeleccionado == null) {
            lblResumen.setText("Seleccione médico y franja");
            return;
        }
        String nombre = medicoSeleccionado.get("nombre")
                + " " + medicoSeleccionado.get("apellido");
        String fecha  = dpFecha.getValue() != null
                ? dpFecha.getValue().toString() : "—";
        String hora   = franjaSeleccionada != null
                ? franjaSeleccionada.toLocalTime().toString() : "—";
        lblResumen.setText("Dr/a. " + nombre + "  ·  " + fecha + "  ·  " + hora);
    }

    private void feedback(String msg, boolean error) {
        lblFeedback.setText(msg);
        lblFeedback.setTextFill(error
                ? Color.web("#DC2626") : Color.web("#059669"));
    }

    private Label hintLabel(String texto) {
        Label l = new Label(texto);
        l.setFont(Font.font("System", 12));
        l.setTextFill(Color.web("#6B7280"));
        return l;
    }

    private Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.setFont(Font.font("System", FontWeight.BOLD, 12));
        l.setTextFill(Color.web("#4C1D95"));
        return l;
    }

    private VBox panel(javafx.scene.Node contenido) {
        VBox box = new VBox(8, contenido);
        box.setPadding(new Insets(14));
        box.setStyle("""
                -fx-background-color: white;
                -fx-border-color: #E5E7EB;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                """);
        return box;
    }

    // FIX 1: chip corregido — colorBorde es el borde del dot, colorFondo es su relleno
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

    private String estiloBoton(String color) {
        return "-fx-background-color: " + color + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 6;"
                + "-fx-cursor: hand;"
                + "-fx-padding: 8 20;";
    }

    private String estiloBotonSecundario() {
        return """
                -fx-background-color: transparent;
                -fx-text-fill: #7B2FBE;
                -fx-border-color: #7B2FBE;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                -fx-cursor: hand;
                -fx-padding: 6 12;
                """;
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

    // ══════════════════════════════════════════════════════
    // Celda personalizada para la lista de médicos.
    // Muestra nombre completo y especialidad en dos líneas.
    // ══════════════════════════════════════════════════════
    private static class MedicoCell extends ListCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                String[] partes = item.split(" · ");
                Label nombre = new Label(partes[0]);
                nombre.setFont(Font.font("System", FontWeight.BOLD, 13));
                nombre.setTextFill(Color.web("#1F2937"));

                Label esp = new Label(partes.length > 1 ? partes[1] : "");
                esp.setFont(Font.font("System", 11));
                esp.setTextFill(Color.web("#7B2FBE"));

                VBox box = new VBox(2, nombre, esp);
                box.setPadding(new Insets(4, 0, 4, 0));
                setGraphic(box);
                setText(null);
            }
        }

    }

    private void ocultarAcciones(Label lblAcciones, Label lblCita,
                                 Button btnCancelar, Button btnReagendar,
                                 DatePicker dp, ComboBox<String> cb) {
        lblAcciones.setVisible(false);
        lblCita.setVisible(false);
        btnCancelar.setVisible(false);
        btnCancelar.setManaged(false);
        btnReagendar.setVisible(false);
        btnReagendar.setManaged(false);
        dp.setVisible(false);
        dp.setManaged(false);
        cb.setVisible(false);
        cb.setManaged(false);
        dp.setValue(null);
        cb.setValue(null);
    }

    public static void main(String[] args) {
        launch();
    }
}