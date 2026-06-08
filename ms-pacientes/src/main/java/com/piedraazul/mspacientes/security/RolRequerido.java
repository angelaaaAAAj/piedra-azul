package com.piedraazul.mspacientes.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// ══════════════════════════════════════════════════════
// PATRÓN GOF: DECORATOR
// Propósito: enriquecer métodos de controlador con
// metadatos de autorización sin modificar su lógica.
// El interceptor RolInterceptor lee esta anotación
// en tiempo de ejecución para decidir si permite
// o rechaza la petición.
// ══════════════════════════════════════════════════════
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RolRequerido {

    /**
     * Roles permitidos para acceder al endpoint.
     * Si se especifica más de uno, basta con tener cualquiera.
     * Ejemplos: "ADMINISTRADOR", "MEDICO_TERAPISTA", "AGENDADOR", "PACIENTE"
     */
    String[] value();
}
