package com.piedraazul.msagenda.service;

import com.piedraazul.msagenda.model.Cita;
import com.piedraazul.msagenda.model.Medico;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// Implementación concreta del Strategy:
// sugiere el primer horario disponible desde ahora
// usando la franja horaria e intervalo propios del médico
@Component("primerDisponible")
public class PrimerHorarioDisponibleStrategy implements AgendamientoStrategy {

    @Override
    public LocalDateTime sugerirHorario(Medico medico, List<Cita> citasExistentes) {

        // Leer configuración del médico (Convertimos temporalmente a String para que funcione con el método parsearHora de tus compañeros)
        LocalTime franjaInicio = parsearHora(medico.getFranjaInicio() != null ? medico.getFranjaInicio().toString() : null, "08:00");
        LocalTime franjaFin    = parsearHora(medico.getFranjaFin() != null ? medico.getFranjaFin().toString() : null,    "17:00");
        int intervalo          = medico.getIntervaloCitas() > 0
                ? medico.getIntervaloCitas() : 30;

        LocalDateTime candidato = LocalDateTime.now()
                .plusHours(1)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        for (int intentos = 0; intentos < 200; intentos++) {

            // Solo lunes a viernes
            int diaSemana = candidato.getDayOfWeek().getValue();
            if (diaSemana > 5) {
                candidato = candidato.plusDays(1)
                        .withHour(franjaInicio.getHour())
                        .withMinute(franjaInicio.getMinute());
                continue;
            }

            // Ajustar si es antes de franjaInicio
            if (candidato.toLocalTime().isBefore(franjaInicio)) {
                candidato = candidato
                        .withHour(franjaInicio.getHour())
                        .withMinute(franjaInicio.getMinute());
            }

            // Pasar al día siguiente si superó franjaFin
            if (candidato.toLocalTime()
                    .isAfter(franjaFin.minusMinutes(intervalo))) {
                candidato = candidato.plusDays(1)
                        .withHour(franjaInicio.getHour())
                        .withMinute(franjaInicio.getMinute());
                continue;
            }

            // Verificar que el slot esté libre
            final LocalDateTime slot = candidato;
            boolean ocupado = citasExistentes.stream()
                    .anyMatch(c -> Math.abs(
                            java.time.Duration.between(
                                    c.getFechaHora(), slot).toMinutes()
                    ) < intervalo);

            if (!ocupado) return slot;

            candidato = candidato.plusMinutes(intervalo);
        }

        throw new RuntimeException(
                "No hay horarios disponibles para el médico: " + medico.getNombre());
    }

    private LocalTime parsearHora(String valor, String fallback) {
        try {
            return LocalTime.parse(valor != null ? valor : fallback);
        } catch (Exception e) {
            return LocalTime.parse(fallback);
        }
    }
}