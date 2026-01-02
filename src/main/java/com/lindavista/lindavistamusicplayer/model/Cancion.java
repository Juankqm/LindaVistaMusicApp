package com.lindavista.lindavistamusicplayer.model;

public class Cancion {
    private String nombre;
    private String ruta;
    private String genero;

    public Cancion(String nombre, String ruta, String genero) {
        this.nombre = nombre;
        this.ruta = ruta;
        this.genero = genero;
    }

    public String getNombre() { return nombre; }
    public String getRuta() { return ruta; }
    public String getGenero() { return genero; }

    @Override
    public String toString() {
        return nombre;
    }
}
