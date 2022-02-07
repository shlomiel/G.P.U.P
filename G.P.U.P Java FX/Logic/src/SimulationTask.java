import javafx.application.Platform;
import javafx.concurrent.Task;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


public class SimulationTask extends Task<Boolean> {
    private final int numOfThreads;
    Consumer<String> consumer = System.out::println;
    UpdateListConsumerClass UIconsumer; //= new UpdateListConsumerClass(controller);
    taskSceneController controller;
    protected ExecutorService executor;


    public SimulationTask(taskSceneController controller, int ms, Graph.RunningTimeType runTimeSelectionEnum,
                          double successProb, double successWithWarningsProb,
                          Consumer<String> consumer, int incOrScratch, int numOfThreads, ArrayList<Target> targetsForRun){
        this.controller = controller;
        this.ms = ms;
        this.runTimeSelectionEnum = runTimeSelectionEnum;
        this.successProb = successProb;
        this.successWithWarningsProb = successWithWarningsProb;
        this.incOrScratch = incOrScratch;
        this.numOfThreads = numOfThreads;
        consumers.add(consumer);
        this.UIconsumer = new UpdateListConsumerClass(controller);
        this.runTargets = new ArrayBlockingQueue<>(targetsForRun.size());
        this.runTargets.addAll(targetsForRun);
        listMap.putIfAbsent(Target.RunningStatus.FROZEN.toString(),frozenTargets);
        listMap.putIfAbsent(Target.RunningStatus.SKIPPED.toString(),skippedTargets);
        listMap.putIfAbsent(Target.RunningStatus.IN_PROCESS.toString(),inProcessTargets);
        listMap.putIfAbsent(Target.RunningStatus.WAITING.toString(),waitingTargets);
        listMap.putIfAbsent(Target.RunningStatus.FINISHED.toString(),finishedTargets);
    }



    protected int ms;
    protected int incOrScratch;
    protected Graph.RunningTimeType runTimeSelectionEnum;
    protected double successProb;
    protected double successWithWarningsProb;
    protected  int processed = 0;
    protected ArrayList<Consumer<String>> consumers = new ArrayList();

    public synchronized Graph getCurrentRunGraph() {
        return currentRunGraph;
    }

    private Graph currentRunGraph;



    public ArrayList<Target> frozenTargets = new ArrayList<>();
    public ArrayList<Target> skippedTargets = new ArrayList<>();
    public ArrayList<Target> waitingTargets = new ArrayList<>();

    public synchronized ArrayList<Target> getInProcessTargets() {
        return inProcessTargets;
    }

    public ArrayList<Target> inProcessTargets = new ArrayList<>();
    public ArrayList<Target> finishedTargets = new ArrayList<>();


    Map<String,ArrayList<Target>> listMap = new HashMap<>();

    public synchronized List<Target> getRunOrder() {
        return runOrder;
    }

    private List<Target> runOrder;

    public synchronized ArrayBlockingQueue<Target> getRunTargets() {
        return runTargets;
    }

    private ArrayBlockingQueue<Target> runTargets;

    @Override
    protected Boolean call() throws Exception {
       try{
        updateMessage("Fetching file...");
           String pattern = "dd.MM.yyyy HH.mm.ss";
           SimpleDateFormat simpleDateFormat =
                   new SimpleDateFormat(pattern);
           String date = simpleDateFormat.format(new Date());

           Gpup.getInstance().setWorkingDirectory(Gpup.getInstance().getWorkingDirectory() + '\\' + "Simulation - " + date);

           currentRunGraph = new Graph();
           currentRunGraph.buildGraph("task graph",Gpup.getInstance().getTargetMapToBuildGraph());
           Gpup.getInstance().addDependenciesToTargets(currentRunGraph);
           for (Target t: Gpup.getInstance().getTargetList()) {
               if(!runTargets.contains(t)){
                   currentRunGraph.removeTarget(t);
               }
           }

           if(incOrScratch == 1)
               runOrder = currentRunGraph.getTopologicalSortOfNodes();
           else
               runOrder = Gpup.getInstance().getIncrementalGraph().getTopologicalSortOfNodes();




           for (Target target: runTargets)
               target.setStatusInRun();


           for(Target target : runTargets){
               if(target.getStatusInRun().equals(Target.RunningStatus.WAITING) && !checkForSerialSets(target)) {
                   target.setStatusInRun(Target.RunningStatus.FROZEN);
               }
               listMap.get(target.getStatusInRun().toString()).add(target); // add to state lists
           }


           executor = Executors.newFixedThreadPool(this.numOfThreads);
           for(Target target : runTargets){
               if(target.getStatusInRun().equals(Target.RunningStatus.WAITING)){
                   SimulationRunTask worker = new SimulationRunTask(target, this);
                   executor.execute(worker);//calling execute method of ExecutorService
               }
           }

           //executor.shutdown();
           while (!executor.isTerminated()) {  updateProgress(runOrder.size() - runTargets.size(), runOrder.size());  }

           System.out.println("Finished all threads");

           //listMap.remove(Target.RunningStatus.FROZEN);
           //listMap.remove(Target.RunningStatus.WAITING);

           Platform.runLater(
                   () -> {
                       UIconsumer.accept(listMap);
                   });




           TextareaConsumer cons = new TextareaConsumer(controller.getMidrunTextArea());
           cons.accept("");
           Gpup.getInstance().printGeneralInfoOnRun(runOrder, date, cons);
           Gpup.getInstance().setFirstRun(false);
           Gpup.getInstance().setIncrementalGraph(runOrder);


           Gpup.getInstance().getSystemGraph().printGraph();
           Gpup.getInstance().getIncrementalGraph().printGraph();

        } catch (TaskIsCanceledException e) {

        }
        updateMessage("Done...");
        return Boolean.TRUE;
    }

    protected synchronized boolean checkForSerialSets(Target target) { // check if any of the targets in common serial sets is waiting
        for (String setName : target.getMySets()) {
            for(String setFriend : serialSets.get(setName)){
                if(setFriend.equals(target.getTargetName()))
                    continue;
                if(!runTargets.contains(setFriend))
                    continue;
                Target t = Gpup.getInstance().getTarget(setFriend);
                if( t.getStatusInRun().equals(Target.RunningStatus.WAITING) ||
                        t.getStatusInRun().equals(Target.RunningStatus.IN_PROCESS)){
                    return false;
                }
            }
            }
        return true;
        }



    protected synchronized void updateLists(Target.RunningStatus oldStatus, Target target) {
        listMap.get(oldStatus.toString()).remove(target);
        listMap.get(target.getStatusInRun().toString()).add(target);
    }


    @Override
    protected void cancelled() {
        super.cancelled();
        updateMessage("Cancelled!");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    //NEW STUFF
///////////////////////////////////////////////////////////////////////////////////////////////////
    private Map<String, List<String>> serialSets = Gpup.getInstance().getSerialSets();

    public synchronized Map<String,List<String>> getSerialSets(){
        return this.serialSets;
    }

    protected synchronized boolean serialSetCondition(Target target) {
        if (target.getMySets().isEmpty()){
            return true;
        }

        for (String setName : target.getMySets()) {
            //mainTask.getSerialSets().get(setName).remove(target.getTargetName());
            for (String targetInSet: getSerialSets().get(setName)) {
                if(!targetInSet.equals(target.getTargetName()) &&
                        getInProcessTargets().contains(Gpup.getInstance().getTarget(targetInSet))){
                             return false;
                }
            }
        }
        return true;
    }


}