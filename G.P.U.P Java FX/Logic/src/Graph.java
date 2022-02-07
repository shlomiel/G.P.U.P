import java.util.*;

public class Graph {
    private final Map<Target, List<Target>> adjTargets = new HashMap<>();
    private String name;

    public synchronized boolean isLeafOrIndependent(Target targetName) { // true if so else false
        if(targetName == null)
            return false;
       return adjTargets.get(targetName).isEmpty();

    }



    enum RoutSelection {
        REQUIRED_FOR,
        DEPENDS_ON
    }

    enum RunningTimeType {
        RANDOM,
        FIXED
    }

    public synchronized Map<Target, List<Target>> getAdjTargets() {
        return adjTargets;
    }

    public void buildGraph(String graphName, Map<String, Target> targetsToBuildFrom) {
        name = graphName;
        for (Target target : targetsToBuildFrom.values()) {
            addTarget(target);
        }
    }

    protected Target getSingleTargetFromGraph(String targetName) {
        for (Target target: adjTargets.keySet()) {
            if (target.getTargetName().equals(targetName)) {
                return target;
            }
        }
        return null; // Target does not exist in graph
    }

    void addEdge(String source, String dest) {

        for (Target sourceTarget : adjTargets.keySet()) {
            if (sourceTarget.getTargetName().equals(source)) {
                for (Target destTarget : adjTargets.keySet()) {
                    if (destTarget.getTargetName().equals(dest)) {
                        if(!adjTargets.get(sourceTarget).contains(destTarget))
                            adjTargets.get(sourceTarget).add(destTarget);

                        sourceTarget.addTargetToRequiredFor(destTarget);
                        destTarget.addTargetToDependsOn(sourceTarget);
                        break;
                    }
                }
                break;
            }
        }
    }

    void addTarget(Target target) {
        adjTargets.putIfAbsent(target, new ArrayList<>());
    }

     synchronized void removeTarget(Target targetToRemove) {
        for (Target targetInGraph: adjTargets.keySet()) {
                targetInGraph.removeNeighborFromDepOn(targetToRemove);
                targetInGraph.removeNeighborFromReqFor(targetToRemove);
                if(adjTargets.get(targetInGraph).contains(targetToRemove))
                    adjTargets.get(targetInGraph).remove(targetToRemove);
        }

        adjTargets.remove(targetToRemove);
        for (Target target:adjTargets.keySet()) {
            target.setStatusInGraph();
        }
    }


    protected int getCountOfIndependentNodes(){
        int counter = 0 ;
        for (Target target:adjTargets.keySet() ) {
            if (target.getStatusInGraph() == Target.GraphStatus.INDEPENDENT){
                counter++;
            }
        }
        return counter;
    }


    public int getCountOfRootNodes() {
        int counter = 0 ;
        for (Target target:adjTargets.keySet() ) {
            if (target.getStatusInGraph() == Target.GraphStatus.ROOT){
                counter++;
            }
        }
        return counter;
    }
    public int getCountOfLeafNodes() {
        int counter = 0 ;
        for (Target target:adjTargets.keySet() ) {
            if (target.getStatusInGraph() == Target.GraphStatus.LEAF){
                counter++;
            }
        }
        return counter;
    }

    public int getCountOfMiddleNodes() {
        int counter = 0 ;
        for (Target target:adjTargets.keySet() ) {
            if (target.getStatusInGraph() == Target.GraphStatus.MIDDLE){
                counter++;
            }
        }
        return counter;
    }


    public String getGraphName() {
        return name;
    }

    public int getCountOfNodes() {
        return adjTargets.size();
    }

    public boolean isEmpty()
    {
        return adjTargets.isEmpty();
    }

    public void printGraph() {
        for (Target target : adjTargets.keySet()) {
            System.out.print(target.getTargetName() + "--->");

            if (!adjTargets.get(target).isEmpty()) {
                for (Target neighbor : adjTargets.get(target)) {
                    System.out.print(neighbor.getTargetName() + ' ');
                }
            }
            System.out.println();
        }
    }


    public void setNodesStatus(Map <String,List<String>> serialSets) {
        for (Target target:adjTargets.keySet()) {
            target.setStatusInGraph();
            target.setSets(serialSets);
            target.setAllDependsForNames(this);
            target.setAllRequiredForNames(this);
        }
    }


    public List<List<String>> findAllPaths(String sourceTarget, String destTarget, RoutSelection routDirection) {


        List<Target> neighbors;
        List<List<String>> result = new ArrayList<>();
        Queue<List<String>> queue = new LinkedList<> ();
        queue.add(Arrays.asList(sourceTarget));

        while(!queue.isEmpty()){
            List<String> path = queue.poll();
            String lastNode = path.get(path.size()-1);

            if(lastNode.equals(destTarget) && path.size() > 1){
                result.add(new ArrayList<>(path));
            }
            else{
                if(routDirection == RoutSelection.REQUIRED_FOR)
                    neighbors = getSingleTargetFromGraph(lastNode).getRequiredFor();
                else
                    neighbors = getSingleTargetFromGraph(lastNode).getDependsOn();


                for(Target neighbor : neighbors){
                    List<String> list = new ArrayList<>(path);
                    if(list.size() > 2*adjTargets.size()) // stuck in cycle
                        return result;
                    list.add(neighbor.getTargetName());
                    queue.add(list);

                }
            }
        }

        return result;
    }




protected List<Target> getTopologicalSortOfNodes() {
        Map<Target,Integer> inDegree= new HashMap<>();
        Queue<Target> queue = new LinkedList<>();
        List<Target> result = new LinkedList<>();

        for(Target node:adjTargets.keySet()) {
            inDegree.putIfAbsent(node,node.getRequiredFor().size());
        }
        for (Target node:inDegree.keySet()) {
            if (inDegree.get(node) == 0) {
                queue.add(node);
            }
        }

        while(!queue.isEmpty()) {
            Target u = queue.poll();
            result.add(u);
            for (Target v: u.getDependsOn()) {
                inDegree.put(v, inDegree.get(v)-1 );
                if(inDegree.get(v) == 0){
                    queue.add(v);
                }
            }
        }

        return result;
    }

}


