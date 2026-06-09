package com.piedraazul.ui.usuarios;

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

public class CuentaApp extends Application {

    private final Long   usuarioId;
    private final String nombreActual;
    private final String emailActual;

    private final HttpClient   http   = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public CuentaApp(Long usuarioId, String nombreActual, String emailActual) {
        this.usuarioId    = usuarioId;
        this.nombreActual = nombreActual;
        this.emailActual  = emailActual;
    }

    // Constructor sin args requerido por JavaFX launch()
    public CuentaApp() {
        this.usuarioId    = null;
        this.nombreActual = "";
        this.emailActual  = "";
    }

    @Override
    public void start(Stage stage) {

        // ── Encabezado ──
        Label titulo = new Label("👤  Configuración de cuenta");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 20));
        titulo.setTextFill(Color.web("#4C1D95"));

        Label sub = new Label("Actualiza tu información personal");
        sub.setFont(Font.font("System", 13));
        sub.setTextFill(Color.web("#6B7280"));

        // ── Sección: datos personales ──
        Label secDatos = seccionLabel("Datos personales");

        Label lblEmail = etiqueta("Correo electrónico");
        TextField txtEmail = campo(emailActual);

        // ── Sección: cambio de contraseña ──
        Label secPass = seccionLabel("Cambiar contraseña");

        Label lblPassActual = etiqueta("Contraseña actual");
        PasswordField txtPassActual = new PasswordField();
        txtPassActual.setPromptText("Ingresa tu contraseña actual");
        txtPassActual.setPrefHeight(40);
        txtPassActual.setStyle(campoEstilo());

        Label lblPassNueva = etiqueta("Nueva contraseña");
        PasswordField txtPassNueva = new PasswordField();
        txtPassNueva.setPromptText("Mínimo 6 caracteres");
        txtPassNueva.setPrefHeight(40);
        txtPassNueva.setStyle(campoEstilo());

        Label lblPassConfirm = etiqueta("Confirmar nueva contraseña");
        PasswordField txtPassConfirm = new PasswordField();
        txtPassConfirm.setPromptText("Repite la nueva contraseña");
        txtPassConfirm.setPrefHeight(40);
        txtPassConfirm.setStyle(campoEstilo());

        // ── Feedback ──
        Label lblFeedback = new Label();
        lblFeedback.setFont(Font.font("System", 13));
        lblFeedback.setWrapText(true);

        // ── Botón guardar ──
        Button btnGuardar = new Button("Guardar cambios");
        btnGuardar.setPrefWidth(Double.MAX_VALUE);
        btnGuardar.setPrefHeight(44);
        btnGuardar.setFont(Font.font("System", FontWeight.BOLD, 14));
        btnGuardar.setStyle(estiloBoton("#7B2FBE"));
        btnGuardar.setOnMouseEntered(e -> btnGuardar.setStyle(estiloBoton("#6D28D9")));
        btnGuardar.setOnMouseExited(e -> btnGuardar.setStyle(estiloBoton("#7B2FBE")));

        btnGuardar.setOnAction(e -> {
            lblFeedback.setText("");

            String nuevoEmail  = txtEmail.getText().trim();
            String passActual  = txtPassActual.getText();
            String passNueva   = txtPassNueva.getText();
            String passConfirm = txtPassConfirm.getText();

            // Validaciones

            // Si quiere cambiar contraseña, validar que los campos estén completos
            boolean cambiaPass = !passNueva.isBlank() || !passActual.isBlank();
            if (cambiaPass) {

                if (passActual.isBlank()) {
                    feedback(lblFeedback, "✗ Ingresa tu contraseña actual.", false);
                    return;
                }
                if (passNueva.length() < 6) {
                    feedback(lblFeedback, "✗ La nueva contraseña debe tener al menos 6 caracteres.", false);
                    return;
                }
                if (!passNueva.equals(passConfirm)) {
                    feedback(lblFeedback, "✗ Las contraseñas no coinciden.", false);
                    return;
                }
            }

            try {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("email",  nuevoEmail);
                if (cambiaPass) {
                    body.put("nuevaPassword", passNueva);
                }

                String url = "http://localhost:8080/api/auth/usuarios/"
                        + usuarioId + "/perfil";

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer "
                                + com.piedraazul.ui.app.PiedraAzulApp.getToken())
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(
                                mapper.writeValueAsString(body)))
                        .build();

                HttpResponse<String> resp =
                        http.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() == 200) {
                    feedback(lblFeedback, "✓ Perfil actualizado correctamente.", true);
                    // Limpiar campos de contraseña tras éxito
                    txtPassActual.clear();
                    txtPassNueva.clear();
                    txtPassConfirm.clear();
                } else {
                    Map<?, ?> err = mapper.readValue(resp.body(), Map.class);
                    Object errorMsg = err.get("error");
                    feedback(lblFeedback,
                            "✗ " + (errorMsg != null ? errorMsg.toString() : "Error al actualizar."),
                            false);
                }
            } catch (Exception ex) {
                feedback(lblFeedback, "✗ Error de conexión con el servidor.", false);
            }
        });

        // ── Layout ──
        VBox form = new VBox(10,
                secDatos,
                lblEmail,    txtEmail,
                new Separator(),
                secPass,
                lblPassActual,  txtPassActual,
                lblPassNueva,   txtPassNueva,
                lblPassConfirm, txtPassConfirm,
                lblFeedback,
                btnGuardar
        );
        form.setPadding(new Insets(24));
        form.setMaxWidth(460);
        form.setStyle("""
                -fx-background-color: white;
                -fx-border-color: #E5E7EB;
                -fx-border-radius: 10;
                -fx-background-radius: 10;
                """);

        VBox root = new VBox(20, new VBox(4, titulo, sub), form);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #F5F3FF;");

        stage.setTitle("Configuración de cuenta - Piedra Azul");
        stage.setScene(new Scene(root, 520, 620));
        stage.setResizable(false);
        stage.show();
    }

    // ── Helpers ──────────────────────────────────────
    private void feedback(Label lbl, String msg, boolean ok) {
        lbl.setText(msg);
        lbl.setTextFill(ok ? Color.web("#059669") : Color.web("#DC2626"));
    }

    private Label seccionLabel(String texto) {
        Label lbl = new Label(texto);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 14));
        lbl.setTextFill(Color.web("#4C1D95"));
        return lbl;
    }

    private Label etiqueta(String texto) {
        Label lbl = new Label(texto);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 12));
        lbl.setTextFill(Color.web("#374151"));
        return lbl;
    }

    private TextField campo(String valorInicial) {
        TextField tf = new TextField(valorInicial);
        tf.setPrefHeight(40);
        tf.setStyle(campoEstilo());
        return tf;
    }

    private String campoEstilo() {
        return """
                -fx-background-color: white;
                -fx-border-color: #C084FC;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                -fx-padding: 8 12;
                -fx-font-size: 13px;
                """;
    }

    private String estiloBoton(String color) {
        return "-fx-background-color: " + color + ";"
                + "-fx-text-fill: white; -fx-font-weight: bold;"
                + "-fx-background-radius: 8; -fx-cursor: hand;";
    }

    public static void main(String[] args) { launch(); }
}