package alquilerpatin.modelo;

import java.util.List;

public record Pagina<T>(List<T> elementos, int numero, int totalPaginas, int totalElementos) {
    public Pagina {
        elementos = List.copyOf(elementos);
    }
}
