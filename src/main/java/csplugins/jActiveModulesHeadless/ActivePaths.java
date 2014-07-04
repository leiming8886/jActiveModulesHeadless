// ActivePaths.java:  a plugin for CytoscapeWindow,
// which uses VERA & SAM expression data
// to propose active gene regulatory paths
//------------------------------------------------------------------------------
// $Revision: 11526 $
// $Date: 2007-09-05 14:14:24 -0700 (Wed, 05 Sep 2007) $
// $Author: rmkelley $
//------------------------------------------------------------------------------
package csplugins.jActiveModulesHeadless;
//------------------------------------------------------------------------------

import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;



import csplugins.jActiveModulesHeadless.data.ActivePathFinderParameters;
//import csplugins.jActiveModules.util.Scaler;
import csplugins.jActiveModulesHeadless.util.ScalerFactory;

import csplugins.jActiveModulesHeadless.networkUtils.*;
import java.util.Collection;
//import java.io.File;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;


//-----------------------------------------------------------------------------------
public class ActivePaths  {


	protected boolean showTable = true;
	protected boolean hideOthers = true;
	protected boolean randomize = false;

	protected JMenuBar menubar;
	protected JMenu expressionConditionsMenu;
	protected String currentCondition = "none";
	protected Component[] activePaths;
	
	protected int attrNamesLength;
	protected static boolean activePathsFindingIsAvailable;
	protected Network network;
	protected String titleForCurrentSelection;
	protected ActivePathFinderParameters apfParams;
	protected static double MIN_SIG = 0.0000000000001;
	protected static double MAX_SIG = 1 - MIN_SIG;

	protected static int resultsCount = 1;
	
	private static int MAX_NETWORK_VIEWS =5; // = PropUtil.getInt(CytoscapeInit.getProperties(), "moduleNetworkViewCreationThreshold", 5);
	private static int runCount = 0;	
	
	// This is common prefix for all finders.
	private static final String MODULE_FINDER_PREFIX = "jActiveModules.";
	private static final String EDGE_SCORE = MODULE_FINDER_PREFIX + "overlapScore";
	
	private static final String NODE_SCORE = MODULE_FINDER_PREFIX + "activepathScore";
	
	
	private static boolean eventFired = false;
		
	// ----------------------------------------------------------------
	public ActivePaths(Network Network, ActivePathFinderParameters apfParams) {
		this.apfParams = apfParams;


		if (Network == null || Network.getNodeCount() == 0) {
			throw new IllegalArgumentException("Please select a network");
		}

		attrNamesLength = apfParams.getSizeExpressionAttributes();
		
		if (attrNamesLength == 0) {
			throw new RuntimeException("No expression data selected!");
		}
		this.network = Network;
		
		this.network.generateNeighborList();
		
	} // ctor

	// --------------------------------------------------------------
	protected void setShowTable(boolean showTable) {
		this.showTable = showTable;
	}
	protected void clearActivePaths() {
		this.activePaths = null;
	}

	public void run() {
		
		
		
	    System.gc();
		//long start = System.currentTimeMillis();
		HashMap expressionMap = generateExpressionMap();

		Vector<Component> activePathsVect= new Vector<Component>();

		// run the path finding algorithm
		final ActivePathsFinder apf =
			new ActivePathsFinder(expressionMap, attrNamesLength, network, apfParams, activePathsVect);
		
		apf.run();
		//ActivePathsTaskFactory factory = new ActivePathsTaskFactory(apf);
		//ServicesUtil.synchronousTaskManagerServiceRef.execute(factory.createTaskIterator());
				
		activePaths = new Component[activePathsVect.size()];
		for (int i=0; i< activePathsVect.size(); i++){
			activePaths[i] = activePathsVect.get(i);
		}				
		 
	}
	

	private static int getNumberOfSharedNodes(Network networkA, Network networkB){
		
		Long[] nodeIndicesA = new Long[networkA.getNodeCount()];
		Long[] nodeIndicesB = new Long[networkB.getNodeCount()];
		
		Iterator<Node> it = networkA.getNodeList().iterator();
		int iA=0;
		while (it.hasNext()){
			nodeIndicesA[iA] = it.next().getSUID();
			iA++;
		}
		
		Iterator<Node> it2 = networkB.getNodeList().iterator();
		int iB=0;
		while (it2.hasNext()){
			nodeIndicesB[iB] = it2.next().getSUID();
			iB++;
		}
		
		HashSet<Long> hashSet = new HashSet<Long>();
		for (int i=0; i< nodeIndicesA.length; i++){
			hashSet.add( new Long(nodeIndicesA[i]));
		}

		int sharedNodeCount =0;
		for (int i=0; i< nodeIndicesB.length; i++){
			if (hashSet.contains(new Long(nodeIndicesB[i]))){
				sharedNodeCount++;
			}
		}
		
		return sharedNodeCount;
	}
	

