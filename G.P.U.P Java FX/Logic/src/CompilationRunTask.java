import javafx.application.Platform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


public class CompilationRunTask implements Runnable {

    private Target target;
    private CompilationTask mainTask;


    // constructor of the class Tasks
    public CompilationRunTask(Target t, CompilationTask mainTask )
    {
        this.target = t;
        this.mainTask = mainTask;
    }
    private synchronized void doAtomicCalcs() throws InterruptedException {
        while (Gpup.getInstance().getPause().get() == true)
            Gpup.getInstance().waitOnMyPause();
    }

    public void run()
    {
        try{
            String filePath = Gpup.getInstance().getWorkingDirectory() + "\\" + target.getTargetName();
            new File(filePath).mkdirs();
            WriteToFileConsumerClass fileConsumer = new WriteToFileConsumerClass(filePath+'\\' + target.getTargetName());
            ArrayList<Consumer> consumers = new ArrayList<>();
            consumers.add(mainTask.consumer);
            consumers.add(fileConsumer);


            ProcessBuilder builder = getProcessBuilder(mainTask.getSourcePath(), mainTask.getDestPath());
            runTask(consumers,mainTask.getSourcePath(), mainTask.getDestPath(), builder);
            updateRunStatus();
            doAtomicCalcs();

            Platform.runLater(
                    () -> {
                        mainTask.UIconsumer.accept(mainTask.listMap);
                    });

        }

        catch(InterruptedException | IOException ie)
        {
            ie.printStackTrace();
        }
    }

    private ProcessBuilder getProcessBuilder(String targetFolder_d, String targetFolder_cp) {
        String fileFQN = target.getUserData();
        String filePath = getFilePathFromFQN(fileFQN);

        ProcessBuilder processBuilderCompilation = new ProcessBuilder(
                "javac",
                "-d", targetFolder_cp,
                "-cp", targetFolder_cp,
                filePath).redirectErrorStream(false);
        processBuilderCompilation.directory(new File(targetFolder_d));

        return processBuilderCompilation;
    }

    private String getFilePathFromFQN(String fileFQN) {
        String newStr = fileFQN.replaceAll("\\.", "/");

        newStr = newStr + ".java";

        return newStr;
    }

    private synchronized void updateRunStatus() {
        mainTask.getCurrentRunGraph().removeTarget(target);

        if (mainTask.getCurrentRunGraph().getCountOfNodes() == 0)
            mainTask.executor.shutdown();


        if (target.getlastRunResult().equals(Target.RunResult.SKIPPED) || target.getlastRunResult().equals(Target.RunResult.FAILURE)) {
            for (Target neighbor : target.getDependsOn()) {
                Target.RunningStatus oldStatus = neighbor.getStatusInRun();
                neighbor.setStatusInRun(Target.RunningStatus.SKIPPED);
                mainTask.updateLists(oldStatus,neighbor);

                if (mainTask.getCurrentRunGraph().isLeafOrIndependent(neighbor)) {
                    CompilationRunTask worker = new CompilationRunTask(neighbor, mainTask);
                    mainTask.executor.execute(worker);//calling execute method of ExecutorService
                }
            }
        }
        else {
            for (Target neighbor : target.getDependsOn()) {
                if (neighbor.getStatusInRun().equals(Target.RunningStatus.FROZEN)) {
                    neighbor.setStatusInRun(Target.RunningStatus.WAITING);
                    mainTask.updateLists(Target.RunningStatus.FROZEN, neighbor);
                }

                if (mainTask.getCurrentRunGraph().isLeafOrIndependent(neighbor)) {
                    CompilationRunTask worker = new CompilationRunTask(neighbor, mainTask);
                    mainTask.executor.execute(worker);//calling execute method of ExecutorService
                }

            }
        }

    }


    public void runTask(ArrayList<Consumer> consumers,
             String sourcePath, String destPath,ProcessBuilder builder) throws InterruptedException, IOException {
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

        Target.RunResult runResult = target.runCompilationTaskUtil(consumers,sourcePath, destPath, getProcessBuilder(sourcePath,destPath));

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
