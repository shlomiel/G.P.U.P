import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

public class GpupUIOLD {
    private final Gpup GpupSystem = new Gpup();
    private final Scanner sc= new Scanner(System.in); //System.in is a standard input stream.
    private boolean fileSuccessfullyLoaded = false;

    enum OptionNumber {
        OPTION_ONE,
        OPTION_TWO,
        OPTION_THREE,
        OPTION_FOUR,
        OPTION_FIVE,
        OPTION_SIX,
        OPTION_BONUS
    }


    private void loadXmlFile() {
        System.out.println("Please provide a path to the XML file you wish to load(eg: C:/temp/myXML.xml)\n");
        String FilePath = sc.nextLine();

        try {
            GpupSystem.buildGraphFromXml(FilePath);
        }  catch (FileNotFoundException | WrongFileTypeException | TargetNameExistsException |
                UnknownTargetInDescpritionException | ConflictException e){
            System.out.println(e.getMessage());
            return;
        }

          catch(JAXBException e) {
            System.out.println(e.getMessage());
            return;
        }
        fileSuccessfullyLoaded = true;

    }



    public void startUserInteraction() {

        if (!fileSuccessfullyLoaded) {
            System.out.println("\nWelcome to GPUP System menu please select an option from the menu: \n" +
                    "1) Load XML file\n"+"6) Exit GPUP");

            try {
                int selectedOption = Integer.parseInt(sc.nextLine());
                if ( !(selectedOption == 1 || selectedOption == 6) )
                    throw new NumberFormatException();

                handleUserSelection(OptionNumber.values()[selectedOption - 1]);
            } catch (NumberFormatException e) {
                System.out.println("Wrong input, please load an xml file before viewing other options (enter 1 as input)");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else{
                System.out.println("\nWelcome to GPUP System menu please select an option from the menu (1-7): \n" +
                        "1) Load XML file\n" + "2) Present general info on target graph\n" + "3) Present specific target info\n" +
                        "4) Find route between two targets\n" + "5) Run simulation task\n" + "6) Exit GPUP\n" + "7) Bonus - Find a circle in the system`s graph\n");

                try {
                    int selectedOption = Integer.parseInt(sc.nextLine());
                    if (selectedOption < 1 || selectedOption > 7)
                        throw new NumberFormatException();

                    handleUserSelection(OptionNumber.values()[selectedOption - 1]);
                } catch (NumberFormatException e) {
                    System.out.println("Wrong input for menu option please select an option from  1) to 7) ");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            startUserInteraction();
        }

    private void handleUserSelection(OptionNumber value) throws InterruptedException {

        switch (value) {
            case OPTION_ONE:
                loadXmlFile();
                break;

            case OPTION_TWO:
                getGeneralGraphInfo();
                break;

            case OPTION_THREE:
                getSpecificTargetInfo();
                break;

            case OPTION_FOUR:
                findAllPathsBetweenTwoTargets();
                break;

            case OPTION_FIVE:
                runSimulationTask();
                break;

            case OPTION_SIX:
                System.out.println("Exiting GPUP...");
                System.exit(0);

            case OPTION_BONUS:
                findCircleInGraph();
                break;


            default:
                System.out.println("Out of range menu option");

        }
        Thread.sleep(1000);
    }

    private void findCircleInGraph() {
        System.out.println("Please enter a targets name: ");
        String targetName = sc.nextLine();
        if(!GpupSystem.includesTarget(targetName))
        {
            System.out.println("No target by name " + targetName + " exists in the system, aborting");
            return;
        }

        List<List<String>> cyclePath = GpupSystem.findRoutesBetweenTwoTargets(targetName,targetName,1);
        if(!cyclePath.isEmpty()) {
            System.out.println("Cycle found: ");
            for (List<String> Path : GpupSystem.findRoutesBetweenTwoTargets(targetName, targetName, 1)) {
                System.out.println(Path);
            }
        }
        else{
            System.out.println("No cycle found involving target "+ targetName);
        }
    }

    private void runSimulationTask() {
        int ms =0 , processingTime = 0, incOrScratch = 0;
        double successProb = 0.0, successWithWarningsProb = 0.0;
        Consumer<String> consumer = System.out::println;
        String nextLine;

        System.out.println("Do you wish to run the task From Scratch or Incremental? Please select (1,2): \n" +
                "1) From Scratch\n" + "2) Incremental(Only in case there was a previous run)\n");
        try{
            incOrScratch = Integer.parseInt(sc.nextLine());
            if(GpupSystem.getFirstRun() == true && incOrScratch == 2)
            {
                System.out.println("No previous run on this graph, aborting");
                return;
            }
        }
        catch (NumberFormatException e){
            System.out.println("wrong input, please select option 1 or 2");
        }
        System.out.println("Please enter the processing time that each task will run for (Integer representing ms): ");

        try{ms = Integer.parseInt(sc.nextLine());}
        catch(NumberFormatException e){System.out.println("wrong input for processing time (ms) please use an Integer");
            return;}

        System.out.println("Would you like the processing time to be (1,2): \n" + "1. Random\n" +
            "2. Fixed");

        try {
            processingTime = Integer.parseInt(sc.nextLine());
            if (1 > processingTime || processingTime > 2)
                throw new NumberFormatException();
        }
        catch(NumberFormatException e){System.out.println("wrong input for processing time , please select option 1 or 2");
        return;}

        System.out.println("Please enter the probability that will determine" +
                " if a single target process is successful (decimal 0.0-1.0)");
        try {
            successProb = Double.parseDouble(sc.nextLine());
            if (0.0 > successProb || successProb > 1.0)
                throw new NumberFormatException();
        }
        catch (NumberFormatException e){System.out.println("wrong input for success probability please use a decimal number in range [0.0,1.0]");
        return;}


        System.out.println("Please enter the probability that will determine" +
                " if successful processes will have warnings   (decimal 0.0-1.0)");
        try {
            successWithWarningsProb = Double.parseDouble(sc.nextLine());
            if (0.0 > successWithWarningsProb || successWithWarningsProb > 1.0)
                throw new NumberFormatException();
        }
        catch (NumberFormatException e){System.out.println("wrong input for warning probability please use a decimal number in range [0.0,1.0]");
        return;}


        try {
            GpupSystem.runSimulationTask(ms, processingTime-1, successProb, successWithWarningsProb, consumer, incOrScratch);
        } catch (InterruptedException | ParseException | IOException e) {
            e.printStackTrace();
        }

    }

    private void findAllPathsBetweenTwoTargets() {
        int routSelection = 0;
        System.out.println("    Please provide the source target name :");
        String sourceTarget = sc.nextLine();
        if( !GpupSystem.includesTarget(sourceTarget) ){
            System.out.println("No target by name " + sourceTarget + " exists in the graph, aborting ");
            return;
        }

        System.out.println("    Please provide the destination target name :");
        String destTarget = sc.nextLine();
        if( !GpupSystem.includesTarget(destTarget) ){
            System.out.println("No target by name " + destTarget + " exists in the graph, aborting ");
            return;
        }




        System.out.println("Please select the running route relation: (1,2): \n" + "1) Required for\n"
        + "2)Depends On");


        try {
             routSelection = Integer.parseInt(sc.nextLine()) ;
             if (1 > routSelection || routSelection > 2)
                throw new NumberFormatException();
        }
        catch(NumberFormatException e){System.out.println("wrong input for route selection, please select option 1 or 2");
            return;}


        for (List<String> Path :  GpupSystem.findRoutesBetweenTwoTargets(sourceTarget, destTarget, routSelection)) {
            System.out.println(Path);
        }
    }

    private void getSpecificTargetInfo() {
        System.out.println("Please provide the targets name :");
        String targetName = sc.nextLine();
        if(!GpupSystem.includesTarget(targetName)) {
            System.out.println("No target by that name exists. ");
            return;
        }
        System.out.println("    1. Target name is "+ targetName + "\n    2. Target location in the graph is: " + GpupSystem.getTargetStatusInGraph(targetName) +
                "\n    3. The targets that are required for this target: "  + GpupSystem.getTargetDependsOnList(targetName) +
                "\n    4. The targets that depend on this target: " + GpupSystem.getTargetRequiredForList(targetName) + "\n");

    }

    private void getGeneralGraphInfo() {
        int totalNumberOfTargets = GpupSystem.getTotalCountOfTargets();
        int countIndependent = GpupSystem.getCountOfIndependentTargets();
        int countRoot = GpupSystem.getCountOfRootTargets();
        int countLeaf = GpupSystem.getCountOfLeafTargets();
        int countMid = totalNumberOfTargets-(countLeaf+countRoot+countIndependent);


        System.out.println("There are "+totalNumberOfTargets + " targets\n"+
                countIndependent + " Independent\n" +
                countRoot+ " Root\n"+
                countLeaf + " Leaf\n"+
                countMid + " Middle\n\n");
    }


}
