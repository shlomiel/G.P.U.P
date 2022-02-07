import javafx.application.Platform;
import javafx.concurrent.Task;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


public class CompilationTask extends Task<Boolean> {
    private final int numOfThreads;
    private final String sourcePath;
    private final String destPath;
    Consumer<String> consumer = System.out::println;
    UpdateListConsumerClass UIconsumer;
    taskSceneController controller;
    protected ExecutorService executor;


    public synchronized String getSourcePath() {
        return sourcePath;
    }

    public synchronized String getDestPath() {
        return destPath;
    }

    public CompilationTask(taskSceneController controller, Consumer<String> consumer, int incOrScratch, int numOfThreads,
                           String sourcePath, String destPath, ArrayList<Target> targetsForRun){
        this.controller = controller;
        this.incOrScratch = incOrScratch;
        this.numOfThreads = numOfThreads;
        this.sourcePath = sourcePath;
        this.destPath = destPath;
        consumers.add(consumer);
        this.UIconsumer = new UpdateListConsumerClass(this.controller);
        this.runTargets = targetsForRun;
        listMap.putIfAbsent(Target.RunningStatus.FROZEN.toString(),frozenTargets);
        listMap.putIfAbsent(Target.RunningStatus.SKIPPED.toString(),skippedTargets);
        listMap.putIfAbsent(Target.RunningStatus.IN_PROCESS.toString(),inProcessTargets);
        listMap.putIfAbsent(Target.RunningStatus.WAITING.toString(),waitingTargets);
        listMap.putIfAbsent(Target.RunningStatus.FINISHED.toString(),finishedTargets);
    }



    protected int incOrScratch;
    protected  int processed = 0;
    protected ArrayList<Consumer<String>> consumers = new ArrayList();

    public synchronized Graph getCurrentRunGraph() {
        return currentRunGraph;
    }

    private Graph currentRunGraph;

    public ArrayList<Target> frozenTargets = new ArrayList<>();
    public ArrayList<Target> skippedTargets = new ArrayList<>();
    public ArrayList<Target> waitingTargets = new ArrayList<>();
    public ArrayList<Target> inProcessTargets = new ArrayList<>();
    public ArrayList<Target> finishedTargets = new ArrayList<>();


    Map<String,ArrayList<Target>> listMap = new HashMap<>();

    public synchronized List<Target> getRunOrder() {
        return runOrder;
    }

    private List<Target> runOrder;

    public synchronized List<Target> getRunTargets() {
        return runTargets;
    }

    private List<Target> runTargets;

    @Override
    protected Boolean call() throws Exception {
        try{
            updateMessage("Fetching file...");
            String pattern = "dd.MM.yyyy HH.mm.ss";
            SimpleDateFormat simpleDateFormat =
                    new SimpleDateFormat(pattern);
            String date = simpleDateFormat.format(new Date());

            Gpup.getInstance().setWorkingDirectory(Gpup.getInstance().getWorkingDirectory() + '\\' + "Compilation - " + date);
            prepareOutFolder();

            currentRunGraph = new Graph();
            currentRunGraph.buildGraph("task graph",Gpup.getInstance().getTargetMapToBuildGraph());
            Gpup.getInstance().addDependenciesToTargets(currentRunGraph);
            for (Target t:Gpup.getInstance().getTargetList()) {
                if(!runTargets.contains(t)){
                    currentRunGraph.removeTarget(t);
                }
            }


            if(incOrScratch == 1) {
                runOrder = currentRunGraph.getTopologicalSortOfNodes();
               // runTargets = Gpup.getInstance().getSystemGraph().getTopologicalSortOfNodes();
            }

            else {
                runOrder = Gpup.getInstance().getIncrementalGraph().getTopologicalSortOfNodes();
            }


            for (Target target: runTargets) {
                target.setStatusInRun();
                listMap.get(target.getStatusInRun().toString()).add(target); // add to state lists
            }


           // currentRunGraph = Gpup.getInstance().getSystemGraph();


            executor = Executors.newFixedThreadPool(this.numOfThreads);
            for(Target target : runTargets){
                if(target.getStatusInRun().equals(Target.RunningStatus.WAITING)){
                    CompilationRunTask worker = new CompilationRunTask(target, this);
                    executor.execute(worker);//calling execute method of ExecutorService
                }
            }

            //executor.shutdown();
            while (!executor.isTerminated()) {  updateProgress(runOrder.size() - getCurrentRunGraph().getCountOfNodes(), runOrder.size());  }
            updateProgress(runOrder.size() - getCurrentRunGraph().getCountOfNodes(), runOrder.size());
            System.out.println("Finished all threads");

            //listMap.remove(Target.RunningStatus.FROZEN);
            //listMap.remove(Target.RunningStatus.WAITING);

            Platform.runLater(
                    () -> {
                        UIconsumer.accept(listMap);
                    });





            Gpup.getInstance().printGeneralInfoOnRun(runOrder, date, consumer);
            Gpup.getInstance().setFirstRun(false);
            Gpup.getInstance().setIncrementalGraph(runOrder);


            Gpup.getInstance().getSystemGraph().printGraph();
            Gpup.getInstance().getIncrementalGraph().printGraph();

        } catch (TaskIsCanceledException e) {

        }
        updateMessage("Done...");
        return Boolean.TRUE;
    }

    private void prepareOutFolder() throws IOException {
        File f = new File(destPath);
        if (!f.exists()){
            new File(destPath).mkdirs();
        }
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



}