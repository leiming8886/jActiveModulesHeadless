// ActivePaths.java:  a plugin for CytoscapeWindow,
// which uses VERA & SAM expression data
// to propose active gene regulatory paths
//------------------------------------------------------------------------------
// $Revision: 11526 $
// $Date: 2007-09-05 14:14:24 -0700 (Wed, 05 Sep 2007) $
// $Author: rmkelley $
//------------------------------------------------------------------------------
package csplugins.jActiveModulesHeadless.tests;
//------------------------------------------------------------------------------

import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;



import csplugins.jActiveModulesHeadless.data.ActivePathFinderParameters;
import csplugins.jActiveModulesHeadless.*;
//import csplugins.jActiveModules.util.Scaler;
import csplugins.jActiveModulesHeadless.util.ScalerFactory;

import csplugins.jActiveModulesHeadless.networkUtils.*;

import java.util.Collection;
//import java.io.File;
import java.io.OutputStreamWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


//-----------------------------------------------------------------------------------
public class ScoreTests  {


	protected int attrNamesLength;
	protected Network network;
	protected String titleForCurrentSelection;
	protected ActivePathFinderParameters apfParams;

	protected static int resultsCount = 1;
	
	protected Set<Set<Node>> subnetworks;
	HashMap expressionMap;
	
	Random rn;
	
	ActivePaths activePaths;
	
		
	public ScoreTests(Network Network, ActivePathFinderParameters apfParams) {
		this.apfParams = apfParams;


		if (Network == null || Network.getNodeCount() == 0) {
			throw new IllegalArgumentException("Please select a network");
		}

		attrNamesLength = apfParams.getSizeExpressionAttributes();
		
		/*if (attrNamesLength == 0) {
			throw new RuntimeException("No expression data selected!");
		}*/
		
		this.network = Network;
		
		rn = new Random();
		
		
		activePaths = new ActivePaths(Network, apfParams);
	}
	// ATTENTION: this constructor can only use the methods where apfParams isn't needed!(such as isConnected for instance)
	public ScoreTests(Network Network) {
		this.apfParams = null;


		if (Network == null || Network.getNodeCount() == 0) {
			throw new IllegalArgumentException("Please select a network");
		}

		
		
		this.network = Network;
		
		rn = new Random();
		
		
		activePaths =null;
	}

	// ----------------------------------------------------------------
	public ScoreTests(Network Network, ActivePathFinderParameters apfParams, int size) {
		this.apfParams = apfParams;


		if (Network == null || Network.getNodeCount() == 0) {
			throw new IllegalArgumentException("Please select a network");
		}
		

		attrNamesLength = apfParams.getSizeExpressionAttributes();
		
		/*if (attrNamesLength == 0) {
			throw new RuntimeException("No expression data selected!");
		}*/
		this.network = Network;
		
		rn = new Random();
		
		
		activePaths = new ActivePaths(Network, apfParams);
		if(size > (network.getNodeCount() - 10))
		{
			subnetworks = new HashSet<Set<Node>>();
			Set<Node> firstSet = new HashSet<Node>();
			firstSet.addAll(network.getAllNodes());
			subnetworks.add(firstSet);
			for(int i = (network.getNodeCount() - 1);i>=size;i--)
				subnetworks = getNextLevelSubnetworksDown(subnetworks);
				
		}
		else
		{
			subnetworks = findSubnetworks(3);
			
			for(int i = 4;i<=size;i++)
				subnetworks = getNextLevelSubnetworks(subnetworks);
		
		}
		
	} // ctor

	// --------------------------------------------------------------
	protected Set<Set<Node>> findSubnetworks(int size) {
		
		int space =size;
		Set<Set<Node>> listOfNodes = new HashSet<Set<Node>>();
		Set<Node> shortSet ;
				
		for(Node node : network.getAllNodes())
		{
			space =size;
			shortSet = new HashSet<Node>();
			shortSet.add(node);
			
			addNodes(listOfNodes,shortSet,node,space - 1);
			
		}
		
		System.out.println("Number of Subnetworks of size " + size + ": " + listOfNodes.size());
		//System.out.println("Sets: " + listOfNodes.toString());
		
		return listOfNodes;
		
	}
	
