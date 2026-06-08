package com.piedraazul.msagenda.application.services;

import com.piedraazul.msagenda.domain.model.Cita;
import com.piedraazul.msagenda.domain.model.Medico;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component("horarioCercano")
public class HorarioMasCercanoStrategy implements AgendamientoStrategy {

    @Override
    public LocalDateTime sugerirHorario(Medico medico, List<Cita> citasExistentes) {

        LocalTime franjaInicio = parsearHora(medico.getFranjaInicio(), "08:00");
        LocalTime franjaFin    = parsearHora(medico.getFranjaFin(),    "17:00");
        int intervalo          = medico.getIntervaloCitas() > 0
                ? medico.getIntervaloCitas() : 30;

        int minutosMedio = (franjaInicio.toSecondOfDay()
                + franjaFin.toSecondOfDay()) / 2 / 60;
        LocalTime horaObjetivo = LocalTime.of(minutosMedio / 60, minutosMedio % 60);

        LocalDateTime candidato = LocalDateTime.now()
                .plusDays(1)
                .withHour(horaObjetivo.getHour())
                .withMinute(horaObjetivo.getMinute())
                .withSecond(0)
                .withNano(0);

        int diaSemana = candidato.getDayOfWeek().getValue();
        if (diaSemana == 6) candidato = candidato.plusDays(2);
        if (diaSemana == 7) candidato = candidato.plusDays(1);

        for (int intentos = 0; intentos < 100; intentos++) {
            int dia = candidato.getDayOfWeek().getValue();
            if (dia > 5) {
                candidato = candidato.plusDays(1)
                        .withHour(franjaInicio.getHour())
                        .withMinute(franjaInicio.getMinute());
                continue;
            }
            final LocalDateTime slot = candidato;
            boolean ocupado = citasExistentes.stream()
                    .anyMatch(c -> Math.abs(
                            java.time.Duration.between(c.getFechaHora(), slot).toMinutes()
                    ) < intervalo);
            if (!ocupado) return slot;
            candidato = candidato.plusMinutes(intervalo);
            if (candidato.toLocalTime().isAfter(franjaFin)) {
                candidato = candidato.plusDays(1)
                        .withHour(franjaInicio.getHour())
                        .withMinute(franjaInicio.getMinute());
            }
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