package sparcplexv2.functions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.api.java.function.FlatMapFunction;

import scala.Tuple2;
import sparcplexv2.constantsAndParams.Constants;
import sparcplexv2.intermidiateDataTypes.NodeAttachment;
import sparcplexv2.intermidiateDataTypes.NodetypeCount;

public class NodeCounter implements FlatMapFunction<Iterator<Tuple2<Integer,NodeAttachment>>,NodetypeCount>{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public Iterable<NodetypeCount> call(
            Iterator<Tuple2<Integer, NodeAttachment>> iterator) throws Exception {
        
        int numEasyNodes = Constants.ZERO, numHardNodes =Constants.ZERO;
        
        //our return value
        List<NodetypeCount> attributeList = new ArrayList<NodetypeCount>();
        
        Tuple2<Integer, NodeAttachment> tuple =null ;
        while (  iterator.hasNext()){
            
            tuple =iterator.next();
            NodeAttachment attachment = tuple._2;
            
            if (attachment.isEasy()) numEasyNodes++; else            numHardNodes ++; 
         
        }
        
        NodetypeCount attr = new NodetypeCount();
        attr.setID(tuple._1);
        attr.setNumEasyNodes(numEasyNodes);
        attr.setNumHardNodes(numHardNodes);
        attributeList.add(attr);
        
        return   attributeList;
    }

}
