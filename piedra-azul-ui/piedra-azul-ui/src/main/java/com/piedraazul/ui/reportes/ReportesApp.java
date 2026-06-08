package com.piedraazul.ui.reportes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
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
import java.util.*;

public class ReportesApp extends Application {

    private static final String BASE = "http://localhost:8080/api/reportes";
    private final HttpClient   http   = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // Paneles de gráficos
    private final Canvas canvasMes          = new Canvas(700, 220);
    private final Canvas canvasMedico       = new Canvas(700, 220);
    private final Canvas canvasEspecialidad = new Canvas(500, 220);

    // Tablas
    private final TableView<ReporteEntry> tablaMes          = new TableView<>();
    private final TableView<ReporteEntry> tablaMedico       = new TableView<>();
    private final TableView<ReporteEntry> tablaEspecialidad = new TableView<>();

    @Override
    public void start(Stage stage) {

        Label titulo = new Label("Reportes y Estadísticas");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 22));
        titulo.setTextFill(Color.web("#4C1D95"));

        Label sub = new Label("Resumen estadístico de citas — Clínica Piedra Azul");
        sub.setFont(Font.font("System", 13));
        sub.setTextFill(Color.web("#6B7280"));

        Button btnActualizar = new Button("↺  Actualizar datos");
        btnActualizar.setStyle(estiloBoton("#7B2FBE"));
        btnActualizar.setOnAction(e -> cargarTodo());

        HBox encabezado = new HBox(20, new VBox(3, titulo, sub), btnActualizar);
        encabezado.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(encabezado.getChildren().get(0), Priority.ALWAYS);

        // -- Sección citas por mes --
        VBox secMes = seccion("📅  Citas por mes",
                canvasMes, configurarTabla(tablaMes));

        // -- Sección citas por médico --
        VBox secMedico = seccion("👩‍⚕️  Citas por médico / terapista",
                canvasMedico, configurarTabla(tablaMedico));

        // -- Sección citas por especialidad --
        VBox secEsp = seccion("🏥  Citas por especialidad",
                canvasEspecialidad, configurarTabla(tablaEspecialidad));

        VBox root = new VBox(20, encabezado, secMes, secMedico, secEsp);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #F5F3FF;");

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F5F3FF; -fx-background-color: #F5F3FF;");

        stage.setTitle("Reportes - Piedra Azul");
        stage.setScene(new Scene(scroll, 820, 700));
        stage.show();

        cargarTodo();
    }

    // ── Carga los 3 reportes ──────────────────────────
    private void cargarTodo() {
        cargarCitasPorMes();
        cargarCitasPorMedico();
        cargarCitasPorEspecialidad();
    }

    private void cargarCitasPorMes() {
        try {
            HttpResponse<String> resp = http.send(
                    requestAuth(BASE + "/citas-por-mes").GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return;

            Map<String, Long> datos = mapper.readValue(
                    resp.body(), new TypeReference<>() {});

            List<ReporteEntry> lista = datos.entrySet().stream()
                    .map(e -> new ReporteEntry(e.getKey(), e.getValue()))
                    .toList();

            tablaMes.setItems(FXCollections.observableArrayList(lista));
            dibujarBarras(canvasMes, lista, Color.web("#7B2FBE"));
        } catch (Exception e) { /* sin conexión */ }
    }

    private void cargarCitasPorMedico() {
        try {
            HttpResponse<String> resp = http.send(
                    requestAuth(BASE + "/citas-por-medico").GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return;

            List<Map<String, Object>> raw = mapper.readValue(
                    resp.body(), new TypeReference<>() {});

            List<ReporteEntry> lista = raw.stream()
                    .map(m -> new ReporteEntry(
                            m.get("nombre").toString(),
                            Long.parseLong(m.get("total").toString())))
                    .toList();

            tablaMedico.setItems(FXCollections.observableArrayList(lista));
            dibujarBarras(canvasMedico, lista, Color.web("#059669"));
        } catch (Exception e) { /* sin conexión */ }
    }

    private void cargarCitasPorEspecialidad() {
        try {
            HttpResponse<String> resp = http.send(
                    requestAuth(BASE + "/citas-por-especialidad").GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return;

            Map<String, Long> datos = mapper.readValue(
                    resp.body(), new TypeReference<>() {});

            List<ReporteEntry> lista = datos.entrySet().stream()
                    .map(e -> new ReporteEntry(e.getKey(), e.getValue()))
                    .toList();

            tablaEspecialidad.setItems(FXCollections.observableArrayList(lista));
            dibujarBarras(canvasEspecialidad, lista, Color.web("#D97706"));
        } catch (Exception e) { /* sin conexión */ }
    }

    // ── Gráfico de barras simple con Canvas ───────────
    private void dibujarBarras(Canvas canvas,
                               List<ReporteEntry> datos, Color color) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (datos == null || datos.isEmpty()) return;

        double w      = canvas.getWidth();
        double h      = canvas.getHeight();
        double margin = 40;
        double maxVal = datos.stream().mapToLong(ReporteEntry::getValor).max().orElse(1);

        int    n      = datos.size();
        double paso   = (w - margin * 2) / n;
        double barW   = paso * 0.6;

        // Fondo
        gc.setFill(Color.WHITE);
        gc.fillRoundRect(0, 0, w, h, 12, 12);

        // Líneas de referencia
        gc.setStroke(Color.web("#E5E7EB"));
        gc.setLineWidth(1);
        for (int i = 1; i <= 4; i++) {
            double y = margin + (h - margin * 2) * (1 - i / 4.0);
            gc.strokeLine(margin, y, w - margin, y);
            gc.setFill(Color.web("#9CA3AF"));
            gc.setFont(Font.font("System", 10));
            gc.fillText(String.valueOf((long)(maxVal * i / 4)), 2, y + 4);
        }

        // Barras
        for (int i = 0; i < n; i++) {
            ReporteEntry e  = datos.get(i);
            double barH     = (h - margin * 2) * (e.getValor() / (double) maxVal);
            double x        = margin + i * paso + (paso - barW) / 2;
            double y        = h - margin - barH;

            gc.setFill(color);
            gc.fillRoundRect(x, y, barW, barH, 4, 4);

            // Etiqueta valor
            gc.setFill(Color.web("#374151"));
            gc.setFont(Font.font("System", FontWeight.BOLD, 10));
            gc.fillText(String.valueOf(e.getValor()), x + barW / 2 - 6, y - 4);

            // Etiqueta eje X
            gc.setFill(Color.web("#6B7280"));
            gc.setFont(Font.font("System", 9));
            String etiq = e.getEtiqueta().length() > 10
                    ? e.getEtiqueta().substring(0, 10) + "…"
                    : e.getEtiqueta();
            gc.fillText(etiq, x, h - margin + 14);
        }
    }

    // ── Configura columnas de tabla ───────────────────
    private TableView<ReporteEntry> configurarTabla(TableView<ReporteEntry> tabla) {
        TableColumn<ReporteEntry, String> colEtiq = new TableColumn<>("Categoría");
        colEtiq.setCellValueFactory(new PropertyValueFactory<>("etiqueta"));
        colEtiq.setPrefWidth(260);

        TableColumn<ReporteEntry, Long> colVal = new TableColumn<>("Total citas");
        colVal.setCellValueFactory(new PropertyValueFactory<>("valor"));
        colVal.setPrefWidth(120);
        colVal.setStyle("-fx-alignment: CENTER;");

        tabla.getColumns().addAll(colEtiq, colVal);
        tabla.setPrefHeight(150);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setStyle("""
                -fx-background-color: white;
                -fx-border-color: #E5E7EB;
                -fx-border-radius: 6;
                """);
        return tabla;
    }

    // ── Sección con gráfico + tabla ───────────────────
    private VBox seccion(String tituloSec, Canvas canvas,
                         TableView<ReporteEntry> tabla) {
        Label lbl = new Label(tituloSec);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 15));
        lbl.setTextFill(Color.web("#4C1D95"));

        VBox card = new VBox(10, lbl, canvas, tabla);
        card.setPadding(new Insets(16));
        card.setStyle("""
                -fx-background-color: white;
                -fx-border-color: #E5E7EB;
                -fx-border-radius: 10;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);
                """);
        return card;
    }

    private String estiloBoton(String color) {
        return "-fx-background-color: " + color + ";"
                + "-fx-text-fill: white; -fx-font-weight: bold;"
                + "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16;";
    }

    private HttpRequest.Builder requestAuth(String url) {
        String token = com.piedraazul.ui.app.PiedraAzulApp.getToken();
        HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(url));
        if (token != null) b.header("Authorization", "Bearer " + token);
        return b;
    }

    public static void main(String[] args) { launch(); }
}