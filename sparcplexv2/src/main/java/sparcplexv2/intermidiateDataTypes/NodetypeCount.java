package sparcplexv2.intermidiateDataTypes;

import java.io.Serializable;

/**
 * 
 * for a subtree or node list in a partition ,   its easy and hard node counts
 * 
 * @author SRINI
 *
 */
public class NodetypeCount implements Serializable {
    
    
    /**
     * 
     */
    private static final long serialVersionUID = -118784928902205000L;
    /**
     * 
     */
     
    private int partitionID;
    private int numEasyNodes;
    private int numHardNodes;
    
    public int getID () {
        return partitionID;
    }
    
    public int getNumEasyNodes () {
        return numEasyNodes;
    }
    
    public int getNumHardNodes () {
        return numHardNodes;
    }
    
    public void setID (int id) {
        this.partitionID= id;
    }
    
    public void setNumEasyNodes (int num) {
        this. numEasyNodes=num;
    }
    
    public void setNumHardNodes (int num) {
        this. numHardNodes=num;
    }
    
}
