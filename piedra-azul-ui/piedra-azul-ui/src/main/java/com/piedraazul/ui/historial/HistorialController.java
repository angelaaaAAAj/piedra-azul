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
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/paciente/" + pacienteId))
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
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/cita/" + citaId))
                    .GET().build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            cargarTabla(response.body(), tabla, lblTotal);
            feedback("Registros cargados para cita " + citaId, false, lblFeedback);
        } catch (Exception e) {
            feedback("Error: " + e.getMessage(), true, lblFeedback);
        }
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
    public void cargarReagendamientos(Long citaId,
                                      TableView<ReagendamientoEntry> tabla, Label lblFeedback) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/cambios/cita/" + citaId))
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
}