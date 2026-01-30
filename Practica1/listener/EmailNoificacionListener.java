package Practica1.listener;

import java.io.File;

public class EmailNoificacionListener implements EventListener {

    private String email;

    public EmailNoificacionListener(String email) {
        this.email = email;
    }

    @Override
    public void update(String eventType, File file) {
        System.out.println("Enviando email a " + email + ": Se ha detectado un evento '" + eventType + "' en el archivo: " + file.getName());
    }
}
