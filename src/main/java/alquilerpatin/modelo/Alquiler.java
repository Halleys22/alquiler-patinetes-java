package alquilerpatin.modelo;

import java.time.LocalDate;

public record Alquiler(
        int id,
        String nombreCompleto,
        int numSerie,
        LocalDate fechaRecogida,
        LocalDate fechaDevolucion) {
}