	private Set<Edge> getPathEdges(Network overview, Set path_nodes) {
		HashSet<Edge> edgeSet = new HashSet<Edge>();
		
		Object[] nodes = path_nodes.toArray();
		
		HashSet[] hashSet = new HashSet[nodes.length];
		for (int i=0; i< nodes.length; i++){
			hashSet[i] = new HashSet<Node>(((Node)nodes[i]).getNetworkPointer().getNodeList());
		}
		
		for (int i=0; i< nodes.length-1; i++){
			for (int j=i+1; j<nodes.length; j++){
				// determine if there are overlap between nested networks
				if (hasTwoSetOverlap(hashSet[i], hashSet[j])){
					//Edge edge = Cytoscape.getEdge((Node)nodes[i], (Node)nodes[j], Semantics.INTERACTION, "overlap", true);
					Edge newEdge = overview.addEdge((Node)nodes[i], (Node)nodes[j], false);
					edgeSet.add(newEdge);
				}
			}
		}
		
		return edgeSet;
	}
	
	
	private boolean hasTwoSetOverlap(HashSet<Node> set1, HashSet<Node> set2) {
		Iterator<Node> it = set1.iterator();
		while (it.hasNext()){
			if (set2.contains(it.next())){
				return true;
			}
		}		
		return false;
	}
	public Network[] createSubnetworks(Component[] components) {
		activePaths = components;
		
		return createSubnetworks();
	}
	
	public Network[] createSubnetworks() {
		//Network[] subnetworks = new Network[activePaths.length];

		Network[] subnetworks2 = new Network[activePaths.length];
		
		
		for (int i = 0; i < activePaths.length; i++) {
			Component thePath = activePaths[i];
			
			
			// get nodes for this path
			Vector nodeVect = (Vector) thePath.getDisplayNodes();
			Set<Node> nodeSet = new HashSet<Node>();
			for (int j = 0; j < nodeVect.size(); j++) {
				Node oneNode = (Node) nodeVect.elementAt(j);
				if (oneNode != null)
					nodeSet.add(oneNode);
			}
			
			// get edges for this path
			Set edgeSet = new HashSet();
			Iterator iterator = network.getEdgeList().iterator(); //.edgesIterator();
			System.out.println("num nodes: " + nodeVect.size());
			while (iterator.hasNext()) {
				Edge edge = (Edge) iterator.next();
				if (nodeSet.contains(edge.getSource()) && nodeSet.contains(edge.getTarget()))
					edgeSet.add(edge);
			}

			subnetworks2[i] =  new Network();
			if (nodeSet != null)
				for (Node n : nodeSet)
					subnetworks2[i].addNode(n);
			if (edgeSet != null)
				for (Object e : edgeSet)
					subnetworks2[i].addEdge((Edge)e);
			//subnetworks[i] = Cytoscape.createNetwork(nodeSet, edgeSet, pathName, Network, false);
			subnetworks2[i].setScore(thePath.getScore());
			String pathName = "Module_" + (i + 1) + "_" + new DecimalFormat("#.##").format(subnetworks2[i].getScore()) ;
			subnetworks2[i].setName(pathName);

		}
		
		return subnetworks2;
	}
	
	/**
	 * Returns the best scoring path from the last run. This is mostly used by
	 * the score distribution when calculating the distribution
	 */
	protected Component getHighScoringPath() {
		System.out.println("High Scoring Path: " + activePaths[0].toString());
		System.out.println("Score: " + activePaths[0].getScore());
		int size = activePaths[0].getNodes().size();
		System.out.println("Size: " + size);
		System.out.println("Raw score: " + activePaths[0].calculateSimpleScore());
		System.out.println("Mean: " + Component.pStats.getMean(size));
		System.out.println("Std: " + Component.pStats.getStd(size));
		return activePaths[0];
	}

