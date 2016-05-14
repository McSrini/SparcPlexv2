package sparcplexv2.functions;

import java.util.*;

import org.apache.spark.api.java.function.*; 

import scala.Tuple2;
import sparcplexv2.constantsAndParams.Constants;
import sparcplexv2.intermidiateDataTypes.ActiveSubTree;
import sparcplexv2.intermidiateDataTypes.NodetypeCount;

/**
 * 
 * @author SRINI
 * 
 * count number of easy and hard nodes in each partition
 *
 */
public class SubtreeNodeCounter implements FlatMapFunction<Iterator<Tuple2<Integer,ActiveSubTree>>,NodetypeCount>{

    /**
     * 
     */
    private static final long serialVersionUID = -2867119556094473540L;
    
    public Iterable<NodetypeCount> call(
            Iterator<Tuple2<Integer, ActiveSubTree>> iterator) throws Exception {
          
        int numEasyNodes = Constants.ZERO, numHardNodes =Constants.ZERO;
        
        int subtreeCount = Constants.ZERO;
        
        //our return value
        List<NodetypeCount> attributeList = new ArrayList<NodetypeCount>();
        
        Tuple2<Integer, ActiveSubTree> tuple =null ;
        while (  iterator.hasNext()){
            
            tuple =iterator.next();
            ActiveSubTree tree = tuple._2;
            
            numEasyNodes += tree.getNumEasyLeafNodes()  ;
            numHardNodes += tree.getNumHardLeafNodes() ;
            
            subtreeCount++;
         
        }
        
        NodetypeCount attr = new NodetypeCount();
        attr.setID(tuple._1);
        attr.setNumEasyNodes(numEasyNodes);
        attr.setNumHardNodes(numHardNodes);
        attr.setNumberOfTrees(subtreeCount);
        attributeList.add(attr);
        
        return   attributeList;
    }

  
}
