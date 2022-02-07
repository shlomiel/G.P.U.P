
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.Initializable;
import resources.generated.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


public class Gpup {

    public synchronized  void waitOnMyPause() throws InterruptedException {
        while(getPause().get() == true){
            wait();
        }
    }

    public synchronized AtomicBoolean getPause() {
        return pause;
    }

    private AtomicBoolean pause = new AtomicBoolean();


    private final Graph systemGraph = new Graph();
    private String xmlFilePath;

    public Map<String, Target> getTargetMapToBuildGraph() {
        return targetMapToBuildGraph;
    }

    private Map<String,Target> targetMapToBuildGraph;

    public Graph getIncrementalGraph() {
        return IncrementalGraph;
    }

    private final Graph IncrementalGraph = new Graph();

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    private String workingDirectory;

    public void setFirstRun(boolean firstRun) {
        this.firstRun = firstRun;
    }

    private boolean firstRun = true;
    private int maxParallelism;


    private Task<Boolean> currentRunningTask;
    private taskSceneController currentController;

    public Map<String, List<String>> getSerialSets() {

        return serialSets;
    }

    private final Map<String, List<String>> serialSets = new HashMap<>();
    private static final Gpup singleGpupInstance = new Gpup();
    private Gpup(){pause.set(false);}

    public synchronized static Gpup getInstance(){ // Singleton class
        return singleGpupInstance;
    }
    private volatile int counter = 0;

    public ExecutorService executor;
    private GPUPDescriptor descriptionOfTargets;


    private synchronized void updateCounter()
    {
        counter++;
    }

    public  void  buildGraphFromXml(String filePath) throws FileNotFoundException, JAXBException, WrongFileTypeException, TargetNameExistsException, UnknownTargetInDescpritionException, ConflictException, SerialSettNameExistsException {
        this.xmlFilePath = filePath;

        verifyXmlIntegrity(filePath);

        InputStream inputStream = new FileInputStream(filePath);


        this.descriptionOfTargets = deserializeFrom(inputStream);
        GPUPConfiguration gpupConfiguration = descriptionOfTargets.getGPUPConfiguration();

        String graphName = gpupConfiguration.getGPUPGraphName();
        workingDirectory = gpupConfiguration.getGPUPWorkingDirectory();
        maxParallelism = gpupConfiguration.getGPUPMaxParallelism();

        Map<String,Target> targetMap= getTargetsFromDescriptor(descriptionOfTargets);
        buildSystemGraph(graphName, targetMap);


        addDependenciesToTargets(targetMap,descriptionOfTargets);
        setAllTargetsStatus();
       // targetArrayBlockingQueue = new ArrayBlockingQueue<Target>(systemGraph.getAdjTargets().size(),false);
    }

    /*protected synchronized  void addToBlockingQue(Target T) throws InterruptedException {

        if(!targetArrayBlockingQueue.contains(T)){
            System.out.println("in add function ");
            targetArrayBlockingQueue.put(T);
            System.out.println(targetArrayBlockingQueue);
        }
    }
    */

