package sparcplexv2.cplex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
 
 








import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import sparcplexv2.constantsAndParams.Constants;
import sparcplexv2.constantsAndParams.Messages;
import sparcplexv2.constantsAndParams.Parameters;
import sparcplexv2.intermidiateDataTypes.ActiveSubTree;
import sparcplexv2.intermidiateDataTypes.NodeAttachment;
import ilog.concert.IloException;
import ilog.concert.IloModel;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BranchDirection;

/**
 * 
 * @author SRINI
 * 
 * Handles tree branching.
 * 
 *  When a solution tree branches, farm out the new nodes.
 *  Or do no farming, simply keep track of the new leaf nodes added to the solution tree.
 *
 */

/*
 
 to important do notes from meeting:
 
 1) use sav file , not lp; read lp on master and distribute ; keep in memory so you dont have to go back to disk
 2) lp relax time should be low for migrated nodes, farm shallow node so less info is lost
 3) also look getlb(), getub() for non branching vars,  for int vars only. Use the bounds along with branching variable bounds.
 4) discard entire tree before branch handler, if the tree is inferior

 */


public class BranchHandler  extends IloCplex.BranchCallback{

    private   Logger logger ;
    private String activeSubtreeGuid;
    private boolean isLoggingInitialized = false;
    
    //root of the subtree which we are monitoring
    private NodeAttachment subTreeRoot;
    
    //whether we should farm, or just monitor the branching
    private   boolean farmingInstruction;
    //depending on how the tree grows, the handler may decide to branch or not , ignoring the farming Instruction
    private boolean farmingDecision;
    
    //list of leaf nodes farmed out of the tree
    private  List<NodeAttachment> farmedOutNodeList;
    
    //keep track of new nodes created in the subtree
    private int numEasyNodesCreated ;
    private int numHardNodesCreated ;  
   
    //when farming, we also prune nodes in the tree
    private int numEasyNodesPruned ;
    private int numHardNodesPruned ;  
   
    //best known optimum is used to prune nodes
    private double bestKnownOptimum;
    
    //how many hard leafs does this tree have, at the outset?
    private int startingHardLeafnodeCount;
    
    private int depthOfSubTreeRoot ;
    
    //set this flag if there is no point in solving this subtree any longer
    private boolean abortFlag;
    
    public BranchHandler(       NodeAttachment subTreeRoot ){
        
        this.subTreeRoot = subTreeRoot;
        depthOfSubTreeRoot = subTreeRoot.getDepth();
        
        reset(subTreeRoot.isEasy()?Constants.ZERO: Constants.ONE);
    }
        
    public boolean isLoggingInitialized(){
        return isLoggingInitialized;
    }
   
    public void initLogging (   String logfile, String myGuid) throws IOException{
        if(! isLoggingInitialized){
            logger= Logger.getLogger(BranchHandler.class);
            logger.setLevel(Level.DEBUG);
            PatternLayout layout = new PatternLayout("%d{ISO8601} [%t] %-5p %c %x - %m%n"); 
            logger.addAppender(new RollingFileAppender(layout,logfile));
            activeSubtreeGuid=myGuid;
            isLoggingInitialized = true;
        }

    }
    
    //reset the branch handler, every time you start solving a partly solved tree for some more time
    public void reset(int startingHardLeafnodeCount     ){
        clearFarmedOutNodeList();
        numHardNodesCreated=Constants.ZERO;
        numEasyNodesCreated=Constants.ZERO;
        numEasyNodesPruned =Constants.ZERO;
        numHardNodesPruned =Constants.ZERO; 
                
        this.startingHardLeafnodeCount = startingHardLeafnodeCount;
        
        abortFlag = false;
        
    }
    
    public boolean isAborted () {
        return abortFlag;
    }
     
    public void setFarming (boolean doFarming) {
        this.farmingInstruction =doFarming;
    }
    
    public void setBestKnownOptimum (double bestKnownOptimum) {
        this.bestKnownOptimum= bestKnownOptimum;
    }
    
    public void setStartingHardLeafnodeCount (int count) {
        startingHardLeafnodeCount = count;
    }
    
    public  void clearFarmedOutNodeList() {
        farmedOutNodeList = new ArrayList<NodeAttachment>();
    }
    
