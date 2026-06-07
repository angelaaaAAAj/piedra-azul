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
import javafx.stage.Modality;
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

public class AgendaPacienteApp extends Application {

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private Long pacienteId;
    private Map<String, Object> medicoSeleccionado = null;
    private LocalDateTime franjaSeleccionada = null;

    private final ListView<String> listaMedicos = new ListView<>();
    private final ObservableList<String> itemsMedicos = FXCollections.observableArrayList();
    private final List<Map<String, Object>> datosMedicos = new ArrayList<>();

    private final DatePicker dpFecha = new DatePicker(LocalDate.now().plusDays(1));
    private final FlowPane panelFranjas = new FlowPane();
    private final TextArea txtMotivo = new TextArea();
    private final Label lblResumen = new Label("Seleccione médico y franja");
    private final Label lblFeedback = new Label();

    public AgendaPacienteApp(Long pacienteId) {
        this.pacienteId = pacienteId;
    }

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

        // ── LISTA DE MÉDICOS ──
        Label lblMedicos = etiqueta("1 · Elige un médico disponible");
        listaMedicos.setItems(itemsMedicos);
        listaMedicos.setPrefHeight(280);
        listaMedicos.setCellFactory(lv -> new MedicoCell());
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

        // ── FECHA Y FRANJAS ──
        Label lblFecha = etiqueta("2 · Selecciona la fecha");
        dpFecha.setMaxWidth(Double.MAX_VALUE);
        dpFecha.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now().plusDays(1)));
            }
        });
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

        HBox leyenda = new HBox(12,
                chip("libre", "#C084FC", "#EDE9FE"),
                chip("ocupado", "#9CA3AF", "#F3F4F6"),
                chip("seleccionado", "#7B2FBE", "#7B2FBE"));
        leyenda.setAlignment(Pos.CENTER_LEFT);

        txtMotivo.setPromptText("Motivo de la cita (opcional)");
        txtMotivo.setPrefHeight(70);
        txtMotivo.setMaxWidth(Double.MAX_VALUE);
        txtMotivo.setStyle(campoEstilo());

        VBox panelDerecho = panel(new VBox(10,
                lblFecha, dpFecha,
                lblFranjas, panelFranjas, leyenda,
                etiqueta("4 · Motivo (opcional)"), txtMotivo));

        HBox contenido = new HBox(16, panelMedicos, panelDerecho);
        HBox.setHgrow(panelDerecho, Priority.ALWAYS);

        // ── BARRA INFERIOR ──
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

        // ── SECCIÓN MIS CITAS con botones por fila ──
        Label lblMisCitas = etiqueta("Mis citas");
        lblMisCitas.setFont(Font.font("System", FontWeight.BOLD, 14));

        Label lblFeedbackCitas = new Label();
        lblFeedbackCitas.setFont(Font.font("System", 12));
        lblFeedbackCitas.setWrapText(true);

        TableView<Agenda> tablaMisCitas = new TableView<>();

        TableColumn<Agenda, String> colMedico = new TableColumn<>("Médico");
        colMedico.setCellValueFactory(new PropertyValueFactory<>("medico"));
        colMedico.setPrefWidth(150);

        TableColumn<Agenda, String> colFechaHora = new TableColumn<>("Fecha y Hora");
        colFechaHora.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        colFechaHora.setPrefWidth(140);

        TableColumn<Agenda, String> colMotivoCita = new TableColumn<>("Motivo");
        colMotivoCita.setCellValueFactory(new PropertyValueFactory<>("motivo"));
        colMotivoCita.setPrefWidth(130);

        TableColumn<Agenda, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setPrefWidth(100);

        // Columna de acciones con botones Cancelar y Reagendar por fila
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
                    cancelarCita(cita, tablaMisCitas, lblFeedbackCitas);
                });

                btnReagendar.setOnAction(e -> {
                    Agenda cita = getTableView().getItems().get(getIndex());
                    abrirVentanaReagendar(cita, tablaMisCitas, lblFeedbackCitas);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Agenda cita = getTableView().getItems().get(getIndex());
                    boolean activa = !"CANCELADA".equals(cita.getEstado())
                            && !"COMPLETADA".equals(cita.getEstado());
                    btnCancelar.setDisable(!activa);
                    btnReagendar.setDisable(!activa);
                    setGraphic(caja);
                }
            }
        });

        tablaMisCitas.getColumns().addAll(
                colMedico, colFechaHora, colMotivoCita, colEstado, colAcciones);
        tablaMisCitas.setPrefHeight(200);
        tablaMisCitas.setPlaceholder(new Label("No tienes citas programadas"));

        Button btnVerMisCitas = new Button("↺ Actualizar mis citas");
        btnVerMisCitas.setStyle(estiloBotonSecundario());
        btnVerMisCitas.setOnAction(e -> cargarMisCitas(tablaMisCitas));

        VBox seccionMisCitas = panel(new VBox(10,
                lblMisCitas,
                tablaMisCitas,
                btnVerMisCitas,
                lblFeedbackCitas));

        // ── RAÍZ ──
        VBox root = new VBox(12,
                encabezado, contenido, barraInferior, lblFeedback, seccionMisCitas);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #F5F3FF;");

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F5F3FF; -fx-background-color: #F5F3FF;");

        stage.setTitle("Agendar cita - Piedra Azul");
        stage.setScene(new Scene(scroll, 950, 720));
        stage.show();

        cargarMedicos();
        cargarMisCitas(tablaMisCitas);
    }

    // ── Cancelar cita directamente desde la fila ──
    private void cancelarCita(Agenda cita,
                              TableView<Agenda> tabla,
                              Label lblFeedback) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Seguro que deseas cancelar la cita con "
                        + cita.getMedico() + " el " + cita.getFechaHora() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmar cancelación");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                new Thread(() -> {
                    try {
                        String token1 = com.piedraazul.ui.app.PiedraAzulApp.getToken();
                        HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create("http://localhost:8080/api/citas/"
                                        + cita.getId() + "/cancelar"))
                                .header("Authorization", "Bearer " + (token1 != null ? token1 : ""))
                                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                                .build();
                        HttpResponse<String> resp =
                                http.send(req, HttpResponse.BodyHandlers.ofString());
                        Platform.runLater(() -> {
                            if (resp.statusCode() == 200) {
                                lblFeedback.setText("✓ Cita cancelada correctamente");
                                lblFeedback.setTextFill(Color.web("#059669"));
                                cargarMisCitas(tabla);
                            } else {
                                lblFeedback.setText("✗ No se pudo cancelar la cita");
                                lblFeedback.setTextFill(Color.web("#DC2626"));
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            lblFeedback.setText("✗ Error de conexión");
                            lblFeedback.setTextFill(Color.web("#DC2626"));
                        });
                    }
                }).start();
            }
        });
    }

    // ── Ventana de reagendamiento: mismo flujo que agendar pero para una cita existente ──
    private void abrirVentanaReagendar(Agenda cita,
                                       TableView<Agenda> tabla,
                                       Label lblFeedbackPadre) {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Reagendar cita - " + cita.getMedico());

        // ── Info de la cita actual ──
        Label lblInfo = new Label(
                "Cita actual:  " + cita.getMedico()
                        + "  ·  " + cita.getFechaHora());
        lblInfo.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblInfo.setTextFill(Color.web("#4C1D95"));

        // ── Selector de nueva fecha ──
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

        // ── Panel de franjas ──
        Label lblFranjas = etiqueta("Selecciona una franja disponible:");
        FlowPane franjas = new FlowPane();
        franjas.setHgap(8);
        franjas.setVgap(8);
        franjas.setPrefHeight(130);
        franjas.getChildren().add(hintLabel("Selecciona una fecha para ver las franjas"));

        // Leyenda igual que en agendar
        HBox leyenda = new HBox(12,
                chip("libre", "#C084FC", "#EDE9FE"),
                chip("ocupado", "#9CA3AF", "#F3F4F6"),
                chip("seleccionado", "#7B2FBE", "#7B2FBE"));
        leyenda.setAlignment(Pos.CENTER_LEFT);

        // Hora seleccionada (se actualiza al hacer clic en una franja)
        final String[] horaSeleccionada = {null};

        // Al cambiar fecha, cargar franjas del médico de esa cita
        dpNuevaFecha.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            horaSeleccionada[0] = null;
            franjas.getChildren().setAll(hintLabel("Cargando franjas..."));

            new Thread(() -> {
                try {
                    // Configuración del médico
                    String token2 = com.piedraazul.ui.app.PiedraAzulApp.getToken();
                    HttpRequest reqMedico = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/api/medicos/" + cita.getMedicoId()))
                            .header("Authorization", "Bearer " + (token2 != null ? token2 : ""))
                            .GET().build();
                    HttpResponse<String> respMedico =
                            http.send(reqMedico, HttpResponse.BodyHandlers.ofString());
                    Map<String, Object> medico = mapper.readValue(
                            respMedico.body(), new TypeReference<>() {});

                    String inicioStr = medico.getOrDefault("franjaInicio", "08:00").toString();
                    String finStr    = medico.getOrDefault("franjaFin",    "17:00").toString();
                    int intervalo    = Integer.parseInt(
                            medico.getOrDefault("intervaloCitas", "30").toString());

                    // Citas ya ocupadas ese día (excluyendo la que se está reagendando)
                    HttpRequest reqCitas = autenticado("http://localhost:8080/api/citas/medico/" + cita.getMedicoId())
                            .GET().build();
                    HttpResponse<String> respCitas =
                            http.send(reqCitas, HttpResponse.BodyHandlers.ofString());
                    List<Map<String, Object>> citasMedico = mapper.readValue(
                            respCitas.body(), new TypeReference<>() {});

                    Set<String> ocupadas = citasMedico.stream()
                            .filter(c -> {
                                String fh = c.getOrDefault("fechaHora", "").toString();
                                // Excluir la propia cita que se reagenda
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
                            String hora   = slot.format(hf);
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
                                btn.setOnAction(e -> {
                                    // Deseleccionar otros
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
                    });
                } catch (Exception ex) {
                    Platform.runLater(() ->
                            franjas.getChildren().setAll(
                                    hintLabel("Error al cargar franjas")));
                }
            }).start();
        });

        // ── Botones de la ventana ──
        Label lblFeedbackVentana = new Label();
        lblFeedbackVentana.setFont(Font.font("System", 12));
        lblFeedbackVentana.setWrapText(true);

        Button btnConfirmarReagendar = new Button("✓  Confirmar reagendamiento");
        btnConfirmarReagendar.setStyle(estiloBoton("#7B2FBE"));
        btnConfirmarReagendar.setPrefHeight(40);
        btnConfirmarReagendar.setOnAction(e -> {
            if (dpNuevaFecha.getValue() == null || horaSeleccionada[0] == null) {
                lblFeedbackVentana.setText("✗ Selecciona fecha y franja horaria");
                lblFeedbackVentana.setTextFill(Color.web("#DC2626"));
                return;
            }
            String nuevaFechaHora = dpNuevaFecha.getValue().toString()
                    + "T" + horaSeleccionada[0];
            String json = "{\"fechaHora\": \"" + nuevaFechaHora + "\"}";

            new Thread(() -> {
                try {
                    String token = com.piedraazul.ui.app.PiedraAzulApp.getToken();
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/api/citas/"
                                    + cita.getId() + "/reagendar"))
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer " + (token != null ? token : ""))
                            .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                            .build();
                    HttpResponse<String> resp =
                            http.send(req, HttpResponse.BodyHandlers.ofString());
                    Platform.runLater(() -> {
                        if (resp.statusCode() == 200) {
                            lblFeedbackPadre.setText("✓ Cita reagendada correctamente");
                            lblFeedbackPadre.setTextFill(Color.web("#059669"));
                            cargarMisCitas(tabla);
                            ventana.close();
                        } else {
                            lblFeedbackVentana.setText("✗ No se pudo reagendar: "
                                    + resp.body());
                            lblFeedbackVentana.setTextFill(Color.web("#DC2626"));
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        lblFeedbackVentana.setText("✗ Error de conexión");
                        lblFeedbackVentana.setTextFill(Color.web("#DC2626"));
                    });
                }
            }).start();
        });

        Button btnCerrar = new Button("Cancelar");
        btnCerrar.setStyle(estiloBotonSecundario());
        btnCerrar.setOnAction(e -> ventana.close());

        HBox botones = new HBox(10, btnConfirmarReagendar, btnCerrar);
        botones.setAlignment(Pos.CENTER_RIGHT);

        VBox layoutVentana = new VBox(12,
                lblInfo, lblFecha, dpNuevaFecha,
                lblFranjas, franjas, leyenda,
                botones, lblFeedbackVentana);
        layoutVentana.setPadding(new Insets(20));
        layoutVentana.setStyle("-fx-background-color: #F5F3FF;");

        ventana.setScene(new Scene(layoutVentana, 700, 450));
        ventana.show();
    }

    private void cargarMedicos() {
        new Thread(() -> {
            try {
                HttpRequest req = autenticado("http://localhost:8080/api/medicos/disponibles")
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

    private void cargarMisCitas(TableView<Agenda> tabla) {
        if (pacienteId == null) return;
        new Thread(() -> {
            try {
                HttpRequest req = autenticado("http://localhost:8080/api/citas/paciente/" + pacienteId)
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
                                    a.setMedico(med.get("nombre") + " "
                                            + med.get("apellido"));
                                    a.setMedicoId(Long.parseLong(
                                            med.get("id").toString()));
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

    private void cargarFranjas() {
        if (medicoSeleccionado == null || dpFecha.getValue() == null) return;
        Long medicoId = Long.parseLong(medicoSeleccionado.get("id").toString());
        LocalDate fecha = dpFecha.getValue();
        String inicioStr = medicoSeleccionado.getOrDefault("franjaInicio", "08:00").toString();
        String finStr    = medicoSeleccionado.getOrDefault("franjaFin",    "17:00").toString();
        int intervalo    = Integer.parseInt(
                medicoSeleccionado.getOrDefault("intervaloCitas", "30").toString());

        panelFranjas.getChildren().setAll(hintLabel("Cargando franjas..."));

        new Thread(() -> {
            try {
                HttpRequest req = autenticado("http://localhost:8080/api/citas/medico/" + medicoId)
                        .GET().build();
                HttpResponse<String> resp =
                        http.send(req, HttpResponse.BodyHandlers.ofString());
                List<Map<String, Object>> citas = mapper.readValue(
                        resp.body(), new TypeReference<>() {});

                Set<String> ocupadas = citas.stream()
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

    private Button buildSlotButton(String hora, boolean libre, LocalDate fecha) {
        Button btn = new Button(hora);
        btn.setPrefWidth(72);
        btn.setPrefHeight(36);
        btn.setFont(Font.font("System", 13));
        if (!libre) {
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
                panelFranjas.getChildren().stream()
                        .filter(n -> n instanceof Button)
                        .map(n -> (Button) n)
                        .filter(b -> !b.isDisabled())
                        .forEach(b -> b.setStyle(estiloSlotLibre()));
                btn.setStyle(estiloSlotSeleccionado());
                franjaSeleccionada = LocalDateTime.of(fecha, LocalTime.parse(hora));
                actualizarResumen();
            });
        }
        return btn;
    }

    private void confirmarCita() {
        if (medicoSeleccionado == null) {
            feedback("Seleccione un médico.", true); return;
        }
        if (franjaSeleccionada == null) {
            feedback("Seleccione una franja horaria.", true); return;
        }
        if (pacienteId == null) {
            feedback("Error interno: sesión sin pacienteId.", true); return;
        }
        Long medicoId = Long.parseLong(medicoSeleccionado.get("id").toString());
        String motivo = txtMotivo.getText().isBlank()
                ? "Consulta general" : txtMotivo.getText().trim();
        String json = """
                {
                  "pacienteId": %d,
                  "medicoId": %d,
                  "motivo": "%s",
                  "fechaHoraManual": "%s"
                }
                """.formatted(pacienteId, medicoId,
                motivo.replace("\"", "'"), franjaSeleccionada.toString());

        new Thread(() -> {
            try {
                HttpRequest req = autenticado("http://localhost:8080/api/citas")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                HttpResponse<String> resp =
                        http.send(req, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> {
                    if (resp.statusCode() == 201) {
                        feedback("✓ Cita confirmada: " + lblResumen.getText(), false);
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

    private void actualizarResumen() {
        if (medicoSeleccionado == null) {
            lblResumen.setText("Seleccione médico y franja");
            return;
        }
        String nombre = medicoSeleccionado.get("nombre")
                + " " + medicoSeleccionado.get("apellido");
        String fecha = dpFecha.getValue() != null
                ? dpFecha.getValue().toString() : "—";
        String hora = franjaSeleccionada != null
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

    // Botones pequeños para usar dentro de las celdas de la tabla
    private String estiloBotonTabla(String color) {
        return "-fx-background-color: " + color + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 11px;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 5;"
                + "-fx-cursor: hand;"
                + "-fx-padding: 4 8;";
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

    private HttpRequest.Builder autenticado(String url) {
        String token = com.piedraazul.ui.app.PiedraAzulApp.getToken();
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url));
        if (token != null) b.header("Authorization", "Bearer " + token);
        return b;
    }

    public static void main(String[] args) {
        launch();
    }
}