	private void addNodes(Set<Set<Node>> bigSet, Set<Node> shortSet, Node parent,int leftSpace)
	{
		Set<Node> tempSet ;
		if(leftSpace == 0)
		{
			//System.out.println("Adding set: " + shortSet.toString());
			bigSet.add(shortSet);

			return;
		}
		
		for(Node node2 : network.getNeighborList(parent))
		{
			tempSet = new HashSet<Node>();
			tempSet.addAll(shortSet);
			if(tempSet.contains(node2))
				continue;
			tempSet.add(node2);
			//System.out.println("size: " + tempSet.size() + " space: " + (leftSpace -1) );
			addNodes(bigSet,tempSet,node2,leftSpace -1 );
		}
		
		shortSet.clear();
	}
	
	public void getSubNetworkSizes(int range)
	{
		Set<Set<Node>> listOfNodes = new HashSet<Set<Node>>();
		Set<Node> tempSet = new HashSet<Node>();
		
		System.out.println("Number of Subnetworks of size 1: " + network.getNodeCount());
		
		System.out.println("Number of Subnetworks of size 2: " + network.getEdgeCount());
		
		for(Edge edge : network.getEdgeList())
		{
			tempSet = new HashSet<Node>();
			tempSet.add(edge.getSource());
			tempSet.add(edge.getTarget());
			listOfNodes.add(tempSet);
		}
		
		for(int i = 3 ; i <= range ; i++)
		{
			
			listOfNodes = getNextLevelSubnetworks(listOfNodes);
			// findSubnetworks(i);
			System.out.println("Number of Subnetworks of size " + i + ": " + listOfNodes.size());
			//TODO:create an option savenetworks. False by default, can be set to true when launching programme.
			//if (savenetworks)
				//saveNetworks(listOfNodes, i);
		}
		
	}
	/*public void saveNetworks(Set<Set<Node>> listOfiNodes, int i){
		File outiNodeNetworksFile = new File (outputDir,"NetworksOfSize" + i+".sif");
		FileOutputStream outStream = null;
		SifWriter writer = null;
		
		try {
			outStream = new FileOutputStream(outiNodeNetworksFile);
			for (Set<Node> j : listOfiNodes){
				for (Node k :j)
				//TODO:I can't use SifWriter, since it takes a network as parameter and not a node. I have to write a program "interaction writer"
				writer = new SifWriter(outStream, k);
			}
			writer.writeSif(readDelimiter);
		
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
	}*/
	public Network[] generateSubnetwork(int size)
	{
		Component[] components = new Component[1];
		List<Node> nodes = network.getNodeList();
		Set<Node> visited = new HashSet<Node>();
		LinkedList<Node> toVisit = new LinkedList<Node>();
		
		Set<Node> subnodes = new HashSet<Node>();
		
		
		toVisit.add( nodes.get(rn.nextInt(nodes.size())));
		
		while(!toVisit.isEmpty() && subnodes.size() < size) 
		{
			Node node = toVisit.remove();
			
			if(!subnodes.contains(node) )
			{
				subnodes.add(node);
				
				toVisit.addAll(network.getNeighborList(node));
			}
		}
		 
		components[0] = new Component(Arrays.asList(subnodes.toArray()));
		components[0].finalizeDisplay();
		
		return activePaths.createSubnetworks(components);
	}
	
	public void addSubnetwork(Node node, Set<Set<Node>> list, int size)
	{
		LinkedList<Node> toVisit = new LinkedList<Node>();
		Set<Node> subnodes = new HashSet<Node>();
		
		toVisit.add(node);
		
		while(!toVisit.isEmpty() && subnodes.size() < size) 
		{
			Node temp = toVisit.remove();
			
			if(!subnodes.contains(temp)  )
			{
				subnodes.add(temp);
				
				if(list.contains(subnodes))
				{
					subnodes.remove(temp);
				}
				else
				{
					List<Node> tempList = network.getNeighborList(temp);
					Collections.shuffle(tempList);
					toVisit.addAll(tempList);
				}
			}
		}
		
		if(subnodes.size() == size)
			list.add(subnodes);
	}
	
	public void getSubNetworkSizesDown(int range)
	{
		Set<Set<Node>> listOfNodes = new HashSet<Set<Node>>();		
		Set<Node> firstSet = new HashSet<Node>();
		
		firstSet.addAll(network.getAllNodes());
		listOfNodes.add(firstSet);
		
		for(int i = (network.getNodeCount() - 1);i>=range;i--)
		{
			
			listOfNodes = getNextLevelSubnetworksDown(listOfNodes);
			// findSubnetworks(i);
			System.out.println("Number of Subnetworks of size " + i + ": " + listOfNodes.size());
		}
		
	}
	
