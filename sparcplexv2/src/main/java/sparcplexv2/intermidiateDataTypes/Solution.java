package sparcplexv2.intermidiateDataTypes;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;

import sparcplexv2.constantsAndParams.Constants;
import sparcplexv2.constantsAndParams.Parameters;
 
 
//objective value of any subtree, its corresponding variables, whether it is feasible or not etc.

public class Solution implements Serializable {
    
    /**
     * 
     */
    private static final long serialVersionUID = 7879596549786859622L;

    /**
     * 
     */
    

    private double objectiveValue;    
    
    private boolean isFeasible; 
    private boolean isOptimal;
    private boolean isUnbounded; 
    private boolean isError; 
    
    //a map of variable names, and their values
    private  Map< String, Double> variableMap ;

    public Solution( ) {
        //default solution  will be inferior to 
        //any other feasible or optimal solution
        isFeasible = false;
        isOptimal=false;
        isError=false;
        isUnbounded=false;              
        variableMap =  new Hashtable<String, Double>();
       
        objectiveValue = Parameters.isMaximization? Constants.MINUS_INFINITY: Constants.PLUS_INFINITY;
    }
    
    //other methods, get set etc.
    
    //set value for variable
    public void setVariableValue(String name, double val){
        variableMap.put(name, val);
    }
    
    public double getVariableValue(String name){
        return variableMap.get(name);
    }
    
    public Map< String, Double> getVariableMap(){
        return variableMap;
    }
    
    public boolean isError(){
        return this.isError;
    }
    
    public void setError(boolean err){
        isError=err;
    }
    
    public boolean isOptimal(){
        return isOptimal;
    }
    
    public void setOptimal(boolean opt){
        this.isOptimal=opt;
    }
    
    public boolean isFeasible(){
        return isFeasible;
    }
    
    public void setFeasible(boolean feasible){
        isFeasible=feasible;
    }
        
    public boolean isUnbounded(){
        return this.isUnbounded;
    }
    
    public void setUnbounded(boolean unbounded){
        this.isUnbounded=unbounded;
    }
    public void setOptimumValue(double optimumValue){
        this.objectiveValue= optimumValue;
    }
    
    public double getObjectiveValue(){
        return objectiveValue;
    }

}
