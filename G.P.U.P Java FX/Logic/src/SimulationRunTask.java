import javafx.application.Platform;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


public class SimulationRunTask implements Runnable {

    private Target target;
    private SimulationTask mainTask;


    // constructor of the class Tasks
    public SimulationRunTask(Target t, SimulationTask mainTask )
    {
        this.target = t;
        this.mainTask = mainTask;
        t.setLongStartTime(System.currentTimeMillis());
    }


    public void run()
    {
        try{

        while(Gpup.getInstance().getPause().get() == true)
                wait();

        String filePath = Gpup.getInstance().getWorkingDirectory() + "\\" + target.getTargetName();
        new File(filePath).mkdirs();
        WriteToFileConsumerClass fileConsumer = new WriteToFileConsumerClass(filePath+'\\' + target.getTargetName());
        ArrayList<Consumer> consumers = new ArrayList<>();
        consumers.add(mainTask.consumer);
        consumers.add(fileConsumer);
        //Target.RunningStatus oldStatus = target.getStatusInRun();

        runTask(mainTask.ms, mainTask.runTimeSelectionEnum, mainTask.successProb, mainTask.successWithWarningsProb, consumers);


        doAtomicCalcs();


        //mainTask.updateLists(target);

            Platform.runLater(
                    () -> {
                        mainTask.UIconsumer.accept(mainTask.listMap);
                    });


    }

        catch(InterruptedException ie)
        {
            ie.printStackTrace();
        }
    }

    private synchronized void doAtomicCalcs() throws InterruptedException {
        while(Gpup.getInstance().getPause().get() == true)
            Gpup.getInstance().waitOnMyPause();
        updateRunStatus();
       // runNewTargets();
    }

    private synchronized void runNewTargets() {
        for (Target t: mainTask.getRunTargets()) {
            if(t.getStatusInRun().equals(Target.RunningStatus.FINISHED)||
                    t.getStatusInRun().equals(Target.RunningStatus.IN_PROCESS)){continue;}
            if(! mainTask.getCurrentRunGraph().isLeafOrIndependent(t)){continue;}
            if(! mainTask.checkForSerialSets(t)){continue;}


            /*if(t.getStatusInRun().equals(Target.RunningStatus.WAITING)){
                SimulationRunTask worker = new SimulationRunTask(t, mainTask);
                mainTask.executor.execute(worker);//calling execute method of ExecutorService
            }
*/
            if(t.getStatusInRun().equals(Target.RunningStatus.FROZEN)){
                t.setStatusInRun(Target.RunningStatus.WAITING);
                mainTask.updateLists(Target.RunningStatus.FROZEN,t);
                SimulationRunTask worker = new SimulationRunTask(t, mainTask);
                mainTask.executor.execute(worker);//calling execute method of ExecutorService
            }
        }
    }

    private synchronized void updateRunStatus() {
        mainTask.getCurrentRunGraph().removeTarget(target);
        mainTask.getRunTargets().remove(target);


        if (mainTask.getCurrentRunGraph().getCountOfNodes() == 0)
            mainTask.executor.shutdown();


        if (target.getlastRunResult().equals(Target.RunResult.SKIPPED) || target.getlastRunResult().equals(Target.RunResult.FAILURE)) {
            for (Target neighbor : mainTask.getRunTargets()) {
                if(target.getAllDependsForNames().contains(neighbor.getTargetName())) { /// setting all grandchildern and such skipped
                    Target.RunningStatus oldStatus = neighbor.getStatusInRun();
                    neighbor.setStatusInRun(Target.RunningStatus.SKIPPED);
                    mainTask.updateLists(oldStatus, neighbor);


                    SimulationRunTask worker = new SimulationRunTask(neighbor, mainTask);
                    mainTask.executor.execute(worker);//calling execute method of ExecutorService

                }

                /*if (mainTask.getCurrentRunGraph().isLeafOrIndependent(neighbor)) {
                    SimulationRunTask worker = new SimulationRunTask(neighbor, mainTask);
                    mainTask.executor.execute(worker);//calling execute method of ExecutorService
                }
*/
            }
        }
        else {
            for (Target neighbor : mainTask.getRunTargets()) {
                if(target.getDependsOn().contains(neighbor) &&
                neighbor.getStatusInRun().equals(Target.RunningStatus.FROZEN)&&
                mainTask.getCurrentRunGraph().isLeafOrIndependent(neighbor)){
                    neighbor.setStatusInRun(Target.RunningStatus.WAITING);
                    mainTask.updateLists(Target.RunningStatus.FROZEN, neighbor);
                    SimulationRunTask worker = new SimulationRunTask(neighbor, mainTask);
                    mainTask.executor.execute(worker);//calling execute method of ExecutorService
                }

               /* if (mainTask.getCurrentRunGraph().isLeafOrIndependent(neighbor) &&
                        neighbor.getStatusInRun().equals(Target.RunningStatus.WAITING) ) {
                    SimulationRunTask worker = new SimulationRunTask(neighbor, mainTask);
                    mainTask.executor.execute(worker);//calling execute method of ExecutorService

*/
            }
        }
        runNewTargets();
    }




    public void runTask(int ms, Graph.RunningTimeType runTimeSelection, double successProb, double successWithWarningsProb, ArrayList<Consumer> consumers) throws InterruptedException {
        if(target.getStatusInRun() == Target.RunningStatus.SKIPPED) {
            target.setStatusInRun(Target.RunningStatus.SKIPPED);
            target.setLastRunResult(Target.RunResult.SKIPPED);
            List<String> targetsThatAreNowBlocked = target.getDependsOnNames();
            for (Consumer<String> consumer: consumers)
                consumer.accept("\nTarget " + target.getTargetName() +" Skipped, the targets that are now prevented from processing and will be skipped: "+ targetsThatAreNowBlocked);

            return;
        }


        target.setStatusInRun(Target.RunningStatus.IN_PROCESS);
        mainTask.updateLists(Target.RunningStatus.WAITING,target);

        for (Consumer<String> consumer: consumers) {
            consumer.accept("\nStarting to run task on target " + target.getTargetName());//1
            consumer.accept(target.getUserData());//2
        }

        Target.RunResult runResult = target.runSimulationTaskUtil(ms,runTimeSelection,successProb,successWithWarningsProb,consumers);

        for (Consumer<String> consumer: consumers)
            consumer.accept("Processing of target "+target.getTargetName()+" The result of the run is: " + runResult.toString());//3

        if(runResult != Target.RunResult.FAILURE){
            List<String> targetsThatAreNowOpen = target.getDependsOnNames();
            for (Consumer<String> consumer : consumers)
                consumer.accept("New targets that are now available for processing: "+ targetsThatAreNowOpen);//4

            if(runResult == Target.RunResult.SUCCESS)
                target.setLastRunResult(Target.RunResult.SUCCESS);
            else
                target.setLastRunResult(Target.RunResult.WARNING);
        }

        else {
            List<String> targetsThatAreNowBlocked = target.getDependsOnNames();
            for (Consumer<String> consumer: consumers)
                consumer.accept("The targets that are now prevented from processing and will be skipped: "+ targetsThatAreNowBlocked);//5


            target.setLastRunResult(Target.RunResult.FAILURE);
        }

        target.setStatusInRun(Target.RunningStatus.FINISHED);
        mainTask.updateLists(Target.RunningStatus.IN_PROCESS,target);
    }




}
