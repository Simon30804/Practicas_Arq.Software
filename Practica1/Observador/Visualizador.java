package Practica1.Observador;

import java.util.List;

import Practica1.Sujeto.DataMart;
import Practica1.eventos.Evento;
import Practica1.payload.Payload;

public abstract class Visualizador implements Observador {
    public abstract void mostrarVisualizacion(List<Integer> x, List<Integer> y);

    public abstract void mostrarVisualizacion(List<Integer> x, List<Integer> y, int minY, int maxY);

    @Override
    public void actualizar(DataMart dataMart, Evento evento, Payload payload) {
        System.out.println("Visualizador actualizado con eventos: " + evento.getEventosInteres()
                + " y payload: " + payload.getContenido());
    }
}

