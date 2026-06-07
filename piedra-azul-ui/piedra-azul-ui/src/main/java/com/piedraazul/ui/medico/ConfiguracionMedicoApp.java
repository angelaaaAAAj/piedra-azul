package com.piedraazul.ui.medico;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

// Ventana de configuración de disponibilidad del médico.
// Acceso: desde PiedraAzulApp → menú MEDICO_TERAPISTA.
// Permite configurar: días de atención, franja horaria,
// intervalo entre citas y ventana de agendamiento.
// Endpoint destino: PATCH /api/medicos/{id}/configuracion
public class ConfiguracionMedicoApp extends Application {

    // -- Colores de la paleta Piedra Azul --
    private static final String COLOR_PRIMARIO    = "#7B2FBE";
    private static final String COLOR_OSCURO      = "#4C1D95";
    private static final String COLOR_FONDO       = "#F5F3FF";
    private static final String COLOR_BLANCO      = "#FFFFFF";
    private static final String COLOR_BORDE       = "#C084FC";
    private static final String COLOR_EXITO       = "#059669";
    private static final String COLOR_ERROR       = "#DC2626";
    private static final String COLOR_TEXTO_MUTED = "#6B7280";

    // -- ID del médico autenticado (se inyecta desde el menú principal) --
    private Long medicoId;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper   = new ObjectMapper();

    // -- Controles de días --
    private final CheckBox chkLunes     = new CheckBox("Lunes");
    private final CheckBox chkMartes    = new CheckBox("Martes");
    private final CheckBox chkMiercoles = new CheckBox("Miércoles");
    private final CheckBox chkJueves    = new CheckBox("Jueves");
    private final CheckBox chkViernes   = new CheckBox("Viernes");
    private final CheckBox chkSabado    = new CheckBox("Sábado");
    private final CheckBox chkDomingo   = new CheckBox("Domingo");

    // -- Controles de franja y configuración --
    private final ComboBox<String> cbFranjaInicio  = new ComboBox<>();
    private final ComboBox<String> cbFranjaFin     = new ComboBox<>();
    private final ComboBox<String> cbIntervalo     = new ComboBox<>();
    private final Slider           sldVentana      = new Slider(1, 12, 4);
    private final Label            lblVentanaValor = new Label("4 semanas");

    private final Label lblFeedback = new Label();

    // Constructor sin args requerido por Application.launch()
    public ConfiguracionMedicoApp() {
        this.medicoId = null;
    }

    // Constructor con medicoId (para lanzar desde el menú)
    public ConfiguracionMedicoApp(Long medicoId) {
        this.medicoId = medicoId;
    }

