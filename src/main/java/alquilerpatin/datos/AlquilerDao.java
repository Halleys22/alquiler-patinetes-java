package alquilerpatin.datos;

import alquilerpatin.modelo.Alquiler;
import alquilerpatin.modelo.Pagina;
import alquilerpatin.modelo.Patinete;
import alquilerpatin.modelo.Usuario;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public final class AlquilerDao {
    private static final int PATINETES_POR_PAGINA = 10;
    private static final int ALQUILERES_POR_PAGINA = 5;

    private final String url;
    private final String usuario;
    private final String clave;

    public AlquilerDao() {
        this(configuracion("url", "URL", "jdbc:mysql://localhost:3306/ALQUILERPATIN"),
                configuracion("user", "USER", "root"),
                configuracion("password", "PASSWORD", ""));
    }

    public AlquilerDao(String url, String usuario, String clave) {
        this.url = url;
        this.usuario = usuario;
        this.clave = clave;
    }

    public Pagina<Patinete> listarPatinetes(int pagina) throws SQLException {
        return consultarPagina(pagina, PATINETES_POR_PAGINA, null,
                "SELECT COUNT(*) FROM Patinetes",
                """
                SELECT NumSerie, Color, Modelo, Marca
                FROM Patinetes
                ORDER BY NumSerie
                LIMIT ? OFFSET ?
                """,
                fila -> new Patinete(fila.getInt("NumSerie"), fila.getString("Color"),
                        fila.getString("Modelo"), fila.getString("Marca")));
    }

    public Pagina<Alquiler> buscarPorDni(String dni, int pagina) throws SQLException {
        return consultarPagina(pagina, ALQUILERES_POR_PAGINA, Objects.requireNonNull(dni),
                "SELECT COUNT(*) FROM Registro WHERE DNI = ?",
                """
                SELECT r.ID, u.NombreCompleto, r.NumSerie, r.FechaRecog, r.FechaDev
                FROM Registro r
                JOIN Usuarios u ON u.DNI = r.DNI
                WHERE r.DNI = ?
                ORDER BY r.ID DESC
                LIMIT ? OFFSET ?
                """,
                fila -> new Alquiler(fila.getInt("ID"), fila.getString("NombreCompleto"),
                        fila.getInt("NumSerie"), fila.getDate("FechaRecog").toLocalDate(),
                        fila.getDate("FechaDev").toLocalDate()));
    }

    public void insertar(Usuario usuario, int numSerie, LocalDate fechaRecogida,
            LocalDate fechaDevolucion) throws SQLException {
        enTransaccion(conexion -> {
            guardarUsuario(conexion, usuario);
            try (PreparedStatement consulta = conexion.prepareStatement("""
                    INSERT INTO Registro (DNI, NumSerie, FechaRecog, FechaDev)
                    VALUES (?, ?, ?, ?)
                    """)) {
                consulta.setString(1, usuario.dni());
                consulta.setInt(2, numSerie);
                consulta.setDate(3, Date.valueOf(fechaRecogida));
                consulta.setDate(4, Date.valueOf(fechaDevolucion));
                consulta.executeUpdate();
            }
            return null;
        });
    }

    public boolean actualizar(int id, int numSerie, LocalDate fechaRecogida,
            LocalDate fechaDevolucion) throws SQLException {
        try (Connection conexion = abrirConexion();
                PreparedStatement consulta = conexion.prepareStatement("""
                        UPDATE Registro
                        SET NumSerie = ?, FechaRecog = ?, FechaDev = ?
                        WHERE ID = ?
                        """)) {
            consulta.setInt(1, numSerie);
            consulta.setDate(2, Date.valueOf(fechaRecogida));
            consulta.setDate(3, Date.valueOf(fechaDevolucion));
            consulta.setInt(4, id);
            return consulta.executeUpdate() == 1;
        }
    }

    public boolean eliminar(int id) throws SQLException {
        try (Connection conexion = abrirConexion();
                PreparedStatement consulta = conexion.prepareStatement(
                        "DELETE FROM Registro WHERE ID = ?")) {
            consulta.setInt(1, id);
            return consulta.executeUpdate() == 1;
        }
    }

    private static void guardarUsuario(Connection conexion, Usuario usuario) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement("""
                INSERT INTO Usuarios (DNI, NombreCompleto, Email, Telefono)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE NombreCompleto = ?, Email = ?, Telefono = ?
                """)) {
            consulta.setString(1, usuario.dni());
            consulta.setString(2, usuario.nombreCompleto());
            consulta.setString(3, usuario.email());
            consulta.setInt(4, usuario.telefono());
            consulta.setString(5, usuario.nombreCompleto());
            consulta.setString(6, usuario.email());
            consulta.setInt(7, usuario.telefono());
            consulta.executeUpdate();
        }
    }

    private <T> Pagina<T> consultarPagina(int solicitada, int tamano, String dni, String sqlTotal,
            String sqlPagina, Mapeador<T> mapeador) throws SQLException {
        return enTransaccion(conexion -> {
            int total;
            try (PreparedStatement consulta = conexion.prepareStatement(sqlTotal)) {
                if (dni != null) {
                    consulta.setString(1, dni);
                }
                try (ResultSet fila = consulta.executeQuery()) {
                    fila.next();
                    total = fila.getInt(1);
                }
            }

            int totalPaginas = Math.max(1, (int) ((total + (long) tamano - 1) / tamano));
            int numero = Math.min(Math.max(solicitada, 1), totalPaginas);
            List<T> elementos = new ArrayList<>(tamano);
            try (PreparedStatement consulta = conexion.prepareStatement(sqlPagina)) {
                int parametro = 1;
                if (dni != null) {
                    consulta.setString(parametro++, dni);
                }
                consulta.setInt(parametro++, tamano);
                consulta.setInt(parametro, (numero - 1) * tamano);
                try (ResultSet filas = consulta.executeQuery()) {
                    while (filas.next()) {
                        elementos.add(mapeador.leer(filas));
                    }
                }
            }
            return new Pagina<>(elementos, numero, totalPaginas, total);
        });
    }

    private <T> T enTransaccion(Operacion<T> operacion) throws SQLException {
        try (Connection conexion = abrirConexion()) {
            // El recuento y las filas de una página se leen desde la misma instantánea.
            conexion.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            conexion.setAutoCommit(false);
            try {
                T resultado = operacion.ejecutar(conexion);
                conexion.commit();
                return resultado;
            } catch (SQLException | RuntimeException error) {
                try {
                    conexion.rollback();
                } catch (SQLException errorRollback) {
                    error.addSuppressed(errorRollback);
                }
                throw error;
            }
        }
    }

    private Connection abrirConexion() throws SQLException {
        Properties opciones = new Properties();
        opciones.setProperty("user", usuario);
        opciones.setProperty("password", clave);
        opciones.setProperty("connectTimeout", "5000");
        opciones.setProperty("socketTimeout", "10000");
        return DriverManager.getConnection(url, opciones);
    }

    private static String configuracion(String propiedad, String variable, String porDefecto) {
        String entorno = System.getenv("ALQUILERPATIN_DB_" + variable);
        return System.getProperty("alquilerpatin.db." + propiedad, entorno == null ? porDefecto : entorno);
    }

    @FunctionalInterface
    private interface Operacion<T> {
        T ejecutar(Connection conexion) throws SQLException;
    }

    @FunctionalInterface
    private interface Mapeador<T> {
        T leer(ResultSet fila) throws SQLException;
    }
}
