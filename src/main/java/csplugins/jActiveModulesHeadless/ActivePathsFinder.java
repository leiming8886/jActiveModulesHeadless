package csplugins.jActiveModulesHeadless;
//------------------------------------------------------------------------------

import csplugins.jActiveModulesHeadless.networkUtils.*;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.*;

import javax.swing.JFrame;


import csplugins.jActiveModulesHeadless.data.ActivePathFinderParameters;
import csplugins.jActiveModulesHeadless.util.MyMonitorThread;


/**
 * This class contains the main logic for finding Active Paths The important
 * function is findActivePaths() which calls the simulated annealing subroutine
 * to find the active paths
 */
public class ActivePathsFinder {

	
	/**
	 * See constructor
	 */
	private int attrNamesLength;
	/**
	 * See constructor
	 */
	private Network network;
	/**
	 * parameters for path finding
	 */
	private ActivePathFinderParameters apfParams;
	/**
	 * an array containing all of the nodes initially in the graph
	 */
	private Node[] nodes;
	/**
	 * This is a hashmap which maps from nodes to an array of edges (Edge []).
	 * This is used to determine which edges belonged to which nodes before any
	 * changes were made to the graph. This hash map is then used to recover
	 * edges when reinserting nodes into a graph it is initialized in
	 * setupScoring() and used in toggleNode()
	 */
	// HashMap node2edges;
	// Global Variables for the Greedy Search
	/**
	 * Maps from a node to the best component found for that node
	 */
	HashMap node2BestComponent;
	/**
	 * The neighborhood for the current best component
	 */
	// HashSet bestNeighborhood;
	HashMap expressionMap;
	protected static int DISPLAY_STEP = 50;
	
	private Component[] activePaths = null;
	 Vector<Component> activePathsVect;

	/**
	 * This is the only constructor for ActivePathsFinder. In order to find the
	 * paths, we need certain information.
	 * 
	 * @param attrNames
	 *            The names of hte attributes which correspond to significance
	 * @param Network
	 *            The Network which contains our graph, should divorce this
	 *            from the window before running
	 * @param apfp
	 *            The object specifying the parameters for this run
	 * @param parentFrame
	 *            The JFrame which is our parent window, if this is null, then
	 *            we won't display any progress information
	 */
	public ActivePathsFinder(HashMap expressionMap, int attrNamesLength,
			Network Network, ActivePathFinderParameters apfp,
			 Vector<Component> activePathsVect) {
		this.expressionMap = expressionMap;
		this.attrNamesLength = attrNamesLength;
		this.network = Network;
		apfParams = apfp;
		this.activePathsVect = activePathsVect;
	}

	/**
	 * This function will determine a score for the nodes currently selected in
	 * the graph. It does not try to determine components from these selected
	 * nodes, merely assuming they are all in the same connected component.
	 * 
	 * @param nodeList
	 *            the node list
	 * @return The score for the selected nodes
	 */
	public double scoreList(List nodeList) {
		setupScoring();
		
		Component selected = new Component(nodeList);
		return selected.getScore();
	}

