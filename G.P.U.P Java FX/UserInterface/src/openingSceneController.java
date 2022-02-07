import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;


public class openingSceneController implements Initializable {
    FileChooser fileChooser = new FileChooser();

    @FXML
    private TextArea alertTextArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
    }

    public void loadFilePressed(ActionEvent actionEvent) {
        try {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("xml files", "*.xml"));
            File file = fileChooser.showOpenDialog(new Stage());
            Gpup.getInstance().buildGraphFromXml(file.getPath());
            changeScene(actionEvent,"mainScene.fxml");
        }  catch (FileNotFoundException | WrongFileTypeException | TargetNameExistsException |
                UnknownTargetInDescpritionException | ConflictException | SerialSettNameExistsException e){
            alertTextArea.setVisible(true);
            alertTextArea.setText(e.getMessage());
            return;
        }

        catch(JAXBException e) {
            System.out.println(e.getMessage());
            return;
        }
    }


    //When this method is called, it will change the Scene
    public void changeScene(ActionEvent event, String sceneFile){
        try {
            Parent mainSceneParent = FXMLLoader.load(getClass().getResource(sceneFile));
            Scene mainScene = new Scene(mainSceneParent);
            //Getting the Stage information
            Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();

            window.setScene((mainScene));
            window.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}
