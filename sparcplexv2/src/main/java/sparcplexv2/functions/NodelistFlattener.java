package sparcplexv2.functions;

import java.util.*;

import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.PairFlatMapFunction;

import scala.Tuple2;
import sparcplexv2.intermidiateDataTypes.NodeAttachment;
import sparcplexv2.intermidiateDataTypes.SolverResult;

public class NodelistFlattener implements PairFlatMapFunction<Iterator<Tuple2<Integer,SolverResult>>,Integer,NodeAttachment> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public Iterable<Tuple2<Integer, NodeAttachment>> call(
            Iterator<Tuple2<Integer, SolverResult>> iterator) throws Exception {
        
        List<Tuple2<Integer, NodeAttachment>> flattenedList = new ArrayList<Tuple2<Integer, NodeAttachment>> ();
        
        while (iterator.hasNext()) {
            Tuple2<Integer, SolverResult> tuple = iterator.next();
            
            int pid = tuple._1;
            List<NodeAttachment> attachmentList  = tuple._2.getNodeList();
            
            for (NodeAttachment node : attachmentList) {
                Tuple2<Integer, NodeAttachment> nodePair = new Tuple2<Integer, NodeAttachment> (pid,node);
                flattenedList.add(nodePair);
            }
            
        }
        
        return flattenedList;
    }

}
