package sparcplexv2.functions;

import java.util.*;

import org.apache.spark.api.java.function.PairFlatMapFunction;

import scala.Tuple2;
import sparcplexv2.constantsAndParams.Constants;
import sparcplexv2.constantsAndParams.Parameters;
import sparcplexv2.intermidiateDataTypes.NodeAttachment;
import sparcplexv2.intermidiateDataTypes.NodeRedistributionMap;

public class PartitionkeyChanger implements PairFlatMapFunction<Iterator<Tuple2<Integer,NodeAttachment>>,Integer,NodeAttachment> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private NodeRedistributionMap reDistMap;
    
    public PartitionkeyChanger ( NodeRedistributionMap reDistMap) {
     
        this.reDistMap = reDistMap;   
    }

    public Iterable<Tuple2<Integer, NodeAttachment>> call(
            Iterator<Tuple2<Integer, NodeAttachment>> iterator) throws Exception {
         
        List <Tuple2<Integer, NodeAttachment>> listWithKeyChanged  = new ArrayList <Tuple2<Integer, NodeAttachment>> ();
        
        while (iterator.hasNext()) {
            Tuple2<Integer, NodeAttachment> input = iterator.next();
            
            int newpartitionKey = getNewPartitionIdForNode ( input._1,    reDistMap) ;
            Tuple2<Integer, NodeAttachment> output = new Tuple2<Integer, NodeAttachment> (newpartitionKey, input._2);
            
            listWithKeyChanged.add(output);
        }
        
        return listWithKeyChanged;
        
    }
    
    /**
     * returns the new partition for a node
     * 
     * The redistribution map tells us how many nodes from this partition should move to other partitions
     * 
     * the assumption here is that we can edit the redistribution map, as it is our local copy ( 1 copy per executor, 1 executor per core)
     * 
     * @param oldKey
     * @param reDistMap
     * @return
     */
    private int getNewPartitionIdForNode (int currentPartitionID,  NodeRedistributionMap reDistMap) {
        
        //default is to keep the partition ID the same as before
        int newKey = currentPartitionID;
        
        if (reDistMap.makesDonations(currentPartitionID)){
            
            //move this node to the first destination partition 
            int index = Constants.ZERO;
            
            for ( ;index< Parameters.NUM_CORES; index++){
                
                int count = reDistMap.getDonation( currentPartitionID, index) ;
                if (count > Constants.ZERO) {
                    
                    //this is the destination partition id
                    newKey = index;
                    
                    //before exiting, lower down the count, since we just donated one node
                    reDistMap.addDonation(currentPartitionID, index, count-Constants.ONE);               
                    
                    break;
                }
            }            
             
        }
        
        return  newKey;
    }

}
