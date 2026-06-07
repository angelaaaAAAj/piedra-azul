package com.piedraazul.ui.historial;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class HistorialController {

    private static final String API_URL = "http://localhost:8080/api/historial";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public void cargarPorPacienteId(Long pacienteId,
                                    TableView<HistorialEntry> tabla,
                                    Label lblFeedback,
                                    Label lblTotal) {
        try {
            HttpRequest request = autenticado(API_URL + "/paciente/" + pacienteId)
                    .GET().build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            cargarTabla(response.body(), tabla, lblTotal);
            feedback("Registros cargados para paciente " + pacienteId, false, lblFeedback);
        } catch (Exception e) {
            feedback("Error: " + e.getMessage(), true, lblFeedback);
        }
    }

    public void cargarPorCitaId(Long citaId,
                                TableView<HistorialEntry> tabla,
                                Label lblFeedback,
                                Label lblTotal) {
        try {
            HttpRequest request = autenticado(API_URL + "/cita/" + citaId)
                    .GET().build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            cargarTabla(response.body(), tabla, lblTotal);
            feedback("Registros cargados para cita " + citaId, false, lblFeedback);
        } catch (Exception e) {
            feedback("Error: " + e.getMessage(), true, lblFeedback);
        }
    }

    public void cargarReagendamientos(Long citaId,
                                      TableView<ReagendamientoEntry> tabla,
                                      Label lblFeedback) {
        try {
            HttpRequest request = autenticado(API_URL + "/cambios/cita/" + citaId)
                    .GET().build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            List<Map<String, Object>> lista =
                    mapper.readValue(response.body(), new TypeReference<>() {});
            var registros = FXCollections.observableArrayList(
                    lista.stream().map(m -> new ReagendamientoEntry(
                            m.get("fechaAnterior") != null ? m.get("fechaAnterior").toString() : "",
                            m.get("fechaNueva")    != null ? m.get("fechaNueva").toString()    : "",
                            m.get("motivo")        != null ? m.get("motivo").toString()        : "",
                            m.get("responsable")   != null ? m.get("responsable").toString()   : ""
                    )).toList());
            tabla.setItems(registros);
            feedback("Reagendamientos cargados para cita " + citaId, false, lblFeedback);
        } catch (Exception e) {
            feedback("Error: " + e.getMessage(), true, lblFeedback);
        }
    }

    public void registrarEntrada(Long pacienteId,
                                 Long medicoId,
                                 Long citaId,
                                 String tipo,
                                 String descripcion,
                                 String registradoPor,
                                 Label lblFeedback) {
        try {
            String json = """
                    {
                      "pacienteId": %d,
                      "medicoId": %d,
                      "citaId": %d,
                      "tipoRegistro": "%s",
                      "descripcion": "%s",
                      "registradoPor": "%s"
                    }
                    """.formatted(pacienteId, medicoId, citaId, tipo,
                    descripcion.replace("\"", "'"), registradoPor);

            HttpRequest request = autenticado(API_URL)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                feedback("✓ Registro guardado correctamente", false, lblFeedback);
            } else {
                feedback("✗ Error: " + response.body(), true, lblFeedback);
            }
        } catch (Exception e) {
            feedback("✗ Error de conexión: " + e.getMessage(), true, lblFeedback);
        }
    }

    // ── Helper: añade el token JWT a cualquier petición ──────────────────
    private HttpRequest.Builder autenticado(String url) {
        String token = com.piedraazul.ui.app.PiedraAzulApp.getToken();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    private void cargarTabla(String json,
                             TableView<HistorialEntry> tabla,
                             Label lblTotal) throws Exception {
        List<Map<String, Object>> lista =
                mapper.readValue(json, new TypeReference<>() {});
        var registros = FXCollections.observableArrayList(
                lista.stream().map(m -> new HistorialEntry(
                        ((Number) m.get("id")).longValue(),
                        ((Number) m.get("pacienteId")).longValue(),
                        ((Number) m.get("medicoId")).longValue(),
                        ((Number) m.get("citaId")).longValue(),
                        (String) m.get("tipoRegistro"),
                        (String) m.get("descripcion"),
                        m.get("fechaRegistro") != null
                                ? LocalDateTime.parse((String) m.get("fechaRegistro"))
                                : null,
                        (String) m.get("registradoPor")
                )).toList());
        tabla.setItems(registros);
        if (lblTotal != null)
            lblTotal.setText("Registros: " + registros.size());
    }

    private void feedback(String message, boolean error, Label lbl) {
        if (lbl != null) {
            lbl.setText(message);
            lbl.setStyle(error ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
        }
    }
}