import javafx.scene.control.ListCell;
import javafx.scene.paint.Color;


public class FormatCell extends ListCell<String> {

    public FormatCell() {}


    @Override protected void updateItem(String item, boolean empty) {
        // calling super here is very important - don't skip this!
        super.updateItem(item, empty);


        if (item != null) {
            setTextFill(item.contains(Target.RunResult.FAILURE.toString()) ? Color.RED :
                    item.contains(Target.RunResult.WARNING.toString())  ? Color.YELLOW : Color.GREEN);
        }
    }
}