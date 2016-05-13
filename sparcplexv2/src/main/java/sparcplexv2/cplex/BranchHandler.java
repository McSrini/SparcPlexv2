package sparcplexv2.cplex;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
 
 




import sparcplexv2.constantsAndParams.Constants;
import sparcplexv2.constantsAndParams.Parameters;
import sparcplexv2.intermidiateDataTypes.NodeAttachment;
import ilog.concert.IloException;
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
public class BranchHandler  extends IloCplex.BranchCallback{

    private static Logger logger=Logger.getLogger(BranchHandler.class);
    
    //root of the subtree which we are monitoring
    private NodeAttachment subTreeRoot;
    
    //whether we should farm, or just monitor the branching
    private   boolean farmingInstruction;
    //depending on how the tree grows, the handler may decide to branch or not , ignoring the farming Instruction
    private boolean farmingDecision;
    
    //list of leaf nodes farmed out of the tree
    private  List<NodeAttachment> farmedOutNodes;
    
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
    
    public BranchHandler(       NodeAttachment subTreeRoot ){
        
        this.subTreeRoot = subTreeRoot;
        depthOfSubTreeRoot = subTreeRoot.getDepth();
        
        reset(subTreeRoot.isEasy()?Constants.ZERO: Constants.ONE);
    }
    
    //reset the branch handler, every time you start solving a partly solved tree for some more time
    public void reset(int startingHardLeafnodeCount){
        clearFarmedOutNodes();
        numHardNodesCreated=Constants.ZERO;
        numEasyNodesCreated=Constants.ZERO;
        numEasyNodesPruned =Constants.ZERO;
        numHardNodesPruned =Constants.ZERO; 
                
        this.startingHardLeafnodeCount = startingHardLeafnodeCount;
        
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
    
    public  void clearFarmedOutNodes() {
        farmedOutNodes = new ArrayList<NodeAttachment>();
    }
    
    public  List<NodeAttachment>  getFarmedOutNodes() {
        return farmedOutNodes;
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
        
        if ( getNbranches()> 0 ){   
            
            //tree is branching
    
            if (haltingCondition()    ){
                
                //prune this node , no point in solving it or its children
                //every remaining leaf node can be pruned as well
                //
                //Note that we take care not to prune the root of this subtree
                if (getNodeData()!=null  ) {
                    NodeAttachment thisNodeData =(NodeAttachment) getNodeData();
                    if (thisNodeData.getDepth()>depthOfSubTreeRoot) {
                        
                        if (thisNodeData.isEasy()) numEasyNodesPruned++; else numHardNodesPruned++;
                        prune();
                        
                    }
                }                

            } else {
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
                
                //now get both kids 
                for (int childNum = 0 ;childNum<getNbranches();  childNum++) {                      
                   
                    //apply the bound changes specific to this child
                    NodeAttachment thisChild  = UtilityLibrary.createChildNode( parentNodeData,
                            dirs[childNum], bounds[childNum], vars[childNum]  , isChildEasy() );   
                                  
                    if (farmingDecision) {
                        //we will collect the children and prune the parent
                        // this will effect the number of easy and hard nodes in the subtree
                        farmedOutNodes.add(thisChild);                        
                        
                    } else {
                        //   no farming     , just update counts and let the kids be created                   
                        if (isChildEasy()) this.numEasyNodesCreated ++; else this.numHardNodesCreated++;
                        
                        //   create the  kid,  and attach node data  to the kid
                        makeBranch(childNum,thisChild );
                    }

                    
                    
                } //end , get both kids
                
                if (farmingDecision) {
                    //prune this node, we have collected its children
                    
                    if (parentNodeData.isEasy()) numEasyNodesPruned++; else numHardNodesPruned++;
                    prune();
                }   
                
            }//if else halting condition
  
        } //if get N branches
         
    } //end main

    //the branch handler can dynamically make the farming decision    
    private void makeFarmingDecison ( NodeAttachment parentNodeData) throws IloException{
        
        //default
        farmingDecision= farmingInstruction ; 
                
        //farm if th etree has grown too big
        if ( this.startingHardLeafnodeCount + numHardNodesCreated - numHardNodesPruned > Parameters.THRESHOLD_MAX_HARD_LEAFS_PER_SUBTREE) 
            farmingDecision = true; 
        
        //only hard nodes are potentially pruned and their children farmed
        if (parentNodeData.isEasy())    farmingDecision = false;
        
        //if this tree has only 1 node left, could be the root, do not prune the root and farm its kids
        if (getNremainingNodes()==Constants.ONE)  farmingDecision = false;
        
       
    }

    private boolean isChildEasy(){
        //fill up later
        return false;
    }
    
    private boolean haltingCondition(  ) throws IloException{        
         
        double metric =  getBestObjValue() -bestKnownOptimum ;
        metric = metric /(Constants.EPSILON +bestKnownOptimum);
        
        //|bestnode-bestinteger|/(1e-10+|bestinteger|) 
        boolean mipHaltCondition =  Parameters.RELATIVE_MIP_GAP >= Math.abs(metric)  ;
        
        //also halt if we cannot do better than existing solution
        boolean inferiorityHaltCondition = Parameters.isMaximization && (bestKnownOptimum>=getBestObjValue());
        inferiorityHaltCondition = inferiorityHaltCondition || ( ! Parameters.isMaximization && (bestKnownOptimum<=getBestObjValue())  );
        
        return  mipHaltCondition || inferiorityHaltCondition ;       
      
    }

}
