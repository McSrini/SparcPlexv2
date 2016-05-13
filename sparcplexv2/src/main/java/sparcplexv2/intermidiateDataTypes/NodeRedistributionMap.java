package sparcplexv2.intermidiateDataTypes;

import java.util.*;

import scala.Tuple2;

/**
 * 
 * @author SRINI
 * 
 * used to decide how the farmed out nodes should be re-distributed to achieve load balancing
 *
 */
public class NodeRedistributionMap {
    
    // Map< SID, Tuple2< PID , count>>
    //from every partition SID , how many nodes (count) to move to partition PID
    
    public Map<Integer, Tuple2<Integer, Integer>> redistributionMap = new HashMap<Integer, Tuple2<Integer, Integer>> ();
    
     

}
