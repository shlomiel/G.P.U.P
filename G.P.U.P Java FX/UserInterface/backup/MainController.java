import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class MainController {

    private final static String PREFIX = "<PREFIX START>";
    private final static String SUFFIX = "<SUFFIX START>";

    @FXML
    private CheckBox dareToCheckMeCB;

    @FXML
    private TextArea descriptionTA;

    @FXML
    private Label bruria;
    @FXML
    private TextField nameTextField;

    @FXML
    void clickMeButtonAction(ActionEvent event) {
        String nameText = nameTextField.getText();
        String taText = descriptionTA.getText();
        String addition = PREFIX + nameText + SUFFIX;
        descriptionTA.setText(taText + addition);
    }

    @FXML
    void dareCheckBoxAction(ActionEvent event) {

        descriptionTA.setDisable(dareToCheckMeCB.isSelected());
    }

}
