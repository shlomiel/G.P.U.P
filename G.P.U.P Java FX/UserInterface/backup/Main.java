import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;



public class Main extends Application{
    @Override
    public void start(Stage primaryStage) throws Exception {
            Parent rootContainer = FXMLLoader.load(getClass().getResource("backup/Main.fxml"));
            primaryStage.setScene(new Scene(rootContainer, 500, 400));
            primaryStage.setTitle("Live Example");
            primaryStage.show();
        }

        public static void main(String[] args) {
            Thread.currentThread().setName("main");
            launch(args);
        }
/*
    public static void main(String[] args) {
	// write your code here
        GpupUI newGPUPSystem = new GpupUI();
        newGPUPSystem.startUserInteraction();
    }
*/

}
