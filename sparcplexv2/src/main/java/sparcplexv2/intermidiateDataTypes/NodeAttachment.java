package sparcplexv2.intermidiateDataTypes;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import sparcplexv2.constantsAndParams.Constants;

/**
 * 
 * @author srini
 *
 * This object is the leaf node representation .
 * This object is shuffled across the network for load balancing.
 */
public class NodeAttachment implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    // distance From Original Node
    //  this is NOT the depth in the current subtree   
    private int depth;   

    //every time there is a branching on a variable, we update on of these lists with the
    //new bound corresponding to the branching condition
    private Map< String, Double > upperBounds ;
    private Map< String, Double > lowerBounds ;
    
    //easy nodes are close to being solved
    private boolean isEasy = false;
    
    //constructors    
    public NodeAttachment () {
        depth = Constants.ZERO ;
        upperBounds = new HashMap< String, Double >();
        lowerBounds = new HashMap< String, Double >();
        isEasy = false;
    }
    
    public NodeAttachment (int depth , boolean easy,  Map< String, Double > upperBounds, Map< String, Double > lowerBounds) {
        this.depth = depth ;
        this.upperBounds = upperBounds;
        this.lowerBounds = lowerBounds;
        this.isEasy = easy;
    }
    
    public void setDepth(int depth ){
        this.depth = depth;
    }
    
    public int getDepth(){
        return depth  ;
    }

    
    public void setEasy(){
        isEasy = true;
    }
    
    public boolean isEasy(){
        return isEasy  ;
    }

    public Map< String, Double >   getUpperBounds   () {
        return  upperBounds ;
    }

    public Map< String, Double >   getLowerBounds   () {
        return  lowerBounds ;
    }
    
}
