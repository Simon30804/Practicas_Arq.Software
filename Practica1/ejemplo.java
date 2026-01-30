package Practica1;

import java.io.File;
import Practica1.editor.Editor;
import Practica1.listener.EmailNoificacionListener;
import Practica1.listener.LogOpenListener;

public class ejemplo {
    public static void main(String[] args) {
        System.out.println("Este es un ejemplo.");

        Editor editor = new Editor();
        editor.events.subscribe("open", new LogOpenListener(new File("log.txt")));
        editor.events.subscribe("save", new EmailNoificacionListener("ejemplo@gmail.com"));

        try {
            editor.openFile("documento.txt");
            editor.saveFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
