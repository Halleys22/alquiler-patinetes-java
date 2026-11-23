package alquilerpatin.controladores;

import alquilerpatin.modelo.Alquiler;
import alquilerpatin.modelo.Pagina;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.regex.Pattern;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Modality;
import javafx.stage.Stage;

final class Vistas {
    static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    private static final int LONGITUD_DNI = 9;

    private static final String RUTA_VISTAS = "/alquilerpatin/vistas/";
    private static final Pattern FORMATO_DNI = Pattern.compile("[0-9]{8}[A-Za-z]");
    private static final ButtonType ACEPTAR = new ButtonType("Aceptar", ButtonData.OK_DONE);
    private static final ButtonType CANCELAR = new ButtonType("Cancelar", ButtonData.CANCEL_CLOSE);
    private static final System.Logger LOG = System.getLogger(Vistas.class.getName());

    private Vistas() {
    }

    static void limitarDni(TextField campo, Label avisoLimite) {
        limitarLongitud(campo, LONGITUD_DNI, "El DNI admite exactamente 9 caracteres.", avisoLimite);
    }

    static void limitarLongitud(TextField campo, int maximo, String mensaje, Label avisoLimite) {
        campo.setTextFormatter(new TextFormatter<>(cambio -> {
            if (cambio.getControlNewText().length() > maximo) {
                avisoLimite.setText(mensaje);
                return null;
            }
            return cambio;
        }));
        campo.textProperty().addListener((observable, anterior, nuevo) -> avisoLimite.setText(""));
    }

    static String leerDni(TextField campo) {
        String dni = campo.getText().strip();
        if (FORMATO_DNI.matcher(dni).matches()) {
            return dni.toUpperCase(Locale.ROOT);
        }
        aviso(campo, dni.length() != LONGITUD_DNI
                ? "El DNI debe tener exactamente 9 caracteres."
                : "El DNI debe empezar con ocho números y terminar con una letra.");
        campo.requestFocus();
        return null;
    }

    static FXMLLoader cargador(String archivo) {
        return new FXMLLoader(Vistas.class.getResource(RUTA_VISTAS + archivo));
    }

    static Stage ventanaModal(Node origen, Parent contenido, String titulo) {
        Stage ventana = new Stage();
        ventana.initOwner(origen.getScene().getWindow());
        ventana.initModality(Modality.WINDOW_MODAL);
        ventana.setTitle(titulo);
        ventana.setResizable(false);
        ventana.setScene(new Scene(contenido));
        return ventana;
    }

    static void cerrar(Node origen) {
        origen.getScene().getWindow().hide();
    }

    static void mostrarPaginacion(Pagina<?> pagina, Label etiqueta, Button anterior, Button siguiente) {
        etiqueta.setText("Página " + pagina.numero() + " de " + pagina.totalPaginas());
        anterior.setDisable(pagina.numero() == 1);
        siguiente.setDisable(pagina.numero() >= pagina.totalPaginas());
    }

    static void aviso(Node origen, String mensaje) {
        alerta(origen, Alert.AlertType.WARNING, "Revisa los datos", mensaje, ACEPTAR).showAndWait();
    }

    static void informacion(Node origen, String titulo, String mensaje) {
        alerta(origen, Alert.AlertType.INFORMATION, titulo, mensaje, ACEPTAR).showAndWait();
    }

    static boolean confirmarEliminacion(Node origen, Alquiler alquiler) {
        String recogida = alquiler.fechaRecogida().format(FORMATO_FECHA);
        return alerta(origen, Alert.AlertType.CONFIRMATION, "Eliminar alquiler",
                "¿Quieres eliminar el alquiler del patinete " + alquiler.numSerie()
                        + " con recogida el " + recogida + "?", ACEPTAR, CANCELAR)
                .showAndWait()
                .filter(ACEPTAR::equals)
                .isPresent();
    }

    static void errorSql(Node origen, String operacion, SQLException causa) {
        LOG.log(System.Logger.Level.WARNING, "{0} (SQLState {1}, código {2})",
                operacion, causa.getSQLState(), causa.getErrorCode());
        alerta(origen, Alert.AlertType.ERROR, "Error de base de datos",
                operacion + "\n" + motivoSql(causa), ACEPTAR).showAndWait();
    }

    private static String motivoSql(SQLException causa) {
        return switch (causa.getErrorCode()) {
            case 1044, 1142, 1143, 1227 -> "La cuenta de MySQL no tiene permisos para esta operación.";
            case 1045 -> "MySQL rechazó el usuario o la contraseña.";
            case 1049 -> "La base de datos configurada no existe. Comprueba el esquema de la conexión.";
            case 1054, 1146 -> "Falta una tabla o columna. Comprueba que el esquema esté actualizado.";
            case 1062 -> "Ya existe un registro con una clave que debe ser única.";
            case 1205, 1213 -> "Otra operación estaba usando los mismos datos. Inténtalo de nuevo.";
            case 1264 -> "Un valor numérico está fuera del rango admitido por la base de datos.";
            case 1406 -> "Un dato supera la longitud admitida por la base de datos.";
            case 1451 -> "Hay otros registros asociados a este alquiler que impiden eliminarlo.";
            case 1452 -> "El usuario o el patinete asociado ya no existe. Comprueba la selección.";
            default -> {
                String estado = causa.getSQLState();
                if (estado != null && estado.startsWith("08")) {
                    yield "Se perdió la conexión o no se pudo contactar con MySQL.";
                }
                if (estado != null && estado.startsWith("28")) {
                    yield "MySQL rechazó el acceso con la cuenta configurada.";
                }
                yield "MySQL devolvió el error " + causa.getErrorCode()
                        + (estado == null ? "." : " (SQLState " + estado + ").");
            }
        };
    }

    static void errorVista(Node origen, String mensaje, Exception causa) {
        LOG.log(System.Logger.Level.ERROR, mensaje, causa);
        alerta(origen, Alert.AlertType.ERROR, "Error al abrir la ventana",
                mensaje + "\nNo se pudo cargar el archivo de la pantalla.", ACEPTAR).showAndWait();
    }

    private static Alert alerta(Node origen, Alert.AlertType tipo, String titulo, String mensaje,
            ButtonType... botones) {
        Alert alerta = new Alert(tipo, mensaje, botones);
        alerta.initOwner(origen.getScene().getWindow());
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        return alerta;
    }
}
