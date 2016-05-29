package sparcplexv2.intermidiateDataTypes;

import java.util.*;

import scala.Tuple2;
import sparcplexv2.constantsAndParams.Constants;

/**
 * 
 * @author SRINI
 * 
 * used to decide how the farmed out nodes should be re-distributed to achieve load balancing
 *
 */
public class NodeRedistributionMap {
    
    // Map< SID,List of Tuple2< PID , count>>
    //from every partition SID , how many nodes (count) to move to partition PID
    
    public Map<Integer, List <Tuple2<Integer, Integer>>> redistributionMap = new HashMap<Integer, List<Tuple2<Integer, Integer>>> ();
    
     
    public String toString() {
        String result = Constants.EMPTY_STRING;
        
        for (int key : redistributionMap.keySet()){
            result = Constants.NEWLINE+ result + key + Constants.NEWLINE;
            List <Tuple2<Integer, Integer>> decisionList = redistributionMap.get(key);
            
            for (Tuple2<Integer, Integer> tuple : decisionList){
                result =   result + tuple._1 +    Constants.BLANKSPACE + tuple._2 + Constants.NEWLINE;
            }
            
        }
        
        return result+ Constants.NEWLINE;
    }
    
    public void addDecision(int srcId, int pid, int count) {
        
        if (! redistributionMap.containsKey(srcId)){
            redistributionMap.put(srcId,  new ArrayList <Tuple2<Integer, Integer>>());
        }
        
        List <Tuple2<Integer, Integer>> decisionList = redistributionMap.get(srcId);
        Tuple2<Integer, Integer> tuple = new Tuple2 (pid, count);
        decisionList.add(tuple);
        redistributionMap.put(srcId, decisionList);
        
    }

}
