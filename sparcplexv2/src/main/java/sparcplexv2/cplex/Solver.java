package sparcplexv2.cplex;

import java.util.List;

import sparcplexv2.constantsAndParams.Constants;
import sparcplexv2.constantsAndParams.Parameters;
import sparcplexv2.intermidiateDataTypes.NodeAttachment;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;

/**
 * 
 * @author SRINI
 * 
 * CPLEX based solver
 * 
 * This object is not really required, probably should be merged into ActiveSubTree
 *
 */
public class Solver {

    //this is the CPLEX object we are attached to  
    private IloCplex cplex   ;
    
    //this is the branch handler for the CPLEX object
    private BranchHandler branchHandler;
         
    public Solver (IloCplex cplex , NodeAttachment attachment ) throws IloException{
      
        branchHandler = new BranchHandler(         attachment);
        this.cplex=cplex;
        this.cplex.use(branchHandler);   
        setSolverParams();
  
    
    }
    
    public void setSolverParams() throws IloException {
        //depth first?
        if (Parameters.isDepthFirstSearch) cplex.setParam(IloCplex.Param.MIP.Strategy.NodeSelect, Constants.ZERO); 
        
        //MIP gap
        if (Parameters.RELATIVE_MIP_GAP>Constants.ZERO) cplex.setParam( IloCplex.Param.MIP.Tolerances.MIPGap, Parameters.RELATIVE_MIP_GAP);

        //others
    }
    
    public IloCplex.Status solve(double timeSliceInSeconds,   boolean doFarming, double bestKnownOptimum , int currentHardLeafnodeCount) throws IloException{
        
        //reset the branch handler 
        branchHandler.reset(currentHardLeafnodeCount );
        
        branchHandler.setFarming(doFarming);
        branchHandler.setBestKnownOptimum(bestKnownOptimum);
        
        
        cplex.setParam(IloCplex.Param.TimeLimit, timeSliceInSeconds); 
        cplex.solve();
        
        return cplex.getStatus();
    }
    
    public boolean isAborted () {
        return branchHandler.isAborted();
    }
     
    public double getNumEasyNodesAdded(){
        return this.branchHandler.getNumEasyNodesCreated();
    }
    
    public double getNumHardNodesAdded(){
        return this.branchHandler.getNumHardNodesCreated();
    }
    
    public double getNumEasyNodesPruned(){
        return this.branchHandler.getNumEasyNodesPruned();
    }
    
    public double getNumHardNodesPruned(){
        return this.branchHandler.getNumHardNodesPruned();
    }
    
    public List<NodeAttachment>  getFarmedOutNodes(){
        return this.branchHandler.getFarmedOutNodes();
    }
  
}
