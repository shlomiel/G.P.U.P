import javafx.scene.control.TextArea;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Consumer;

public class TextareaConsumer implements Consumer {
    private TextArea textArea;


    public TextareaConsumer(TextArea t){
        this.textArea = t;
    }

    @Override
    public void accept(Object o) {
        textArea.setVisible(true);
        textArea.setText(textArea.getText() + "\n"+(String) o);
    }


}
