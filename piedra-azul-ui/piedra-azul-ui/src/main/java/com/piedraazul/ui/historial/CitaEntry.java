package com.piedraazul.ui.historial;

import javafx.beans.property.*;

public class CitaEntry {

    private final LongProperty   id             = new SimpleLongProperty();
    private final LongProperty   pacienteId     = new SimpleLongProperty();
    private final StringProperty hora           = new SimpleStringProperty();
    private final StringProperty nombrePaciente = new SimpleStringProperty();
    private final StringProperty motivo         = new SimpleStringProperty();
    private final StringProperty estado         = new SimpleStringProperty();

    public Long   getId()             { return id.get(); }
    public Long   getPacienteId()     { return pacienteId.get(); }
    public String getHora()           { return hora.get(); }
    public String getNombrePaciente() { return nombrePaciente.get(); }
    public String getMotivo()         { return motivo.get(); }
    public String getEstado()         { return estado.get(); }

    public void setId(Long v)             { id.set(v); }
    public void setPacienteId(Long v)     { pacienteId.set(v); }
    public void setHora(String v)         { hora.set(v); }
    public void setNombrePaciente(String v){ nombrePaciente.set(v); }
    public void setMotivo(String v)        { motivo.set(v); }
    public void setEstado(String v)        { estado.set(v); }

    public LongProperty   idProperty()             { return id; }
    public LongProperty   pacienteIdProperty()     { return pacienteId; }
    public StringProperty horaProperty()           { return hora; }
    public StringProperty nombrePacienteProperty() { return nombrePaciente; }
    public StringProperty motivoProperty()         { return motivo; }
    public StringProperty estadoProperty()         { return estado; }
}