package com.piedraazul.ui.historial;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ReagendamientoEntry {

    private final StringProperty fechaAnterior;
    private final StringProperty fechaNueva;
    private final StringProperty motivo;
    private final StringProperty responsable;

    public ReagendamientoEntry(String fechaAnterior,
                               String fechaNueva,
                               String motivo,
                               String responsable) {
        this.fechaAnterior = new SimpleStringProperty(fechaAnterior);
        this.fechaNueva    = new SimpleStringProperty(fechaNueva);
        this.motivo        = new SimpleStringProperty(motivo);
        this.responsable   = new SimpleStringProperty(responsable);
    }

    public StringProperty fechaAnteriorProperty() { return fechaAnterior; }
    public StringProperty fechaNuevaProperty()    { return fechaNueva; }
    public StringProperty motivoProperty()        { return motivo; }
    public StringProperty responsableProperty()   { return responsable; }
}
