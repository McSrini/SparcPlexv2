package sparcplexv2.functions;

import org.apache.spark.api.java.function.Function;

import sparcplexv2.intermidiateDataTypes.ActiveSubTree;
import sparcplexv2.intermidiateDataTypes.NodeAttachment;

public class AttachmentConverter implements Function <NodeAttachment, ActiveSubTree>{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public ActiveSubTree call(NodeAttachment node) throws Exception {
         
        return new ActiveSubTree(node) ;
    }

}
