package alquilerpatin.controladores;

import alquilerpatin.datos.AlquilerDao;
import alquilerpatin.modelo.Usuario;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.regex.Pattern;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class RegistroController {
    private static final int MAX_NOMBRE = 100;
    private static final int MIN_EMAIL = 6;
    private static final int MAX_EMAIL = 100;
    private static final int MAX_TELEFONO = 9;
    private static final int LONGITUD_FECHA = 10;
    private static final String PARTE_EMAIL = "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+";
    private static final String DOMINIO = "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?";
    private static final Pattern FORMATO_EMAIL = Pattern.compile(
            PARTE_EMAIL + "(?:\\." + PARTE_EMAIL + ")*@" + DOMINIO
                    + "(?:\\." + DOMINIO + ")*\\.[A-Za-z]{2,63}");
    private static final Pattern FORMATO_TELEFONO = Pattern.compile("[0-9]+");
    private static final Pattern FORMATO_TEXTO_FECHA = Pattern.compile("[0-9]{2}/[0-9]{2}/[0-9]{4}");

    @FXML private TextField dniField;
    @FXML private TextField nombreField;
    @FXML private TextField emailField;
    @FXML private TextField telefonoField;
    @FXML private TextField patineteField;
    @FXML private DatePicker recogidaPicker;
    @FXML private DatePicker devolucionPicker;
    @FXML private Label avisoLimiteLabel;

    private AlquilerDao dao;
    private Integer numSerie;
    private String dniGuardado;

    public void setDao(AlquilerDao dao) {
        this.dao = Objects.requireNonNull(dao);
    }

    public String getDniGuardado() {
        return dniGuardado;
    }

    @FXML
    private void initialize() {
        Vistas.limitarDni(dniField, avisoLimiteLabel);
        Vistas.limitarLongitud(nombreField, MAX_NOMBRE,
                "El nombre admite hasta 100 caracteres.", avisoLimiteLabel);
        Vistas.limitarLongitud(emailField, MAX_EMAIL,
                "El correo admite hasta 100 caracteres.", avisoLimiteLabel);
        Vistas.limitarLongitud(telefonoField, MAX_TELEFONO,
                "El teléfono admite hasta 9 dígitos.", avisoLimiteLabel);
        patineteField.setEditable(false);
        configurarFecha(recogidaPicker, "recogida");
        configurarFecha(devolucionPicker, "devolución");
    }

    @FXML
    private void buscarPatinete() {
        try {
            FXMLLoader cargador = Vistas.cargador("lista-patinetes.fxml");
            Parent contenido = cargador.load();
            ListaPatinetesController lista = cargador.getController();
            lista.setDao(dao);
            Stage ventana = Vistas.ventanaModal(patineteField, contenido, "Lista de patinetes");
            ventana.setOnShown(evento -> lista.cargar());
            ventana.showAndWait();

            Integer seleccionado = lista.getNumSerieSeleccionado();
            if (seleccionado != null) {
                numSerie = seleccionado;
                patineteField.setText(seleccionado.toString());
            }
        } catch (IOException causa) {
            Vistas.errorVista(patineteField, "No se pudo abrir la lista de patinetes.", causa);
        }
    }

    @FXML
    private void limpiar() {
        dniField.clear();
        nombreField.clear();
        emailField.clear();
        telefonoField.clear();
        numSerie = null;
        patineteField.clear();
        recogidaPicker.setValue(null);
        recogidaPicker.getEditor().clear();
        devolucionPicker.setValue(null);
        devolucionPicker.getEditor().clear();
        avisoLimiteLabel.setText("");
        dniField.requestFocus();
    }

    @FXML
    private void aceptar() {
        Usuario usuario = leerUsuario();
        if (usuario == null) {
            return;
        }
        if (numSerie == null) {
            avisar(patineteField, "Selecciona un patinete con la lupa.");
            return;
        }
        LocalDate recogida = leerFecha(recogidaPicker, "recogida");
        if (recogida == null) {
            return;
        }
        if (recogida.isBefore(LocalDate.now())) {
            avisar(recogidaPicker, "La fecha de recogida debe ser hoy o posterior.");
            return;
        }
        LocalDate devolucion = leerFecha(devolucionPicker, "devolución");
        if (devolucion == null) {
            return;
        }
        if (devolucion.isBefore(recogida)) {
            avisar(devolucionPicker, "La fecha de devolución debe ser igual o posterior a la de recogida.");
            return;
        }

        try {
            dao.insertar(usuario, numSerie, recogida, devolucion);
            dniGuardado = usuario.dni();
            Vistas.informacion(dniField, "Alquiler registrado", "El alquiler se ha guardado correctamente.");
            Vistas.cerrar(dniField);
        } catch (SQLException causa) {
            Vistas.errorSql(dniField, "No se pudo registrar el alquiler.", causa);
        }
    }

    @FXML
    private void cancelar() {
        Vistas.cerrar(dniField);
    }

    private Usuario leerUsuario() {
        String dni = Vistas.leerDni(dniField);
        if (dni == null) {
            return null;
        }
        String nombre = nombreField.getText().strip();
        if (nombre.isEmpty()) {
            avisar(nombreField, "Introduce el nombre completo.");
            return null;
        }
        String email = emailField.getText().strip();
        if (email.isEmpty()) {
            avisar(emailField, "Introduce un correo electrónico.");
            return null;
        }
        if (email.length() < MIN_EMAIL) {
            avisar(emailField, "El correo debe tener al menos 6 caracteres.");
            return null;
        }
        if (!FORMATO_EMAIL.matcher(email).matches()) {
            avisar(emailField, "El correo debe tener un formato como usuario@correo.com.");
            return null;
        }
        String telefono = telefonoField.getText();
        if (telefono.isBlank()) {
            avisar(telefonoField, "Introduce un teléfono.");
            return null;
        }
        if (!FORMATO_TELEFONO.matcher(telefono).matches()) {
            avisar(telefonoField, "El teléfono solo puede contener números.");
            return null;
        }
        return new Usuario(dni, nombre, email, Integer.parseInt(telefono));
    }

    private void configurarFecha(DatePicker campo, String tipo) {
        Vistas.limitarLongitud(campo.getEditor(), LONGITUD_FECHA,
                "La fecha de " + tipo + " admite hasta 10 caracteres.", avisoLimiteLabel);
        campo.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate fecha) {
                if (fecha == null) {
                    return "";
                }
                return fecha.format(Vistas.FORMATO_FECHA);
            }

            @Override
            public LocalDate fromString(String texto) {
                if (texto == null || texto.isBlank()) {
                    return null;
                }
                LocalDate fecha = convertirFecha(texto);
                // El DatePicker confirma al perder el foco. Conserva el texto incorrecto
                // para que Aceptar pueda indicar qué debe corregirse.
                return fecha == null ? campo.getValue() : fecha;
            }
        });
        campo.setOnShowing(evento -> campo.setValue(convertirFecha(campo.getEditor().getText())));
        campo.addEventFilter(KeyEvent.KEY_RELEASED, evento -> {
            if (evento.getCode() == KeyCode.ENTER && !campo.isShowing()
                    && convertirFecha(campo.getEditor().getText()) == null) {
                evento.consume();
            }
        });
    }

    private LocalDate leerFecha(DatePicker campo, String tipo) {
        String texto = campo.getEditor().getText();
        if (texto.isBlank()) {
            avisar(campo, "Introduce la fecha de " + tipo + ".");
            return null;
        }
        if (!FORMATO_TEXTO_FECHA.matcher(texto).matches()) {
            avisar(campo, "La fecha de " + tipo + " debe tener el formato dd/mm/aaaa (10 caracteres).");
            return null;
        }
        LocalDate fecha = convertirFecha(texto);
        if (fecha == null) {
            avisar(campo, "La fecha de " + tipo + " no existe en el calendario.");
        }
        return fecha;
    }

    private static LocalDate convertirFecha(String texto) {
        if (texto == null || !FORMATO_TEXTO_FECHA.matcher(texto).matches()) {
            return null;
        }
        try {
            LocalDate fecha = LocalDate.parse(texto, Vistas.FORMATO_FECHA);
            return fecha.getYear() == 0 ? null : fecha;
        } catch (DateTimeParseException causa) {
            return null;
        }
    }

    private void avisar(Node campo, String mensaje) {
        Vistas.aviso(campo, mensaje);
        campo.requestFocus();
    }
}