    public  List<NodeAttachment>  getFarmedOutNodes() {
        return farmedOutNodeList;
    }
    
    public int getNumEasyNodesPruned(){
        return numEasyNodesPruned;
    }
    
    public int getNumHardNodesPruned(){
        return numHardNodesPruned;
    }
    
    public int getNumEasyNodesCreated(){
        return numEasyNodesCreated;
    }
    
    public int getNumHardNodesCreated(){
        return numHardNodesCreated;
    }
    
    
    protected void main() throws IloException {
        
        //is branching really required for this node?
        boolean isBranchingRequired = true;
        
        if ( getNbranches()> 0 ){   
            
            //tree is branching
    
            //first check if entire tree can be discarded
            if (haltingCondition()    ){
                
                //no point solving this tree any longer 
                abortFlag = true;
                isBranchingRequired= false;
                abort();               
                

            } else {
                
                //check if this node can be discarded
                if (this.canNodeBeDiscarded()) {
                    
                    //prune this node
                    if (isSubtreeRoot()){
                        //no point solving this tree any longer 
                        abortFlag = true;
                        abort();
                        
                        logger.info(Messages.DiscardSubtree_Root_MSG + activeSubtreeGuid   );
                        
                    } else {
                        //only this node is useless
                        if (((NodeAttachment) getNodeData()).isEasy()) numEasyNodesPruned++; else numHardNodesPruned++;
                        prune();
                        logger.info(Messages.DiscardNode_MSG + activeSubtreeGuid   );
                         
                    }
                    
                    isBranchingRequired = false;
                    
                } 
            }
            
            if (isBranchingRequired) {
                
                //get the branches about to be created
                IloNumVar[][] vars = new IloNumVar[Constants.TWO][] ;
                double[ ][] bounds = new double[Constants.TWO ][];
                BranchDirection[ ][]  dirs = new  BranchDirection[ Constants.TWO][];
                getBranches(  vars, bounds, dirs);
                
                if (  getNodeData()==null){
                    //it will be null for the root of every sub problem, i.e. the first time we branch
                    //
                    //So set it .
                    
                    setNodeData(subTreeRoot);
                }; 
                
                //get the node attachment for this node, child nodes will accumulate the branching conditions
                NodeAttachment parentNodeData =    (NodeAttachment) getNodeData();
                
                //make a dynamic update to the farming decision, before letting the children spawn
                makeFarmingDecison(parentNodeData);
                logger.info(Messages.Farm_MSG + activeSubtreeGuid   );
                
                
                //now get both kids 
                for (int childNum = 0 ;childNum<getNbranches();  childNum++) {                      
                   
                    //apply the bound changes specific to this child
                    NodeAttachment thisChild  = UtilityLibrary.createChildNode( parentNodeData,
                            dirs[childNum], bounds[childNum], vars[childNum]  , isChildEasy() );   
                    
                    /**
                     * Instead of farming out node attachment, try to farm out the model.
                     * Not sure if this model corresponds to the branching node or the subtree root 
                     * ( accordingly may need to apply variable bounds for this node)
                     * 
                     * The model is serializable, so may be able to use it directly with Spark. 
                     * If not, then have to extract objective, variables, and constraints, and send primitive data types over the network.
                     * This may or may not be better than reading the MPS file from disk, as we do now.
                     * 
                     */
                    
                    IloModel model = getModel();
                    
                    //not sure how to get basis, although lpex6.java  sets basis.
                                  
                    if (farmingDecision) {
                        //we will collect the children and prune the parent
                        // this will effect the number of easy and hard nodes in the subtree
                        farmedOutNodeList.add(thisChild);       
                        
                        /**
                         * Instead of farming the children, its a much better idea to farm the parent. ( as in PARALEX).
                         * The parent's model and basis can be farmed out. ( if we farm a  child we have neither the model nor the basis).
                         * 
                         * Also see AdMIPex6.java . If we cannot get the basis, we can try a MIP start. In this case,
                         * we need to farm out not just the node attachment , but also the variable values.
                         * 
                         * 
                         * If the farmed parent node is redistributed by the driver, then in the next iteration we can prune this parent from it original tree.
                         * Otherwise just solve it again, and this time let its children spawn.
                         * 
                         */
                        
                        //see lpex2.java, lpex6.java, and cplexserver.java for using basis and model 
                       
                     
                        
                    } else {
                        //   no farming     , just update counts and let the kids be created                   
                        if (isChildEasy()) this.numEasyNodesCreated ++; else this.numHardNodesCreated++;
                        
                        //   create the  kid,  and attach node data  to the kid
                        makeBranch(childNum,thisChild );
                    }

                    
                    
                } //end , for loop , get both kids
                
                if (farmingDecision) {
                    //prune this node, we have collected its children
                    
                    if (parentNodeData.isEasy()) numEasyNodesPruned++; else numHardNodesPruned++;
                    prune();
                    
                    /**
                     * Note: it may be a good idea to abort instead of pruning. 
                     * Make a note inside this node data that its kids were farmed out 
                     * ( we may not even need the note since restart of the tree always starts at this node).
                     * If the driver decided to move the kids from this node, then in the next iteration, we can prune this node.
                     * 
                     * Also, generation cycle time should be much less than solution cycle time.
                     * Every subtree can be instructed to branch once and abort.
                     * If enough nodes have been farmed out , we can enter solution phase, or else we can farm for another iteration.
                     * 
                     * May also be a good idea to farm very large trees ( since we anyway only allow a max tree size) , or 
                     * very small trees, so that not much information is lost. Need to keep track of depth of node from subtree root.
                     * 
                     */
                    
                    
                }   
                
            }//if else is branching required condition
  
        } //if get N branches
         
    } //end main

