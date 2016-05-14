package sparcplexv2.functions;

import java.util.List;

import sparcplexv2.intermidiateDataTypes.NodeRedistributionMap;
import sparcplexv2.intermidiateDataTypes.NodetypeCount;

public class NodeRedistributor {
    
    public NodeRedistributor (List<NodetypeCount> newNodesTypeCountPerPartition ,
            List<NodetypeCount> partitionNodetypeCount) {
        
        //we have been given the number of easy and hard nodes per partition, in both the
        //1) newly farmed nodes - which can be moved around, AND
        //2) existing nodes inside subtrees - which cannot move
        
        //we also have the number of subtrees in each partition
    }
    
    public NodeRedistributionMap redistribute () {
     
        //decide how to distribute the new nodes among partitions to achieve 
        //load balancing with minimum network transfer
        
        /*
         * similar # of nodes per partition
         * similar # of subtrees per partition
         */
        
        /**
         * Can solve a simple CPLEX problem for this.
         * OR
         * A simple heuristic like this:
         * 1) find the partition H that is highest above the threshold and has farmed out nodes
         * 2) find the partition B that is lowest below the threshold
         * 3) move one leaf from H to B
         * 4) repeat until no more leafs left to move, or every partition is above threshold
         * 5) if some partitions still have leafs, repeat the above steps, but this time instead of threshold, try to reach average.
         * 
         */
        
        //Also we have the drastic option of aborting a subtree being solved, and restarting it on another partition
        
        return new NodeRedistributionMap();
    }

}
