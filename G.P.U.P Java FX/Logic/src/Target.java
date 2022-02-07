import javafx.scene.control.CheckBox;
import sun.misc.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;


public class Target {

    private final String targetName;
    private final String userData;

    private GraphStatus statusInGraph;
    private RunningStatus statusInRun;
    private final ArrayList<Target> dependsOn = new ArrayList<>();  // From Them To Me
    private final ArrayList<Target> requiredFor = new ArrayList<>(); // From me to them
    private final ArrayList<String> dependsOnNames = new ArrayList<>();
    private final ArrayList<String> requiredForNames = new ArrayList<>();
    private final ArrayList<String> allDependsForNames = new ArrayList<>();
    private final ArrayList<String> allRequiredForNames = new ArrayList<>();
    private long lastSuccessfulRun;

    public synchronized void setLastRunResult(RunResult lastRunResult) {
        this.lastRunResult = lastRunResult;
    }

    private RunResult lastRunResult;
    private int sets;
    private CheckBox select;

    public  long getLongStartTime() {
        return longStartTime;
    }

    private long longStartTime;

    private long startProcess;

    public long getStartProcess() {
        return startProcess;
    }

    public void setLongStartTime(long longStartTime) {
        this.longStartTime = longStartTime;
    }

    public ArrayList<String> getMySets() {
        return mySets;
    }

    private ArrayList<String> mySets = new ArrayList<>();

    //Getters/////////////////////
    public synchronized long getLastSuccessfulRun() {
        return lastSuccessfulRun;
    }

    public String getTargetName() {
        return targetName;
    }

    public synchronized RunningStatus getStatusInRun() {
        return statusInRun;
    }

    public GraphStatus getStatusInGraph() {
        return statusInGraph;
    }

    public int getSets() {
        return sets;
    }

    public String getUserData() {
        return userData;
    }

    public ArrayList<String> getAllDependsForNames() {
        return allDependsForNames;
    }

    public ArrayList<String> getAllRequiredForNames() {
        return allRequiredForNames;
    }

    public ArrayList<String> getDependsOnNames() {
        return dependsOnNames;
    }

    public ArrayList<String> getRequiredForNames() {
        return requiredForNames;
    }

    public List<Target> getRequiredFor() {
        return requiredFor;
    }

    public List<Target> getDependsOn() {
        return dependsOn;
    }
    //////////////////////////////


    protected void removeNeighborFromDepOn(Target targetToRemove) {
        if (dependsOn.contains(targetToRemove))
            dependsOn.remove(targetToRemove);

    }

    protected void removeNeighborFromReqFor(Target targetToRemove) {
        if (requiredFor.contains(targetToRemove))
            requiredFor.remove(targetToRemove);
    }

    public CheckBox getSelect() {
        return select;
    }

    public void setSelect(CheckBox select) {
        this.select = select;
    }

    public void setSets(Map<String, List<String>> serialSets) {
        int setAppearanceCounter = 0;
        for (String set : serialSets.keySet()) {
            for (String target : serialSets.get(set)) {
                if (target.equals(this.targetName)) {
                    mySets.add(set);
                    setAppearanceCounter++;
                }
            }
        }
        this.sets = setAppearanceCounter;
    }


    public void setAllDependsForNames(Graph graph) {
        for (Target neighbor : graph.getAdjTargets().keySet()) {
            if (!targetName.equals(neighbor.targetName) && !allDependsForNames.contains(neighbor.targetName) &&
                    !graph.findAllPaths(targetName, neighbor.targetName, Graph.RoutSelection.DEPENDS_ON).isEmpty())
                allDependsForNames.add(neighbor.targetName);
        }
    }


    public void setAllRequiredForNames(Graph graph) {
        for (Target neighbor : graph.getAdjTargets().keySet()) {
            if (!targetName.equals(neighbor.targetName) && !allRequiredForNames.contains(neighbor.targetName) &&
                    !graph.findAllPaths(targetName, neighbor.targetName, Graph.RoutSelection.REQUIRED_FOR).isEmpty())
                allRequiredForNames.add(neighbor.targetName);
        }
    }

    public List<String> getFailedTargets() {
        ArrayList<String> res = new ArrayList<>();
        for (Target t: requiredFor
             ) {
            if(t.getlastRunResult().equals(RunResult.SKIPPED) || t.getlastRunResult().equals(RunResult.FAILURE)){
                res.add(t.getTargetName());
            }
        }
        return res;
    }

