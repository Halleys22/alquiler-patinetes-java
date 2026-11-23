package alquilerpatin;

import alquilerpatin.controladores.ConsultaController;
import alquilerpatin.datos.AlquilerDao;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class AlquilerPatinApp extends Application {
    @Override
    public void start(Stage ventana) throws IOException {
        FXMLLoader loader = new FXMLLoader(AlquilerPatinApp.class.getResource("/alquilerpatin/vistas/consulta.fxml"));
        Scene escena = new Scene(loader.load());
        ConsultaController consulta = loader.getController();
        consulta.setDao(new AlquilerDao());

        ventana.setTitle("Alquiler de patinetes");
        ventana.setScene(escena);
        ventana.setMinWidth(720);
        ventana.show();
        ventana.setMinHeight(ventana.getHeight());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