	public HashMap generateExpressionMap() {
		// set up the HashMap which is used to map from nodes
		// to z values. At this point, we are mapping from the
		// p values for expression to z values
		//System.out.println("Processing Expression Data into Hash");
		HashMap tempHash = new HashMap();
		//System.out.println("Do some testing of the ExpressionData object");
		NodeTable nodeAttributes = this.network.getNodeTable();

		// Create two identical lists of genes
		List<Node> geneList = new ArrayList<Node>();
		List<Node> shuffledList = new ArrayList<Node>();
		for (Iterator nodeIt = network.getNodeList().iterator(); nodeIt.hasNext();) {
			Node n = (Node) nodeIt.next();
			geneList.add(n);
			shuffledList.add(n);
		}

		// If randomize, permute the second list of genes
		if ( randomize ) 
			Collections.shuffle(shuffledList);

		final Double[][] attribValues = new Double[attrNamesLength][geneList.size()];
		final Map<String, Integer> geneNameToIndexMap = new HashMap<String, Integer>();
		for (int i = 0; i < geneList.size(); i++) {
			final String geneName = geneList.get(i).getName();
			
			geneNameToIndexMap.put(geneName, new Integer(i));
			for (int j = 0; j < attrNamesLength; j++)
			{
				//attribValues[j][i] = nodeAttributes.get.getDoubleAttribute(geneName, attrNames[j]);
				Row row = nodeAttributes.getRow(geneList.get(i));
				
				attribValues[j][i] = row.getDataColumn(j);//.get(attrNames[j], Double.class);
			}
		}

		// Perform the scaling:
		for (int j = 0; j < attrNamesLength; j++) {
			//final int index = apfParams.getExpressionAttributes().indexOf(attrNames[j]);
			//final ScalingMethodX scalingMethod = ScalingMethodX.getEnumValue(apfParams.getScalingMethods().get(j));
			final ScalingMethodX scalingMethod = ScalingMethodX.getEnumValue(ScalingMethodX.NONE.getDisplayString());
			attribValues[j] = scaleInputValues(attribValues[j], scalingMethod);
		}

		for (int i = 0; i < geneList.size(); i++) {
		
			// If not randomizing these will be identical.
			Node current = geneList.get(i); 
			Node shuffle = shuffledList.get(i);

			// If randomizing, you'll get p-values for a different gene. 
			String canonicalName = shuffle.getName();

			double[] tempArray = new double[attrNamesLength];
			for (int j = 0; j < attrNamesLength; j++) {
				final Double d = attribValues[j][geneNameToIndexMap.get(canonicalName)];
				if (d == null)
					tempArray[j] = ZStatistics.oneMinusNormalCDFInverse(.5);
				else {
					double sigValue = d.doubleValue();
					if (sigValue < MIN_SIG) {
						sigValue = MIN_SIG;
						System.out.println("Warning: value for " + current.getName()+ 
						                   " (" + canonicalName + ") adjusted to " + MIN_SIG);
					} 
					if (sigValue > MAX_SIG) {
						sigValue = MAX_SIG;
						System.out.println("Warning: value for " + current.getName()+ 
						                   " (" + canonicalName + ") adjusted to " + MAX_SIG);
					} 

					// transform the p-value into a z-value and store it in the
					// array of z scores for this particular node
					tempArray[j] = ZStatistics.oneMinusNormalCDFInverse(sigValue);
				}
			}
			tempHash.put(current, tempArray);
		}
		//System.out.println("Done processing into Hash");
		return tempHash;
	}

	private Double[] scaleInputValues(final Double[] inputValues, final ScalingMethodX scalingMethod) {
		if (scalingMethod == ScalingMethodX.NONE)
			return inputValues;

		int nullCount = 0;
		for (final Double inputValue : inputValues) {
			if (inputValue == null)
				++nullCount;
		}
		if (nullCount == inputValues.length)
			return null;

		final double[] unscaledValues = new double[inputValues.length - nullCount];
		int i = 0;
		for (final Double inputValue : inputValues) {
			if (inputValue != null)
				unscaledValues[i++] = inputValue;
		}

		if (scalingMethod == ScalingMethodX.RANK_LOWER || scalingMethod == ScalingMethodX.LINEAR_LOWER) {
			for (int k = 0; k < unscaledValues.length; ++k)
				unscaledValues[k] = -unscaledValues[k];
		}

		final String type;
		final double from, to;
		if (scalingMethod == ScalingMethodX.RANK_LOWER || scalingMethod == ScalingMethodX.RANK_UPPER) {
			from = 0.0;
			to   = 1.0;
			type = "rank";
		} else if (scalingMethod == ScalingMethodX.LINEAR_LOWER || scalingMethod == ScalingMethodX.LINEAR_UPPER) {
			final double EPS = 0.5 / unscaledValues.length;
			from = 0.0 + EPS;
			to   = 1.0 - EPS;
			type = "linear";
		} else
			throw new IllegalArgumentException("unknown scaling method: " + scalingMethod);

		final double[] scaledValues;
		try {
			scaledValues = ScalerFactory.getScaler(type).scale(unscaledValues, from, to);
		} catch (final IllegalArgumentException e) {
			System.out.println("Scaling failed: " + e.getMessage());
			return null;
		}

		final Double[] outputValues = new Double[inputValues.length];
		int k = 0;
		i = 0;
		for (final Double inputValue : inputValues) {
			if (inputValue == null)
				outputValues[k++] = null;
			else
				outputValues[k++] = scaledValues[i++];
		}

		return outputValues;
	}

	
	
} // class ActivePaths (a CytoscapeWindow plugin)
