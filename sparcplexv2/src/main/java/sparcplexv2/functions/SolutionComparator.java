package sparcplexv2.functions;

import java.io.Serializable;
import java.util.*;
 
import sparcplexv2.constantsAndParams.Constants;
import sparcplexv2.constantsAndParams.Parameters;
import sparcplexv2.intermidiateDataTypes.Solution;

public class SolutionComparator implements Comparator<  Solution > , Serializable {
 
    /**
     * 
     */
    private static final long serialVersionUID = 6312143274435503803L;

    public int compare(Solution soln1, Solution soln2) {

        int retval = Constants.ZERO;
        
        if (soln2.isFeasible() || soln2.isOptimal())    {
            if (soln1.isFeasible()|| soln1.isOptimal()) {
                if (Parameters.isMaximization &&  soln1.getObjectiveValue() < soln2.getObjectiveValue()  ) retval = Constants.ONE;
                if (!Parameters.isMaximization &&  soln1.getObjectiveValue() > soln2.getObjectiveValue()  ) retval = Constants.ONE;
            }else{
                retval=Constants.ONE;
            }             
        } 
        
        return retval;
        
    }
  
}