    @Override
    public void start(Stage stage) {
        poblarCombos();

        // ── ENCABEZADO ──
        Label lblTitulo = new Label("Configuración de Disponibilidad");
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 20));
        lblTitulo.setTextFill(Color.web(COLOR_OSCURO));

        Label lblSub = new Label("Defina su horario y ventana de agendamiento");
        lblSub.setFont(Font.font("System", 13));
        lblSub.setTextFill(Color.web(COLOR_TEXTO_MUTED));

        VBox encabezado = new VBox(4, lblTitulo, lblSub);

        // ── SECCIONES ──
        VBox seccionDias      = crearSeccion("📅  Días de atención",       crearPanelDias());
        VBox seccionFranja    = crearSeccion("🕐  Franja horaria",          crearPanelFranja());
        VBox seccionIntervalo = crearSeccion("⏱️  Intervalo entre citas",   crearPanelIntervalo());
        VBox seccionVentana   = crearSeccion("📆  Ventana de agendamiento", crearPanelVentana());

        // ── BOTONES DE ACCIÓN ──
        Button btnGuardar = new Button("💾  Guardar configuración");
        btnGuardar.setPrefWidth(220);
        btnGuardar.setPrefHeight(42);
        btnGuardar.setFont(Font.font("System", FontWeight.BOLD, 14));
        btnGuardar.setStyle(estiloBoton(COLOR_PRIMARIO));
        btnGuardar.setOnMouseEntered(e -> btnGuardar.setStyle(estiloBoton(COLOR_OSCURO)));
        btnGuardar.setOnMouseExited(e  -> btnGuardar.setStyle(estiloBoton(COLOR_PRIMARIO)));
        btnGuardar.setOnAction(e -> guardarConfiguracion());

        Button btnCargar = new Button("↺  Cargar actual");
        btnCargar.setPrefWidth(160);
        btnCargar.setPrefHeight(42);
        btnCargar.setFont(Font.font("System", FontWeight.BOLD, 13));
        btnCargar.setStyle(estiloBotonSecundario());
        btnCargar.setOnAction(e -> cargarConfiguracionActual());

        HBox panelBotones = new HBox(12, btnGuardar, btnCargar);
        panelBotones.setAlignment(Pos.CENTER_LEFT);

        // ── FEEDBACK ──
        lblFeedback.setFont(Font.font("System", 13));
        lblFeedback.setWrapText(true);

        // ── RAÍZ ──
        VBox root = new VBox(16,
                encabezado,
                seccionDias,
                seccionFranja,
                seccionIntervalo,
                seccionVentana,
                panelBotones,
                lblFeedback);
        root.setPadding(new Insets(24));
        root.setBackground(new Background(new BackgroundFill(
                Color.web(COLOR_FONDO), CornerRadii.EMPTY, Insets.EMPTY)));

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + COLOR_FONDO
                + "; -fx-background-color: " + COLOR_FONDO + ";");

        stage.setTitle("Configuración de disponibilidad - Piedra Azul");
        stage.setScene(new Scene(scroll, 580, 680));
        stage.setResizable(false);
        stage.show();

        seleccionarDefaults();
        if (medicoId != null) cargarConfiguracionActual();
    }

    // -- Paneles internos de cada sección --

    private HBox crearPanelDias() {
        CheckBox[] dias = {chkLunes, chkMartes, chkMiercoles,
                chkJueves, chkViernes, chkSabado, chkDomingo};
        for (CheckBox cb : dias) {
            cb.setFont(Font.font("System", 13));
            cb.setTextFill(Color.web(COLOR_OSCURO));
        }
        HBox fila = new HBox(14, dias);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private GridPane crearPanelFranja() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        grid.add(etiqueta("Inicio"), 0, 0);
        grid.add(cbFranjaInicio, 1, 0);
        grid.add(etiqueta("Fin"), 2, 0);
        grid.add(cbFranjaFin, 3, 0);
        cbFranjaInicio.setPrefWidth(120);
        cbFranjaFin.setPrefWidth(120);
        return grid;
    }

    private HBox crearPanelIntervalo() {
        cbIntervalo.setPrefWidth(140);
        Label nota = new Label("minutos por cita");
        nota.setFont(Font.font("System", 13));
        nota.setTextFill(Color.web(COLOR_TEXTO_MUTED));
        HBox fila = new HBox(10, cbIntervalo, nota);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private VBox crearPanelVentana() {
        sldVentana.setMajorTickUnit(1);
        sldVentana.setMinorTickCount(0);
        sldVentana.setSnapToTicks(true);
        sldVentana.setShowTickMarks(true);
        sldVentana.setShowTickLabels(true);
        sldVentana.setPrefWidth(380);

        sldVentana.valueProperty().addListener((obs, ov, nv) -> {
            int val = nv.intValue();
            lblVentanaValor.setText(val + (val == 1 ? " semana" : " semanas"));
        });

        lblVentanaValor.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblVentanaValor.setTextFill(Color.web(COLOR_PRIMARIO));

        Label desc = new Label(
                "Los pacientes podrán agendar citas hasta esta cantidad de semanas en el futuro.");
        desc.setFont(Font.font("System", 11));
        desc.setTextFill(Color.web(COLOR_TEXTO_MUTED));
        desc.setWrapText(true);

        HBox fila = new HBox(14, sldVentana, lblVentanaValor);
        fila.setAlignment(Pos.CENTER_LEFT);
        return new VBox(6, fila, desc);
    }

    // -- Guardar configuración: PATCH /api/medicos/{id}/configuracion --
    private void guardarConfiguracion() {
        String dias = obtenerDiasSeleccionados();
        if (dias.isBlank()) {
            feedback("Seleccione al menos un día de atención.", true);
            return;
        }

        String inicio    = cbFranjaInicio.getValue();
        String fin       = cbFranjaFin.getValue();
        String intervalo = cbIntervalo.getValue();
        int    ventana   = (int) sldVentana.getValue();

        if (inicio == null || fin == null || intervalo == null) {
            feedback("Complete todos los campos de horario.", true);
            return;
        }
        if (inicio.compareTo(fin) >= 0) {
            feedback("La hora de inicio debe ser anterior a la hora de fin.", true);
            return;
        }


        if (medicoId == null) {
            feedback("✗ No se pudo identificar el médico. Cierre sesión y vuelva a ingresar.", true);
            return;
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("diasAtencion",   dias);
            body.put("franjaInicio",   inicio);
            body.put("franjaFin",      fin);
            body.put("intervaloCitas", Integer.parseInt(intervalo.split(" ")[0]));
            body.put("ventanaSemanas", ventana);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/medicos/"
                            + medicoId + "/configuracion"))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(
                            mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                feedback("✓ Configuración guardada correctamente.", false);
            } else {
                // Usar TypeReference para obtener Map tipado y evitar warning
                Map<String, Object> err = mapper.readValue(
                        response.body(), new TypeReference<Map<String, Object>>() {});
                feedback("✗ " + err.getOrDefault("error", "Error desconocido"), true);
            }
        } catch (Exception e) {
            feedback("✗ Error de conexión: " + e.getMessage(), true);
        }
    }

    // -- Cargar configuración actual del médico: GET /api/medicos/{id} --
    private void cargarConfiguracionActual() {
        if (medicoId == null) return;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/medicos/" + medicoId))
                    .GET().build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // TypeReference garantiza Map<String, Object> y elimina los errores
                // "incompatible types: Map<capture#2 of ?...>" de la línea 289
                Map<String, Object> data = mapper.readValue(
                        response.body(), new TypeReference<Map<String, Object>>() {});
                aplicarConfiguracion(data);
                feedback("✓ Configuración cargada.", false);
            }
        } catch (Exception e) {
            feedback("No se pudo cargar la configuración actual.", true);
        }
    }

    // -- Aplica los valores recibidos del servidor a los controles de UI --
    private void aplicarConfiguracion(Map<String, Object> data) {
        // Días de atención
        String dias = (String) data.getOrDefault("diasAtencion",
                "LUNES,MARTES,MIERCOLES,JUEVES,VIERNES");
        if (dias != null) {
            String d = dias.toUpperCase();
            chkLunes.setSelected(d.contains("LUNES"));
            chkMartes.setSelected(d.contains("MARTES"));
            chkMiercoles.setSelected(d.contains("MIERCOLES"));
            chkJueves.setSelected(d.contains("JUEVES"));
            chkViernes.setSelected(d.contains("VIERNES"));
            chkSabado.setSelected(d.contains("SABADO"));
            chkDomingo.setSelected(d.contains("DOMINGO"));
        }

        // Franja horaria
        String ini = (String) data.getOrDefault("franjaInicio", "08:00");
        String fin = (String) data.getOrDefault("franjaFin",    "17:00");
        if (cbFranjaInicio.getItems().contains(ini)) cbFranjaInicio.setValue(ini);
        if (cbFranjaFin.getItems().contains(fin))    cbFranjaFin.setValue(fin);

        // Intervalo entre citas
        Object intervalo = data.get("intervaloCitas");
        if (intervalo != null) {
            cbIntervalo.getItems().stream()
                    .filter(i -> i.startsWith(intervalo.toString()))
                    .findFirst()
                    .ifPresent(cbIntervalo::setValue);
        }

        // Ventana de agendamiento
        Object ventana = data.get("ventanaSemanas");
        if (ventana != null) {
            double v = Double.parseDouble(ventana.toString());
            sldVentana.setValue(Math.min(12, Math.max(1, v)));
        }
    }

    // -- Utilidades --

    private void poblarCombos() {
        // Horas de 06:00 a 22:00 en intervalos de 30 min
        for (int h = 6; h <= 22; h++) {
            cbFranjaInicio.getItems().addAll(
                    String.format("%02d:00", h),
                    String.format("%02d:30", h));
            cbFranjaFin.getItems().addAll(
                    String.format("%02d:00", h),
                    String.format("%02d:30", h));
        }
        cbIntervalo.getItems().addAll(
                "10 min", "15 min", "20 min",
                "30 min", "45 min", "60 min", "90 min", "120 min");
    }

    private void seleccionarDefaults() {
        chkLunes.setSelected(true);
        chkMartes.setSelected(true);
        chkMiercoles.setSelected(true);
        chkJueves.setSelected(true);
        chkViernes.setSelected(true);
        cbFranjaInicio.setValue("08:00");
        cbFranjaFin.setValue("17:00");
        cbIntervalo.setValue("30 min");
        sldVentana.setValue(4);
    }

    private String obtenerDiasSeleccionados() {
        Map<CheckBox, String> mapa = new LinkedHashMap<>();
        mapa.put(chkLunes,     "LUNES");
        mapa.put(chkMartes,    "MARTES");
        mapa.put(chkMiercoles, "MIERCOLES");
        mapa.put(chkJueves,    "JUEVES");
        mapa.put(chkViernes,   "VIERNES");
        mapa.put(chkSabado,    "SABADO");
        mapa.put(chkDomingo,   "DOMINGO");

        return mapa.entrySet().stream()
                .filter(e -> e.getKey().isSelected())
                .map(Map.Entry::getValue)
                .collect(Collectors.joining(","));
    }

    private void feedback(String mensaje, boolean error) {
        lblFeedback.setText(mensaje);
        lblFeedback.setTextFill(Color.web(error ? COLOR_ERROR : COLOR_EXITO));
    }

    // -- Constructores de componentes de UI --

    private VBox crearSeccion(String titulo, javafx.scene.Node contenido) {
        Label lbl = new Label(titulo);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 13));
        lbl.setTextFill(Color.web(COLOR_OSCURO));

        VBox caja = new VBox(10, lbl, contenido);
        caja.setPadding(new Insets(14));
        caja.setBackground(new Background(new BackgroundFill(
                Color.web(COLOR_BLANCO), new CornerRadii(8), Insets.EMPTY)));
        caja.setStyle("-fx-border-color: " + COLOR_BORDE + ";"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;");
        return caja;
    }

    private Label etiqueta(String texto) {
        Label lbl = new Label(texto);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 12));
        lbl.setTextFill(Color.web(COLOR_OSCURO));
        return lbl;
    }

    private String estiloBoton(String color) {
        return "-fx-background-color: " + color + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 8;"
                + "-fx-cursor: hand;";
    }

    private String estiloBotonSecundario() {
        return "-fx-background-color: transparent;"
                + "-fx-text-fill: " + COLOR_PRIMARIO + ";"
                + "-fx-border-color: " + COLOR_PRIMARIO + ";"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;"
                + "-fx-cursor: hand;";
    }

    public static void main(String[] args) { launch(); }
}