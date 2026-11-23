package alquilerpatin.controladores;

import alquilerpatin.datos.AlquilerDao;
import alquilerpatin.modelo.Alquiler;
import alquilerpatin.modelo.Pagina;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ConsultaController {
    @FXML private TextField dniField;
    @FXML private Label avisoLimiteDni;
    @FXML private TableView<Alquiler> tablaAlquileres;
    @FXML private TableColumn<Alquiler, String> nombreCol;
    @FXML private TableColumn<Alquiler, Integer> serieCol;
    @FXML private TableColumn<Alquiler, String> recogidaCol;
    @FXML private TableColumn<Alquiler, String> devolucionCol;
    @FXML private Button anteriorButton;
    @FXML private Button siguienteButton;
    @FXML private Button eliminarButton;
    @FXML private Label paginaLabel;
    @FXML private Label totalLabel;
    @FXML private Label sinResultadosLabel;

    private AlquilerDao dao;
    private String dniConsultado;
    private int paginaActual = 1;

    public void setDao(AlquilerDao dao) {
        this.dao = Objects.requireNonNull(dao);
    }

    @FXML
    private void initialize() {
        Vistas.limitarDni(dniField, avisoLimiteDni);
        dniField.textProperty().addListener((observable, anterior, nuevo) -> restablecerConsulta());
        nombreCol.setCellValueFactory(celda -> new ReadOnlyStringWrapper(celda.getValue().nombreCompleto()));
        serieCol.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().numSerie()));
        recogidaCol.setCellValueFactory(celda -> new ReadOnlyStringWrapper(
                celda.getValue().fechaRecogida().format(Vistas.FORMATO_FECHA)));
        devolucionCol.setCellValueFactory(celda -> new ReadOnlyStringWrapper(
                celda.getValue().fechaDevolucion().format(Vistas.FORMATO_FECHA)));
        eliminarButton.disableProperty().bind(tablaAlquileres.getSelectionModel().selectedItemProperty().isNull());
        restablecerConsulta();
    }

    @FXML
    private void buscar() {
        String dni = Vistas.leerDni(dniField);
        if (dni != null) {
            mostrarPagina(dni, 1);
        }
    }

    @FXML
    private void paginaAnterior() {
        mostrarPagina(dniConsultado, paginaActual - 1);
    }

    @FXML
    private void paginaSiguiente() {
        mostrarPagina(dniConsultado, paginaActual + 1);
    }

    @FXML
    private void alta() {
        try {
            FXMLLoader cargador = Vistas.cargador("registro.fxml");
            Parent contenido = cargador.load();
            RegistroController registro = cargador.getController();
            registro.setDao(dao);
            Stage ventana = Vistas.ventanaModal(dniField, contenido, "Registro de alquiler");
            ventana.showAndWait();
            String dniGuardado = registro.getDniGuardado();
            if (dniGuardado != null) {
                dniField.setText(dniGuardado);
                mostrarPagina(dniGuardado, 1);
            }
        } catch (IOException causa) {
            Vistas.errorVista(dniField, "No se pudo abrir el formulario de registro.", causa);
        }
    }

    @FXML
    private void eliminar() {
        Alquiler alquiler = tablaAlquileres.getSelectionModel().getSelectedItem();
        if (alquiler == null || !Vistas.confirmarEliminacion(tablaAlquileres, alquiler)) {
            return;
        }

        try {
            boolean eliminado = dao.eliminar(alquiler.id());
            mostrarPagina(dniConsultado, paginaActual);
            if (!eliminado) {
                Vistas.aviso(tablaAlquileres, "El alquiler seleccionado ya se había eliminado.");
            }
        } catch (SQLException causa) {
            Vistas.errorSql(tablaAlquileres, "No se pudo eliminar el alquiler.", causa);
        }
    }

    @FXML
    private void salir() {
        Platform.exit();
    }

    private void restablecerConsulta() {
        dniConsultado = null;
        paginaActual = 1;
        tablaAlquileres.getSelectionModel().clearSelection();
        tablaAlquileres.getItems().clear();
        sinResultadosLabel.setText("Introduce un DNI y pulsa Buscar.");
        paginaLabel.setText("Página 1 de 1");
        totalLabel.setText("");
        anteriorButton.setDisable(true);
        siguienteButton.setDisable(true);
    }

    private void mostrarPagina(String dni, int numero) {
        try {
            Pagina<Alquiler> pagina = dao.buscarPorDni(dni, numero);
            dniConsultado = dni;
            paginaActual = pagina.numero();
            tablaAlquileres.getSelectionModel().clearSelection();
            tablaAlquileres.getItems().setAll(pagina.elementos());
            tablaAlquileres.scrollTo(0);
            sinResultadosLabel.setText("No hay alquileres para este DNI.");
            int total = pagina.totalElementos();
            totalLabel.setText(total == 1 ? "1 alquiler" : total + " alquileres");
            Vistas.mostrarPaginacion(pagina, paginaLabel, anteriorButton, siguienteButton);
        } catch (SQLException causa) {
            restablecerConsulta();
            sinResultadosLabel.setText("No se pudo completar la consulta.");
            Vistas.errorSql(tablaAlquileres, "No se pudieron consultar los alquileres.", causa);
        }
    }
}