    private void verifyXmlIntegrity(String filePath) throws FileNotFoundException, WrongFileTypeException, JAXBException,
            TargetNameExistsException, UnknownTargetInDescpritionException, ConflictException, SerialSettNameExistsException {



        if( !filePath.startsWith(".xml", filePath.length()-4) ) // 2.1
            throw new WrongFileTypeException("Wrong file type, the required type is .xml");

        InputStream inputStream = new FileInputStream(filePath);
        Map<String,ArrayList<String>> targetsInFile = new HashMap<>();
        GPUPDescriptor descriptionOfTargets = deserializeFrom(inputStream);

        for (GPUPTarget gpupTarget : descriptionOfTargets.getGPUPTargets().getGPUPTarget()) { // 2.2
            if (targetsInFile.containsKey(gpupTarget.getName())) {
                throw new TargetNameExistsException("Failed loading file, this xml file contains two targets with the same name ");
            }

            targetsInFile.put(gpupTarget.getName(), new ArrayList<>());
        }
        for(GPUPTarget gpupTarget: descriptionOfTargets.getGPUPTargets().getGPUPTarget()){
            if(gpupTarget.getGPUPTargetDependencies()!=null) { //2.3
                for (GPUPTargetDependencies.GPUGDependency dependencyTarget : gpupTarget.getGPUPTargetDependencies().getGPUGDependency()) {
                    if (!targetsInFile.containsKey(dependencyTarget.getValue())) {
                        throw new UnknownTargetInDescpritionException("Failed loading file, This xml file contains an undeclared target named " + dependencyTarget.getValue() +
                                " in target "+ gpupTarget.getName() + " dependency list ");
                    }

                    if (dependencyTarget.getType().equals("requiredFor")) {
                        if (targetsInFile.get(dependencyTarget.getValue()).contains(gpupTarget.getName() + dependencyTarget.getType())) {
                            throw new ConflictException("Failed loading file, if target " + gpupTarget.getName() + " is requiredFor " + dependencyTarget.getValue() +
                                    " than " + dependencyTarget.getValue() + " can’t be requiredFor " + gpupTarget.getName());
                        }
                    } else {
                        if (targetsInFile.get(dependencyTarget.getValue()).contains(gpupTarget.getName() + dependencyTarget.getType())) {
                            throw new ConflictException("Failed loading file, if target " + gpupTarget.getName() + " dependsOn  " + dependencyTarget.getValue() +
                                    " then " + dependencyTarget.getValue() + " can’t be dependsOn  " + gpupTarget.getName());
                        }
                    }

                    targetsInFile.get(gpupTarget.getName()).add(dependencyTarget.getValue()+dependencyTarget.getType());
                }
            }
        }

        if(descriptionOfTargets.getGPUPSerialSets()==null)
                return;
        for (GPUPDescriptor.GPUPSerialSets.GPUPSerialSet gpupSet : descriptionOfTargets.getGPUPSerialSets().getGPUPSerialSet()){
            if(serialSets.containsKey(gpupSet.getName()))
                throw new SerialSettNameExistsException("Failed loading file, this xml file contains two serial sets with the same name ");

            String[] targets =  gpupSet.getTargets().split(",");
            for(String targetName : targets){
                if(!targetsInFile.containsKey(targetName)){
                    throw new UnknownTargetInDescpritionException("Failed loading file, This xml file contains an undeclared target named " + targetName +
                            " in the serial sets declaration");
                }
            }

            serialSets.put(gpupSet.getName(), Arrays.asList(targets));
        }
    }

    private void setAllTargetsStatus() {
        systemGraph.setNodesStatus(serialSets);
    }

    private Map<String,Target>  getTargetsFromDescriptor(GPUPDescriptor descriptionOfTargets) {

        Map<String,Target> targetMap= new HashMap<>();
        for (GPUPTarget GPUPtarget: descriptionOfTargets.getGPUPTargets().getGPUPTarget()) {
            targetMap.putIfAbsent(GPUPtarget.getName(),new Target(GPUPtarget.getName().toUpperCase(), GPUPtarget.getGPUPUserData()));
        }

        return targetMap;
    }

    private void buildSystemGraph(String graphName, Map<String,Target> targetMap) {
        this.targetMapToBuildGraph = targetMap;
        systemGraph.buildGraph(graphName, targetMap);
        IncrementalGraph.buildGraph(graphName,targetMap);

    }

    private  void addDependenciesToTargets(Map<String,Target> targets, GPUPDescriptor descriptionOftargets) {

        GPUPTargets targetsFromFile = descriptionOftargets.getGPUPTargets();

        for (Target target : targets.values()) {
            for (GPUPTarget GPUPtarget : targetsFromFile.getGPUPTarget()) {
                if (target.getTargetName().equals(GPUPtarget.getName()) && GPUPtarget.getGPUPTargetDependencies() != null) {
                    for (GPUPTargetDependencies.GPUGDependency gpupTargetDep : GPUPtarget.getGPUPTargetDependencies().getGPUGDependency()) {
                        if (gpupTargetDep.getType().equals("requiredFor")) {
                            systemGraph.addEdge(gpupTargetDep.getValue(), target.toString());
                            IncrementalGraph.addEdge(gpupTargetDep.getValue(), target.toString());
                        } else {
                            systemGraph.addEdge(target.toString(), gpupTargetDep.getValue());
                            IncrementalGraph.addEdge(target.toString(), gpupTargetDep.getValue());
                        }
                    }
                    break;
                }
            }
        }
    }


