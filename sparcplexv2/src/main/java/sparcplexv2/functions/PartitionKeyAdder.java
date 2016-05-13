package sparcplexv2.functions;

import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;  
import sparcplexv2.intermidiateDataTypes.NodeAttachment;

/**
 * 
 * @author SRINI
 *
 * add a key to convert tree into pair
 */
public class PartitionKeyAdder implements PairFunction<NodeAttachment,Integer,NodeAttachment>{

    /**
     * 
     */
    private static final long serialVersionUID = -7474219718414305808L;
    private int id ;
    
    public PartitionKeyAdder (int id) {
        this.id = id;
    }

    public Tuple2<Integer, NodeAttachment> call(NodeAttachment node)
            throws Exception {
        Tuple2<Integer, NodeAttachment>  tuple = new Tuple2<Integer, NodeAttachment> (id, node);

        return tuple ;
    }
    

}
