package sparcplexv2.cplex;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BranchDirection;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.util.Map;
import java.util.Map.Entry;

import sparcplexv2.constantsAndParams.Constants;
import sparcplexv2.intermidiateDataTypes.NodeAttachment;
import sparcplexv2.intermidiateDataTypes.Solution;

/**
 * 
 * @author SRINI
 *
 * contains a collection of CPLEX utility functions
 */
public class UtilityLibrary {
    
     
    /**
     * 
     * when a parent node branches with some variable bounds, create the child node attachment by incorporating those bounds
     * 
     * 
     * @param directionArray
     * @param boundArray
     * @param varArray
     * @param easy
     * @return
     */
    public static NodeAttachment createChildNode (NodeAttachment parentNode, BranchDirection[ ] directionArray, 
            double[ ] boundArray, IloNumVar[] varArray , boolean easy) {

        //depth of child is 1 more than parent
        NodeAttachment child = new NodeAttachment(  );
        child.setDepth(    Constants.ONE+parentNode.getDepth()) ;
        if (easy ) child.setEasy();

        //copy parents bounds
        for (Entry <String, Double> entry : parentNode.getUpperBounds().entrySet()){
            mergeBound(child,entry.getKey(), entry.getValue(), true);
        }
        for (Entry <String, Double> entry : parentNode.getLowerBounds().entrySet()){
            mergeBound(child,entry.getKey(), entry.getValue(), false);
        }

        //now apply the new bounds to the existing bounds
        for (int index = 0 ; index < varArray.length; index ++) {                           
            mergeBound(child, varArray[index].getName(), boundArray[index] , 
                    directionArray[index].equals(BranchDirection.Down));
        }

        return child;
    }

    
    /**
     * 
     * To the CPLEX object ,  apply all the bounds mentioned in attachment
     */
    public static void  merge ( IloCplex cplex, NodeAttachment attachment   ) throws IloException {

        IloLPMatrix lpMatrix = (IloLPMatrix) cplex .LPMatrixIterator().next();

        //WARNING : we assume that every variable appears in at least 1 constraint or variable bound
        IloNumVar[] variables = lpMatrix.getNumVars();

        for (int index = 0 ; index <variables.length; index ++ ){

            IloNumVar thisVar = variables[index];
            updateVariableBounds(thisVar,attachment.getLowerBounds(), false );
            updateVariableBounds(thisVar,attachment.getUpperBounds(), true );

        }       
    }
    
   /**
    * merge this variable bound into existing bounds 
    * 
    * 
    * @param node
    * @param varName
    * @param value
    * @param isUpperBound
    * @return
    */
    public static boolean mergeBound(NodeAttachment node, String varName, double value, boolean isUpperBound) {
        boolean isMerged = false;

        if (isUpperBound){
            Map< String, Double >  upperBounds = node.getUpperBounds() ;
            if (upperBounds.containsKey(varName)) {
                if (value < upperBounds.get(varName)){
                    //update the more restrictive upper bound
                    upperBounds.put(varName, value);
                    isMerged = true;
                }
            }else {
                //add the bound
                upperBounds.put(varName, value);
                isMerged = true;
            }
        } else {
            //it is a lower bound
            Map< String, Double >  lowerBounds = node.getLowerBounds() ;
            if (lowerBounds.containsKey(varName)) {
                if (value > lowerBounds.get(varName)){
                    //update the more restrictive lower bound
                    lowerBounds.put(varName, value);
                    isMerged = true;
                }               
            }else {
                //add the bound
                lowerBounds.put(varName, value);
                isMerged = true;
            }
        }

        return isMerged;
    }

    /**
     * read the CPLEX object and update solution object with variables and their values
     * 
     * Assumes that the CPLEX object is solved to optimality or feasibility
     * 
     * @param cplex
     * @param soln
     * @throws IloException
     */
    public static void addVariablevaluesToSolution    (IloCplex cplex, Solution soln) throws  IloException {
        
        IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();

        //WARNING: we assume that every variable appears in at least 1 constraint or variable bound
        //Otherwise, this method of getting all the variables from the matrix may not yield all the
        //variables
        IloNumVar[] variables = lpMatrix.getNumVars();
        double[] variableValues = cplex.getValues(variables);                 

        for ( int index = 0; index < variableValues.length; index ++){

            String varName = variables[index].getName();
            double varValue = variableValues[index];
            soln.setVariableValue (varName,  varValue);

        }
    }
    
    /**
     * 
     *  Update variable bounds as specified
     * 
     * @param var
     * @param bounds
     * @param isUpperBound
     * @throws IloException
     */
    public static void updateVariableBounds(IloNumVar var, Map< String, Double > bounds, boolean isUpperBound   ) 
            throws IloException{

        String varName = var.getName();
        boolean isPresentInNewBounds = bounds.containsKey(varName);

        if (isPresentInNewBounds) {
            double newBound =   bounds.get(varName)  ;
            if (isUpperBound){
                if ( var.getUB() > newBound ){
                    //update the more restrictive upper bound
                    var.setUB( newBound );
                }
            }else{
                if ( var.getLB() < newBound){
                    //update the more restrictive lower bound
                    var.setLB(newBound);
                }
            }               
        }

    }    
 
}
