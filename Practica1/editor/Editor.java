package Practica1.editor;

import java.io.File;
import Practica1.publisher.EventManager;

public class Editor {
    public EventManager events;
    private File file;

    public Editor() {
        this.events = new EventManager("open", "save");
    }

    public void openFile(String filePath) {
        this.file = new File(filePath);
        events.notify("open", this.file);
    }

    public void saveFile() throws Exception {
        if (this.file != null) {
            events.notify("save", this.file);
        } else {
            throw new Exception("Por favor, abre un archivo primero.");
        }
    }
    
}