	/**
	 * In order to score the components, we need to set up certain data
	 * structures first. Mainly this includes seting up the z scores table and
	 * the monte carlo correction. Chris sez it shouldn't be called a monte
	 * carlo correction, and I tend to agree, but this has some historical
	 * inertia behind it (ie, it would involve changing maybe 6 lines of code,
	 * which is simply unthinkable) These data structures are initialized as
	 * static data structures in the Component class, where the scoring is
	 * actually done
	 */
	private void setupScoring() {
		Network perspective = network;
		// Here we initialize the z table. We use this data structure when we
		// want to get an adjusted z score
		// based on how many conditions we are looking at.
		System.out.println("Initializing Z Table");
		Component.zStats = new ZStatistics(attrNamesLength);
		System.out.println("Done initializing Z Table");

		nodes = new Node[1];
		nodes = (Node[]) (perspective.getNodeList().toArray(nodes));

		// Component.node2edges = node2edges;
		Component.graph = perspective;
		// Component needs the condition names to return which conditions
		// yield significant scores
		Component.attrNamesLength = attrNamesLength;
		// Determine whether or not we want to correct for the size
		// of active paths
		Component.monteCorrection = apfParams.getMCboolean();
		Component.regionScoring = apfParams.getRegionalBoolean();
		Component.exHash = expressionMap;
		// Initialize the param statistics object. The pStats object uses
		// randomized methods ot determine the
		// mean and standard deviation for networks of size 1 through n.
		Component.pStats = new ParamStatistics(new Random(apfParams
				.getRandomSeed()), Component.zStats);
		// The statistics object is required fro the component scoring function
		// We want to use a monte carlo correction
		if (apfParams.getMCboolean()) {
			boolean failed = false;
			// and we want to load the state from a file
			if (apfParams.getToUseMCFile()) {
				// read in the monte carlo file, it is stored as a serialized
				// ParamStatistics object
				System.out.println("Trying to read monte carlo file");
				try {
					FileInputStream fis = new FileInputStream(apfParams
							.getMcFileName());
					ObjectInputStream ois = new ObjectInputStream(fis);
					Component.pStats = (ParamStatistics) ois.readObject();
					ois.close();
					if (Component.pStats.getNodeNumber() != nodes.length) {
						// whoops, the file we loaded doesn't look like it
						// contains the correct information for the set
						// of nodes we are dealing with, user specified a bad
						// file, I hope he feels shame
						System.out.println("Monte Carlo file calculated for incorrect number of nodes. Using correct file?");
						failed = true;
						throw new Exception("wrong number of nodes");
					}
				} catch (Exception e) {
					System.out.println("Loading monte carlo file failed" + e);
					failed = true;
				}
			}

			if (failed || !apfParams.getToUseMCFile()) {
				System.out.println("Initializing monte carlo state");
				
				// Component.pStats.calculateMeanAndStd(nodes,ParamStatistics.DEFAULT_ITERATIONS,apfParams.getMaxThreads(),
				// new MyProgressMonitor(cytoscapeWindow, "Sampling Mean and
				// Standard
				// Deviation","",0,ParamStatistics.DEFAULT_ITERATIONS));
				Component.pStats.calculateMeanAndStd(nodes,
						ParamStatistics.DEFAULT_ITERATIONS, apfParams
								.getMaxThreads());
				System.out.println("Finished initializing monte carlo state");

				System.out.println("Trying to save monte carlo state");
				try {
					FileOutputStream fos = new FileOutputStream("last.mc");
					ObjectOutputStream oos = new ObjectOutputStream(fos);
					oos.writeObject(Component.pStats);
					oos.close();
					System.out.println("Saved monte carlo state to last.mc");
				} catch (Exception e) {
					System.out.println("Failed to save monte carlo state " + e);
				}
			}
		}

	}
	
	public void run() {

		activePaths = findActivePaths();

		for (int i=0; i< activePaths.length; i++){
			activePathsVect.add(activePaths[i]);			
		}		
	}

	/**
	 * This is the method called to determine the activePaths. Its operation
	 * depends on the parameters specified in hte activePathsFinderParameters
	 * object passed into the constructor.
	 */
	private Component[] findActivePaths() {
		setupScoring();

		Vector comps;
		if(apfParams.getGreedySearch()){
		//if (apfParams.getSearchDepth() > 0) {
			// this will read the parameters out of apfParams and
			// store the result into bestComponnet			
			System.out.println("Starting greedy search");

			runGreedySearch();


			System.out.println("Greedy search finished");

			// after the call to run greedy search, each node is associated
			// with the best scoring component to which it belongs. Need to
			// take the values from this hashmap and put them into a vector
			// so that there are no duplicates.
			comps = new Vector(new HashSet(node2BestComponent.values()));

		} else {			
			System.out.println("Starting simulated annealing");

			
			Vector resultPaths = new Vector();

			
			SimulatedAnnealingSearchThread thread = new SimulatedAnnealingSearchThread(network,
					resultPaths, nodes, apfParams);

			
						
			thread.run();
			

			System.out.println("Finished simulated annealing run");
			if (apfParams.getToQuench()) {

				
				System.out.println("Starting quenching run");
				SortedVector oldPaths = new SortedVector(resultPaths);
				resultPaths = new Vector();
				QuenchingSearchThread thread_2 = new QuenchingSearchThread(network,
						resultPaths, nodes, apfParams,
						oldPaths);

				
				thread_2.run();


				System.out.println("Quenching run finished");

			}
			comps = new Vector(resultPaths);
			// restoreNodes();
		}

		
		Collections.sort(comps);


		comps = filterResults(comps);
		
		Component [] temp = new Component[0];
		int size = Math.min(comps.size(), apfParams.getNumberOfPaths());
		temp = (Component[]) comps.subList(0, size).toArray(temp);
		
		return temp;
	}
	