        protected  void addDependenciesToTargets(Graph graph) {

            GPUPTargets targetsFromFile = descriptionOfTargets.getGPUPTargets();

            for (Target target : this.targetMapToBuildGraph.values()) {
                for (GPUPTarget GPUPtarget : targetsFromFile.getGPUPTarget()) {
                    if (target.getTargetName().equals(GPUPtarget.getName()) && GPUPtarget.getGPUPTargetDependencies() != null) {
                        for (GPUPTargetDependencies.GPUGDependency gpupTargetDep : GPUPtarget.getGPUPTargetDependencies().getGPUGDependency()) {
                            if (gpupTargetDep.getType().equals("requiredFor")) {
                                graph.addEdge(gpupTargetDep.getValue(), target.toString());
                                graph.addEdge(gpupTargetDep.getValue(),target.toString());
                            }

                            else {
                                graph.addEdge(target.toString(), gpupTargetDep.getValue());
                                graph.addEdge(target.toString(),gpupTargetDep.getValue());
                            }
                        }
                        break;
                    }
                }
            }

    }




    private static GPUPDescriptor deserializeFrom(InputStream in) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance("resources.generated");
        Unmarshaller u = jc.createUnmarshaller();
        return (GPUPDescriptor) u.unmarshal(in);
    }

    public int getTotalCountOfTargets() {
        return systemGraph.getCountOfNodes();
    }

    public boolean getFirstRun()
    {
        return firstRun;
    }

    public int getCountOfIndependentTargets() {
        return systemGraph.getCountOfIndependentNodes();
    }

    public int getCountOfRootTargets() {
        return systemGraph.getCountOfRootNodes();
    }

    public int getCountOfLeafTargets() {
        return systemGraph.getCountOfLeafNodes();
    }

    public int getCountOfMiddleTargets() {
        return systemGraph.getCountOfMiddleNodes();
    }


    public ArrayList<String> getTargetDependsOnList(String targetName)  {
        Target targetInstance = systemGraph.getSingleTargetFromGraph(targetName);
        if(targetInstance == null)
            return null;

        ArrayList<String> depOnList = new ArrayList<>();
        for (Target neighbor: targetInstance.getDependsOn() ) {
            depOnList.add(neighbor.getTargetName());
        }
        return depOnList;
    }

    public ArrayList<String> getTargetRequiredForList(String targetName) {
        Target targetInstance = systemGraph.getSingleTargetFromGraph(targetName);
        if(targetInstance == null)
            return null;

        ArrayList<String> reqForList = new ArrayList<>();
        for (Target neighbor: targetInstance.getRequiredFor() ) {
            reqForList.add(neighbor.getTargetName());
        }
        return reqForList;

    }

    public String getTargetStatusInGraph(String targetName) {
        Target targetInstance = systemGraph.getSingleTargetFromGraph(targetName);
        return targetInstance.getStatusInGraph().toString();
    }

    public boolean includesTarget(String targetName) {
        return systemGraph.getSingleTargetFromGraph(targetName) != null;
    }

    protected List<List<String>> findRoutesBetweenTwoTargets(String sourceTarget, String destTarget, Graph.RoutSelection routSelection) {
        return systemGraph.findAllPaths(sourceTarget,destTarget, routSelection);
    }





        public void runSimulationTask(taskSceneController controller, int ms, Graph.RunningTimeType runTimeSelectionEnum,
                                      double successProb, double successWithWarningsProb, Consumer<String> consumer,
                                      int incOrScratch, int numOfThreads, ArrayList<Target> targetsForRun)
                throws InterruptedException, IOException, ParseException {

            currentRunningTask = new SimulationTask(controller,ms, runTimeSelectionEnum, successProb,
                    successWithWarningsProb, consumer, incOrScratch,numOfThreads, targetsForRun);
            currentController = controller;
            currentController.bindTaskToUIComponents(currentRunningTask);//onFinish);

            new Thread(currentRunningTask).start();

        }


    public void setIncrementalGraph(List<Target> runOrder) {
        for (Target target: runOrder) {
            if (target.getlastRunResult() == Target.RunResult.SUCCESS || target.getlastRunResult() == Target.RunResult.WARNING)
                IncrementalGraph.removeTarget(target);
        }

    }

     public void printGeneralInfoOnRun(List<Target> runOrder, String startDateString,  Consumer<String> consumer) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        startDateString = startDateString.split(" ")[1];
        startDateString = startDateString.replace(".",":");

        String endDateString = df.format(new Date());
        Date startDate, endDate;

        startDate = df.parse(startDateString);
        endDate = df.parse(endDateString);

        //difference in milliseconds
        long diff = endDate.getTime() - startDate.getTime();
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        String taskTime = df.format(new Date(diff));

        consumer.accept("---------------------------------------------------------------------------------");
        int skippedTargets = getTargetCount(runOrder, Target.RunResult.SKIPPED);
        int failedTargets = getTargetCount(runOrder,Target.RunResult.FAILURE);
        int successfulTargets = getTargetCount(runOrder,Target.RunResult.SUCCESS);
        int warningTargets = getTargetCount(runOrder,Target.RunResult.WARNING);

        consumer.accept("Run ended with " + skippedTargets + " targets " + Target.RunResult.SKIPPED + ", " +
                 failedTargets + " targets " + Target.RunResult.FAILURE + ", " +
                successfulTargets + " targets " + Target.RunResult.SUCCESS + ", " + warningTargets + " targets " + Target.RunResult.WARNING);
        consumer.accept("The task ran for: "+ taskTime);
        for (Target target: runOrder) {
            consumer.accept("Target: " + target.getTargetName() + " Result of run: " + target.getlastRunResult() + " task ran on target for: " + df.format(new Date(target.getLastSuccessfulRun()))) ;
        }
        if(IncrementalGraph.isEmpty()) {
            consumer.accept("All of the Targets were successfully simulated, you cannot use incremental run next.");
            firstRun = true;
        }
        consumer.accept("---------------------------------------------------------------------------------");

    }

    private int getTargetCount(List<Target> runOrder, Target.RunResult resultOfRun) {
        int res = 0;
        for (Target target: runOrder) {
            if(target.getlastRunResult() == resultOfRun)
                res++;
        }
        return res;
    }

    public Graph getSystemGraph() {
        return systemGraph;

    }

    public ObservableList<Target> getTargetList() {
        ArrayList<Target> targetList = new ArrayList<Target>(systemGraph.getAdjTargets().keySet());
        ObservableList<Target> observableList = FXCollections.observableList(targetList);
        return observableList;


    }


    public Target getTarget(String targetName) {
        return systemGraph.getSingleTargetFromGraph(targetName);
    }


    public String getWorkingDirectory() {
            return this.workingDirectory;
    }

    public void runCompilationTask(taskSceneController controller, Consumer<String> consumer,
                                   int incOrScratch, int numOfThreads, String sourcePath, String destPath,
                                   ArrayList<Target> targetsForRun)
            throws InterruptedException, IOException, ParseException {

        currentRunningTask = new CompilationTask(controller, consumer, incOrScratch,numOfThreads, sourcePath, destPath,targetsForRun);
        currentController = controller;
        currentController.bindTaskToUIComponents(currentRunningTask);//onFinish);

        new Thread(currentRunningTask).start();


    }

    public int getMaxParallelism() {
        return maxParallelism;
    }

    public synchronized void unpauseSystem() {
        pause.compareAndSet(true,false);
        notifyAll();
    }
}
