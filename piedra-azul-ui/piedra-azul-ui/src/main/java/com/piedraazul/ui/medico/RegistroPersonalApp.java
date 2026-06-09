package com.piedraazul.ui.medico;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piedraazul.ui.app.PiedraAzulApp;
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

/**
 * Ventana para que el ADMINISTRADOR registre médicos y agendadores.
 * Solo se abre desde el menú principal (después del login),
 * por lo que siempre hay token disponible.
 */
public class RegistroPersonalApp {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper   = new ObjectMapper();

    public void abrir() {
        Stage stage = new Stage();
        stage.setTitle("Registrar personal - Clínica Piedra Azul");

        // ── Campos ──────────────────────────────────────────────────────
        TextField txtNombre    = campo("Nombre *");
        TextField txtApellido  = campo("Apellido *");
        TextField txtDocumento = campo("Número de documento *");
        TextField txtTelefono  = campo("Teléfono *");
        TextField txtEmail     = campo("Email *");

        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Contraseña *");
        txtPassword.setPrefHeight(38);
        txtPassword.setStyle(estiloInput());

        ComboBox<String> cbRol = new ComboBox<>();
        cbRol.getItems().addAll("MEDICO_TERAPISTA", "AGENDADOR");
        cbRol.setPromptText("Rol *");
        cbRol.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> cbEspecialidad = new ComboBox<>();
        cbEspecialidad.getItems().addAll(
                "TERAPIA_NEURAL", "QUIROPRAXIA", "FISIOTERAPIA");
        cbEspecialidad.setPromptText("Especialidad *");
        cbEspecialidad.setMaxWidth(Double.MAX_VALUE);

        // Mostrar especialidad solo si el rol es médico
        Label lblEspecialidad = etiqueta("Especialidad *");
        cbRol.valueProperty().addListener((obs, o, n) -> {
            boolean esMedico = "MEDICO_TERAPISTA".equals(n);
            lblEspecialidad.setVisible(esMedico);
            lblEspecialidad.setManaged(esMedico);
            cbEspecialidad.setVisible(esMedico);
            cbEspecialidad.setManaged(esMedico);
        });
        lblEspecialidad.setVisible(false);
        lblEspecialidad.setManaged(false);
        cbEspecialidad.setVisible(false);
        cbEspecialidad.setManaged(false);

        Label lblFeedback = new Label();
        lblFeedback.setFont(Font.font("System", 13));
        lblFeedback.setWrapText(true);

        // ── Formulario ───────────────────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(etiqueta("Nombre *"),     0, 0); grid.add(txtNombre,    1, 0);
        grid.add(etiqueta("Apellido *"),   0, 1); grid.add(txtApellido,  1, 1);
        grid.add(etiqueta("Documento *"),  0, 2); grid.add(txtDocumento, 1, 2);
        grid.add(etiqueta("Teléfono *"),   0, 3); grid.add(txtTelefono,  1, 3);
        grid.add(etiqueta("Email *"),      0, 4); grid.add(txtEmail,     1, 4);
        grid.add(etiqueta("Contraseña *"), 0, 5); grid.add(txtPassword,  1, 5);
        grid.add(etiqueta("Rol *"),        0, 6); grid.add(cbRol,        1, 6);
        grid.add(lblEspecialidad,          0, 7); grid.add(cbEspecialidad, 1, 7);

        // ── Botón registrar ───────────────────────────────────────────────
        Button btnRegistrar = new Button("✓  Registrar");
        btnRegistrar.setPrefWidth(Double.MAX_VALUE);
        btnRegistrar.setPrefHeight(42);
        btnRegistrar.setFont(Font.font("System", FontWeight.BOLD, 14));
        btnRegistrar.setStyle(estiloBoton("#7B2FBE"));
        btnRegistrar.setOnAction(e -> {
            // Validaciones
            if (txtNombre.getText().isBlank()
                    || txtApellido.getText().isBlank()
                    || txtDocumento.getText().isBlank()
                    || txtTelefono.getText().isBlank()
                    || txtEmail.getText().isBlank()
                    || txtPassword.getText().isBlank()
                    || cbRol.getValue() == null) {
                feedback(lblFeedback, "✗ Complete todos los campos obligatorios", true);
                return;
            }
            boolean esMedico = "MEDICO_TERAPISTA".equals(cbRol.getValue());
            if (esMedico && cbEspecialidad.getValue() == null) {
                feedback(lblFeedback, "✗ Seleccione la especialidad del médico", true);
                return;
            }

            try {
                Long medicoIdNuevo = null;

                // PASO 1: Si es médico, crearlo en ms-agenda primero
                if (esMedico) {
                    Map<String, Object> bodyMedico = new LinkedHashMap<>();
                    bodyMedico.put("nombre",         txtNombre.getText().trim());
                    bodyMedico.put("apellido",       txtApellido.getText().trim());
                    bodyMedico.put("registroMedico", "N/A");
                    bodyMedico.put("especialidad",   cbEspecialidad.getValue());
                    bodyMedico.put("disponible",     true);
                    bodyMedico.put("intervaloCitas", 30);
                    bodyMedico.put("franjaInicio",   "08:00");
                    bodyMedico.put("franjaFin",      "17:00");
                    bodyMedico.put("ventanaSemanas", 4);

                    HttpRequest reqMedico = autenticado("http://localhost:8080/api/medicos")
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    mapper.writeValueAsString(bodyMedico)))
                            .build();

                    HttpResponse<String> respMedico = httpClient.send(
                            reqMedico, HttpResponse.BodyHandlers.ofString());

                    if (respMedico.statusCode() != 201) {
                        feedback(lblFeedback,
                                "✗ Error al crear médico en agenda ("
                                        + respMedico.statusCode() + "): "
                                        + respMedico.body(), true);
                        return;
                    }
                    Map<String, Object> medicoCreado = mapper.readValue(
                            respMedico.body(), new TypeReference<>() {});
                    medicoIdNuevo = Long.parseLong(medicoCreado.get("id").toString());
                }

                // PASO 2: Crear usuario en ms-auth
                Map<String, Object> bodyUsuario = new LinkedHashMap<>();
                bodyUsuario.put("username", txtDocumento.getText().trim());
                bodyUsuario.put("nombre",   txtNombre.getText().trim()
                        + " " + txtApellido.getText().trim());
                bodyUsuario.put("email",    txtEmail.getText().trim());
                bodyUsuario.put("password", txtPassword.getText().trim());
                bodyUsuario.put("rol",      cbRol.getValue());
                if (medicoIdNuevo != null)
                    bodyUsuario.put("medicoId", medicoIdNuevo.toString());

                HttpRequest reqUsuario = autenticado(
                        "http://localhost:8080/api/auth/registro")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                mapper.writeValueAsString(bodyUsuario)))
                        .build();

