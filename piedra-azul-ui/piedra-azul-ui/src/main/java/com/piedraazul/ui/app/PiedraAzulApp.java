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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.effect.DropShadow;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class PiedraAzulApp extends Application {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private static String tokenSesion = null;
    public static String getToken() { return tokenSesion; }

    @Override
    public void start(Stage stage) {

        // --- PANEL IZQUIERDO ---
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

        var url = getClass().getResource("/imagenes/logo.png");

        System.out.println("URL = " + url);

        if (url == null) {
            throw new RuntimeException("NO SE ENCONTRO EL LOGO");
        }

        Image logo = new Image(url.toExternalForm());

        ImageView icono = new ImageView(logo);
        icono.setFitWidth(140);
        icono.setFitHeight(140);
        icono.setPreserveRatio(true);
        icono.setSmooth(true);

        DropShadow glow = new DropShadow();
        glow.setRadius(20);
        glow.setSpread(0.3);
        glow.setColor(Color.rgb(255,255,255,0.4));

        icono.setEffect(glow);

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

        // ---- PANEL DERECHO ----
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
        txtUsuario.setPromptText("Número de documento (cédula o T.I.)");
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
                    Long pacienteId = data.get("pacienteId") != null
                            ? Long.parseLong(data.get("pacienteId").toString()) : null;
                    lblError.setText("");
                    Long medicoId = data.get("medicoId") != null
                            ? Long.parseLong(data.get("medicoId").toString()) : null;
                    Long usuarioId = data.get("usuarioId") != null
                            ? Long.parseLong(data.get("usuarioId").toString()) : null;
                    tokenSesion = (String) data.get("token");
                    abrirMenuPrincipal(stage, rol, nombre, pacienteId, medicoId, usuarioId);
                } else {
                    lblError.setText("Usuario o contraseña incorrectos.");
                }
            } catch (Exception ex) {
                lblError.setText("Error de conexión con el servidor.");
            }
        });

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

    // ---- MENÚ SEGÚN ROL -----
    private void abrirMenuPrincipal(Stage stage, String rol, String nombre, Long pacienteId, Long medicoId, Long usuarioId) {
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
                        crearBotonMenu("🔍  Auditoría", () ->
                                new com.piedraazul.ui.auditoria.AuditoriaApp().start(new Stage())),
                        crearBotonMenu("📊  Reportes y Estadísticas", () ->
                                new com.piedraazul.ui.reportes.ReportesApp().start(new Stage())),
                        crearBotonMenu("➕  Registrar médico / agendador", () ->
                                new com.piedraazul.ui.medico.RegistroPersonalApp().abrir()),
                        crearBotonMenu("👤  Mi cuenta", () ->
                                new com.piedraazul.ui.usuarios.CuentaApp(usuarioId, nombre, "")
                                        .start(new Stage()))
                );
            }
            case "MEDICO_TERAPISTA" -> {
                menu.getChildren().addAll(
                        crearBotonMenu("📅  Agendar Citas", () ->
                                new com.piedraazul.ui.agenda.AgendaApp().start(new Stage())),
                        crearBotonMenu("📋  Historial Clínico", () ->
                                new com.piedraazul.ui.historial.HistorialApp(medicoId, nombre).start(new Stage())),
                        crearBotonMenu("📊  Mi Disponibilidad", () ->
                                new com.piedraazul.ui.medico.ConfiguracionMedicoApp(medicoId)
                                        .start(new Stage())),
                        crearBotonMenu("👤  Mi cuenta", () ->
                                new com.piedraazul.ui.usuarios.CuentaApp(usuarioId, nombre, "")
                                        .start(new Stage()))
                );
            }
            case "AGENDADOR" -> {
                menu.getChildren().addAll(
                        crearBotonMenu("\uD83D\uDCC1  Gestión de Pacientes", () ->
                                new com.piedraazul.ui.pacientes.PacienteApp().start(new Stage())),
                        crearBotonMenu("📅  Agenda de Citas", () ->
                                new com.piedraazul.ui.agenda.AgendaApp().start(new Stage())),
                        crearBotonMenu("👤  Mi cuenta", () ->
                                new com.piedraazul.ui.usuarios.CuentaApp(usuarioId, nombre, "")
                                        .start(new Stage()))
                );
            }
            case "PACIENTE" -> {
                menu.getChildren().addAll(
                        crearBotonMenu("📅  Mis Citas", () ->
                                new com.piedraazul.ui.agenda.AgendaPacienteApp(pacienteId)
                                        .start(new Stage())),
                        crearBotonMenu("👤  Mi cuenta", () ->
                                new com.piedraazul.ui.usuarios.CuentaApp(usuarioId, nombre, "")
                                        .start(new Stage()))
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
        btnCerrar.setOnAction(e -> {
            tokenSesion = null;
            start(stage);
        });
        menu.getChildren().add(btnCerrar);

        stage.setScene(new Scene(menu, 700, 460));
    }

    // ----- OLVIDÓ CONTRASEÑA -----
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

        Text instruccion = new Text("Ingresa tu número de documento y\ntu nueva contraseña.");
        instruccion.setFont(Font.font("System", 13));
        instruccion.setFill(Color.web("#6B7280"));

        Label lblDoc = etiqueta("Número de documento");
        TextField txtDoc = crearCampo("Ingrese su documento");

        Label lblNuevaPass = etiqueta("Nueva contraseña");
        PasswordField txtNuevaPass = new PasswordField();
        txtNuevaPass.setPromptText("Nueva contraseña");
        txtNuevaPass.setPrefHeight(38);
        txtNuevaPass.setStyle(campoEstilo());

        Button btnEnviar = new Button("Actualizar contraseña");
        btnEnviar.setPrefWidth(Double.MAX_VALUE);
        btnEnviar.setPrefHeight(42);
        btnEnviar.setFont(Font.font("System", FontWeight.BOLD, 14));
        btnEnviar.setStyle(botonEstilo("#7B2FBE"));

        Label lblFeedback = new Label();
        lblFeedback.setFont(Font.font("System", 13));
        lblFeedback.setWrapText(true);

        btnEnviar.setOnAction(e -> {
            if (txtDoc.getText().isBlank() || txtNuevaPass.getText().isBlank()) {
                lblFeedback.setText("Complete todos los campos.");
                lblFeedback.setTextFill(Color.web("#DC2626"));
            } else {
                lblFeedback.setText("✓ Funcionalidad en desarrollo.");
                lblFeedback.setTextFill(Color.web("#059669"));
                btnEnviar.setDisable(true);
            }
        });

        root.getChildren().addAll(titulo, instruccion,
                lblDoc, txtDoc, lblNuevaPass, txtNuevaPass,
                btnEnviar, lblFeedback);
        ventana.setTitle("Recuperar Contraseña");
        ventana.setScene(new Scene(root, 420, 320));
        ventana.show();
    }

    // ---- REGISTRO -----
    private void abrirRegistro() {
        Stage ventana = new Stage();

        ComboBox<String> cbTipo = new ComboBox<>();
        cbTipo.getItems().addAll(
                "Paciente nuevo",
                "Ya soy paciente, quiero crear mi cuenta");

        // -- Campos para paciente nuevo (separados) --
        TextField txtNombrePaciente    = crearCampo("Nombre *");
        TextField txtApellidoPaciente  = crearCampo("Apellido *");
        TextField txtEmailPaciente     = crearCampo("Email *");
        TextField txtDocumentoPaciente = crearCampo("Número de documento *");
        TextField txtTelefonoPaciente  = crearCampo("Teléfono *");
        ComboBox<String> cbGenero = new ComboBox<>();
        cbGenero.getItems().addAll("HOMBRE", "MUJER", "OTRO");
        cbGenero.setPromptText("Género *");
        cbGenero.setPrefWidth(Double.MAX_VALUE);
        DatePicker dpFechaNacimiento = new DatePicker();
        dpFechaNacimiento.setPromptText("Fecha de nacimiento *");
        dpFechaNacimiento.setPrefWidth(Double.MAX_VALUE);
        TextField txtEps = crearCampo("EPS (opcional)");
        PasswordField txtPasswordPaciente = new PasswordField();
        txtPasswordPaciente.setPromptText("Contraseña *");
        txtPasswordPaciente.setPrefHeight(38);
        txtPasswordPaciente.setStyle(campoEstilo());

        // --- Campos para paciente existente ---
        TextField txtDocumentoExistente = crearCampo("Número de documento *");
        TextField txtEmailExistente     = crearCampo("Email (opcional)");
        PasswordField txtPasswordExistente = new PasswordField();
        txtPasswordExistente.setPromptText("Contraseña *");
        txtPasswordExistente.setPrefHeight(38);
        txtPasswordExistente.setStyle(campoEstilo());

        // --- Paneles ---
        VBox camposPacienteNuevo = new VBox(8,
                etiqueta("Nombre *"), txtNombrePaciente,
                etiqueta("Apellido *"), txtApellidoPaciente,
                etiqueta("Email (opcional)"), txtEmailPaciente,
                etiqueta("Documento *"), txtDocumentoPaciente,
                etiqueta("Teléfono *"), txtTelefonoPaciente,
                etiqueta("Género *"), cbGenero,
                etiqueta("Fecha de nacimiento *"), dpFechaNacimiento,
                etiqueta("EPS (opcional)"), txtEps,
                etiqueta("Contraseña *"), txtPasswordPaciente);
        camposPacienteNuevo.setVisible(false);
        camposPacienteNuevo.setManaged(false);

        VBox camposPacienteExistente = new VBox(8,
                etiqueta("Documento *"), txtDocumentoExistente,
                etiqueta("Email (opcional)"), txtEmailExistente,
                etiqueta("Contraseña *"), txtPasswordExistente);
        camposPacienteExistente.setVisible(false);
        camposPacienteExistente.setManaged(false);

        cbTipo.setOnAction(e -> {
            String tipo = cbTipo.getValue();
            camposPacienteNuevo.setVisible(tipo.equals("Paciente nuevo"));
            camposPacienteNuevo.setManaged(tipo.equals("Paciente nuevo"));
            camposPacienteExistente.setVisible(
                    tipo.equals("Ya soy paciente, quiero crear mi cuenta"));
            camposPacienteExistente.setManaged(
                    tipo.equals("Ya soy paciente, quiero crear mi cuenta"));
        });

        cbTipo.setValue("Paciente nuevo");
        camposPacienteNuevo.setVisible(true);
        camposPacienteNuevo.setManaged(true);

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

                // ── Validar y construir url + body según el tipo ──
                if (tipo == null) {
                    lblFeedback.setText("✗ Seleccione un tipo de registro");
                    lblFeedback.setTextFill(Color.web("#DC2626"));
                    return;
                } else if (tipo.equals("Paciente nuevo")) {
                    if (txtNombrePaciente.getText().isBlank()
                            || txtApellidoPaciente.getText().isBlank()
                            || txtDocumentoPaciente.getText().isBlank()
                            || txtTelefonoPaciente.getText().isBlank()
                            || cbGenero.getValue() == null
                            || dpFechaNacimiento.getValue() == null
                            || txtPasswordPaciente.getText().isBlank()) {
                        lblFeedback.setText("✗ Complete todos los campos obligatorios (*)");
                        lblFeedback.setTextFill(Color.web("#DC2626"));
                        return;
                    }
                    url = "http://localhost:8080/api/auth/registro/paciente-nuevo";
                    body.put("nombre",          txtNombrePaciente.getText().trim());
                    body.put("apellido",        txtApellidoPaciente.getText().trim());
                    body.put("email",           txtEmailPaciente.getText().isBlank()
                            ? null : txtEmailPaciente.getText().trim());
                    body.put("numeroDocumento", txtDocumentoPaciente.getText().trim());
                    body.put("telefono",        txtTelefonoPaciente.getText().trim());
                    body.put("genero",          cbGenero.getValue());
                    body.put("eps",             txtEps.getText().isBlank()
                            ? null : txtEps.getText().trim());
                    body.put("fechaNacimiento", dpFechaNacimiento.getValue().toString());
                    body.put("username",        txtDocumentoPaciente.getText().trim());
                    body.put("password",        txtPasswordPaciente.getText().trim());
                } else {
                    if (txtDocumentoExistente.getText().isBlank()
                            || txtPasswordExistente.getText().isBlank()) {
                        lblFeedback.setText("✗ Complete todos los campos obligatorios (*)");
                        lblFeedback.setTextFill(Color.web("#DC2626"));
                        return;
                    }
                    url = "http://localhost:8080/api/auth/registro/paciente-existente";
                    body.put("numeroDocumento", txtDocumentoExistente.getText().trim());
                    body.put("username",        txtDocumentoExistente.getText().trim());
                    body.put("password",        txtPasswordExistente.getText().trim());
                    body.put("email",           txtEmailExistente.getText().isBlank()
                            ? null : txtEmailExistente.getText().trim());
                }

                // ── Enviar petición con token si existe ──
                String token = PiedraAzulApp.getToken();
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json");
                if (token != null) {
                    builder.header("Authorization", "Bearer " + token);
                }
                HttpRequest req = builder
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
                    Map<String, Object> err = mapper.readValue(resp.body(),
                            new com.fasterxml.jackson.core.type.TypeReference<>() {});
                    Object errorMsg = err.get("error");
                    lblFeedback.setText("✗ " + (errorMsg != null
                            ? errorMsg.toString() : "Error en el registro"));
                    lblFeedback.setTextFill(Color.web("#DC2626"));
                }
            } catch (Exception ex) {
                lblFeedback.setText("✗ Error de conexión: " + ex.getMessage());
                lblFeedback.setTextFill(Color.web("#DC2626"));
            }
        });
        VBox contenido = new VBox(12,
                etiqueta("Tipo de registro"), cbTipo,
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

    // ---- UTILIDADES ----
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