package sparcplexv2.functions;

import org.apache.spark.api.java.function.Function;

import sparcplexv2.intermidiateDataTypes.Solution;
import sparcplexv2.intermidiateDataTypes.SolverResult;

public class SolutionFetcher implements Function<SolverResult, Solution>{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public Solution call(SolverResult solverResult) throws Exception {
        
        return solverResult.getSolution();
    }

}
