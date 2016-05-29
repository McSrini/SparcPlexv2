package sparcplexv2.functions;

import java.util.List;

import sparcplexv2.constantsAndParams.Constants;
import sparcplexv2.constantsAndParams.Parameters;
import sparcplexv2.intermidiateDataTypes.NodeRedistributionMap;
import sparcplexv2.intermidiateDataTypes.NodetypeCount;

public class NodeRedistributor {
    
    private List<NodetypeCount> newNodesTypeCountPerPartition;
    private List<NodetypeCount> existingPartitionNodetypeCount;
    
    public NodeRedistributor (List<NodetypeCount> newNodesTypeCountPerPartition ,
            List<NodetypeCount> partitionNodetypeCount) {
        
        //we have been given the number of easy and hard nodes per partition, in both the
        //1) newly farmed nodes - which can be moved around, AND
        //2) existing nodes inside subtrees - which cannot move
        
        this.newNodesTypeCountPerPartition = newNodesTypeCountPerPartition;
        existingPartitionNodetypeCount = partitionNodetypeCount;
        
        //we also have the number of subtrees in each partition inside both lists
    }
    
    public NodeRedistributionMap redistribute () {
        
        NodeRedistributionMap redistributionMap = new NodeRedistributionMap();
     
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
        
        //HEURISTIC:
        
        int threshold= Parameters.THRESHOLD_MIN_HARD_LEAFS_PER_CORE;
        donationHeuristic (  redistributionMap,   threshold);
        
        threshold= getAverageHardLeafsPerPartition();
        donationHeuristic (  redistributionMap,   threshold);        
        
        return redistributionMap;
    }
    

    private NodeRedistributionMap donationHeuristic (NodeRedistributionMap redistributionMap, int threshold) {
        
        int reciever=   findReciever(threshold);
        int donor =  findDonor (  threshold);
        
        while (Constants.ZERO<=reciever && Constants.ZERO<=donor){
            
            //take 1 more node from the donor and give it to the receiver
            donateNode(donor, reciever, redistributionMap);
            
            reciever= findReciever(threshold);
            donor = findDonor (  threshold);
        }
        
        return redistributionMap;
        
    }    
    
    private void donateNode(int donor, int reciever, NodeRedistributionMap redistributionMap) {
        
        redistributionMap.addDonation (donor, reciever, redistributionMap.getDonation(donor, reciever) +Constants.ONE );
        
        for(int index = Constants.ZERO ; index <Parameters.NUM_CORES; index ++){
            if (newNodesTypeCountPerPartition.get(index).getID()==donor){
                newNodesTypeCountPerPartition.get(index).decrementNumHardNodes(Constants.ONE);                 
            } else if (newNodesTypeCountPerPartition.get(index).getID()==reciever){
                newNodesTypeCountPerPartition.get(index).incrementNumHardNodes(Constants.ONE);     
            }
        }
        
    }

    //find partition that is lowest below supplied threshold
    //Return -1 if no receiver
    private int findReciever (int threshold){
        int pid = Constants.MINUS_ONE;
        int lowestKnown =  (int) Constants.PLUS_INFINITY;
        
        for(int partitionIndex =Constants.ZERO; partitionIndex < Parameters.NUM_CORES;partitionIndex++){
            
            int numNewLeafs=Constants.ZERO;
            for(int index = Constants.ZERO ; index <Parameters.NUM_CORES; index ++){
                if (newNodesTypeCountPerPartition.get(index).getID()==partitionIndex){
                    numNewLeafs = newNodesTypeCountPerPartition.get(index).getNumHardNodes();
                    break;
                }
            }
            
            int numOldLeafs=Constants.ZERO;
            for(int index = Constants.ZERO ; index <Parameters.NUM_CORES; index ++){
                if (this.existingPartitionNodetypeCount.get(index).getID()==partitionIndex){
                    numOldLeafs = existingPartitionNodetypeCount.get(index).getNumHardNodes();
                    break;
                }
            }
            
            if ( ( numOldLeafs+numNewLeafs) <threshold && ( numOldLeafs+numNewLeafs)<lowestKnown){
                pid = partitionIndex;
                lowestKnown = numOldLeafs+numNewLeafs;
                
            }
            
        }
        
        return pid;
    }
    
    //find partition that is highest above supplied threshold, and has nodes to spare
    //Return -1 if no donor
    private int findDonor (int threshold){
        int pid = Constants.MINUS_ONE;
        int highestKnown =  Constants.MINUS_ONE;
        
        for(int partitionIndex =Constants.ZERO; partitionIndex < Parameters.NUM_CORES;partitionIndex++){
            
            int numNewLeafs=Constants.ZERO;
            for(int index = Constants.ZERO ; index <Parameters.NUM_CORES; index ++){
                if (newNodesTypeCountPerPartition.get(index).getID()==partitionIndex){
                    numNewLeafs = newNodesTypeCountPerPartition.get(index).getNumHardNodes();
                    break;
                }
            }
            
            int numOldLeafs=Constants.ZERO;
            for(int index = Constants.ZERO ; index <Parameters.NUM_CORES; index ++){
                if (this.existingPartitionNodetypeCount.get(index).getID()==partitionIndex){
                    numOldLeafs = existingPartitionNodetypeCount.get(index).getNumHardNodes();
                    break;
                }
            }
            
            if (numNewLeafs>Constants.ZERO && ( numOldLeafs+numNewLeafs) >threshold && ( numOldLeafs+numNewLeafs)>highestKnown){
                pid = partitionIndex;
                highestKnown = numOldLeafs+numNewLeafs;
                
            }
            
        }
        
        return pid;
        
    }

    private int getAverageHardLeafsPerPartition(){
        
        int average = Constants.ZERO;
        
        for(int index =Constants.ZERO; index < Parameters.NUM_CORES;index++){
            
            int numNewLeafs = newNodesTypeCountPerPartition.get(index).getNumHardNodes();
            int numOldLeafs = existingPartitionNodetypeCount.get(index).getNumHardNodes();
            average += (numOldLeafs+numNewLeafs);            
        }
        
        return (int) Math.ceil(average/Parameters.NUM_CORES);
        
    }
    
}