	protected Set<Set<Node>> getNextLevelSubnetworks(Set<Set<Node>> oldList)
	{
		Set<Set<Node>> listOfNodes = new HashSet<Set<Node>>();
		Set<Node> neighboors = new HashSet<Node>();
		Set<Node> newSet ;
		
		int size = oldList.size();
		//System.out.println("original size: " + size);
		for(Set<Node> oldSet : oldList)
		{
			neighboors.clear();
			for(Node node : oldSet)
			{
				neighboors.addAll(network.getNeighborList(node));
			}
			neighboors.removeAll(oldSet);
			//System.out.println("neightboors: " + neighboors.size() + " element: " + size--);
			for(Node newNode : neighboors)
			{
				newSet =  new HashSet<Node>();
				newSet.addAll(oldSet);
				newSet.add(newNode);
				listOfNodes.add(newSet);
			}
		}
		
		return listOfNodes;
	}
	
	protected Set<Set<Node>> getNextLevelSubnetworksDown(Set<Set<Node>> oldList)
	{
		Set<Set<Node>> listOfNodes = new HashSet<Set<Node>>();
		Set<Node> testSet = new HashSet<Node>();
		
		int size = oldList.size();
		//System.out.println("original size: " + size);
		for(Set<Node> oldSet : oldList)
		{
			testSet = new HashSet<Node>();
			for(Node node : oldSet)
			{
				
				testSet.addAll(oldSet);
				testSet.remove(node);
				if(isConnected(testSet))
				{
					listOfNodes.add(testSet);
					testSet = new HashSet<Node>();
				}
				else
					testSet.clear();
			}
			
		}
		
		return listOfNodes;
	}
	public boolean isConnected(Set<Node> nodes)
	   {
	       boolean isCon = false;
	       Set<Node> visited = new HashSet<Node>();
	       LinkedList<Node> toVisit = new LinkedList<Node>();
	       Set<Node> notAllowed = new HashSet<Node>();
	       List<Node> list ;
	       notAllowed.addAll(network.getAllNodes());
	       notAllowed.removeAll(nodes);

	       toVisit.add(nodes.iterator().next());

	       while(!toVisit.isEmpty())
	       {
	           Node node = toVisit.remove();
	           if(!visited.contains(node))
	           {
	               list = network.getNeighborList(node);
	               visited.add(node);
	               for( Node nodeTemp : list)
	               {
	                   if(!notAllowed.contains(nodeTemp))
	                       toVisit.add(nodeTemp);
	               }

	           }
	       }

	       if(visited.size() == nodes.size())
	           isCon = true;

	       return isCon;
	   }
	
	public boolean isConnected(Node[] nodes)
	   {
	       boolean isCon = false;
	       Set<Node> visited = new HashSet<Node>();
	       LinkedList<Node> toVisit = new LinkedList<Node>();
	       Set<Node> allowed = new HashSet<Node>();
	       List<Node> list ;
	       for(int i =0 ; i < nodes.length ; i++)
	           allowed.add(nodes[i]);

	       toVisit.add(nodes[0]);

	       while(!toVisit.isEmpty())
	       {
	           Node node = toVisit.remove();
	           if(!visited.contains(node))
	           {
	               list = network.getNeighborList(node);
	               visited.add(node);
	               for( Node nodeTemp : list)
	               {
	                   if(allowed.contains(nodeTemp))
	                       toVisit.add(nodeTemp);
	               }

	           }
	       }

	       if(visited.size() == nodes.length)
	           isCon = true;

	       return isCon;
	   }
	
	public double getBestScore()
	{
		double best = 0;
		
		Component tempComp;
		
		
		for(Set<Node> nodes : subnetworks)
		{
			//System.out.println(nodes.toString());
			tempComp = new Component(Arrays.asList(nodes.toArray()));
			//System.out.println("score: " + tempComp.getScore());
			if(tempComp.getScore() > best)
				best = tempComp.getScore();
		}
		
		return best;
	}
	
	public void generateSampleSubnetworks(int size, int length)
	{
		Set<Set<Node>> sample = new HashSet<Set<Node>>();
		List<Node> nodes = network.getNodeList();
		
		while(sample.size() < length)
		{
			//System.out.println("size samples: " + sample.size());
			addSubnetwork(nodes.get(rn.nextInt(nodes.size())),sample,size);
		}
		
		subnetworks = sample;
	}
	
	public List<Double> getAllScores()
	{
		ArrayList<Double> scores = new ArrayList<Double>();
		
		Component tempComp;
		
		
		for(Set<Node> nodes : subnetworks)
		{
			tempComp = new Component(Arrays.asList(nodes.toArray()));
			scores.add(tempComp.getScore());
		}
		
		return scores;
	}
	
