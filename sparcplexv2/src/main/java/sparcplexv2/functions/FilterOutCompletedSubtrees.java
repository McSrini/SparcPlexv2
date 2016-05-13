package sparcplexv2.functions;

import org.apache.spark.api.java.function.Function;

import scala.Tuple2;
import sparcplexv2.intermidiateDataTypes.ActiveSubTree;

public class FilterOutCompletedSubtrees implements Function<Tuple2<Integer, ActiveSubTree>, Boolean>{

    public Boolean call(Tuple2<Integer, ActiveSubTree> tuple) throws Exception {
         
        return ! tuple._2.isSolvedToCompletion();
    }

}
