package Pr1;


public interface Sujeto {
    void suscribir(Observer o);
    void borrar(Observer o);
    void notificar(EventoForo evento);
}