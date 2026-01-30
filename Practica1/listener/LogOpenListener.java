package Practica1.listener;

import java.io.File;

public class LogOpenListener implements EventListener {

    private File logFile;

    public LogOpenListener(File logFile) {
        this.logFile = logFile;
    }

    @Override
    public void update(String eventType, File file) {
        System.out.println("Guardando el log " + logFile.getName() + ": Se ha detectado un evento '" + eventType + "' en el archivo: " + file.getName());
    }
    
}