    /*public synchronized void addToExecution(int ms, Graph.RunningTimeType runTimeSelection, double successProb, double successWithWarningsProb, ArrayList<Consumer> consumers) {
        for(Target neighbor: dependsOn){
            for(Target neighborsNegibor: neighbor.getRequiredFor()){
                if(neighborsNegibor.getStatusInRun().equals(RunningStatus.IN_PROCESS) || neighborsNegibor.getStatusInRun().equals(RunningStatus.WAITING)){
                    return;
                }
                if(neighborsNegibor.getStatusInRun().equals(RunningStatus.SKIPPED) || neighborsNegibor.getStatusInRun().equals()){
                    neighbor.setStatusInRun(RunningStatus.SKIPPED);
                    Runnable worker = new Task(neighbor, ms, runTimeSelection, successProb, successWithWarningsProb);
                    Gpup.getInstance().executor.execute(worker);//calling execute method of ExecutorService
                    return;
                }

            }
            if(lastRunResult.equals(RunResult.SKIPPED))
                neighbor.setStatusInRun(RunningStatus.SKIPPED);
            else if(lastRunResult.equals(RunResult.FAILURE))
                neighbor.setStatusInRun(RunningStatus.SKIPPED);
            else if(neighbor.getStatusInRun() != RunningStatus.SKIPPED)
                neighbor.setStatusInRun(RunningStatus.WAITING);

            Runnable worker = new Task(neighbor, ms, runTimeSelection, successProb, successWithWarningsProb);
            Gpup.getInstance().executor.execute(worker);//calling execute method of ExecutorService

        }
    }
*/

    public enum RunResult {
        SUCCESS, WARNING, FAILURE, SKIPPED
    }

    public enum GraphStatus {
        LEAF, ROOT, MIDDLE, INDEPENDENT
    }

    public enum RunningStatus {
        FROZEN, SKIPPED, WAITING, IN_PROCESS, FINISHED
    }


    public Target(String name, String freeuserData) {
        targetName = name;
        if (freeuserData == null)
            userData = "";
        else
            userData = freeuserData;

        this.select = new CheckBox();

    }


    public void addTargetToDependsOn(Target target) {
        if (!dependsOn.contains(target)) {
            dependsOn.add(target);
            dependsOnNames.add(target.getTargetName());
        }
    }

    public void addTargetToRequiredFor(Target target) {
        if (!requiredFor.contains(target)) {
            requiredFor.add(target);
            requiredForNames.add(target.getTargetName());
        }
    }


    public void runTask(int ms, Graph.RunningTimeType runTimeSelection, double successProb, double successWithWarningsProb, ArrayList<Consumer> consumers) throws InterruptedException {
        if(statusInRun == RunningStatus.SKIPPED) {
            setStatusInRun(RunningStatus.SKIPPED);
            lastRunResult = RunResult.SKIPPED;
            List<String> targetsThatAreNowBlocked = getDependsOnNames();
            for (Consumer<String> consumer: consumers)
                consumer.accept("\nTarget " + targetName +" Skipped, the targets that are now prevented from processing and will be skipped: "+ targetsThatAreNowBlocked);

            for (Target neighbor: dependsOn)
                neighbor.setStatusInRun(RunningStatus.SKIPPED);

            setStatusInRun(RunningStatus.SKIPPED);
            setLastRunResult(RunResult.SKIPPED);

            return;
        }

        statusInRun = RunningStatus.IN_PROCESS;

        for (Consumer<String> consumer: consumers) {
            consumer.accept("\nStarting to run task on target " + targetName);//1
            consumer.accept("Here is where free info on the target will be");//2
        }

        RunResult runResult = runSimulationTaskUtil(ms,runTimeSelection,successProb,successWithWarningsProb,consumers);

        for (Consumer<String> consumer: consumers)
            consumer.accept("Processing of target "+targetName+" The result of the run is: " + runResult.toString());//3

        if(runResult != RunResult.FAILURE){
            List<String> targetsThatAreNowOpen = getDependsOnNames();
            for (Consumer<String> consumer : consumers)
                consumer.accept("New targets that are now available for processing: "+ targetsThatAreNowOpen);//4

            for(Target neighbor:dependsOn) {
                if (neighbor.getStatusInRun() != RunningStatus.SKIPPED)
                    neighbor.setStatusInRun(RunningStatus.WAITING);
            }
            setLastRunResult(RunResult.SUCCESS);
        }

        else {
            List<String> targetsThatAreNowBlocked = getDependsOnNames();
            for (Consumer<String> consumer: consumers)
                consumer.accept("The targets that are now prevented from processing and will be skipped: "+ targetsThatAreNowBlocked);//5

            for (Target neighbor: dependsOn)
                neighbor.setStatusInRun(RunningStatus.SKIPPED);

            setLastRunResult(RunResult.FAILURE);
        }

        setStatusInRun(RunningStatus.FINISHED);
    }


