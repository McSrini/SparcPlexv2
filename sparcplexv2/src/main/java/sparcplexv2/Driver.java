package sparcplexv2;

import ilog.concert.IloException;

/**
 * Author SRINI
 * 
 * Driver for SparcPlex
 * 
 */

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.spark.HashPartitioner;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import sparcplexv2.constantsAndParams.Constants;
import sparcplexv2.constantsAndParams.Messages;
import sparcplexv2.constantsAndParams.Parameters;
import sparcplexv2.functions.*;
import sparcplexv2.intermidiateDataTypes.*;
 
public class Driver {
    
    private static Logger logger=Logger.getLogger(Driver.class);
    
    public static void main(String[] args) throws IloException {
        
        //Driver for distributing the CPLEX  BnB solver on Spark
        SparkConf conf = new SparkConf().setAppName("SparcPlex V2");
        JavaSparkContext sc = new JavaSparkContext(conf);
        
        //We have an RDD which holds the frontier, i.e. subtrees with unsolved tree nodes
        //the key is the partition number
        JavaPairRDD < Integer, ActiveSubTree> frontier ; 
                
        //Initially the frontier only has the original problem  
        // Root node of the BnB tree is represented by an empty node attachment, i.e. no branching variables  
        //
        //let us start this root node on partition 0
        //
        // Note how the RDD is created, we never shuffle ActiveSubTree objects ; Only node attachments are shuffled across the network
        
        List<NodeAttachment> nodeList  = new ArrayList<NodeAttachment> ();
        nodeList.add(new NodeAttachment());
        frontier =sc.parallelize(nodeList) .mapToPair( new PartitionKeyAdder(Constants.ZERO)).mapValues( new AttachmentConverter() ); 
         
        //initialize  the incumbent
        Solution incumbent = new Solution( ); 
        

        //loop till frontier is empty, or some iteration count or time limit is exceeded
        for (int iteration = 0;iteration <Parameters.MAX_ITERATIONS;iteration++){ 
            
            //STEP 0 : 
            //*****************************************************************************************************************
            //prepare for this iteration, if it is needed at all
                 
            frontier.cache(); //used twice, so cache it
            
            long numFrontierNodes = frontier.count();
            
            if ( numFrontierNodes== Constants.ZERO) {
                
                logger.debug(Messages.Complete_MSG + iteration) ;
                //we are done
                break; 
                
            } else {
                logger.debug(Messages.ITER_MSG + iteration) ;
            }
            
            //STEP 1 : 
            //*****************************************************************************************************************
            //Before starting the iteration, decide if some subtrees need farming.
            //Note that we farm for leaf nodes if some subtree is too big, or some partition is almost empty (generation phase)
            //When every core has enough work, and no subtree is too big, we just continue solving without farming (solution phase)
            
            //We count exactly how many easy and hard leaf nodes each partition has. 
            //This is used to make farming decisions and also to allocate solving time.
            
            JavaRDD<  NodetypeCount> partitionNodetypeCountRDD =
                    frontier.mapPartitions (new SubtreeNodeCounter() );            
            List<NodetypeCount> partitionNodetypeCounts = partitionNodetypeCountRDD.collect();
            
            //Now we know exactly how many easy and hard nodes are there in each partition
            //We can broadcast this if we want, or use it as an argument (currently not broadcasting)
            
            //should we farm?
            boolean doFarming = ! isSolutionPhase(partitionNodetypeCounts);
            
            //farming phase need not last very long.
            //We can instruct some partitions, and some subtrees in them, to generate 2 kids each, and then return.
            //Upon return to the driver, check if we have enough new nodes for balancing the starving partitions.
            //If yes, can enter solution phase. If no the farm for one more iteration ,and so on.
            //
            // When do  farming is false, SolveWithCplex would focus on solving, and farm only when a certain tree becomes too big.
            //Other wise it would instruct every subtree to return after branching for the first time
            //
            
             
            //get a reasonable end time for this iteration.
            //this requires the average number of hard nodes per partition, and some other info
            Instant endTimeOnWorkerMachine = getIterationEndTime( partitionNodetypeCounts);
            

            
            //if farming is needed, then make a better decision as to which subtrees on which partition should be farmed
            //Maybe better to farm small subtrees, or trees with nodes whose depth is closer to root ?
            //Also, must farm trees which are too big, but never farm a tree with only one node in it
            //
            //For now, we assume that every subtree obeys the farming instruction given by the driver, except trees  which are 
            // too small (never farm) or too big ( always farm). See branch handler.
            
            
            
            //We are now ready to solve the subtrees            
            
            
            //STEP 2 : 
            //*****************************************************************************************************************
            //Solve with CPLEX and collect any farmed out nodes
            SolveWithCplex cplexSolver = new SolveWithCplex(  endTimeOnWorkerMachine,
                    doFarming, incumbent ,  partitionNodetypeCounts);
            JavaPairRDD< Integer, SolverResult> resultsRDD  = frontier.mapPartitionsToPair( cplexSolver, true);
            
            resultsRDD.cache(); //used more than once
            resultsRDD.count(); //to force immediate computation
            
           
            
               
            //STEP 3 : 
            //*****************************************************************************************************************
            //Update the incumbent.
            JavaRDD<  Solution> solutionsRDD = resultsRDD.mapValues( new SolutionFetcher()).values();
            
            //we will have 1 solution from every partition, i.e. the local optimum
            //We can perform the reduction in the driver (i.e. right here)
            List<Solution> solutionsFound = solutionsRDD.collect();
            for (Solution soln : solutionsFound) {
                if ( Constants.ZERO == (new SolutionComparator()).compare(incumbent, soln))
                    incumbent = soln;
            }

            
            
            //STEP 4 : 
            //*****************************************************************************************************************            
            //Decide how to distribute the farmed out nodes, if any. This may be a small optimization problem :) .
            
            //Let us recount how many nodes each partition has left.
            //For this , we need to  filter out any subtrees that were completely solved in this iteration .
            //If any subtree is in error , should we  abort the computation , or resolve the subtree maybe ? TO DO.
            JavaPairRDD < Integer, ActiveSubTree> filteredFrontier = frontier.filter(new FilterOutCompletedSubtrees()) ; 
            filteredFrontier.cache();
            
            //Now count how many nodes each partition has.
            partitionNodetypeCountRDD =    filteredFrontier.mapPartitions (new SubtreeNodeCounter() );            
            partitionNodetypeCounts = partitionNodetypeCountRDD.collect();
            
            // Get the freshly farmed out nodes. 
            //Keep the partition key, we will try to minimize node movement by changing as few keys as we can.
            //For details of redistribution, see the redistribution functions which follow
            JavaPairRDD< Integer, NodeAttachment> farmedNodesRDD =
                    resultsRDD.mapPartitionsToPair( new NodelistFlattener() , true);
            farmedNodesRDD.cache();  
            farmedNodesRDD.count(); //force evaluation and caching
            
            //count how many new nodes have been farmed out on each partition
            JavaRDD<  NodetypeCount> newNodesTypecountPerPartitionRDD = 
                    farmedNodesRDD.mapPartitions(new NodeCounter(), true);
            List<NodetypeCount> newNodesTypecountPerPartition =newNodesTypecountPerPartitionRDD.collect();
            
            //Maybe solve a small optimization problem, or use a heuristic, to decide which 
            //node goes where to achieve balancing while minimizing node movement.
            NodeRedistributionMap redistributionMap = (new NodeRedistributor(     newNodesTypecountPerPartition ,   partitionNodetypeCounts)).redistribute();
            
            //redistributionMap now contains instructions about which keys to change, i.e. which nodes to move and where to move them
            
            //use a transformation that assigns new changed key, then hash partition to realize the node movement
            //Note that, except for the nodes marked for movement, all other entities have stayed in the same partition throughout
            JavaPairRDD< Integer, NodeAttachment> balancedFarmNodes     = 
                    farmedNodesRDD.mapPartitionsToPair( new PartitionkeyChanger(redistributionMap) , true)
                    .partitionBy(new HashPartitioner(Parameters.NUM_CORES)) 
                    .cache( ); 
            
            //we now have the farmed out nodes, some of them shuffled around the network so that, 
            //when added to the existing frontier, the frontier will be more or less balanced.
            
            
            //STEP 5 : 
            //*****************************************************************************************************************            
            //union with existing frontier after converting farmed out nodes into subtree objects
            frontier = filteredFrontier.union(balancedFarmNodes.mapValues( new AttachmentConverter() ));             

            //we have our new frontier,  repeat the iterations until done
            
        
        } //end for - till frontier is empty OR iteration limit reached
        
    } //end main

    
    private static boolean isSolutionPhase(List<NodetypeCount> partitionNodeCountList ) {
        double minHardNodes = Constants.PLUS_INFINITY ;
        
        if (partitionNodeCountList.size()< Parameters.NUM_CORES) {
            //there is effectively at least 1 partition that is empty
            minHardNodes =Constants.ZERO;
        } else {
            for(NodetypeCount pattr :partitionNodeCountList){
                if (pattr.getNumHardNodes()<minHardNodes) minHardNodes = pattr.getNumHardNodes();
            }
        }
        
        return minHardNodes > Parameters.THRESHOLD_MIN_HARD_LEAFS_PER_CORE;
    }

    
    //we assume all worker machines have a synchronized clock
    //
    //Specifying a wall-clock time when this iteration should end is an approach that works even in shared clusters
    //
    private static Instant getIterationEndTime ( List<NodetypeCount> partitionAttributeList) {   
        
        double avgNumOfEasyNodesPerCore = Constants.ZERO;
        double avgNumOfHardNodesPerCore = Constants.ZERO;
        
        for (NodetypeCount attr: partitionAttributeList){
            avgNumOfEasyNodesPerCore += attr.getNumEasyNodes();
            avgNumOfHardNodesPerCore += attr.getNumHardNodes();
        }
        avgNumOfEasyNodesPerCore = avgNumOfEasyNodesPerCore/Parameters.NUM_CORES;
        avgNumOfHardNodesPerCore = avgNumOfHardNodesPerCore/Parameters.NUM_CORES;
        
        double timeSlice =     avgNumOfHardNodesPerCore*Parameters.HARD_NODE_TIME_SLICE_SECONDS * Constants.THOUSAND
                                  + avgNumOfEasyNodesPerCore*Parameters.EASY_NODE_TIME_SLICE_SECONDS * Constants.THOUSAND;
        
        if  (timeSlice < Constants.THOUSAND*Parameters.ITERATION_TIME_MIN_SECONDS) timeSlice= Constants.THOUSAND*Parameters.ITERATION_TIME_MIN_SECONDS;
        if (timeSlice > Constants.THOUSAND* Parameters.ITERATION_TIME_MAX_SECONDS ) timeSlice= Constants.THOUSAND*Parameters.ITERATION_TIME_MAX_SECONDS;
        
        return Instant.now().plusMillis((int)timeSlice );
                   
    }

}
