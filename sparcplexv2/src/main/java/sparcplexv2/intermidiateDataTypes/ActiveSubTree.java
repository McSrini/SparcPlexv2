package sparcplexv2.intermidiateDataTypes;

import java.io.IOException;
import java.util.*;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import sparcplexv2.constantsAndParams.Constants;
import sparcplexv2.constantsAndParams.Messages;
import sparcplexv2.constantsAndParams.Parameters;
import sparcplexv2.cplex.Solver;
import sparcplexv2.cplex.UtilityLibrary;

/**
 * 
 * @author SRINI
 * 
 * This class is a wrapper around a CPLEX subtree being solved
 * 
 * Our frontier is a collection of these objects, spread across the cluster.
 * Each partition could have several of these objects. 
 * 
 * note that this object is not serializable, it stays in the same partition for its lifetime 
 *
 */
public class ActiveSubTree { //note that this object is not serializable
    
    //for logging
    private String myGuid ;
    private int myPartitionId;
    private Logger logger ;
    private String logfile;
    private boolean isLoggingInitialized = false;
    
    
    //the CPLEX object representing this partially solved tree 
    private  IloCplex cplex ;
    private int numEasyLeafNodes;
    private int numHardLeafNodes;
  
    //a solver object that is used to solve this tree few seconds at a time 
    private Solver solver ;
 
    private NodeAttachment root ;
    
    //set this flag if there is no point in solving this subtree any longer
    private boolean abortFlag;
     
    /**
     * 
     * @param attachment
     * @throws IloException
     * 
     * Every active subtree starts its life as a tree created from a farmed out leaf node
     * @throws IOException 
     */
    public ActiveSubTree (NodeAttachment attachment) throws IloException, IOException {
        
        //note down the root
        root = attachment;
        
        //my unique ID
        myGuid= UUID.randomUUID().toString();
        
        //initialize the CPLEX object
        cplex= new IloCplex();   
        cplex.importModel(Parameters.LP_FILENAME);
        UtilityLibrary.merge(cplex, attachment); 
        
        //get ourselves a solver
        solver = new Solver( cplex   , attachment);
        
        //keep track of number of easy and hard nodes in this subtree
        numHardLeafNodes=0;
        numEasyLeafNodes=0;
        if (attachment.isEasy()) numEasyLeafNodes++; else numHardLeafNodes++;
        
        abortFlag = false;
        
        
    }
    
    //initialize logging
    public void initLogging (int partitionId) throws IOException {
        if (! isLoggingInitialized){
            
            myGuid  = UUID.randomUUID().toString();
            myPartitionId=partitionId;
            
            isLoggingInitialized = true;
            logger= Logger.getLogger(ActiveSubTree.class);
            logger.setLevel(Level.DEBUG);
            PatternLayout layout = new PatternLayout("%d{ISO8601} [%t] %-5p %c %x - %m%n"); 
            logfile = Parameters.WORKER_LOG_FILE + partitionId +Parameters.DOT_LOG;
            logger.addAppender(new RollingFileAppender(layout,logfile));
        } 
    }
    
    public boolean isLoggingInitialized(){
        return isLoggingInitialized;
    }
    
    public   NodeAttachment getRoot (){
        return root;
    }
    
    public Solution getSolution () throws IloException {
        Solution soln = new Solution () ;
        
        soln.setError(isInError());
        soln.setOptimal(isOptimal());
        soln.setFeasible(isFeasible() );
        soln.setUnbounded(isUnbounded());
        
        soln.setOptimumValue(getObjectiveValue());
        
        if (isOptimalOrFeasible()) UtilityLibrary.addVariablevaluesToSolution(cplex, soln);
        
        return soln;
    }
    
    public boolean isFeasible () throws IloException {
        return cplex.getStatus().equals(IloCplex.Status.Feasible) ;
    }
    
    public boolean isOptimal() throws IloException {
        return cplex.getStatus().equals(IloCplex.Status.Optimal) ;
    }
    public boolean isOptimalOrFeasible() throws IloException {
        return isOptimal()|| isFeasible();
    }
    public boolean isUnbounded() throws IloException {
        return cplex.getStatus().equals(IloCplex.Status.Unbounded) ;
    }
    
    public boolean isInError() throws IloException {
        return cplex.getStatus().equals(IloCplex.Status.Error) ;
    }
  
    //solved to completion?
    public boolean isSolvedToCompletion () throws IloException {
        return cplex.getStatus().equals(IloCplex.Status.Error) || cplex.getStatus().equals(IloCplex.Status.Optimal)
                ||cplex.getStatus().equals(IloCplex.Status.InfeasibleOrUnbounded) || isAborted();
    }
    
    public double getObjectiveValue() throws IloException {
        double inferiorObjective = Parameters.isMaximization?  Constants.MINUS_INFINITY:Constants.PLUS_INFINITY;
        return isFeasible() || isOptimal() ? cplex.getObjValue():inferiorObjective;
    }
    
    public int getNumEasyLeafNodes () {
        return numEasyLeafNodes;
    }
    
    public int getNumHardLeafNodes () {
        return numHardLeafNodes;
    }
    
    /**
     * Solve this subtree for some time, and return any farmed out nodes.
     * Update the number of leafs in this subtree after solving
     * @throws IloException 
     * @throws IOException 
     */
    public List<NodeAttachment> solve ( double timeSlice, boolean doFarming, double bestKnownOptimum  ) throws IloException, IOException{
                
        logger.debug(Messages.ActiveSubtreeSolve_MSG + this.myGuid + Constants.BLANKSPACE +timeSlice);
        
        //solve for some time
        //note that we supply the log file and the active subtree GUID for logging purposes
        solver.solve(timeSlice, doFarming,   bestKnownOptimum, numHardLeafNodes, logfile , this.myGuid);
        
        if (solver.isAborted() ) abortFlag = true;
     
        numEasyLeafNodes += solver.getNumEasyNodesAdded()- solver.getNumEasyNodesPruned();
        numHardLeafNodes += solver.getNumHardNodesAdded()- solver.getNumHardNodesPruned();
        
        logger.debug(this.myGuid+ Messages.ActiveSubtreeSolveComplete_MSG + timeSlice );
        return solver.getFarmedOutNodes();
    }
  
    public boolean isAborted () {
        return abortFlag;
    }
}
