package sparcplexv2.constantsAndParams;

import java.io.Serializable;

public class Parameters implements Serializable {
    
    
    //move to configuration file
    
    /**
     * 
     */
    private static final long serialVersionUID = 8174972951213475878L;
    public  static final  int CORES_PER_MACHINE = 4;
    public  static final  int NUM_MACHINES= 10;
    public  static final  int NUM_CORES = CORES_PER_MACHINE * NUM_MACHINES ;
    
    public  static final  double EASY_NODE_TIME_SLICE_SECONDS =    0.5*60; 
    public  static final  double HARD_NODE_TIME_SLICE_SECONDS =    3*60;
      
    public  static final  int ITERATION_TIME_MIN_SECONDS =    3*60; 
    public  static final  int ITERATION_TIME_MAX_SECONDS =    30*60; 
    
    public  static final  int MAX_ITERATIONS =     100;
    
    //if a partition does not have at least this many leaf nodes, then its time to farm out child nodes for distribution
    public  static final  int THRESHOLD_MIN_HARD_LEAFS_PER_CORE =     5; 
    
    //prevent trees from growing too large
    public  static final  int THRESHOLD_MAX_HARD_LEAFS_PER_SUBTREE =     32; 
    
    public  static final  boolean isMaximization = false;

    public static final boolean isDepthFirstSearch = false; // CPLEX needs this
    
    public static final double RELATIVE_MIP_GAP  = 0.05; 
    
    public static final String LP_FILENAME = "/tmp/timtab1.mps";     
}
