package Practica1.Sujeto;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import Practica1.Observador.Observador;
import Practica1.eventos.Evento;
import Practica1.payload.Payload;

public abstract class DataMart {
    protected HashMap<Observador, List<String>> observadores = new HashMap<>();

    public abstract void registrar(Observador observador, List<String> intereses);

    public abstract void eliminar(Observador observador);

    public abstract void notificar(DataMart dataMart, Evento evento, Payload payload);
}
