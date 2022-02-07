import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class mainSceneController implements Initializable {


    @FXML
    private TableView<Target> targetTable;

    @FXML
    private TableColumn<Target, String> nameCol;

    @FXML
    private TableColumn<Target, Target.GraphStatus> locationCol;

    @FXML
    private TableColumn<Target, ArrayList<String>> directDepOnCol;

    @FXML
    private TableColumn<Target, ArrayList<String>> allDepOnCol;

    @FXML
    private TableColumn<Target, ArrayList<String>> directReqForCol;

    @FXML
    private TableColumn<Target, ArrayList<String>> allReqForCol;

    @FXML
    private TableColumn<Target, String> freeInfoCol;

    @FXML
    private TableColumn<Target, Integer> serialSetsCol;

    @FXML
    private RadioButton depOnRadioButton;

    @FXML
    private RadioButton reqForRadioButton;

    @FXML
    private RadioButton whatIfdepOnRadioButton;

    @FXML
    private RadioButton whatIfreqForRadioButton;


    @FXML
    private AnchorPane anchorForFindRoute;


    @FXML
    private AnchorPane findCycleAnchorPane;


    @FXML
    private MenuButton fromMenuButon;

    @FXML
    private MenuButton toMenuButon;

    @FXML
    private AnchorPane anchorPane;

    @FXML
    private AnchorPane whatIfAnchorPane;

    @FXML
    private MenuButton whatIfMenuButton;

    @FXML
    private Label targetsInSystemTextLabel;

    @FXML
    private Label numberOfTargetsLabel;

    @FXML
    private Label indiTextLabel;

    @FXML
    private Label leafTextLabel;

    @FXML
    private Label rootTextLabel;

    @FXML
    private Label midTextLabel;

    @FXML
    private Label leafNumLabel;

    @FXML
    private Label rootNumLabel;

    @FXML
    private Label midNumLabel;

    @FXML
    private Label indiNumLabel;

    @FXML
    private Label targetSelectedLabelWhatIf;

    @FXML
    private Label serialSetsInSystemTextLabel;

    @FXML
    private Label numberOfSetsLabel;

    @FXML
    private Label srcTargetLabel;

    @FXML
    private Label destTargetLabel;


    @FXML
    private Button findRouteButton;

    @FXML
    private Button findCycleButton;

    @FXML
    private Button whatIfButton;

    @FXML
    private Button runCompilationButton;

    @FXML
    private Button runTaskButton;


    @FXML
    private Button findRouteExecButton;


    @FXML
    private MenuButton targetMenuButton;

    @FXML
    private Label targetSelectedLabel;

    @FXML
    private ListView<String> whatIfListView;

    @FXML
    private ListView<List<String>> findRouteList;

    @FXML
    void toMenuOptionSelected(ActionEvent event) {

    }

    @FXML
    void fromMenuOptionSelected(ActionEvent event) {

    }

    @FXML
    void runCompilationButtonPressed(ActionEvent event) {

    }

    @FXML
    void runTaskButtonPressed(ActionEvent event) {
        changeScene(event, "taskScene.fxml");
    }

    @FXML
    private ListView<List<String>> findCycleList;

    @FXML
    void whatIfButtonPressed(ActionEvent event) {
        findCycleAnchorPane.setDisable(true);
        anchorForFindRoute.setDisable(true);
        findRouteList.setDisable(true);
        findCycleList.setDisable(true);

        whatIfAnchorPane.setDisable(false);
        whatIfListView.setDisable(false);
        ToggleGroup group = new ToggleGroup();
        whatIfdepOnRadioButton.setToggleGroup(group);
        whatIfreqForRadioButton.setToggleGroup(group);


        for (Target target : Gpup.getInstance().getTargetList()) {
            MenuItem newMenuItem = new MenuItem(target.getTargetName());
            newMenuItem.setOnAction(e -> {
                targetSelectedLabelWhatIf.setText(target.getTargetName());
                ObservableList<String> observableList;

                if(whatIfdepOnRadioButton.isSelected()){
                    List<String> allNames = Gpup.getInstance().getTarget(target.getTargetName()).
                            getAllDependsForNames();

                    if(allNames == null){
                        ArrayList<String> empty = new ArrayList<>();
                        empty.add("No depends on");
                        observableList  = FXCollections.observableList(empty);
                        whatIfListView.setItems(observableList);
                        return;
                    }

                    observableList  = FXCollections.observableList(allNames);
                    whatIfListView.setItems(observableList);
                }

                else if(whatIfreqForRadioButton.isSelected()){
                    List<String> allNames = Gpup.getInstance().getTarget(target.getTargetName()).
                            getAllRequiredForNames();

                    if(allNames == null){
                        ArrayList<String> empty = new ArrayList<>();
                        empty.add("No required for");
                        observableList  = FXCollections.observableList(empty);
                        whatIfListView.setItems(observableList);
                        return;
                    }
                    observableList  = FXCollections.observableList(allNames);
                    whatIfListView.setItems(observableList);
                }
                else{return;}
            });


            whatIfMenuButton.getItems().add(newMenuItem);
        }

    }

    @FXML
    void whatIfradioButtonSelected(ActionEvent event) {

    }
    @FXML
    void findCycleButtonPressed(ActionEvent event) {
        whatIfAnchorPane.setDisable(true);
        whatIfListView.setDisable(true);
        anchorForFindRoute.setDisable(true);
        findRouteList.setDisable(true);

        findCycleAnchorPane.setDisable(false);
        findCycleList.setDisable(false);


        if(  !targetMenuButton.getItems().isEmpty())
            return;

        for (Target target : Gpup.getInstance().getTargetList()) {
            MenuItem newMenuItem = new MenuItem(target.getTargetName());
            newMenuItem.setOnAction(e -> {
                targetSelectedLabel.setText(target.getTargetName());
            });
            targetMenuButton.getItems().add(newMenuItem);
        }

    }



    ////////////////////////////////////////////////////////


    @FXML
    void findRouteButtonPresed(ActionEvent event) {
        findCycleAnchorPane.setDisable(true);
        findCycleList.setDisable(true);
        whatIfAnchorPane.setDisable(true);
        whatIfListView.setDisable(true);

        anchorForFindRoute.setDisable(false);
        findRouteList.setDisable(false);

        ToggleGroup group = new ToggleGroup();
        depOnRadioButton.setToggleGroup(group);
        reqForRadioButton.setToggleGroup(group);


        if(  ! (fromMenuButon.getItems().isEmpty() && toMenuButon.getItems().isEmpty()) )
            return;

        for (Target target: Gpup.getInstance().getTargetList()) {
            MenuItem newMenuItemFrom = new MenuItem(target.getTargetName());
            MenuItem newMenuItemTo = new MenuItem(target.getTargetName());
            newMenuItemFrom.setOnAction(e->{
                srcTargetLabel.setText(target.getTargetName());
            });

            newMenuItemTo.setOnAction(e->{
                destTargetLabel.setText(target.getTargetName());
            });

            fromMenuButon.getItems().add(newMenuItemFrom);

            toMenuButon.getItems().add(newMenuItemTo);
        }


    }



    @FXML
    void findCycleExecButtonPressed(ActionEvent event) {
        List<List<String>> cyclePath = Gpup.getInstance().findRoutesBetweenTwoTargets(targetSelectedLabel.getText(),
                targetSelectedLabel.getText(),
                Graph.RoutSelection.DEPENDS_ON);
        ObservableList<List<String>> observableList;

        if(cyclePath.isEmpty()){
            List<String> firstList = new ArrayList<>();
            firstList.add("No cycle found.");
            List<List<String>> secondList = new ArrayList<>();
            secondList.add(firstList);
            observableList = FXCollections.observableList(secondList);
            findCycleList.setItems(observableList);
            return;
        }

            observableList = FXCollections.observableList(cyclePath);
            findCycleList.setItems(observableList);
    }




    @FXML
    void radioButtonSelected(ActionEvent event) {
        /*if(depOnRadioButton.isSelected())
            reqForRadioButton.setSelected(false);

        if(reqForRadioButton.isSelected())
            depOnRadioButton.setSelected(false);
*/}


    @FXML
    void findRouteExecButtonPressed(ActionEvent event) {
        List<List<String>> allPaths ;
        ObservableList<List<String>> observableList;

        if(depOnRadioButton.isSelected()) {
           allPaths = Gpup.getInstance().findRoutesBetweenTwoTargets(srcTargetLabel.getText(),destTargetLabel.getText(),
                   Graph.RoutSelection.DEPENDS_ON);
        }
        else if(reqForRadioButton.isSelected()){
            allPaths = Gpup.getInstance().findRoutesBetweenTwoTargets(srcTargetLabel.getText(),destTargetLabel.getText(),
                    Graph.RoutSelection.REQUIRED_FOR);

        }
        else{return;}

        if(allPaths.isEmpty()){
            List<String> firstList = new ArrayList<>();
            firstList.add("No path found.");
            List<List<String>> secondList = new ArrayList<>();
            secondList.add(firstList);
            observableList = FXCollections.observableList(secondList);
            findRouteList.setItems(observableList);
            return;
        }

        observableList = FXCollections.observableList(allPaths);
        findRouteList.setItems(observableList);
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        //table
        nameCol.setCellValueFactory(new PropertyValueFactory<>("targetName"));
        locationCol.setCellValueFactory(new PropertyValueFactory<>("statusInGraph"));
        directDepOnCol.setCellValueFactory(new PropertyValueFactory<>("dependsOnNames"));
        directReqForCol.setCellValueFactory(new PropertyValueFactory<>("requiredForNames"));
        allDepOnCol.setCellValueFactory(new PropertyValueFactory<>("allDependsForNames"));
        allReqForCol.setCellValueFactory(new PropertyValueFactory<>("allRequiredForNames"));
        freeInfoCol.setCellValueFactory(new PropertyValueFactory<>("userData"));
        serialSetsCol.setCellValueFactory(new PropertyValueFactory<>("sets"));
        //labels
        numberOfTargetsLabel.setText(Integer.toString(Gpup.getInstance().getTotalCountOfTargets()));
        indiNumLabel.setText(Integer.toString(Gpup.getInstance().getCountOfIndependentTargets()));
        leafNumLabel.setText(Integer.toString(Gpup.getInstance().getCountOfLeafTargets()));
        rootNumLabel.setText(Integer.toString(Gpup.getInstance().getCountOfRootTargets()));
        midNumLabel.setText(Integer.toString(Gpup.getInstance().getCountOfMiddleTargets()));
        numberOfSetsLabel.setText(" " + Gpup.getInstance().getSerialSets().size());
        if(Gpup.getInstance().getSerialSets().size() > 0){
          createLabelsForSerialSets();
        }
        //


        targetTable.setItems(Gpup.getInstance().getTargetList());
    }

    private void createLabelsForSerialSets() {
        ArrayList<Label> labels = new ArrayList<>();
        ArrayList<ListView<String>> lists = new ArrayList<>();

        if(Gpup.getInstance().getSerialSets() == null)
            return;

            for (String serialSetName : Gpup.getInstance().getSerialSets().keySet()) {
            Label label = new Label(serialSetName);
            ListView<String> list = new ListView<String>();


            label.setText(serialSetName + ": ");
            label.setFont(leafTextLabel.getFont());
            anchorPane.getChildren().add(label);
            labels.add(label);


            ObservableList<String> observableList = FXCollections.observableList(Gpup.getInstance().getSerialSets().get(serialSetName));
            list.setItems(observableList);
            list.setOrientation(Orientation.HORIZONTAL);
            list.setMaxHeight(25);
            anchorPane.getChildren().add(list);
            lists.add(list);
        }

        double layoutX = midTextLabel.getLayoutX();
        double layoutY = numberOfSetsLabel.getLayoutY() + 70;
        double listX = indiNumLabel.getLayoutX();

        for (Label label : labels){
            label.setLayoutX(layoutX);
            label.setLayoutY(layoutY);
            layoutY += 25;
        }

        layoutY = numberOfSetsLabel.getLayoutY() + 70;

        for(ListView<String> list : lists){
            list.setLayoutX(listX);
            list.setLayoutY(layoutY);

            
            layoutY += 25;
        }
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

}
