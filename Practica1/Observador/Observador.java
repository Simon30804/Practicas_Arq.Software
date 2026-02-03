package Practica1.Observador;

import Practica1.Sujeto.DataMart;
import Practica1.eventos.Evento;
import Practica1.payload.Payload;

public interface Observador {
    void actualizar(DataMart dataMart, Evento evento,  payload);
}