    private synchronized  void checkAllTargetsFinished()
    {
        for(Target target: Gpup.getInstance().getTargetList()){
            if(target.getStatusInRun() == RunningStatus.FROZEN)
                return;
        }

        Gpup.getInstance().executor.shutdown();

    }


    protected RunResult runCompilationTaskUtil(ArrayList<Consumer> consumers, String sourcePath, String destPath, ProcessBuilder processBuilder)
            throws InterruptedException, IOException {

        for (Consumer<String> consumer: consumers)
            consumer.accept("Task on target " + targetName + " going to run " + processBuilder.command());

        this.startProcess = System.currentTimeMillis();
        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ( (line = reader.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }
        String result = builder.toString();

        int resultCode = process.waitFor();
        lastSuccessfulRun = System.currentTimeMillis() - startProcess;

        //Determine result
        if (resultCode != 0) { // javac failed
            setLastRunResult(RunResult.FAILURE);
            for (Consumer<String> consumer: consumers)
                consumer.accept("Task on target " + targetName + " failed  " + result);
        }
        else
            setLastRunResult(RunResult.SUCCESS);


        for (Consumer<String> consumer: consumers)
            consumer.accept("Task on target " + targetName + " compiler ran for  " + lastSuccessfulRun);

        setStatusInRun( RunningStatus.FINISHED);
        return lastRunResult;
    }



    protected RunResult runSimulationTaskUtil(long ms, Graph.RunningTimeType runTimeSelection, double successProb,
                                              double successWithWarningsProb, ArrayList<Consumer> consumers)
            throws InterruptedException {

        Random rand = new Random();

        if (runTimeSelection == Graph.RunningTimeType.RANDOM)   //Case of Rand
            ms = rand.longs(0,ms).findFirst().getAsLong();

        for (Consumer<String> consumer: consumers)
            consumer.accept("Task on target " + targetName + " going to sleep for " + ms + " milliseconds  ");

        long start = System.currentTimeMillis();
        Thread.sleep(ms);
        lastSuccessfulRun = ms;
        for (Consumer<String> consumer: consumers)
            consumer.accept("Task on target " + targetName + " slept for  " + (System.currentTimeMillis() - start));


        //Determine result
        if (rand.nextDouble() <= successProb) {//  success or warning
            if (rand.nextDouble() <= successWithWarningsProb)
                setLastRunResult(RunResult.WARNING);

            else
                setLastRunResult(RunResult.SUCCESS);
        }
        else
            setLastRunResult(RunResult.FAILURE);

        setStatusInRun( RunningStatus.FINISHED);
        return lastRunResult;
    }




    protected synchronized void setStatusInRun() {
        if (getStatusInGraph() == GraphStatus.INDEPENDENT || getStatusInGraph() == GraphStatus.LEAF)
            statusInRun = RunningStatus.WAITING;
        else
            statusInRun = RunningStatus.FROZEN;
    }

    protected synchronized boolean eligibleForQue(){
        for(Target neighbor:requiredFor){
            if(! neighbor.getStatusInRun().equals(RunningStatus.FINISHED) )
                return false;


        }


        return true;
    }

    protected synchronized void setStatusInRun(RunningStatus newStatus){
        statusInRun = newStatus;
    }

    protected void setStatusInGraph() {
        if(dependsOn.isEmpty() && requiredFor.isEmpty()){
            statusInGraph = GraphStatus.INDEPENDENT;
        }
        else if(dependsOn.isEmpty()) {
            statusInGraph = GraphStatus.ROOT;
        }
        else if(requiredFor.isEmpty()) {

            statusInGraph = GraphStatus.LEAF;
        }
        else{
            statusInGraph = GraphStatus.MIDDLE;
        }

    }



    @Override
    public String toString() {
        return targetName;
    }

    @Override
    public boolean equals(Object obj)
    {

        // if both the object references are
        // referring to the same object.
        if(this == obj)
            return true;

        if(obj == null || obj.getClass()!= this.getClass())
            return false;

        // type casting of the argument.
        Target target = (Target) obj;
        return (this.targetName.equals(target.targetName));
    }

    public RunResult getlastRunResult()
    {
        return lastRunResult;
    }

    public int hashCode(){
        int hash = 17;
        hash = hash * 31 + targetName.hashCode();
        return hash;
    }




}