                HttpResponse<String> respUsuario = httpClient.send(
                        reqUsuario, HttpResponse.BodyHandlers.ofString());

                if (respUsuario.statusCode() == 201) {
                    feedback(lblFeedback, "✓ Personal registrado correctamente", false);
                    btnRegistrar.setDisable(true);
                    // Limpiar formulario
                    txtNombre.clear(); txtApellido.clear();
                    txtDocumento.clear(); txtTelefono.clear();
                    txtEmail.clear(); txtPassword.clear();
                    cbRol.setValue(null); cbEspecialidad.setValue(null);
                    btnRegistrar.setDisable(false);
                } else {
                    Map<String, Object> err = mapper.readValue(respUsuario.body(),
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                    feedback(lblFeedback,
                            "✗ " + err.getOrDefault("error", "Error al crear usuario"),
                            true);
                }

            } catch (Exception ex) {
                feedback(lblFeedback, "✗ Error de conexión: " + ex.getMessage(), true);
            }
        });

        // ── Layout ────────────────────────────────────────────────────────
        Label titulo = new Label("Registrar personal");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 20));
        titulo.setTextFill(Color.web("#4C1D95"));

        Label sub = new Label("Solo médicos terapistas y agendadores");
        sub.setFont(Font.font("System", 13));
        sub.setTextFill(Color.web("#6B7280"));

        VBox root = new VBox(14,
                new VBox(4, titulo, sub),
                panelSeccion("👤  Datos del personal", grid),
                btnRegistrar,
                lblFeedback);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #F5F3FF;");

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F5F3FF; -fx-background-color: #F5F3FF;");

        stage.setScene(new Scene(scroll, 480, 600));
        stage.show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private HttpRequest.Builder autenticado(String url) {
        String token = PiedraAzulApp.getToken();
        HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(url));
        if (token != null) b.header("Authorization", "Bearer " + token);
        return b;
    }

    private TextField campo(String placeholder) {
        TextField tf = new TextField();
        tf.setPromptText(placeholder);
        tf.setPrefHeight(38);
        tf.setStyle(estiloInput());
        return tf;
    }

    private Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.setFont(Font.font("System", FontWeight.BOLD, 12));
        l.setTextFill(Color.web("#4C1D95"));
        return l;
    }

    private VBox panelSeccion(String titulo, javafx.scene.Node contenido) {
        Label lbl = new Label(titulo);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 13));
        lbl.setTextFill(Color.web("#4C1D95"));
        VBox box = new VBox(10, lbl, contenido);
        box.setPadding(new Insets(14));
        box.setBackground(new Background(new BackgroundFill(
                Color.WHITE, new CornerRadii(8), Insets.EMPTY)));
        box.setStyle("-fx-border-color: #C084FC; -fx-border-radius: 8;");
        return box;
    }

    private void feedback(Label lbl, String msg, boolean error) {
        lbl.setText(msg);
        lbl.setTextFill(Color.web(error ? "#DC2626" : "#059669"));
    }

    private String estiloInput() {
        return """
                -fx-background-color: white;
                -fx-border-color: #C084FC;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                -fx-padding: 6 10;
                -fx-font-size: 13px;
                """;
    }

    private String estiloBoton(String color) {
        return "-fx-background-color: " + color + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 8;"
                + "-fx-cursor: hand;";
    }
}