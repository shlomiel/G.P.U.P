import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

class RoomCell extends ListCell<Target> {
    @Override
    public void updateItem(Target item, boolean empty) {
        super.updateItem(item, empty);

        if (item != null) {

            final Label lbl = new Label(item.getTargetName()); // The room name will be displayed here
            lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
            lbl.setStyle("-fx-text-fill: green");

            if(item.getlastRunResult().equals(Target.RunResult.WARNING)) {
                lbl.setStyle("-fx-text-fill: yellow");
            }

            if(item.getlastRunResult().equals(Target.RunResult.FAILURE)) {
                lbl.setStyle("-fx-text-fill: red");
            }
            setGraphic(lbl);

        }
    }
}