	public double getBestScoreWithIndependency()
	{
		double best = 0;
		Set<Node> bestNodes = null;
		
		Component tempComp;
		
		
		for(Set<Node> nodes : subnetworks)
		{
			//System.out.println(nodes.toString());
			tempComp = new Component(Arrays.asList(nodes.toArray()));
			if(tempComp.getScore() > best)
			{
				best = tempComp.getScore();
				bestNodes = nodes;
			}
			setRandomPValues(nodes);
		}
		
		//if(bestNodes != null)
		//	setRandomPValues(bestNodes);
		
		return best;
	}
	
	public void setRandomPValues(Set<Node> nodes) {
		
		NodeTable table = network.getNodeTable();
		double[] tempArray = new double[attrNamesLength];
		
		rn = new Random();
		
		for(Node node : nodes)
		{
			Row row = table.getRow(node);
			for(int i=0; i < row.getDataSize() ; i++)
			{
				row.setDataColumn(i, rn.nextDouble());
				tempArray[i] = ZStatistics.oneMinusNormalCDFInverse(row.getDataColumn(i));
			}
			expressionMap.put(node, tempArray);
		}
		
		//expressionMap = activePaths.generateExpressionMap();
		Component.exHash = expressionMap;
		//setupScoring(expressionMap);
	}
	
	public void setRandomPValues() {
		
		NodeTable table = network.getNodeTable();
		
		rn = new Random();
		
		for(Row row : table.getAllRows())
		{
			for(int i=0; i < row.getDataSize() ; i++)
			{
				row.setDataColumn(i, rn.nextDouble());
			}
		}
		
		expressionMap = activePaths.generateExpressionMap();
		setupScoring(expressionMap);
	}

	public void run() {
		
		
		
	    System.gc();
		//long start = System.currentTimeMillis();

		Vector<Component> activePathsVect= new Vector<Component>();

		
		//ActivePathsTaskFactory factory = new ActivePathsTaskFactory(apf);
		//ServicesUtil.synchronousTaskManagerServiceRef.execute(factory.createTaskIterator());
				
		
		 
	}
	
	private void setupScoring(HashMap expressionMap) {
		Network perspective = network;
		Node[] nodes;
	    
		// Here we initialize the z table. We use this data structure when we
		// want to get an adjusted z score
		// based on how many conditions we are looking at.
		//System.out.println("Initializing Z Table");
		Component.zStats = new ZStatistics(attrNamesLength);
		//System.out.println("Done initializing Z Table");

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
				//System.out.println("Trying to read monte carlo file");
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
				
				// Component.pStats.calculateMeanAndStd(nodes,ParamStatistics.DEFAULT_ITERATIONS,apfParams.getMaxThreads(),
				// new MyProgressMonitor(cytoscapeWindow, "Sampling Mean and
				// Standard
				// Deviation","",0,ParamStatistics.DEFAULT_ITERATIONS));
				Component.pStats.calculateMeanAndStd(nodes,
						ParamStatistics.DEFAULT_ITERATIONS, apfParams
								.getMaxThreads());
				//System.out.println("Finished initializing monte carlo state");

				//System.out.println("Trying to save monte carlo state");
				try {
					
						FileOutputStream temp = new FileOutputStream("zscores.txt");
						String lineSep = System.getProperty("line.separator");
						OutputStreamWriter writer = new OutputStreamWriter(temp,"UTF-8");
						for(Node node  : network.getAllNodes())
						{
							writer.write(node.getName());
							writer.write(" ");
							writer.write(Double.valueOf(((double [])expressionMap.get(node))[0]).toString());
							writer.write(lineSep);
							writer.flush();
						}
						for(int i=0;i < network.getNodeCount();i++)
						{
							writer.write(Integer.valueOf(i).toString());
							writer.write(" ");
							writer.write(Double.valueOf(Component.pStats.getMean(i+1)).toString());
							writer.write(" ");
							writer.write(Double.valueOf(Component.pStats.getStd(i+1)).toString());
							writer.write(lineSep);
							writer.flush();
						}
						temp.close();
					
					FileOutputStream fos = new FileOutputStream("last.mc");
					ObjectOutputStream oos = new ObjectOutputStream(fos);
					oos.writeObject(Component.pStats);
					oos.close();
					//System.out.println("Saved monte carlo state to last.mc");
				} catch (Exception e) {
					System.out.println("Failed to save monte carlo state " + e);
				}
			}
		}

	}

	
	
	
	
	
	
} // class ActivePaths (a CytoscapeWindow plugin)
