package sparcplexv2.constantsAndParams;

import java.io.Serializable;

public class Parameters implements Serializable {
    
    
    //move to configuration file
    
    /**
     * 
     */
    private static final long serialVersionUID = 8174972951213475878L;
    public static final String DRIVER_LOG_FILE = "/tmp/sparcplexDriver.log";
    public static final String WORKER_LOG_FILE = "/tmp/sparcplexWorker";
    public static final String DOT_LOG  = ".log";
    
    public  static final  int CORES_PER_MACHINE = 4;
    public  static final  int NUM_MACHINES= 10;
    public  static final  int NUM_CORES = CORES_PER_MACHINE * NUM_MACHINES ;
    
    public  static final  double EASY_NODE_TIME_SLICE_SECONDS =    Constants.SIX; 
    public  static final  double HARD_NODE_TIME_SLICE_SECONDS =    EASY_NODE_TIME_SLICE_SECONDS* Constants.TEN ;
      
    public  static final  double   ITERATION_TIME_MAX_SECONDS =    HARD_NODE_TIME_SLICE_SECONDS*Constants.TEN ; 
    public  static final  double ITERATION_TIME_MIN_SECONDS =   ITERATION_TIME_MAX_SECONDS/Constants.TWO ; 
        
    public  static final  int MAX_ITERATIONS =     100;
    
    //if a partition does not have at least this many leaf nodes, then its time to farm out child nodes for distribution
    public  static final  int THRESHOLD_MIN_HARD_LEAFS_PER_CORE =     5; 
    
    //prevent trees from growing too large
    public  static final  int THRESHOLD_MAX_HARD_LEAFS_PER_SUBTREE =     32; 
    
    public  static final  boolean isMaximization = false;

    public static final boolean isDepthFirstSearch = false; // CPLEX needs this
    
    public static final double RELATIVE_MIP_GAP  = 0.05; 
    
    //implement this option - halt computation if we have got within 95% of LP relax of original problem
    public static final double PERCENT_WITHIN_LP_RELAX_OF_ORIGINAL  = 0.95; 
        
    public static final String LP_FILENAME = "/tmp/timtab1.mps";     
}
