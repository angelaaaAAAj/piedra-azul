package com.piedraazul.ui.reportes;

public class ReporteEntry {

    private String etiqueta;
    private long   valor;

    public ReporteEntry(String etiqueta, long valor) {
        this.etiqueta = etiqueta;
        this.valor    = valor;
    }

    public String getEtiqueta() { return etiqueta; }
    public long   getValor()    { return valor; }
    public void   setEtiqueta(String e) { this.etiqueta = e; }
    public void   setValor(long v)      { this.valor = v; }
}