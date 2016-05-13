package sparcplexv2.intermidiateDataTypes;

import java.io.Serializable;
import java.util.*;

/**
 * 
 * @author SRINI
 *
 *
 * Solver result is a list of farmed out nodes, and a solution object.
 * In other words, it is the result of solving a subtree.
 * 
 * Note that node list could be empty, and solution could be the same as before (if no better solution was found).
 * 
 */
public class SolverResult implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private Solution soln;
    private List<NodeAttachment> attachmentList ;
    
    public SolverResult () {
        soln = new Solution();
        attachmentList = new ArrayList<NodeAttachment>();
    }
    
    public SolverResult (Solution soln, List<NodeAttachment> attachmentList) {
        this.soln = soln;
        this.attachmentList=attachmentList;
    }
    
    public Solution getSolution () {
        return soln;
    }

    public List<NodeAttachment> getNodeList (){
        return attachmentList;
    }
}
