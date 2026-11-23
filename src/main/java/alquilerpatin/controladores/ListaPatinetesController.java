package alquilerpatin.controladores;

import alquilerpatin.datos.AlquilerDao;
import alquilerpatin.modelo.Pagina;
import alquilerpatin.modelo.Patinete;
import java.sql.SQLException;
import java.util.Objects;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class ListaPatinetesController {
    @FXML private TableView<Patinete> tablaPatinetes;
    @FXML private TableColumn<Patinete, Integer> serieCol;
    @FXML private TableColumn<Patinete, String> colorCol;
    @FXML private TableColumn<Patinete, String> modeloCol;
    @FXML private TableColumn<Patinete, String> marcaCol;
    @FXML private Button anteriorButton;
    @FXML private Button siguienteButton;
    @FXML private Button seleccionarButton;
    @FXML private Label paginaLabel;
    @FXML private Label totalLabel;

    private AlquilerDao dao;
    private int paginaActual = 1;
    private Integer numSerieSeleccionado;

    public void setDao(AlquilerDao dao) {
        this.dao = Objects.requireNonNull(dao);
    }

    public Integer getNumSerieSeleccionado() {
        return numSerieSeleccionado;
    }

    public void cargar() {
        mostrarPagina(1);
    }

    @FXML
    private void initialize() {
        serieCol.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().numSerie()));
        colorCol.setCellValueFactory(celda -> new ReadOnlyStringWrapper(celda.getValue().color()));
        modeloCol.setCellValueFactory(celda -> new ReadOnlyStringWrapper(celda.getValue().modelo()));
        marcaCol.setCellValueFactory(celda -> new ReadOnlyStringWrapper(celda.getValue().marca()));
        seleccionarButton.disableProperty().bind(tablaPatinetes.getSelectionModel().selectedItemProperty().isNull());
        anteriorButton.setDisable(true);
        siguienteButton.setDisable(true);
    }

    @FXML
    private void paginaAnterior() {
        mostrarPagina(paginaActual - 1);
    }

    @FXML
    private void paginaSiguiente() {
        mostrarPagina(paginaActual + 1);
    }

    @FXML
    private void seleccionar() {
        Patinete seleccionado = tablaPatinetes.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            numSerieSeleccionado = seleccionado.numSerie();
            Vistas.cerrar(tablaPatinetes);
        }
    }

    @FXML
    private void cancelar() {
        Vistas.cerrar(tablaPatinetes);
    }

    private void mostrarPagina(int numero) {
        try {
            Pagina<Patinete> pagina = dao.listarPatinetes(numero);
            paginaActual = pagina.numero();
            tablaPatinetes.getSelectionModel().clearSelection();
            tablaPatinetes.getItems().setAll(pagina.elementos());
            tablaPatinetes.scrollTo(0);
            int total = pagina.totalElementos();
            totalLabel.setText(total == 1 ? "1 patinete" : total + " patinetes");
            Vistas.mostrarPaginacion(pagina, paginaLabel, anteriorButton, siguienteButton);
        } catch (SQLException causa) {
            Vistas.errorSql(tablaPatinetes, "No se pudieron consultar los patinetes.", causa);
        }
    }
}
