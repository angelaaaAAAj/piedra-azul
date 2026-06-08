package com.piedraazul.ui.auditoria;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class AuditoriaApp extends Application {

    private final ObservableList<Auditoria> todosLosRegistros  = FXCollections.observableArrayList();
    private final ObservableList<Auditoria> registrosFiltrados = FXCollections.observableArrayList();
    private final TableView<Auditoria>      tabla              = new TableView<>();

    // ── Búsqueda ──
    private final TextField        txtBusqueda     = new TextField();
    private final ComboBox<String> cbTipoEvento    = new ComboBox<>();
    private final ComboBox<String> cbMicroservicio = new ComboBox<>();
    private final DatePicker       dpFecha         = new DatePicker();

    // ── Métricas ──
    private final Label lblTotal     = numLabel("0", "#1F2937");
    private final Label lblUsuarios  = numLabel("0", "#4C1D95");
    private final Label lblCitas     = numLabel("0", "#065F46");
    private final Label lblHistorial = numLabel("0", "#7C2D12");

    private final HttpClient   httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper     = new ObjectMapper();
    private static final String API_URL   = "http://localhost:8080/api/auditoria";
    private static final DateTimeFormatter FMT_IN  =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");
    private static final DateTimeFormatter FMT_OUT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void start(Stage stage) {

        // ── ENCABEZADO ──
        Label titulo = new Label("Auditoría del sistema");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 20));
        titulo.setTextFill(Color.web("#4C1D95"));

        Label sub = new Label("Registro de acciones realizadas por usuarios — Clínica Piedra Azul");
        sub.setFont(Font.font("System", 13));
        sub.setTextFill(Color.web("#6B7280"));

        VBox encabezado = new VBox(3, titulo, sub);

        // ── MÉTRICAS ──
        HBox metricas = new HBox(12,
                cardMetrica("Total eventos",     lblTotal),
                cardMetrica("Eventos usuarios",  lblUsuarios),
                cardMetrica("Eventos citas",     lblCitas),
                cardMetrica("Eventos historial", lblHistorial));
        for (javafx.scene.Node n : metricas.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
        }

        // ── BÚSQUEDA ──
        txtBusqueda.setPromptText("Buscar por usuario o descripción...");
        txtBusqueda.setPrefHeight(36);
        txtBusqueda.setStyle(campoEstilo());
        txtBusqueda.textProperty().addListener((obs, o, n) -> aplicarFiltros());
        HBox.setHgrow(txtBusqueda, Priority.ALWAYS);

        cbTipoEvento.getItems().addAll(
                "Todos los eventos",
                "USUARIO_CREADO", "USUARIO_DESACTIVADO",
                "LOGIN_EXITOSO",  "LOGIN_FALLIDO",
                "PACIENTE_REGISTRADO",
                "CITA_AGENDADA",  "CITA_CANCELADA", "CITA_REAGENDADA",
                "HISTORIAL_CREADO", "HISTORIAL_MODIFICADO");
        cbTipoEvento.setValue("Todos los eventos");
        cbTipoEvento.setPrefHeight(36);
        cbTipoEvento.setStyle(campoEstilo());
        cbTipoEvento.setOnAction(e -> aplicarFiltros());

        cbMicroservicio.getItems().addAll(
                "Todos los microservicios",
                "ms-auth", "ms-agenda", "ms-historial",
                "ms-pacientes", "ms-auditoria");
        cbMicroservicio.setValue("Todos los microservicios");
        cbMicroservicio.setPrefHeight(36);
        cbMicroservicio.setStyle(campoEstilo());
        cbMicroservicio.setOnAction(e -> aplicarFiltros());

        dpFecha.setPromptText("Filtrar por fecha");
        dpFecha.setPrefHeight(36);
        dpFecha.valueProperty().addListener((obs, o, n) -> aplicarFiltros());

        Button btnLimpiar = new Button("✕ Limpiar filtros");
        btnLimpiar.setPrefHeight(36);
        btnLimpiar.setStyle(estiloBotonSecundario());
        btnLimpiar.setOnAction(e -> limpiarFiltros());

        Button btnActualizar = new Button("↺  Actualizar");
        btnActualizar.setPrefHeight(36);
        btnActualizar.setStyle(estiloBoton("#7B2FBE"));
        btnActualizar.setOnAction(e -> cargarTodos());

        HBox filaBusqueda = new HBox(8,
                txtBusqueda, cbTipoEvento, cbMicroservicio,
                dpFecha, btnLimpiar, btnActualizar);
        filaBusqueda.setAlignment(Pos.CENTER_LEFT);

        // ── TABLA ──
        TableColumn<Auditoria, Long> colId = col("ID", "id", 50);
        colId.setStyle("-fx-alignment: CENTER;");

        TableColumn<Auditoria, String> colTipo = new TableColumn<>("Tipo de evento");
        colTipo.setPrefWidth(185);
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoEvento"));
        colTipo.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(item.replace("_", " ").toLowerCase());
                badge.setFont(Font.font("System", FontWeight.BOLD, 11));
                badge.setPadding(new Insets(3, 10, 3, 10));
                badge.setStyle(badgeEstilo(item));
                setGraphic(badge);
                setText(null);
            }
        });

        TableColumn<Auditoria, String> colDesc = col("Descripción", "descripcion", 270);

        TableColumn<Auditoria, Long> colEntidad = col("Entidad ID", "entidadId", 85);
        colEntidad.setStyle("-fx-alignment: CENTER;");

        TableColumn<Auditoria, String> colUsuario = col("Usuario", "realizadoPor", 120);

        TableColumn<Auditoria, String> colMs = col("Microservicio", "microservicioOrigen", 120);

        TableColumn<Auditoria, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setPrefWidth(140);
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaEvento"));
        colFecha.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(formatearFecha(item));
                setTextFill(Color.web("#6B7280"));
                setFont(Font.font("System", 12));
            }
        });

        tabla.getColumns().addAll(colId, colTipo, colDesc,
                colEntidad, colUsuario, colMs, colFecha);
        tabla.setItems(registrosFiltrados);
        tabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tabla.setPrefHeight(400);
        tabla.setPlaceholder(new Label("No hay registros para los filtros seleccionados"));
        tabla.setStyle("""
                -fx-background-color: white;
                -fx-border-color: #E5E7EB;
                -fx-border-radius: 8;
                """);

        Label lblConteo = new Label("0 registros");
        lblConteo.setFont(Font.font("System", 12));
        lblConteo.setTextFill(Color.web("#6B7280"));
        registrosFiltrados.addListener(
                (javafx.collections.ListChangeListener<Auditoria>) c ->
                        lblConteo.setText(registrosFiltrados.size() + " registros"));

        // ── LAYOUT ──
        VBox root = new VBox(14,
                encabezado,
                metricas,
                filaBusqueda,
                lblConteo,
                tabla);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #F5F3FF;");

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F5F3FF; -fx-background-color: #F5F3FF;");

        stage.setTitle("Auditoría - Piedra Azul");
        stage.setScene(new Scene(scroll, 1100, 680));
        stage.show();

        cargarTodos();
    }

    // ── Carga todos los registros ──
    private void cargarTodos() {
        try {
            HttpRequest req = requestAutenticado(API_URL).GET().build();
            HttpResponse<String> resp =
                    httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                mostrarAlerta("Error", resp.body()); return;
            }
            List<Auditoria> lista = mapper.readValue(resp.body(), new TypeReference<>() {});
            todosLosRegistros.setAll(lista);
            aplicarFiltros();
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo conectar con ms-auditoria");
        }
    }

    // ── Filtra en memoria ──
    private void aplicarFiltros() {
        String busq  = txtBusqueda.getText().trim().toLowerCase();
        String tipo  = cbTipoEvento.getValue();
        String ms    = cbMicroservicio.getValue();
        LocalDate fd = dpFecha.getValue();

        List<Auditoria> res = todosLosRegistros.stream()
                .filter(a -> tipo == null || tipo.equals("Todos los eventos")
                        || tipo.equals(a.getTipoEvento()))
                .filter(a -> busq.isBlank()
                        || conteniene(a.getDescripcion(),  busq)
                        || conteniene(a.getRealizadoPor(), busq))
                .filter(a -> ms == null || ms.equals("Todos los microservicios")
                        || ms.equals(a.getMicroservicioOrigen()))
                .filter(a -> fd == null
                        || (a.getFechaEvento() != null
                        && a.getFechaEvento().startsWith(fd.toString())))
                .collect(Collectors.toList());

        registrosFiltrados.setAll(res);
        actualizarMetricas();
    }

    private void limpiarFiltros() {
        txtBusqueda.clear();
        cbTipoEvento.setValue("Todos los eventos");
        cbMicroservicio.setValue("Todos los microservicios");
        dpFecha.setValue(null);
        aplicarFiltros();
    }

    // ── Métricas ──
    private void actualizarMetricas() {
        long total = registrosFiltrados.size();
        long usuarios = registrosFiltrados.stream()
                .filter(a -> a.getTipoEvento() != null && (
                        a.getTipoEvento().startsWith("USUARIO") ||
                                a.getTipoEvento().startsWith("LOGIN")))
                .count();
        long citas = registrosFiltrados.stream()
                .filter(a -> a.getTipoEvento() != null &&
                        a.getTipoEvento().startsWith("CITA"))
                .count();
        long historial = registrosFiltrados.stream()
                .filter(a -> a.getTipoEvento() != null &&
                        a.getTipoEvento().startsWith("HISTORIAL"))
                .count();

        lblTotal.setText(String.valueOf(total));
        lblUsuarios.setText(String.valueOf(usuarios));
        lblCitas.setText(String.valueOf(citas));
        lblHistorial.setText(String.valueOf(historial));
    }

    // ── Formatea fecha ISO a "yyyy-MM-dd HH:mm" ──
    private String formatearFecha(String raw) {
        if (raw == null || raw.isBlank()) return "";
        try {
            // Normaliza a 9 decimales para el parser
            String normalizado = raw;
            int tIdx = raw.indexOf('T');
            if (tIdx > 0) {
                int dotIdx = raw.indexOf('.', tIdx);
                if (dotIdx > 0) {
                    String decimales = raw.substring(dotIdx + 1);
                    while (decimales.length() < 9) decimales += "0";
                    decimales = decimales.substring(0, 9);
                    normalizado = raw.substring(0, dotIdx + 1) + decimales;
                }
            }
            return LocalDateTime.parse(normalizado, FMT_IN).format(FMT_OUT);
        } catch (Exception e) {
            // Si el formato varía, devolver solo los primeros 16 chars
            return raw.length() > 16 ? raw.substring(0, 16).replace("T", " ") : raw;
        }
    }

    // ── Helpers ──
    private boolean conteniene(String campo, String busq) {
        return campo != null && campo.toLowerCase().contains(busq);
    }

    @SuppressWarnings("unchecked")
    private <T> TableColumn<Auditoria, T> col(String titulo, String prop, double ancho) {
        TableColumn<Auditoria, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(ancho);
        return c;
    }

    private String badgeEstilo(String tipo) {
        if (tipo == null) return "";
        if (tipo.startsWith("USUARIO") || tipo.startsWith("LOGIN"))
            return "-fx-background-color:#EDE9FE; -fx-text-fill:#4C1D95; -fx-background-radius:999;";
        if (tipo.startsWith("CITA"))
            return "-fx-background-color:#D1FAE5; -fx-text-fill:#065F46; -fx-background-radius:999;";
        if (tipo.startsWith("HISTORIAL"))
            return "-fx-background-color:#FFEDD5; -fx-text-fill:#7C2D12; -fx-background-radius:999;";
        if (tipo.startsWith("PACIENTE"))
            return "-fx-background-color:#E0F2FE; -fx-text-fill:#075985; -fx-background-radius:999;";
        return "-fx-background-color:#F3F4F6; -fx-text-fill:#374151; -fx-background-radius:999;";
    }

    private VBox cardMetrica(String etiqueta, Label numero) {
        Label lbl = new Label(etiqueta);
        lbl.setFont(Font.font("System", 12));
        lbl.setTextFill(Color.web("#6B7280"));
        VBox card = new VBox(2, numero, lbl);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setStyle("""
                -fx-background-color: white;
                -fx-border-color: #E5E7EB;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                """);
        VBox.setVgrow(card, Priority.NEVER);
        return card;
    }

    private static Label numLabel(String valor, String color) {
        Label lbl = new Label(valor);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 24));
        lbl.setTextFill(Color.web(color));
        return lbl;
    }

    private String campoEstilo() {
        return """
                -fx-background-color: white;
                -fx-border-color: #D1D5DB;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                -fx-padding: 6 10;
                -fx-font-size: 13px;
                """;
    }

    private String estiloBoton(String color) {
        return "-fx-background-color: " + color + ";"
                + "-fx-text-fill: white; -fx-font-weight: bold;"
                + "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 14;";
    }

    private String estiloBotonSecundario() {
        return """
                -fx-background-color: white;
                -fx-text-fill: #6B7280;
                -fx-border-color: #D1D5DB;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                -fx-cursor: hand;
                -fx-padding: 6 12;
                """;
    }

    private HttpRequest.Builder requestAutenticado(String url) {
        String token = com.piedraazul.ui.app.PiedraAzulApp.getToken();
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return builder;
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static void main(String[] args) { launch(); }
}