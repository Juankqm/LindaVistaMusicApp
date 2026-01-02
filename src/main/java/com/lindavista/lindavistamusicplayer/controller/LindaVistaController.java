package com.lindavista.lindavistamusicplayer.controller;

import com.lindavista.lindavistamusicplayer.model.Cancion;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javazoom.jl.player.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    // ===== NUEVO: volumen Windows con NirCmd (estable) =====
    // Escala interna de Windows: 0..65535. Este delta suele ser cómodo (ajusta a gusto).
    private static final int NIRCMD_DELTA = 7000;

    private Path obtenerCarpetaCanciones() {
        String userDir = System.getProperty("user.dir"); // Directorio donde se ejecuta el JAR
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

    // ===================== NUEVO: BOTONES VOLUMEN WINDOWS =====================
    @FXML
    private void onVolUp() {
        cambiarVolumenWindows(+NIRCMD_DELTA);
    }

    @FXML
    private void onVolDown() {
        cambiarVolumenWindows(-NIRCMD_DELTA);
    }

    // Opcional (si agregas un botón mute en FXML con onAction="#onMute")
    @FXML
    private void onMute() {
        ejecutarNircmd("mutesysvolume", "2"); // 2 = toggle
    }

    private void cambiarVolumenWindows(int delta) {
        // Requiere nircmd.exe junto al JAR (misma carpeta donde ejecutas el comando)
        ejecutarNircmd("changesysvolume", String.valueOf(delta));
    }

    private void ejecutarNircmd(String... args) {
        if (!isWindows()) {
            System.err.println("Volumen Windows: N/A (no es Windows).");
            return;
        }

        try {
            String userDir = System.getProperty("user.dir");
            File exe = Paths.get(userDir, "nircmdc.exe").toFile(); // ← CLAVE

            if (!exe.exists()) {
                System.err.println("No se encontró nircmdc.exe en: " + exe.getAbsolutePath());
                if (labelVolumen != null) {
                    labelVolumen.setText("Volumen: falta nircmdc.exe");
                }
                return;
            }

            System.out.println("Ejecutando: " + exe.getAbsolutePath() + " " + String.join(" ", args));

            List<String> cmd = new ArrayList<>();
            cmd.add(exe.getAbsolutePath());
            cmd.addAll(List.of(args));

            new ProcessBuilder(cmd).start();

    

        } catch (Exception e) {
            System.err.println("Error ejecutando nircmdc: " + e.getMessage());
            if (labelVolumen != null) {
                labelVolumen.setText("Volumen: error");
            }
        }
    }

    
    
    private boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    // ===================== TU CÓDIGO FUNCIONAL (SIN CAMBIOS) =====================
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