	protected Vector filterResults(Vector unfiltered){
		Vector result = new Vector();
		UNFILTERED_LOOP:
		for(Iterator unfilteredIt = unfiltered.iterator();unfilteredIt.hasNext();){
			Component component = (Component)unfilteredIt.next();
			//if(component.getNodes().size() == 0)
			//	break;
			component.finalizeDisplay();
			for(Iterator resultIt = result.iterator();resultIt.hasNext();){
				Component prevComponent  = (Component)resultIt.next();
				if(overlap(component,prevComponent) > apfParams.getOverlapThreshold()){
				    continue UNFILTERED_LOOP;
				}				
			}
			result.add(component);
			if(result.size() >= apfParams.getNumberOfPaths()){
			    break;
			}
		}
		return result;
	}

	private double overlap(Component component, Component prevComponent) {
	    HashSet nodeSet = new HashSet(prevComponent.getDisplayNodes());
	    int intersection = 0;
	    for(Iterator nodeIt = component.getDisplayNodes().iterator();nodeIt.hasNext();){
		if(nodeSet.contains(nodeIt.next())){
		    intersection++;
		}
	    }
	    return intersection/(double)(component.getDisplayNodes().size());
	}

	/**
	 * Runs the greedy search algorithm. This function will run a greedy search
	 * iteratively using each node of the graph as a starting point
	 */
	private void runGreedySearch() {
		
		runGreedySearch(network.getNodeList());
		
	}

	private void runGreedySearch(Collection seedList) {
		// initialize global best score
		Node nodeTemp;
		node2BestComponent = new HashMap(seedList.size());
		
		Iterator nodeIterator = seedList.iterator();
		int i =0;
		final Runtime runtime = Runtime.getRuntime();
		long freeMem = runtime.freeMemory();
		long totalMem = runtime.totalMemory();
        long usedMem = totalMem - freeMem;
		long maxMem = runtime.maxMemory();
		double usedMemFraction = usedMem / (double) maxMem;
		
		int number_threads = apfParams.getMaxThreads();
		if(number_threads < (Runtime.getRuntime().availableProcessors()-1))
			number_threads = Runtime.getRuntime().availableProcessors()-1;
		
		ThreadPoolExecutor executorPool = new ThreadPoolExecutor(number_threads,number_threads,7, TimeUnit.DAYS,new ArrayBlockingQueue<Runnable>(seedList.size()));
		
		//start the monitoring thread
        MyMonitorThread monitor = new MyMonitorThread(executorPool, 5);

        Thread monitorThread = new Thread(monitor);
        monitorThread.start();
                
		while(nodeIterator.hasNext()){
			Component component = new Component();
			nodeTemp = (Node) nodeIterator.next();
			node2BestComponent.put(nodeTemp, component);
			GreedySearchThread gst = new GreedySearchThread(network,
					apfParams,nodeTemp,
					component, nodes);
			
			executorPool.execute(gst);
		}
		
	
		//executor.shutdown();
		executorPool.shutdown();
		// Wait until all threads are finish
		try {
			executorPool.awaitTermination(7, TimeUnit.DAYS);
         	//executor.awaitTermination(7, TimeUnit.DAYS);
        } catch (Exception e) {}
		monitor.shutdown();
		
	}
}