    //the branch handler can dynamically make the farming decision    
    private void makeFarmingDecison ( NodeAttachment parentNodeData) throws IloException{
        
        //default
        farmingDecision= farmingInstruction ; 
        
        if (isSubtreeRoot() || parentNodeData.isEasy()) {
            //do not farm the root node of any subtree
            //only hard nodes are potentially pruned and their children farmed
            farmingDecision = false;             
        }  else  if ( this.startingHardLeafnodeCount + numHardNodesCreated - numHardNodesPruned > Parameters.THRESHOLD_MAX_HARD_LEAFS_PER_SUBTREE) {
            //farm if the tree has grown too big
            farmingDecision = true; 
            logger.info(Messages.Farm_TREE_toobig_MSG + activeSubtreeGuid   );
        }
               
    }

    private boolean isChildEasy(){
        //fill up later
        return false;
    }
    
    private boolean canNodeBeDiscarded () throws IloException {
        boolean result = false;

        //get LP relax value
        double nodeObjValue = getObjValue();
        
        result = Parameters.isMaximization  ? 
                    (nodeObjValue <= getCutoff()) || (nodeObjValue <= bestKnownOptimum )  : 
                    (nodeObjValue >= getCutoff()) || (nodeObjValue >= bestKnownOptimum );

        return result;
    }
    
    private boolean haltingCondition(  ) throws IloException{        
         
        double metric =  getBestObjValue() -bestKnownOptimum ;
        metric = metric /(Constants.EPSILON +bestKnownOptimum);
        
        //|bestnode-bestinteger|/(1e-10+|bestinteger|) 
        boolean mipHaltCondition =  Parameters.RELATIVE_MIP_GAP >= Math.abs(metric)  ;
        
        //also halt if we cannot do better than existing solution
        boolean inferiorityHaltCondition = Parameters.isMaximization && (bestKnownOptimum>=getBestObjValue());
        inferiorityHaltCondition = inferiorityHaltCondition || ( ! Parameters.isMaximization && (bestKnownOptimum<=getBestObjValue())  );
        
        if (mipHaltCondition) logger.info(Messages.MIPHAlt_Subtree_MSG + activeSubtreeGuid   );
        if (inferiorityHaltCondition) logger.info(Messages.InferiorHAlt_Subtree_MSG + activeSubtreeGuid   );
        
        return  mipHaltCondition || inferiorityHaltCondition ;       
      
    }

    private boolean isSubtreeRoot () throws IloException {
        
        boolean isRoot = true;
        
        if (getNodeData()!=null  ) {
            NodeAttachment thisNodeData =(NodeAttachment) getNodeData();
            if (thisNodeData.getDepth()>depthOfSubTreeRoot) {
                
                isRoot = false;
                
            }
        }    
        
        return isRoot;
        
    }
}
