package com.piedraazul.ui.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class PiedraAzulApp extends Application {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void start(Stage stage) {

        // ── PANEL IZQUIERDO ──────────────────────────────────────
        VBox panelIzquierdo = new VBox(20);
        panelIzquierdo.setAlignment(Pos.CENTER);
        panelIzquierdo.setPrefWidth(320);
        panelIzquierdo.setPadding(new Insets(40));

        LinearGradient gradiente = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#7B2FBE")),
                new Stop(1, Color.web("#C084FC")));
        panelIzquierdo.setBackground(new Background(
                new BackgroundFill(gradiente, CornerRadii.EMPTY, Insets.EMPTY)));

        Text icono = new Text("✦");
        icono.setFont(Font.font("System", FontWeight.BOLD, 60));
        icono.setFill(Color.WHITE);

        Text nombreClinica = new Text("Clínica");
        nombreClinica.setFont(Font.font("System", FontWeight.BOLD, 28));
        nombreClinica.setFill(Color.WHITE);

        Text nombreClinica2 = new Text("Piedra Azul");
        nombreClinica2.setFont(Font.font("System", FontWeight.BOLD, 28));
        nombreClinica2.setFill(Color.WHITE);

        Text slogan = new Text("Medicina Alternativa\ny Bienestar Natural");
        slogan.setFont(Font.font("System", 14));
        slogan.setFill(Color.web("#EDE9FE"));
        slogan.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Rectangle separador = new Rectangle(60, 3);
        separador.setFill(Color.web("#DDD6FE"));

        panelIzquierdo.getChildren().addAll(
                icono, nombreClinica, nombreClinica2, separador, slogan);

        // ── PANEL DERECHO ─────────────────────────────────────────
        VBox panelDerecho = new VBox(16);
        panelDerecho.setAlignment(Pos.CENTER_LEFT);
        panelDerecho.setPadding(new Insets(50, 50, 50, 50));
        panelDerecho.setPrefWidth(380);
        panelDerecho.setBackground(new Background(new BackgroundFill(
                Color.web("#FAFAFA"), CornerRadii.EMPTY, Insets.EMPTY)));

        Text bienvenido = new Text("Bienvenido");
        bienvenido.setFont(Font.font("System", FontWeight.BOLD, 26));
        bienvenido.setFill(Color.web("#4C1D95"));

        Text subtitulo = new Text("Ingresa tus credenciales para continuar");
        subtitulo.setFont(Font.font("System", 13));
        subtitulo.setFill(Color.web("#6B7280"));

        Label lblUsuario = new Label("Usuario");
        lblUsuario.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblUsuario.setTextFill(Color.web("#4C1D95"));

        TextField txtUsuario = new TextField();
        txtUsuario.setPromptText("Ingrese su usuario");
        txtUsuario.setPrefHeight(40);
        txtUsuario.setStyle(campoEstilo());

        Label lblContrasena = new Label("Contraseña");
        lblContrasena.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblContrasena.setTextFill(Color.web("#4C1D95"));

        PasswordField txtContrasena = new PasswordField();
        txtContrasena.setPromptText("Ingrese su contraseña");
        txtContrasena.setPrefHeight(40);
        txtContrasena.setStyle(campoEstilo());

        Button btnLogin = new Button("Iniciar Sesión");
        btnLogin.setPrefWidth(Double.MAX_VALUE);
        btnLogin.setPrefHeight(44);
        btnLogin.setFont(Font.font("System", FontWeight.BOLD, 15));
        btnLogin.setStyle(botonEstilo("#7B2FBE"));
        btnLogin.setOnMouseEntered(e -> btnLogin.setStyle(botonEstilo("#6D28D9")));
        btnLogin.setOnMouseExited(e -> btnLogin.setStyle(botonEstilo("#7B2FBE")));

        Label lblError = new Label();
        lblError.setTextFill(Color.web("#DC2626"));
        lblError.setFont(Font.font("System", 12));
        lblError.setWrapText(true);

        btnLogin.setOnAction(e -> {
            String user = txtUsuario.getText().trim();
            String pass = txtContrasena.getText().trim();
            if (user.isEmpty() || pass.isEmpty()) {
                lblError.setText("Por favor completa todos los campos.");
                return;
            }
            try {
                Map<String, String> body = Map.of(
                        "username", user, "password", pass);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                mapper.writeValueAsString(body)))
                        .build();
                HttpResponse<String> resp =
                        httpClient.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() == 200) {
                    Map<?, ?> data = mapper.readValue(resp.body(), Map.class);
                    String rol = (String) data.get("rol");
                    String nombre = (String) data.get("nombre");
                    lblError.setText("");
                    abrirMenuPrincipal(stage, rol, nombre);
                } else {
                    lblError.setText("Usuario o contraseña incorrectos.");
                }
            } catch (Exception ex) {
                lblError.setText("Error de conexión con el servidor.");
            }
        });

        // Links
        Hyperlink linkOlvide = new Hyperlink("¿Olvidó su contraseña?");
        linkOlvide.setStyle("-fx-text-fill: #7B2FBE; -fx-font-size: 12px; -fx-border-color: transparent;");
        linkOlvide.setOnAction(e -> abrirOlvideContrasena());

        Hyperlink linkRegistro = new Hyperlink("Registrarse");
        linkRegistro.setStyle("-fx-text-fill: #7B2FBE; -fx-font-size: 12px; -fx-border-color: transparent;");
        linkRegistro.setOnAction(e -> abrirRegistro());

        HBox links = new HBox(20, linkOlvide, linkRegistro);
        links.setAlignment(Pos.CENTER);

        Text pie = new Text("© 2026 Clínica Piedra Azul · Medicina Alternativa");
        pie.setFont(Font.font("System", 11));
        pie.setFill(Color.web("#9CA3AF"));

        panelDerecho.getChildren().addAll(
                bienvenido, subtitulo,
                lblUsuario, txtUsuario,
                lblContrasena, txtContrasena,
                btnLogin, lblError, links, pie);

        HBox root = new HBox(panelIzquierdo, panelDerecho);
        Scene scene = new Scene(root, 700, 440);
        stage.setTitle("Clínica Piedra Azul - Sistema de Acceso");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    // ── MENÚ SEGÚN ROL ────────────────────────────────────────────
    private void abrirMenuPrincipal(Stage stage, String rol, String nombre) {
        VBox menu = new VBox(16);
        menu.setAlignment(Pos.CENTER);
        menu.setPadding(new Insets(40));
        menu.setBackground(new Background(new BackgroundFill(
                Color.web("#F5F3FF"), CornerRadii.EMPTY, Insets.EMPTY)));

        Text titulo = new Text("Clínica Piedra Azul");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 24));
        titulo.setFill(Color.web("#4C1D95"));

        Text saludo = new Text("Bienvenido, " + nombre + "  ·  " + rol);
        saludo.setFont(Font.font("System", 13));
        saludo.setFill(Color.web("#6B7280"));

        menu.getChildren().addAll(titulo, saludo);

        switch (rol) {
            case "ADMINISTRADOR" -> {
                menu.getChildren().addAll(
                        crearBotonMenu("👤  Gestión de Pacientes", () ->
                                new com.piedraazul.ui.pacientes.PacienteApp().start(new Stage())),
                        crearBotonMenu("📅  Agenda de Citas", () ->
                                new com.piedraazul.ui.agenda.AgendaApp().start(new Stage())),
                        crearBotonMenu("📋  Historial Clínico", () ->
                                new com.piedraazul.ui.historial.HistorialApp().start(new Stage())),
                        crearBotonMenu("🔍  Auditoría", () ->
                                new com.piedraazul.ui.auditoria.AuditoriaApp().start(new Stage()))
                );
            }
            case "MEDICO_TERAPISTA" -> {
                menu.getChildren().addAll(
                        crearBotonMenu("📅  Mis Citas", () ->
                                new com.piedraazul.ui.agenda.AgendaApp().start(new Stage())),
                        crearBotonMenu("📋  Historial Clínico", () ->
                                new com.piedraazul.ui.historial.HistorialApp().start(new Stage()))
                );
            }
            case "AGENDADOR" -> {
                menu.getChildren().addAll(
                        crearBotonMenu("👤  Gestión de Pacientes", () ->
                                new com.piedraazul.ui.pacientes.PacienteApp().start(new Stage())),
                        crearBotonMenu("📅  Agenda de Citas", () ->
                                new com.piedraazul.ui.agenda.AgendaApp().start(new Stage()))
                );
            }
            case "PACIENTE" -> {
                menu.getChildren().addAll(
                        crearBotonMenu("📅  Mis Citas", () ->
                                new com.piedraazul.ui.agenda.AgendaApp().start(new Stage())),
                        crearBotonMenu("📋  Mi Historial", () ->
                                new com.piedraazul.ui.historial.HistorialApp().start(new Stage()))
                );
            }
        }

        Button btnCerrar = new Button("Cerrar Sesión");
        btnCerrar.setPrefWidth(200);
        btnCerrar.setPrefHeight(38);
        btnCerrar.setFont(Font.font("System", FontWeight.BOLD, 13));
        btnCerrar.setStyle("""
                -fx-background-color: transparent;
                -fx-text-fill: #7B2FBE;
                -fx-border-color: #7B2FBE;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                -fx-cursor: hand;
                """);
        btnCerrar.setOnAction(e -> start(stage));
        menu.getChildren().add(btnCerrar);

        stage.setScene(new Scene(menu, 700, 460));
    }

    // ── OLVIDÓ CONTRASEÑA ─────────────────────────────────────────
    private void abrirOlvideContrasena() {
        Stage ventana = new Stage();
        VBox root = new VBox(18);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(40));
        root.setBackground(new Background(new BackgroundFill(
                Color.web("#F5F3FF"), CornerRadii.EMPTY, Insets.EMPTY)));

        Text titulo = new Text("Recuperar Contraseña");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 22));
        titulo.setFill(Color.web("#4C1D95"));

        Text instruccion = new Text("Ingresa tu email y te enviaremos\nlas instrucciones de recuperación.");
        instruccion.setFont(Font.font("System", 13));
        instruccion.setFill(Color.web("#6B7280"));

        Label lblEmail = etiqueta("Correo electrónico");
        TextField txtEmail = crearCampo("ejemplo@correo.com");

        Button btnEnviar = new Button("Enviar instrucciones");
        btnEnviar.setPrefWidth(Double.MAX_VALUE);
        btnEnviar.setPrefHeight(42);
        btnEnviar.setFont(Font.font("System", FontWeight.BOLD, 14));
        btnEnviar.setStyle(botonEstilo("#7B2FBE"));

        Label lblFeedback = new Label();
        lblFeedback.setFont(Font.font("System", 13));
        lblFeedback.setWrapText(true);

        btnEnviar.setOnAction(e -> {
            String email = txtEmail.getText().trim();
            if (email.isEmpty() || !email.contains("@")) {
                lblFeedback.setText("Ingresa un email válido.");
                lblFeedback.setTextFill(Color.web("#DC2626"));
            } else {
                lblFeedback.setText("✓ Se han enviado las instrucciones a " + email);
                lblFeedback.setTextFill(Color.web("#059669"));
                btnEnviar.setDisable(true);
            }
        });

        root.getChildren().addAll(titulo, instruccion, lblEmail, txtEmail, btnEnviar, lblFeedback);
        ventana.setTitle("Recuperar Contraseña");
        ventana.setScene(new Scene(root, 420, 300));
        ventana.show();
    }

    // ── REGISTRO ──────────────────────────────────────────────────
    private void abrirRegistro() {
        Stage ventana = new Stage();

        // Tipo de registro principal
        ComboBox<String> cbTipo = new ComboBox<>();
        cbTipo.getItems().addAll(
                "Personal médico / Agendador",
                "Paciente nuevo",
                "Ya soy paciente, quiero crear mi cuenta");
        cbTipo.setValue("Personal médico / Agendador");
        cbTipo.setPrefWidth(Double.MAX_VALUE);

        // Campos comunes
        TextField txtNombre    = crearCampo("Nombre");
        TextField txtApellido  = crearCampo("Apellido");
        TextField txtEmail     = crearCampo("Email");
        TextField txtDocumento = crearCampo("Número de documento");
        TextField txtTelefono  = crearCampo("Teléfono");

        // Campos solo para paciente nuevo
        ComboBox<String> cbGenero = new ComboBox<>();
        cbGenero.getItems().addAll("HOMBRE", "MUJER", "OTRO");
        cbGenero.setPromptText("Género *");
        cbGenero.setPrefWidth(Double.MAX_VALUE);

        TextField txtEps = crearCampo("EPS (opcional)");

        DatePicker dpFechaNacimientoPaciente = new DatePicker();
        dpFechaNacimientoPaciente.setPromptText("Fecha de nacimiento *");
        dpFechaNacimientoPaciente.setPrefWidth(Double.MAX_VALUE);

        PasswordField txtPasswordPaciente = new PasswordField();
        txtPasswordPaciente.setPromptText("Contraseña *");
        txtPasswordPaciente.setPrefHeight(38);
        txtPasswordPaciente.setStyle(campoEstilo());

        TextField txtUsernamePaciente = crearCampo("Username *");

        // Campos para paciente existente
        TextField txtDocumentoExistente = crearCampo("Número de documento *");
        TextField txtUsernameExistente  = crearCampo("Username *");
        TextField txtEmailExistente     = crearCampo("Email");

        PasswordField txtPasswordExistente = new PasswordField();
        txtPasswordExistente.setPromptText("Contraseña *");
        txtPasswordExistente.setPrefHeight(38);
        txtPasswordExistente.setStyle(campoEstilo());

        // Campos para personal médico
        TextField txtUsername  = crearCampo("Username *");
        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Contraseña *");
        txtPassword.setPrefHeight(38);
        txtPassword.setStyle(campoEstilo());

        ComboBox<String> cbRol = new ComboBox<>();
        cbRol.getItems().addAll("MEDICO_TERAPISTA", "AGENDADOR");
        cbRol.setPromptText("Rol *");
        cbRol.setPrefWidth(Double.MAX_VALUE);

        // Paneles por tipo
        VBox camposPersonal = new VBox(8,
                etiqueta("Nombre"), txtNombre,
                etiqueta("Apellido"), txtApellido,
                etiqueta("Email"), txtEmail,
                etiqueta("Documento"), txtDocumento,
                etiqueta("Teléfono"), txtTelefono,
                etiqueta("Username"), txtUsername,
                etiqueta("Contraseña"), txtPassword,
                etiqueta("Rol"), cbRol);

        VBox camposPacienteNuevo = new VBox(8,
                etiqueta("Nombre"), txtNombre,
                etiqueta("Apellido"), txtApellido,
                etiqueta("Email"), txtEmail,
                etiqueta("Documento"), txtDocumento,
                etiqueta("Teléfono"), txtTelefono,
                etiqueta("Género"), cbGenero,
                etiqueta("Fecha de nacimiento *"), dpFechaNacimientoPaciente,
                etiqueta("EPS (opcional)"), txtEps,
                etiqueta("Username"), txtUsernamePaciente,
                etiqueta("Contraseña"), txtPasswordPaciente);
        camposPacienteNuevo.setVisible(false);
        camposPacienteNuevo.setManaged(false);

        VBox camposPacienteExistente = new VBox(8,
                etiqueta("Número de documento"), txtDocumentoExistente,
                etiqueta("Email (opcional)"), txtEmailExistente,
                etiqueta("Username"), txtUsernameExistente,
                etiqueta("Contraseña"), txtPasswordExistente);
        camposPacienteExistente.setVisible(false);
        camposPacienteExistente.setManaged(false);

        // Mostrar panel según tipo seleccionado
        cbTipo.setOnAction(e -> {
            String tipo = cbTipo.getValue();
            camposPersonal.setVisible(tipo.equals("Personal médico / Agendador"));
            camposPersonal.setManaged(tipo.equals("Personal médico / Agendador"));
            camposPacienteNuevo.setVisible(tipo.equals("Paciente nuevo"));
            camposPacienteNuevo.setManaged(tipo.equals("Paciente nuevo"));
            camposPacienteExistente.setVisible(
                    tipo.equals("Ya soy paciente, quiero crear mi cuenta"));
            camposPacienteExistente.setManaged(
                    tipo.equals("Ya soy paciente, quiero crear mi cuenta"));
        });

        Button btnRegistrar = new Button("Registrar");
        btnRegistrar.setPrefWidth(Double.MAX_VALUE);
        btnRegistrar.setPrefHeight(42);
        btnRegistrar.setFont(Font.font("System", FontWeight.BOLD, 14));
        btnRegistrar.setStyle(botonEstilo("#7B2FBE"));

        Label lblFeedback = new Label();
        lblFeedback.setFont(Font.font("System", 13));
        lblFeedback.setWrapText(true);

        btnRegistrar.setOnAction(e -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                String url;
                Map<String, Object> body = new java.util.LinkedHashMap<>();
                String tipo = cbTipo.getValue();

                if (tipo.equals("Personal médico / Agendador")) {
                    // Validar campos obligatorios
                    if (txtNombre.getText().isBlank() || txtApellido.getText().isBlank()
                            || txtUsername.getText().isBlank()
                            || txtPassword.getText().isBlank()
                            || cbRol.getValue() == null) {
                        lblFeedback.setText("✗ Complete todos los campos obligatorios (*)");
                        lblFeedback.setTextFill(Color.web("#DC2626"));
                        return;
                    }
                    url = "http://localhost:8080/api/auth/registro";
                    body.put("username", txtUsername.getText().trim());
                    body.put("password", txtPassword.getText().trim());
                    body.put("nombre", txtNombre.getText().trim()
                            + " " + txtApellido.getText().trim());
                    body.put("email", txtEmail.getText().trim());
                    body.put("rol", cbRol.getValue());

                } else if (tipo.equals("Paciente nuevo")) {
                    // Validar campos obligatorios
                    if (txtNombre.getText().isBlank() || txtApellido.getText().isBlank()
                            || txtDocumento.getText().isBlank()
                            || txtTelefono.getText().isBlank()
                            || cbGenero.getValue() == null
                            || dpFechaNacimientoPaciente.getValue() == null
                            || txtUsernamePaciente.getText().isBlank()
                            || txtPasswordPaciente.getText().isBlank()) {
                        lblFeedback.setText("✗ Complete todos los campos obligatorios (*)");
                        lblFeedback.setTextFill(Color.web("#DC2626"));
                        return;
                    }
                    url = "http://localhost:8080/api/auth/registro/paciente-nuevo";
                    body.put("nombre", txtNombre.getText().trim());
                    body.put("apellido", txtApellido.getText().trim());
                    body.put("email", txtEmail.getText().isBlank()
                            ? null : txtEmail.getText().trim());
                    body.put("numeroDocumento", txtDocumento.getText().trim());
                    body.put("telefono", txtTelefono.getText().trim());
                    body.put("genero", cbGenero.getValue());
                    body.put("eps", txtEps.getText().isBlank()
                            ? null : txtEps.getText().trim());
                    body.put("fechaNacimiento", dpFechaNacimientoPaciente.getValue().toString());
                    body.put("username", txtUsernamePaciente.getText().trim());
                    body.put("password", txtPasswordPaciente.getText().trim());

                } else {
                    // Paciente existente
                    if (txtDocumentoExistente.getText().isBlank()
                            || txtUsernameExistente.getText().isBlank()
                            || txtPasswordExistente.getText().isBlank()) {
                        lblFeedback.setText("✗ Complete todos los campos obligatorios (*)");
                        lblFeedback.setTextFill(Color.web("#DC2626"));
                        return;
                    }
                    url = "http://localhost:8080/api/auth/registro/paciente-existente";
                    body.put("numeroDocumento", txtDocumentoExistente.getText().trim());
                    body.put("username", txtUsernameExistente.getText().trim());
                    body.put("password", txtPasswordExistente.getText().trim());
                    body.put("email", txtEmailExistente.getText().isBlank()
                            ? null : txtEmailExistente.getText().trim());
                }

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                mapper.writeValueAsString(body)))
                        .build();
                HttpResponse<String> resp =
                        client.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() == 201) {
                    lblFeedback.setText("✓ Registro exitoso.");
                    lblFeedback.setTextFill(Color.web("#059669"));
                    btnRegistrar.setDisable(true);
                } else {
                    Map<?, ?> err = mapper.readValue(resp.body(), Map.class);
                    Object errorMsg = err.get("error");
                    lblFeedback.setText("✗ " + (errorMsg != null ? errorMsg.toString() : "Error en el registro"));
                    lblFeedback.setTextFill(Color.web("#DC2626"));
                }
            } catch (Exception ex) {
                lblFeedback.setText("✗ Error de conexión: " + ex.getMessage());
                lblFeedback.setTextFill(Color.web("#DC2626"));
            }
        });

        VBox contenido = new VBox(12,
                etiqueta("Tipo de registro"), cbTipo,
                camposPersonal,
                camposPacienteNuevo,
                camposPacienteExistente,
                btnRegistrar, lblFeedback);
        contenido.setPadding(new Insets(30));
        contenido.setBackground(new Background(new BackgroundFill(
                Color.web("#F5F3FF"), CornerRadii.EMPTY, Insets.EMPTY)));

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F5F3FF; -fx-background-color: #F5F3FF;");

        ventana.setTitle("Registro - Clínica Piedra Azul");
        ventana.setScene(new Scene(scroll, 440, 580));
        ventana.show();
    }

    // ── UTILIDADES ────────────────────────────────────────────────
    private Button crearBotonMenu(String texto, Runnable accion) {
        Button btn = new Button(texto);
        btn.setPrefWidth(300);
        btn.setPrefHeight(44);
        btn.setFont(Font.font("System", FontWeight.BOLD, 14));
        btn.setStyle(botonEstilo("#7B2FBE"));
        btn.setOnMouseEntered(e -> btn.setStyle(botonEstilo("#6D28D9")));
        btn.setOnMouseExited(e -> btn.setStyle(botonEstilo("#7B2FBE")));
        btn.setOnAction(e -> accion.run());
        return btn;
    }

    private TextField crearCampo(String placeholder) {
        TextField tf = new TextField();
        tf.setPromptText(placeholder);
        tf.setPrefHeight(38);
        tf.setStyle(campoEstilo());
        return tf;
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
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                -fx-padding: 8 12;
                -fx-font-size: 13px;
                """;
    }

    private String botonEstilo(String color) {
        return "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;";
    }

    public static void main(String[] args) {
        launch();
    }
}
