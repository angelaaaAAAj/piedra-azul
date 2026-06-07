package com.piedraazul.ui.historial;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class HistorialApp extends Application {

    private final Long medicoId;
    private final String nombreMedico;

    public HistorialApp(Long medicoId, String nombreMedico) {
        this.medicoId = medicoId;
        this.nombreMedico = nombreMedico != null ? nombreMedico : "";
    }

    public HistorialApp() {
        this.medicoId = null;
        this.nombreMedico = "";
    }

    @Override
    public void start(Stage stage) {

        // ── TÍTULO ──
        Label titulo = new Label("Historial Clínico - Piedra Azul");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 22));
        titulo.setTextFill(Color.web("#4C1D95"));

        Label subtitulo = new Label(nombreMedico.isBlank()
                ? "Consulta de historial"
                : "Dr/a. " + nombreMedico);
        subtitulo.setFont(Font.font("System", 13));
        subtitulo.setTextFill(Color.web("#6B7280"));

        // ── BÚSQUEDA ──
        TextField txtPacienteId = new TextField();
        txtPacienteId.setPromptText("ID del paciente");
        txtPacienteId.setStyle(campoEstilo());

        TextField txtCitaId = new TextField();
        txtCitaId.setPromptText("ID de la cita");
        txtCitaId.setStyle(campoEstilo());

        Button btnBuscarPaciente = boton("Buscar por paciente", "#7B2FBE");
        Button btnBuscarCita = boton("Buscar por cita", "#7B2FBE");

        Label lblFeedback = new Label();
        lblFeedback.setFont(Font.font("System", 12));
        lblFeedback.setWrapText(true);

        Label lblTotal = new Label("Registros: 0");
        lblTotal.setFont(Font.font("System", 12));
        lblTotal.setTextFill(Color.web("#6B7280"));

        GridPane busqueda = new GridPane();
        busqueda.setHgap(10);
        busqueda.setVgap(10);
        busqueda.add(etiqueta("Paciente ID:"), 0, 0);
        busqueda.add(txtPacienteId, 1, 0);
        busqueda.add(btnBuscarPaciente, 2, 0);
        busqueda.add(etiqueta("Cita ID:"), 0, 1);
        busqueda.add(txtCitaId, 1, 1);
        busqueda.add(btnBuscarCita, 2, 1);

        VBox seccionBusqueda = seccion("🔍  Buscar historial", busqueda);

        // ── TABLA HISTORIAL ──
        TableView<HistorialEntry> tabla = new TableView<>();
        tabla.setPlaceholder(new Label("Busque por paciente o cita"));
        tabla.setPrefHeight(200);

        TableColumn<HistorialEntry, Number> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> c.getValue().idProperty());
        colId.setPrefWidth(60);

        TableColumn<HistorialEntry, String> colFecha = new TableColumn<>("Fecha y Hora");
        colFecha.setCellValueFactory(c -> c.getValue().fechaRegistroProperty());
        colFecha.setPrefWidth(150);

        TableColumn<HistorialEntry, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(c -> c.getValue().tipoRegistroProperty());
        colTipo.setPrefWidth(110);

        TableColumn<HistorialEntry, String> colDesc = new TableColumn<>("Descripción del procedimiento");
        colDesc.setCellValueFactory(c -> c.getValue().descripcionProperty());
        colDesc.setPrefWidth(280);

        TableColumn<HistorialEntry, String> colProfesional = new TableColumn<>("Profesional");
        colProfesional.setCellValueFactory(c -> c.getValue().registradoPorProperty());
        colProfesional.setPrefWidth(140);

        tabla.getColumns().addAll(colId, colFecha, colTipo, colDesc, colProfesional);

        // ── TABLA REAGENDAMIENTOS ──
        Label lblReagendamientos = new Label("Historial de reagendamientos");
        lblReagendamientos.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblReagendamientos.setTextFill(Color.web("#4C1D95"));

        TableView<ReagendamientoEntry> tablaReagendamientos = new TableView<>();
        tablaReagendamientos.setPlaceholder(
                new Label("Sin reagendamientos — busque por ID de cita"));
        tablaReagendamientos.setPrefHeight(150);

        TableColumn<ReagendamientoEntry, String> colFechaAnt =
                new TableColumn<>("Fecha Anterior");
        colFechaAnt.setCellValueFactory(c -> c.getValue().fechaAnteriorProperty());
        colFechaAnt.setPrefWidth(160);

        TableColumn<ReagendamientoEntry, String> colFechaNueva =
                new TableColumn<>("Fecha Nueva");
        colFechaNueva.setCellValueFactory(c -> c.getValue().fechaNuevaProperty());
        colFechaNueva.setPrefWidth(160);

        TableColumn<ReagendamientoEntry, String> colMotivo =
                new TableColumn<>("Motivo");
        colMotivo.setCellValueFactory(c -> c.getValue().motivoProperty());
        colMotivo.setPrefWidth(200);

        TableColumn<ReagendamientoEntry, String> colResponsable =
                new TableColumn<>("Responsable");
        colResponsable.setCellValueFactory(c -> c.getValue().responsableProperty());
        colResponsable.setPrefWidth(130);

        tablaReagendamientos.getColumns().addAll(
                colFechaAnt, colFechaNueva, colMotivo, colResponsable);

        // ── FORMULARIO NUEVO REGISTRO (solo para médicos) ──
        ComboBox<String> cbTipo = new ComboBox<>();
        cbTipo.getItems().addAll("CONSULTA", "CONTROL", "PROCEDIMIENTO");
        cbTipo.setPromptText("Tipo de registro *");
        cbTipo.setMaxWidth(Double.MAX_VALUE);

        TextArea txtDescripcion = new TextArea();
        txtDescripcion.setPromptText(
                "Descripción del procedimiento o control realizado *");
        txtDescripcion.setPrefHeight(80);
        txtDescripcion.setWrapText(true);
        txtDescripcion.setStyle(campoEstilo());

        TextField txtCitaIdRegistro = new TextField();
        txtCitaIdRegistro.setPromptText("ID de la cita atendida *");
        txtCitaIdRegistro.setStyle(campoEstilo());

        TextField txtPacienteIdRegistro = new TextField();
        txtPacienteIdRegistro.setPromptText("ID del paciente *");
        txtPacienteIdRegistro.setStyle(campoEstilo());

        Button btnRegistrar = boton("💾 Registrar entrada clínica", "#2E7D32");

        Label lblFeedbackRegistro = new Label();
        lblFeedbackRegistro.setFont(Font.font("System", 12));
        lblFeedbackRegistro.setWrapText(true);

        GridPane formulario = new GridPane();
        formulario.setHgap(10);
        formulario.setVgap(10);
        formulario.add(etiqueta("Paciente ID *:"), 0, 0);
        formulario.add(txtPacienteIdRegistro, 1, 0);
        formulario.add(etiqueta("Cita ID *:"), 2, 0);
        formulario.add(txtCitaIdRegistro, 3, 0);
        formulario.add(etiqueta("Tipo *:"), 0, 1);
        formulario.add(cbTipo, 1, 1);
        formulario.add(etiqueta("Descripción *:"), 0, 2);
        formulario.add(txtDescripcion, 1, 2, 3, 1);
        formulario.add(btnRegistrar, 3, 3);
        formulario.add(lblFeedbackRegistro, 0, 3, 3, 1);

        // Solo mostrar formulario si es médico
        VBox seccionRegistro = seccion("📝  Registrar nueva entrada clínica", formulario);
        seccionRegistro.setVisible(medicoId != null);
        seccionRegistro.setManaged(medicoId != null);

        // ── CONTROLADOR ──
        HistorialController controller = new HistorialController();

        btnBuscarPaciente.setOnAction(e -> {
            try {
                long id = Long.parseLong(txtPacienteId.getText().trim());
                controller.cargarPorPacienteId(id, tabla, lblFeedback, lblTotal);
                tablaReagendamientos.setItems(FXCollections.observableArrayList());
                // Precargar pacienteId en el formulario
                txtPacienteIdRegistro.setText(String.valueOf(id));
            } catch (NumberFormatException ex) {
                lblFeedback.setText("✗ ID inválido");
                lblFeedback.setTextFill(Color.web("#DC2626"));
            }
        });

        btnBuscarCita.setOnAction(e -> {
            try {
                long id = Long.parseLong(txtCitaId.getText().trim());
                controller.cargarPorCitaId(id, tabla, lblFeedback, lblTotal);
                controller.cargarReagendamientos(id, tablaReagendamientos, lblFeedback);
                // Precargar citaId en el formulario
                txtCitaIdRegistro.setText(String.valueOf(id));
            } catch (NumberFormatException ex) {
                lblFeedback.setText("✗ ID inválido");
                lblFeedback.setTextFill(Color.web("#DC2626"));
            }
        });

        btnRegistrar.setOnAction(e -> {
            String pacIdStr = txtPacienteIdRegistro.getText().trim();
            String citaIdStr = txtCitaIdRegistro.getText().trim();
            String descripcion = txtDescripcion.getText().trim();
            String tipo = cbTipo.getValue();

            if (pacIdStr.isBlank() || citaIdStr.isBlank()
                    || descripcion.isBlank() || tipo == null) {
                lblFeedbackRegistro.setText("✗ Complete todos los campos obligatorios");
                lblFeedbackRegistro.setTextFill(Color.web("#DC2626"));
                return;
            }

            try {
                controller.registrarEntrada(
                        Long.parseLong(pacIdStr),
                        medicoId,
                        Long.parseLong(citaIdStr),
                        tipo,
                        descripcion,
                        nombreMedico,
                        lblFeedbackRegistro);

                // Recargar historial después de registrar
                controller.cargarPorPacienteId(
                        Long.parseLong(pacIdStr), tabla, lblFeedback, lblTotal);
                txtDescripcion.clear();
                cbTipo.setValue(null);
            } catch (NumberFormatException ex) {
                lblFeedbackRegistro.setText("✗ ID de paciente o cita inválido");
                lblFeedbackRegistro.setTextFill(Color.web("#DC2626"));
            }
        });

        // ── LAYOUT ──
        VBox root = new VBox(15,
                new VBox(4, titulo, subtitulo),
                seccionBusqueda,
                seccion("📋  Registros clínicos", new VBox(8, lblTotal, tabla)),
                seccion("🔄  Reagendamientos", new VBox(8, tablaReagendamientos)),
                seccionRegistro,
                lblFeedback);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #F5F3FF;");

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F5F3FF; -fx-background-color: #F5F3FF;");

        stage.setTitle("Historial Clínico - Piedra Azul");
        stage.setScene(new Scene(scroll, 1000, 700));
        stage.show();
    }

    private VBox seccion(String titulo, javafx.scene.Node contenido) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblTitulo.setTextFill(Color.web("#4C1D95"));
        VBox caja = new VBox(8, lblTitulo, contenido);
        caja.setPadding(new Insets(12));
        caja.setBackground(new Background(new BackgroundFill(
                Color.WHITE, new CornerRadii(8), Insets.EMPTY)));
        caja.setStyle("-fx-border-color: #C084FC; -fx-border-radius: 8;");
        return caja;
    }

    private Button boton(String texto, String color) {
        Button btn = new Button(texto);
        btn.setStyle("-fx-background-color: " + color + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 6;"
                + "-fx-cursor: hand;");
        btn.setPrefHeight(36);
        return btn;
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
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                -fx-padding: 6 10;
                -fx-font-size: 12px;
                """;
    }

    public static void main(String[] args) {
        launch();
    }
}