package com.lindavista.lindavistamusicplayer;

import com.lindavista.lindavistamusicplayer.controller.LindaVistaController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lindavista/lindavistamusicplayer/LindaVista.fxml"));
        Parent root = loader.load(); // 🔴 Primero se carga el FXML

        LindaVistaController controller = loader.getController(); // ✅ Ahora el controlador existe

        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(MainApp.class.getResource("/styles/style.css").toExternalForm());

        stage.setTitle("Linda Vista Music Player!");
        stage.setScene(scene);

        // ✅ Abrir en modo maximizado
        stage.setMaximized(true);

        // Detener música al cerrar la ventana
        stage.setOnCloseRequest(event -> controller.detenerReproduccion());

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
