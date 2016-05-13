package sparcplexv2.functions;

import java.util.List;

import org.apache.spark.api.java.function.Function;

import sparcplexv2.intermidiateDataTypes.NodeAttachment;
import sparcplexv2.intermidiateDataTypes.SolverResult;

public class NewNodeFetcher implements   Function<SolverResult,  List<NodeAttachment>>{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public List<NodeAttachment> call(SolverResult solverResult) throws Exception {
         
        return solverResult.getNodeList();
    }

}
