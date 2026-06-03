package com.piedraazul.msauth.model;

// ══════════════════════════════════════════════════════
// PATRÓN GOF: FACTORY METHOD
// Propósito: centralizar la creación de usuarios según
// su rol, sin que el Service sepa los detalles de cada tipo.
// HU-06 (crear admin), HU-07 (médico), HU-08 (terapista),
// HU-09 (paciente)
// ══════════════════════════════════════════════════════
public class UsuarioFactory {

    public static Usuario crear(String username,
                                String password,
                                String nombre,
                                String email,
                                String rol) {
        Rol rolEnum = switch (rol.toUpperCase()) {
            case "ADMINISTRADOR"    -> Rol.ADMINISTRADOR;
            case "MEDICO_TERAPISTA" -> Rol.MEDICO_TERAPISTA;
            case "AGENDADOR"        -> Rol.AGENDADOR;
            case "PACIENTE"         -> Rol.PACIENTE;
            default -> throw new IllegalArgumentException(
                    "Rol no válido: " + rol
            );
        };

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(password);
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setRol(rolEnum);
        usuario.setActivo(true);
        return usuario;
    }
}