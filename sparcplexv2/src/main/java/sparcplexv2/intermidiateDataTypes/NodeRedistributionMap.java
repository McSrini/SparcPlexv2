package sparcplexv2.intermidiateDataTypes;

import java.util.*;
import java.util.Map.Entry;
 
import sparcplexv2.constantsAndParams.Constants;

/**
 * 
 * @author SRINI
 * 
 * used to decide how the farmed out nodes should be re-distributed to achieve load balancing
 *
 */
public class NodeRedistributionMap {
    
    // Map< SID, Map of Tuple2< PID , count>>
    //from every partition SID , how many nodes (count) to move to partition PID
    
    private Map<Integer, HashMap<Integer, Integer>> redistributionMap = new HashMap<Integer, HashMap<Integer, Integer>>();
    
    public HashMap<Integer, Integer> getReciversMap(int key) {
        return redistributionMap.get(key);
    }
     
    public String toString() {
        String result = Constants.EMPTY_STRING;
        
        for (int key : redistributionMap.keySet()){
            result = Constants.NEWLINE+ result + key + Constants.NEWLINE;
            Map<Integer, Integer> reciversMap = redistributionMap.get(key);
            
            for (Entry <Integer, Integer> entry : reciversMap.entrySet()){
                result =   result +entry.getKey()+    Constants.BLANKSPACE + entry.getValue() + Constants.NEWLINE;
            }
            
        }
        
        return result+ Constants.NEWLINE;
    }
    
    //does this partition make any donations?
    public boolean makesDonations (int srcId){
        return redistributionMap.containsKey(srcId);
    }
    
    //add the decision that partition srcId will donate count nodes to partition pid
    public void addDonation   (int srcId, int pid, int count) {
        
        if (! redistributionMap.containsKey(srcId)){
            redistributionMap.put(srcId,  new HashMap  <Integer, Integer>());
        }
        
        HashMap  <Integer, Integer> decisionMap = redistributionMap.get(srcId);
        
        decisionMap.put(pid, count);        
        redistributionMap.put(srcId, decisionMap);
        
    }
    
    //get the donation made by partition srcId to partition pid
    public int getDonation   (int srcId, int pid ) {
        int count = Constants.ZERO;
                
        if (  redistributionMap.containsKey(srcId)){      
            if (  redistributionMap.get(srcId).containsKey(pid)){
                count = redistributionMap.get(srcId).get(pid);
            }
        }
                
        return count;
    }

}
