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
        
        return new NodeRedistributionMap();
    }

}
