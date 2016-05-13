package sparcplexv2.functions;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.api.java.function.PairFlatMapFunction;
 
import scala.Tuple2; 
import sparcplexv2.constantsAndParams.Constants;
import sparcplexv2.constantsAndParams.Parameters;
import sparcplexv2.intermidiateDataTypes.*;


/**
 * 
 * @author SRINI
 *
 * Solves all subtrees in the partition one by one.
 * 
 * Decides the time slice for each subtree, depending on time allocated for the whole partition, clock time remaining, and the size of each sub tree.
 * Note that the users can supply (as parameter) the time slices recommended for solving each type of tree node .
 * In case some subtrees cannot be solved at all in this iteration, they are left alone ( to be picked up next iteration)
 * 
 * When we  solve a subtree for a certain time, we currently exercise very little control over the order in which CPLEX solves the subtree nodes.
 * 
 * After solving each subtree, updates local best known optimum  if needed.
 * Returns a list of farmed out nodes ( which could be empty) , and the best local solution found on this partition.
 * 
 */
public class SolveWithCplex  implements PairFlatMapFunction<Iterator<Tuple2<Integer,ActiveSubTree>>, Integer, SolverResult> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private final Solution bestKnownGlobalSolution;
    private Solution bestKnownLocalOptimum;
    
    private final Instant endTimeOnWorkerMachine;
    private final boolean doFarming;
    
    private int easyNodesRemainingInPartition  =Constants.ZERO;
    private int hardNodesRemainingInPartition   =Constants.ZERO;
    private List<NodetypeCount> partitionAttributeList;
    
    public SolveWithCplex (Instant endTimeOnWorkerMachine, boolean doFarming, 
            Solution bestKnownGlobalSolution, List<NodetypeCount> partitionAttributeList ){
        
        this.endTimeOnWorkerMachine=endTimeOnWorkerMachine;
        this.doFarming =doFarming;     
        
        this.bestKnownGlobalSolution = bestKnownGlobalSolution ;  
        bestKnownLocalOptimum =bestKnownGlobalSolution;
        
        this.partitionAttributeList=partitionAttributeList;
       
    }

    public Iterable<Tuple2<Integer, SolverResult>> call(
            Iterator<Tuple2<Integer, ActiveSubTree>> iterator) throws Exception {
        
        //our return value
        List<Tuple2<Integer, SolverResult>> resultList = new ArrayList<Tuple2<Integer, SolverResult>>();
        //our return value minus the key
        List<  NodeAttachment>  farmedOutNodes  = new ArrayList< NodeAttachment> ();
        
        Tuple2<Integer, ActiveSubTree> inputTuple = null;
        int partitionId = Constants.ZERO;
                        
        //process one subtree at a time 
        while ( iterator.hasNext()){
            
            inputTuple = iterator.next();
            partitionId = inputTuple._1;
            ActiveSubTree subTree = inputTuple._2;
            
            //find the number of easy and hard nodes in the partition
            //this will only be done the first time
            if (easyNodesRemainingInPartition+hardNodesRemainingInPartition==Constants.ZERO) {
                //initialize these counts
                for (NodetypeCount attr :partitionAttributeList){
                    if (attr.getID()==partitionId){
                        easyNodesRemainingInPartition = attr.getNumEasyNodes();
                        hardNodesRemainingInPartition = attr.getNumHardNodes();
                        break;
                    }
                }
            }
            
            //get time slice for this subtree
            double timeSliceForSubTree = getTimeSliceForThisSubtree(  subTree);
            
            //solve the subtree if we have been alloted at least a few seconds
            if (timeSliceForSubTree > Constants.ZERO  ){
                farmedOutNodes.addAll(  subTree.solve(timeSliceForSubTree, doFarming, bestKnownLocalOptimum.getObjectiveValue() ));     
                Solution subTreeSolution = subTree.getSolution() ;
                
                if ( Constants.ZERO == (new SolutionComparator()).compare(bestKnownLocalOptimum, subTreeSolution))
                    bestKnownLocalOptimum = subTreeSolution;
                
            }
                       
            //update counts
            easyNodesRemainingInPartition-=subTree.getNumEasyLeafNodes();
            hardNodesRemainingInPartition-=subTree.getNumHardLeafNodes();
            
        }
        
        //return results
        SolverResult solverResult = new SolverResult(bestKnownLocalOptimum, farmedOutNodes); 
        Tuple2<Integer, SolverResult> resultTuple = new Tuple2<Integer, SolverResult>(partitionId,solverResult );
        resultList.add(resultTuple );
        return resultList;
         
    }//end method call
   
    private double getTimeSliceForThisSubtree(ActiveSubTree subTree){
        
        double timeSliceForSubTree = Constants.ZERO;
        
        int numberOfEasyNodesInSubTree =subTree.getNumEasyLeafNodes();
        int numberOfHardNodesInSubtree =subTree.getNumHardLeafNodes();
        
        double wallClockTimeLeft = Duration.between(Instant.now(), this.endTimeOnWorkerMachine).toMillis()/Constants.THOUSAND;
        
        if (wallClockTimeLeft >Constants.ZERO) {
            
            //we have some time left to solve trees on this partition
            
            //check to see if we have more time than expected for the hard nodes
            double timeLeftForHardNodesInPartition = 
                    wallClockTimeLeft  - easyNodesRemainingInPartition * Parameters.EASY_NODE_TIME_SLICE_SECONDS;
            
            double surplusTime = timeLeftForHardNodesInPartition    - hardNodesRemainingInPartition*Parameters.HARD_NODE_TIME_SLICE_MAX_SECONDS;
            
            if(surplusTime>Constants.ZERO){
                //we try to use up surplus time right away
                timeSliceForSubTree = surplusTime + 
                        numberOfHardNodesInSubtree*Parameters.HARD_NODE_TIME_SLICE_MAX_SECONDS + 
                         + numberOfEasyNodesInSubTree*Parameters.EASY_NODE_TIME_SLICE_SECONDS;
            } else {
                //divide remaining time equally between remaining subtrees. So get the fair share for this subtree.
                timeSliceForSubTree = wallClockTimeLeft *  (numberOfHardNodesInSubtree*Parameters.HARD_NODE_TIME_SLICE_MAX_SECONDS    +numberOfEasyNodesInSubTree *Parameters.EASY_NODE_TIME_SLICE_SECONDS) ;
                timeSliceForSubTree = timeSliceForSubTree/ (hardNodesRemainingInPartition*Parameters.HARD_NODE_TIME_SLICE_MAX_SECONDS +easyNodesRemainingInPartition * Parameters.EASY_NODE_TIME_SLICE_SECONDS) ;
                
                //solve subtree for at least EASY_NODE_TIME_SLICE_SECONDS , even if this sends us slightly over the time limit
                if (timeSliceForSubTree <Parameters.EASY_NODE_TIME_SLICE_SECONDS ) timeSliceForSubTree=Parameters.EASY_NODE_TIME_SLICE_SECONDS;
            }
            
        }  
        
        return timeSliceForSubTree  ;
        
    }// end method getTimeSliceForThisSubtree
    
}
