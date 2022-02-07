import javafx.scene.control.Label;
import javafx.scene.control.TextArea;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Consumer;

public class ErrorLabelConsumer implements Consumer {
    private Label myErrorLabel;


    public ErrorLabelConsumer(Label l){
        this.myErrorLabel = l;
    }

    @Override
    public void accept(Object o) {
        if(o == null)
            return;
        myErrorLabel.setVisible(true);
        myErrorLabel.setText((String) o);
    }


}
