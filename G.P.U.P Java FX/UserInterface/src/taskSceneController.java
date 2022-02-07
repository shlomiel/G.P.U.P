import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class taskSceneController implements Initializable {
    FileChooser fileChooser = new FileChooser();



    @FXML
    private TableView<Target> targetTable;

    @FXML
    private TableColumn<Target, String> nameCol;

    @FXML
    private TableColumn<Target, String> selectCol;


    @FXML
    private MenuButton targetMenuButton;


   /* @FXML
    void targetSelected(ActionEvent event) {
        for (Target target : Gpup.getInstance().getTargetList()) {
            MenuItem newMenuItem = new MenuItem(target.getTargetName());
            newMenuItem.setOnAction(e -> {
                for(:targetTable.getItems()){

                    }

                    observableList  = FXCollections.observableList(allNames);
                    whatIfListView.setItems(observableList);
                }
    }
    }
*/


    @FXML
    private AnchorPane buttomAnchorPane;

    @FXML
    private Button backButton;

    @FXML
    private AnchorPane centerAnchorPane;

    @FXML
    private CheckBox whatIfEnableCheckBox;

    @FXML
    private RadioButton requiredForRadioButton;

    @FXML
    private RadioButton dependsOnRadioButon;

    @FXML
    private RadioButton scratchRadioButton;

    @FXML
    private RadioButton incRadioButton;

    @FXML
    private TextField numOfThreadsTextField;

    @FXML
    private Label numOfThreadsLabel;

    @FXML
    private Button startButton;

    @FXML
    private AnchorPane simulationTaskAnchorPane;

    @FXML
    private Label processingTimeLabel;

    @FXML
    private Label randOrConstLabel;

    @FXML Label progressPercentLabel;

    @FXML
    private Label successProbLabel;

    @FXML
    private Label warningProbLabel;

    @FXML
    private TextField msField;

    @FXML
    private TextField successField;

    @FXML
    private TextField warningField;

    @FXML
    private RadioButton randRadioButton;

    @FXML
    private RadioButton constRadioButton;

    @FXML ProgressBar taskProgressBar;


    @FXML
    public ListView<Target> frozenListt;

    @FXML
    private Label frozenLabel;

    @FXML
    public ListView<Target> skippedList;

    @FXML
    private Label skippedLabel;

    @FXML
    private Label waitingLabel;

    @FXML
    public ListView<Target> waitingList;

    @FXML
    public ListView<Target> inProcessList;

    @FXML
    private Label inProcessLabel;

    @FXML
    private Label finishedLabel;

    @FXML
    public ListView<Target> finishedList;
    @FXML
    private Button pauseButton;

    @FXML
    private AnchorPane compilationTaskAnchorPane;

    @FXML
    private TextField outPathTextField;

    @FXML
    private TextField sourcePathTextField;

    public TextArea getMidrunTextArea() {
        return midrunTextArea;
    }

    @FXML
    private TextArea midrunTextArea;

    @FXML
    private RadioButton simulationTaskRadioButton;

    @FXML
    private RadioButton compilationTaskRadioButton;

    @FXML void simulationTaskRadioButtonSelected(ActionEvent event){
        simulationTaskAnchorPane.setDisable(false);
        compilationTaskAnchorPane.setDisable(true);
    }

    @FXML void compilationTaskRadioButtonSelected(ActionEvent event){
        compilationTaskAnchorPane.setDisable(false);
        simulationTaskAnchorPane.setDisable(true);
    }


    @FXML
    private Label errorLabel;


    @FXML
    private Tooltip frozenListToolTip;


    @FXML
    private ScrollPane scrollPane;

    @FXML
    private BorderPane borderpane;


    @FXML
    void startButtonPressed(ActionEvent event) {

        try {


            ArrayList<Target> targetsForRun = getTargetsFromWhafIf();
            ErrorLabelConsumer errorLabelConsumer = new ErrorLabelConsumer(this.errorLabel);

            Graph.RunningTimeType runType;
            int incOrScratch = 0;
            Consumer<String> consumer = System.out::println;

            if(scratchRadioButton.isSelected()){
                incOrScratch = 1;
            }
            else if(incRadioButton.isSelected()){
                incOrScratch = 2;
            }

            else {errorLabelConsumer.accept("no increment or from scratch selection detcted "); return;}

            if(numOfThreadsTextField.getText().isEmpty() || Integer.parseInt(numOfThreadsTextField.getText()) < 1 ||
                    Integer.parseInt(numOfThreadsTextField.getText()) > Gpup.getInstance().getMaxParallelism() ){
                errorLabelConsumer.accept("illegal number of threads"); return;
            }


            if(simulationTaskRadioButton.isSelected()) { // Run simulation task

                if(randRadioButton.isSelected())
                    runType = Graph.RunningTimeType.RANDOM;

                else if(constRadioButton.isSelected())
                    runType = Graph.RunningTimeType.FIXED;

                else {errorLabelConsumer.accept("no run time type selection detected ");return;}


                errorLabelConsumer.accept(" ");
                Gpup.getInstance().runSimulationTask(this, Integer.parseInt(msField.getText()), runType,
                        Double.parseDouble(successField.getText()), Double.parseDouble(warningField.getText()),
                        consumer, incOrScratch, Integer.parseInt(numOfThreadsTextField.getText()),targetsForRun);
            }


            else if(compilationTaskRadioButton.isSelected()){
                if(sourcePathTextField.getText().isEmpty() || outPathTextField.getText().isEmpty()){
                    errorLabelConsumer.accept("Please provide paths as required");
                }


                errorLabelConsumer.accept(" ");
                Gpup.getInstance().runCompilationTask(this, consumer, incOrScratch, Integer.parseInt(numOfThreadsTextField.getText()),
                        sourcePathTextField.getText(),outPathTextField.getText(),targetsForRun);
            }


            incRadioButton.setDisable(false);

        }

        catch (InterruptedException  | IOException e) {
            e.printStackTrace();
        }
        catch (ParseException e) {
            errorLabel.setText("wrong input for probabilities");
        }

    }

    private ArrayList<Target> getTargetsFromWhafIf() {
        ArrayList<Target> res = new ArrayList<>();
        for (Target t : targetTable.getItems()) {
            if(t.getSelect().isSelected()){
                res.add(t);
            }
        }
        return res;
    }


    @FXML
    private Button resetButton;



    @FXML
    void resetButtonPressed(ActionEvent event) {

        changeScene(event,"taskScene.fxml");


        /*targetTable.setItems(Gpup.getInstance().getTargetList());
        simulationTaskAnchorPane.setDisable(true);
        compilationTaskAnchorPane.setDisable(true);
        midrunTextArea.setVisible(false);
        compilationTaskRadioButton.setSelected(false);
        simulationTaskRadioButton.setSelected(false);


        this.progressPercentLabel.textProperty().unbind();
        progressPercentLabel.setText("0%");
        this.taskProgressBar.progressProperty().unbind();
        taskProgressBar.progressProperty().set(0);
        finishedList.setItems(null);
        skippedList.setItems(null);
        frozenListt.setItems(null);
        inProcessList.setItems(null);
        waitingList.setItems(null);
*/

    }


    @FXML
    void pauseButtonPressed(ActionEvent event) {
        if(pauseButton.getText().equals("Pause")) {
            Gpup.getInstance().getPause().set(true);
            pauseButton.setText("Resume");
        }

        else if(pauseButton.getText().equals("Resume")) {
            Gpup.getInstance().unpauseSystem();
            pauseButton.setText("Pause");
        }


    }



    @FXML
    void whatIfEnableCheckBoxChecked(ActionEvent event) {

        if(whatIfEnableCheckBox.isSelected()) {
            requiredForRadioButton.setDisable(false);
            dependsOnRadioButon.setDisable(false);
            targetMenuButton.setDisable(false);
        }

        if(!whatIfEnableCheckBox.isSelected())
        {
            requiredForRadioButton.setDisable(true);
            dependsOnRadioButon.setDisable(true);
            targetMenuButton.setDisable(true);
        }


        for (Target target : Gpup.getInstance().getTargetList()) {
            MenuItem newMenuItem = new MenuItem(target.getTargetName());
            newMenuItem.setOnAction(e -> {
                if(requiredForRadioButton.isSelected()) {
                    for (Target targetInTable : targetTable.getItems()) {
                        targetInTable.getSelect().setSelected(false);
                        if (target.getRequiredFor().contains(targetInTable))
                            targetInTable.getSelect().setSelected(true);
                    }
                }

                else if(dependsOnRadioButon.isSelected()){
                    for (Target targetInTable : targetTable.getItems()) {
                        targetInTable.getSelect().setSelected(false);
                        if (target.getDependsOn().contains(targetInTable))
                            targetInTable.getSelect().setSelected(true);
                    }
                }
                targetMenuButton.setText(target.getTargetName());
            });
            targetMenuButton.getItems().add(newMenuItem);
        }
    }


    @FXML
    void backButtonPressed(ActionEvent event) {
        changeScene(event, "mainScene.fxml");

    }

    @FXML
  /*  void frozenListPressed(MouseEvent event) {
        String selected = frozenListt.getSelectionModel().getSelectedItem();
        frozenListToolTip.setText(selected);
    }
*/


    public void initialize(URL location, ResourceBundle resources) {
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        //scrollPane.setContent(borderpane);


        nameCol.setCellValueFactory(new PropertyValueFactory<>("targetName"));
        selectCol.setCellValueFactory(new PropertyValueFactory<>("select"));
        targetTable.setItems(Gpup.getInstance().getTargetList());


        ToggleGroup group1 = new ToggleGroup(), group2 = new ToggleGroup(), group3 = new ToggleGroup();
        ToggleGroup groupTaskType = new ToggleGroup();

        dependsOnRadioButon.setToggleGroup(group1);
        requiredForRadioButton.setToggleGroup(group1);
        incRadioButton.setToggleGroup(group2);
        scratchRadioButton.setToggleGroup(group2);
        randRadioButton.setToggleGroup(group3);
        constRadioButton.setToggleGroup(group3);
        compilationTaskRadioButton.setToggleGroup(groupTaskType);
        simulationTaskRadioButton.setToggleGroup(groupTaskType);

        if(Gpup.getInstance().getFirstRun()){
            incRadioButton.setDisable(true);
        }



        setListsOnSelection();


    }

    private void setListsOnSelection() {



        frozenListt.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Target>() {
            @Override
            public void changed(ObservableValue<? extends Target> observable, Target oldValue, Target newValue) {
                // Your action here
                if (newValue == null)
                    return;
                midrunTextArea.visibleProperty().set(true);
                StringBuilder builder = new StringBuilder();
                builder.append("My name: " + newValue.getTargetName() + '\n');
                builder.append("My status: " + newValue.getStatusInRun().toString() + '\n');
                builder.append("My serial sets: " + newValue.getMySets() + '\n');
                midrunTextArea.setText(builder.toString());
            }


        });

        waitingList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Target>() {

            @Override
            public void changed(ObservableValue<? extends Target> observable, Target oldValue, Target newValue) {
                if(newValue == null)
                    return;
                midrunTextArea.visibleProperty().set(true);
                StringBuilder builder = new StringBuilder();
                builder.append("My name: " + newValue.getTargetName() + '\n');
                builder.append("My status: " + newValue.getStatusInRun().toString() + '\n');
                builder.append("My serial sets: " + newValue.getMySets() + '\n');
                builder.append("In line for : " + (System.currentTimeMillis() - newValue.getLongStartTime()) + " ms\n");
                midrunTextArea.setText(builder.toString());
            }
        });

        inProcessList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Target>() {
            @Override
            public void changed(ObservableValue<? extends Target> observable, Target oldValue, Target newValue) {
                System.out.println("IN PRESS PROCCESSED BuTTON");
                if(newValue == null)
                    return;
                long ms = System.currentTimeMillis() - newValue.getStartProcess();
                midrunTextArea.visibleProperty().set(true);
                StringBuilder builder = new StringBuilder();
                builder.append("My name: " + newValue.getTargetName() + '\n');
                builder.append("My status: " + newValue.getStatusInRun().toString() + '\n');
                builder.append("My serial sets: " + newValue.getMySets() + '\n');
                builder.append("In Process for : " + ms + " ms\n");
                midrunTextArea.setText(builder.toString());
            }
        });


        skippedList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Target>() {
            @Override
            public void changed(ObservableValue<? extends Target> observable,Target oldValue, Target newValue) {
                if(newValue == null)
                    return;
                midrunTextArea.visibleProperty().set(true);
                StringBuilder builder = new StringBuilder();
                builder.append("My name: " + newValue.getTargetName()+'\n');
                builder.append("My status: "  + newValue.getStatusInRun().toString()+'\n');
                builder.append("My serial sets: " + newValue.getMySets()+'\n');
                builder.append("Skipped because targets : "  + newValue.getFailedTargets() +" failed\n");
                midrunTextArea.setText(builder.toString());
            }

        });
        finishedList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Target>() {

            @Override
            public void changed(ObservableValue<? extends Target> observable,Target oldValue, Target newValue) {
                if(newValue == null)
                    return;
                midrunTextArea.visibleProperty().set(true);
                StringBuilder builder = new StringBuilder();
                builder.append("My name: " + newValue.getTargetName()+'\n');
                builder.append("My status: "  + newValue.getStatusInRun().toString()+'\n');
                builder.append("My serial sets: " + newValue.getMySets()+'\n');
                builder.append("Result of the run: "  + newValue.getlastRunResult().toString() + " ran for " +
                        newValue.getLastSuccessfulRun() + " ms\n");
                midrunTextArea.setText(builder.toString());
            }

        });
        finishedList.setCellFactory(new Callback<ListView<Target>, ListCell<Target>>() {
            @Override public ListCell<Target> call(ListView<Target> list) {
                return new RoomCell();
            }
        });
    }


    //When this method is called, it will change the Scene
    public void changeScene(ActionEvent event, String sceneFile) {
        try {
            Parent mainSceneParent = FXMLLoader.load(getClass().getResource(sceneFile));
            Scene mainScene = new Scene(mainSceneParent);
            //Getting the Stage information
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();

            window.setScene((mainScene));
            window.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void bindTaskToUIComponents(Task aTask) {

        // task progress bar
        taskProgressBar.progressProperty().bind(aTask.progressProperty());

        // task percent label
        progressPercentLabel.textProperty().bind(
                Bindings.concat(
                        Bindings.format(
                                "%.0f",
                                Bindings.multiply(
                                        aTask.progressProperty(),
                                        100)),
                        " %"));

       // waitingList.accessibleTextProperty().bind((SimulationTask)aTask.frozenTargets);

    }

        // task cleanup upon finish
       // aTask.valueProperty().addListener((observable, oldValue, newValue) -> {
         //   onTaskFinished(Optional.ofNullable(onFinish));
     //   });




    public void onTaskFinished(Optional<Runnable> onFinish) {
        //this.taskMessageLabel.textProperty().unbind();
        this.progressPercentLabel.textProperty().unbind();
        this.taskProgressBar.progressProperty().unbind();
        onFinish.ifPresent(Runnable::run);
    }






}



