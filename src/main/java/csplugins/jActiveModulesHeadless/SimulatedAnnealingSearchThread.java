package csplugins.jActiveModulesHeadless;
//------------------------------------------------------------------------------

import csplugins.jActiveModulesHeadless.networkUtils.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;
import java.util.ArrayList;


import csplugins.jActiveModulesHeadless.data.ActivePathFinderParameters;

public class SimulatedAnnealingSearchThread extends SearchThread {
	
	private FileWriter fw;
   
	//MyProgressMonitor progress;
    public SimulatedAnnealingSearchThread(Network graph, Vector resultPaths, Node [] nodes, ActivePathFinderParameters apfParams )
    {
		super(graph,resultPaths,nodes,apfParams);
		//this.progress = progress;
		super.nodeSet = new HashSet(graph.getNodeList());
		fw = null;
    }

    /**
     *This runs the simulated annealing algorithm
     *and returns an array of ActivePaths which represent
     *the determined active paths. After the simulated annealing run
     *the activePaths are found in oldPaths
     */
	public void run() {

		int timeout = 0;//current number of iterations
		long freeMem ;
		long totalMem;
	    long usedMem ;
	    final Runtime runtime;
		double T = apfParams.getInitialTemperature();
		double temp_step = 1 - Math.pow((apfParams.getFinalTemperature()/apfParams.getInitialTemperature()),(1.0/apfParams.getTotalIterations()));
		Random rand = new Random(apfParams.getRandomSeed());
		//This vector will contain Component objects which refer
		//to the old ActivePaths
		oldPaths = new SortedVector();
		
		runtime = Runtime.getRuntime();
		boolean hubFinding = apfParams.getMinHubSize() > 0;
		if(hubFinding){
		    System.out.println("Using hub finding: "+apfParams.getMinHubSize());
		}
	
		Arrays.sort(nodes);
		
		for(int i = 0;i< nodes.length;i++)
		{
			if(rand.nextDouble() < 0.5)
			{
				if(nodeSet.contains(nodes[i]))
					nodeSet.remove(nodes[i]);
			}
		}
	
		//NodeList [] components = GraphConnectivity.connectedComponents(graph);
		cf = new ComponentFinder(graph,nodeSet);
		//Vector components = cf.getComponents(new Vector(graph.getNodeList()));
	
		//why is a new vector being creater here?
		//Iterator compIt = cf.getComponents(new Vector(graph.nodesList())).iterator();
		Iterator compIt = cf.getComponents( new ArrayList(nodeSet)).iterator();
		while(compIt.hasNext()){
		    //Component tempComponent = new Component((Vector)compIt.next());
		    oldPaths.sortedAdd((Component)compIt.next());
		} 
		
		
		//here we are creating the hashmap which maps from nodes to their
		//respective components. It is important (and sometimes tricky) to 
		//keep this hash map up to date after we change which components
		//are in the graph.
		Iterator it = oldPaths.iterator();
		node2component = new HashMap();
		while(it.hasNext()){
		    Component comp = (Component)it.next();
		    Vector compNodes = comp.getNodes();
		    //System.out.println("components sizes: " + compNodes.size() + " score: " + comp.score);
		    for(int i=0;i<compNodes.size();i++){
			node2component.put(compNodes.get(i),comp);
		    }
		}
		
		//this starts the simulated annealing loop. The temperature
		//step is set so that we will get to the final temperature
		//after the total number of iterations.
		boolean sampleTest = apfParams.getDoSampleTestBoolean();
		int samplingRate = apfParams.getSamplingIterationsSize();
		//int display_step = Math.max(1,apfParams.getTotalIterations()/ActivePathsFinder.UPDATE_COUNT);
		int display_step = ActivePathsFinder.DISPLAY_STEP;
		while(timeout < apfParams.getTotalIterations())
		{
			if(sampleTest && timeout%samplingRate == 0)
			{
				freeMem = runtime.freeMemory();
	    		totalMem = runtime.totalMemory();
	            usedMem = totalMem - freeMem;
				System.out.println("Annealing Running iteration " + timeout + " mem usage: " + usedMem);
				sampleResults(timeout+1);
			}
			//System.out.println("first path num nodes: " + ((Component)oldPaths.lastElement()).getNodes().size());
		    timeout++;
		   
	//	    if(progress != null && timeout%display_step == 0){
	//		progress.update();
	//	    }
		    T *= 1 - temp_step;
		    //when using hub finding, don't accidentally specifiy nodes
		    //for removal.
		    
		    hiddenNodes.clear();
		    
		    //select a node
		    Node current_node = nodes[rand.nextInt(nodes.length)];
		    //toggle the state of that node, it we are doing hubfinding, this may
		    //also involve toggling the state of the surrrounding nodes
			if(hubFinding){
				toggleNodeWithHiding(current_node);
		    }
		    else{
		    	toggleNode(current_node);
		    }
	
		    //get a vector of the new components created by toggling
		    //the current node, in this call, we also update the status of 
		    //newPaths so that it contains a complete list of the components
		    //that would be present in the graph if we made this move
		
		    Vector newComps = updatePaths(current_node);
		    Iterator tempIt = oldPaths.iterator();
		
		    //here we decide if we want to keep the move we made
		    //the first criteria is that the number of paths
		    //cannot fall belong the minimum number of paths specified
		    //by Mr. user.
		    if(newPaths.size() >= oldPaths.size() || newPaths.size() >=  apfParams.getNumberOfPaths()){
				boolean decision = false;
				boolean keep = true;
				int i = 0;
				Iterator oldIt = oldPaths.iterator();
				Iterator newIt = newPaths.iterator();
				
				/*while(oldIt.hasNext()){
				    Component comp = (Component)oldIt.next();
				    System.out.println("components sizes: " +  comp.getNodes().size() + " score: " + comp.score);
				    
				}*/
				//compare the top scoring old and new paths against each other in order. If we find a 
				//better scoring component, we automatically except the move. Otherwise, use the temperature
				//to reject the move with a certain probability.
				//Note that newIt may be larger, but can not be smaller than oldIt, here we will just compare the
				//scores of oldIt versus the matching elements of the new paths
				while(!decision && (newIt.hasNext() && oldIt.hasNext()))
				{
				    double delta = ((Component)newIt.next()).getScore()-((Component)oldIt.next()).getScore();
				    if(delta > .001){
				    	keep = true;
				    	decision = true;
				    }
				    else{
						if(rand.nextDouble() > Math.exp(delta/T)){
						    keep = false;
						    decision = true;
						}    
				    }
				    i++;
				}
				if(keep){
				    //we want to keep the move, update the status
				    //of all the necessary data structures. An important update
				    //is the hashmap from nodes to components. We need to vector
				    //of new components so that we can quickly update the hash (without
				    //rehashing everything in newPaths.
				    oldPaths = newPaths;
				    it = newComps.iterator();
				    while(it.hasNext())
				    {
						Component currComp = ((Component)it.next());
						Iterator nodeIt = currComp.getNodes().iterator();
						while(nodeIt.hasNext()){
						    node2component.put(nodeIt.next(),currComp);
						}
				    }
				}
				else
				{
				    //undo hte current move, if we are dong hubfinding, may need
				    //to restore the hidden nodes as well.
				    toggleNode(current_node);
				    if(hubFinding)
				    {
						it = hiddenNodes.iterator();
						while(it.hasNext())
						{
						    toggleNode((Node)it.next());
						}
				    }
				}
		    }
		    else{
				toggleNode(current_node);		
				if(hubFinding)
				{
				    it = hiddenNodes.iterator();
				    while(it.hasNext()){
					toggleNode((Node)it.next());
				    }
				}
		    }
		    
		}
		if(sampleTest)
			sampleResults(apfParams.getTotalIterations());
			
		resultPaths.addAll(oldPaths);
		System.out.println("End annealing shearch thread");
    }
	
	
	void sampleResults(int iteration)
	{
		String fileName = apfParams.getSamplingTestFile();
		double bestScore =  ((Component)oldPaths.firstElement()).score;
		
		String results = iteration + "\t" + bestScore + "\n";
		
		
		File outFile = new File (fileName);
		
		try {
			if(fw == null)
			{
				outFile.getParentFile().mkdirs();
				FileWriter fw = new FileWriter(outFile,true);
				fw.write(results);
				fw.flush();
			}
			else
			{
					fw.write(results);
					fw.flush();
					if(iteration == apfParams.getTotalIterations())
						fw.close();
				
			}
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
