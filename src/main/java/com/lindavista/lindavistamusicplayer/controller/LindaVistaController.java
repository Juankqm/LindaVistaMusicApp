package com.lindavista.lindavistamusicplayer.controller;

import com.lindavista.lindavistamusicplayer.model.Cancion;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javazoom.jl.player.Player;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class LindaVistaController {

    @FXML
    private Label labelGeneroActual;
    @FXML
    private Label labelCancionActual;
    @FXML
    private Label labelVolumen;
    @FXML
    private ListView<Cancion> listCanciones;
    @FXML
    private BorderPane root;

    @FXML
    private ImageView logoImage;

    private final List<String> generos = List.of("Rancheras", "Salsa", "Reggaeton", "Bachata", "Reggae");
    private int generoActualIndex = 0;
    private int cancionActualIndex = 0;

    private Thread hiloReproduccion;
    private Player player;

    //private final Path carpetaCanciones = Paths.get("MySongs");
    private Path obtenerCarpetaCanciones() {
        String userDir = System.getProperty("user.dir"); // Directorio donde se ejecuta el EXE
        return Paths.get(userDir, "MySongs");
    }

    @FXML
    public void initialize() {
        actualizarGenero(generos.get(generoActualIndex));
        Image logo = new Image(Objects.requireNonNull(getClass().getResource("/icons/logo.png")).toExternalForm());
        logoImage.setImage(logo);

        root.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/styles/style.css")).toExternalForm()
        );

        listCanciones.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Cancion seleccionada = listCanciones.getSelectionModel().getSelectedItem();
                if (seleccionada != null) {
                    detenerReproduccion();
                    labelCancionActual.setText("🎶 Reproduciendo: " + seleccionada.getNombre());
                    cancionActualIndex = listCanciones.getSelectionModel().getSelectedIndex();

                    hiloReproduccion = new Thread(() -> {
                        try (FileInputStream fis = new FileInputStream(seleccionada.getRuta())) {
                            player = new Player(fis);
                            player.play();

                            List<Cancion> canciones = listCanciones.getItems();
                            if (!Thread.currentThread().isInterrupted() && !canciones.isEmpty()) {
                                cancionActualIndex = (cancionActualIndex + 1) % canciones.size();
                                reproducirCancionActual();
                            }
                        } catch (Exception e) {
                            System.err.println("Error reproducción: " + e.getMessage());
                        }
                    });

                    hiloReproduccion.setDaemon(true);
                    hiloReproduccion.start();
                }
            }
        });
    }

    @FXML
    public void onPreviousGenre() {
        generoActualIndex = (generoActualIndex - 1 + generos.size()) % generos.size();
        actualizarGenero(generos.get(generoActualIndex));
    }

    @FXML
    public void onNextGenre() {
        generoActualIndex = (generoActualIndex + 1) % generos.size();
        actualizarGenero(generos.get(generoActualIndex));
    }

    @FXML
    public void onStop() {
        detenerReproduccion();
        labelCancionActual.setText("⏹️ Canción detenida");
    }

    @FXML
    public void onNextSong() {
        List<Cancion> canciones = listCanciones.getItems();
        if (canciones.isEmpty()) {
            return;
        }

        cancionActualIndex = (cancionActualIndex + 1) % canciones.size();
        reproducirCancionActual();
    }

    @FXML
    public void onPreviousSong() {
        List<Cancion> canciones = listCanciones.getItems();
        if (canciones.isEmpty()) {
            return;
        }

        cancionActualIndex = (cancionActualIndex - 1 + canciones.size()) % canciones.size();
        reproducirCancionActual();
    }

    private void actualizarGenero(String genero) {
        labelGeneroActual.setText(genero);
        cargarCanciones(genero);
        cancionActualIndex = 0;
        reproducirCancionActual();
    }

    @FXML
    private void onGeneroRancherasClick() {
        actualizarGenero("Rancheras");
    }

    @FXML
    private void onGeneroSalsaClick() {
        actualizarGenero("Salsa");
    }

    @FXML
    private void onGeneroCumbiaClick() {
        actualizarGenero("Cumbia");
    }

    @FXML
    private void onGeneroReggaetonClick() {
        actualizarGenero("Reggaeton");
    }

    @FXML
    private void onGeneroBachataClick() {
        actualizarGenero("Bachata");
    }

    @FXML
    private void onGeneroReggaeClick() {
        actualizarGenero("Reggae");
    }

    private void cargarCanciones(String genero) {
        //Path carpeta = carpetaCanciones.resolve(genero);
        Path carpeta = obtenerCarpetaCanciones().resolve(genero);
        List<Cancion> canciones = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(carpeta, "*.mp3")) {
            for (Path archivo : stream) {
                canciones.add(new Cancion(archivo.getFileName().toString(), archivo.toString(), genero));
            }
        } catch (IOException e) {
            System.err.println("Error leyendo canciones: " + e.getMessage());
        }

        listCanciones.getItems().setAll(canciones);
    }

    private void reproducirCancionActual() {
        detenerReproduccion();

        List<Cancion> canciones = listCanciones.getItems();
        if (canciones.isEmpty() || cancionActualIndex >= canciones.size()) {
            labelCancionActual.setText("⚠️ No hay canciones disponibles");
            return;
        }

        Cancion cancion = canciones.get(cancionActualIndex);
        labelCancionActual.setText("🎶 Reproduciendo: " + cancion.getNombre());
        listCanciones.getSelectionModel().select(cancionActualIndex);

        hiloReproduccion = new Thread(() -> {
            try (FileInputStream fis = new FileInputStream(cancion.getRuta())) {
                player = new Player(fis);
                player.play();

                if (!Thread.currentThread().isInterrupted() && !canciones.isEmpty()) {
                    cancionActualIndex = (cancionActualIndex + 1) % canciones.size();
                    reproducirCancionActual();
                }
            } catch (Exception e) {
                System.err.println("Error reproducción: " + e.getMessage());
            }
        });

        hiloReproduccion.setDaemon(true);
        hiloReproduccion.start();
    }

    public void detenerReproduccion() {
        if (player != null) {
            try {
                player.close();
            } catch (Exception ignored) {
            }
            player = null;
        }

        if (hiloReproduccion != null && hiloReproduccion.isAlive()) {
            hiloReproduccion.interrupt();
            hiloReproduccion = null;
        }
    }
}
