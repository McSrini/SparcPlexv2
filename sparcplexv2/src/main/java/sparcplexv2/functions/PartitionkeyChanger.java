package sparcplexv2.functions;

import java.util.*;

import org.apache.spark.api.java.function.PairFlatMapFunction;

import scala.Tuple2;
import sparcplexv2.constantsAndParams.Constants;
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
            
            int newpartitionKey = someFunction ( input._1,    reDistMap) ;
            Tuple2<Integer, NodeAttachment> output = new Tuple2<Integer, NodeAttachment> (newpartitionKey, input._2);
            
            listWithKeyChanged.add(output);
        }
        
        return listWithKeyChanged;
        
    }
    
    private int someFunction (int oldKey,  NodeRedistributionMap reDistMap) {
        return Constants.ZERO;
    }

}
