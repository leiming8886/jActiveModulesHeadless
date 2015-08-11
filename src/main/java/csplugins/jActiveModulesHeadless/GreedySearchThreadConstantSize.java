package csplugins.jActiveModulesHeadless;
import csplugins.jActiveModulesHeadless.networkUtils.*;
//import csplugins.jActiveModulesHeadless.subnetSampling.BalancedOrderStatisticTree;
import csplugins.jActiveModulesHeadless.subnetSampling.BalancedOrderStatisticTreeOrderedByNodePval;




import csplugins.jActiveModulesHeadless.tests.ScoreTests;

//import java.util.Collection;
import java.lang.Math;
//import java.lang.Math.log10();
import java.util.HashMap;
import java.util.HashSet;
//import java.util.concurrent.*;
import java.util.Iterator;
import java.util.LinkedList;
//import java.util.Map;
//import java.util.ArrayList;
//import java.util.List;






import java.util.Map;

import csplugins.jActiveModulesHeadless.data.ActivePathFinderParameters;


//FOR INFO: if I want to access k-th best neighbour:http://www.dsalgo.com/2013/03/find-kth-smallest-element-in-binary.html
//or http://stackoverflow.com/questions/2329171/find-kth-smallest-element-in-a-binary-search-tree-in-optimum-way
public class GreedySearchThreadConstantSize  implements Runnable {//extends Thread {
		BalancedOrderStatisticTreeOrderedByNodePval gNeighbors; 
		Map<Node,Integer> nOccurCounter;//number of time a neighbor occurs in neighbor lists of nodes of g.
		//double a=Math.log10(102);
		int max_depth, search_depth;
		ActivePathFinderParameters apfParams;
		Iterator nodeIterator;
		//MyProgressMonitor pm;
		/**
		 * Track the best score generated from the current starting point
		 */
		double bestScore;

		/**
		 * Map from a node to the number of nodes which are dependent on this node
		 * for connectivity into the network
		 */
		HashMap node2DependentCount;

		/**
		 * Map from a node to it's predecessor in the search tree When we remove
		 * this node, that predecessor may be optionally added to the list of
		 * removable nodes, depending on whether it has any other predecessors
		 */
		HashMap node2Predecessor;
		
		Component newComp;

		/**
		 * Lets us know if we need to repeat the greedy search from a new starting
		 * point
		 */
		boolean greedyDone;
		/**
		 * Determines which nodes are within max depth of the starting point
		 */
		HashSet withinMaxDepth;
		
		Node[] nodes;
		Node seed;
		Network network;
		int sizeSubnets;
		
		
		
		public GreedySearchThreadConstantSize( Network network,ActivePathFinderParameters apfParams,
				Node current, Component component,
				Node[] node_array) {
			network.generateNeighborList();
			this.gNeighbors = new BalancedOrderStatisticTreeOrderedByNodePval();
			
			this.nOccurCounter= new HashMap<Node,Integer>();//number of time a neighbor occurs in neighbor lists of nodes of g.
			
			
			this.apfParams = apfParams;
			max_depth = apfParams.getMaxDepth();
			search_depth = apfParams.getSearchDepth();
			
			this.seed = current;
			//pm = tpm;
			//node2BestComponent = temp_hash;
			this.newComp = component;
			nodes = node_array;
			this.network = network;
			this.sizeSubnets=apfParams.getSizeSubnets();
			
			//System.out.println("Max Depth: " + max_depth);
			//System.out.println("Search Depth: " + search_depth);
		}
		/**
		 * Recursively find the nodes within a max depth
		 */
		private void initializeMaxDepth(Node current, int depth) {
			withinMaxDepth.add(current);
			if (depth > 0) {
				Iterator listIt = network.getNeighborList(current).iterator(); 
				while (listIt.hasNext()) {
					Node myNode = (Node) listIt.next();
					if (!withinMaxDepth.contains(myNode)) {
						initializeMaxDepth(myNode, depth - 1);
					}
				}
			}
		}
		
		private class returnedFromRunGreedyInitialisation{
			Component comp;
			HashSet <Node> rNodes;
		}

		/**
		 * Runs the greedy search algorithm. This function will run a greedy search
		 * iteratively using each node of the network as a starting point
		 */
		public void run() {
			
				// determine which nodes are within max-depth
				// of this starting node and add them to a hash set
				// so we can easily identify them
				withinMaxDepth = new HashSet();
				// if the user doesn't wish to limit the maximum
				// depth, just add every node into the max depth
				// hash, thus all nodes are accepted as possible
				// additions
				if (!apfParams.getEnableMaxDepth()) {
					for (int j = 0; j < nodes.length; j++) {
						withinMaxDepth.add(nodes[j]);
					}
				} else {
					// recursively find the nodes within a max depth
					initializeMaxDepth(seed, max_depth);
				}
				
				// set the neighborhood of nodes to initially be only
				// the single node we are starting the search from
				//Component component = new Component();
				Component component = newComp;
				component.addNode(seed);
				// make sure that the seed is never added to the list of removables
				node2DependentCount = new HashMap();
				node2Predecessor = new HashMap();
				node2DependentCount.put(seed, new Integer(1));
				// we don't need to make a predecessor entry for the seed,
				// since it should never be added to the list of removable nodes
				HashSet <Node> removableNodes = new HashSet<Node>();
				removableNodes.add(seed);
				bestScore = Double.NEGATIVE_INFINITY;
				
				returnedFromRunGreedyInitialisation resultInit=runGreedyInitialisation(component, sizeSubnets, seed, removableNodes);

				//System.out.println ("After initialization the component is: "+ component.toString());
				//TODO: uncomment and fix:runGreedySearchRecursiveConstantSize(search_depth, resultInit.comp, seed, resultInit.rNodes);
				//runGreedyRemovalSearch(component, removableNodes);
		}

		
		
		
		private returnedFromRunGreedyInitialisation runGreedyInitialisation (Component initialComponent, int sizeSubnets, Node NeighborSourceNode, HashSet<Node> removableNodes ){
		//	System.out.println(Thread.currentThread().getName()+" :Thread");
					Iterator<Node> nodeIt = network.getNeighborList(NeighborSourceNode).iterator();
	//		System.out.println (Thread.currentThread().getName()+"Beginning runGreedyInit. NeighborSourceNode: "+NeighborSourceNode.toString()+"its name: "+NeighborSourceNode.getName()+
	//				". \n Its neighbors: "+network.getNeighborList(NeighborSourceNode).toString());
			removableNodes.remove(NeighborSourceNode);//TODO: what happens if node not inside?
		//	node2DependentCount.put(lastAdded, new Integer(dependentCount));
		//	System.out.println (Thread.currentThread().getName()+"beginning init removableNodes: "+removableNodes.toString());
			while (initialComponent.getNodes().size()<sizeSubnets){
				
				if (nodeIt.hasNext()) {
					
					Node newNeighbor = (Node) nodeIt.next();
					//TODO: delete this test
					gNeighbors.add(newNeighbor);
					System.out.println (Thread.currentThread().getName()+"pval:"+Math.log10(newNeighbor.getPvalue())+"gNeighbors: "+gNeighbors.toString());
					// this node is only a new neighbor if it is not currently
					// in the component.
					if (withinMaxDepth.contains(newNeighbor)
							&& !initialComponent.contains(newNeighbor)) {
						initialComponent.addNode(newNeighbor);
						removableNodes.add(newNeighbor);
					//	System.out.println (Thread.currentThread().getName()+"added neighbor"+newNeighbor+" removableNodes: "+removableNodes.toString());
					}
					
				}
				else { Iterator<Node> newSourceIt = removableNodes.iterator();
				//System.out.println (Thread.currentThread().getName()+"in else. removableNodes: "+removableNodes.toString());
				if (newSourceIt.hasNext()){
			//		System.out.println (Thread.currentThread().getName()+"relaunching greedy with newSource Node");
					runGreedyInitialisation (initialComponent, sizeSubnets, (Node) newSourceIt.next(), removableNodes );
				}
				else{
						
				//	System.out.println (Thread.currentThread().getName()+"component "+ initialComponent.toString()+"seems isolated.");return null;
				//TODO:throw Exception NotEnoughNeighbours . when catching it, print it and go to next seednode"";
				}break;
				}
			}
			
			
			
			System.out.println (Thread.currentThread().getName()+"Initialisation ok. Initial component: "+initialComponent.toString());
			returnedFromRunGreedyInitialisation result=new returnedFromRunGreedyInitialisation();
			result.comp= initialComponent;
			result.rNodes= removableNodes;
			return result;
			
		}
		
//		private void deleteNode (Node nodeToDel, @SuppressWarnings("rawtypes") BalancedOrderStatisticTree gNeighbors, Node[]arrayG, Map<Node,Integer> nOccurCounter) throws Exception{
//			//System.out.println ("----Deleting from array the node " +nodeToDel+"----");
//			if (nodeToDel==null){
//				throw new Exception("Error in deleteNode. nodeToDel is null:"+nodeToDel);
//
//			}
//			for (Node neighbor : network.getNeighborList (nodeToDel)){
//				
//				
//				// substract -1 to the counter in map for node or delete it
//				Integer v= nOccurCounter.get(neighbor);
//				if ((v==null)||(v<=0)){
//					
//					throw new Exception ("Exception: the node "+ neighbor+"doesn't exist in the neighbors' hashmap or has a negative occurances count.");
//					
//					}
//				else if(v==1){
//					
//					//System.out.println ("before deleting neighbor"+neighbor+" " +neighbor.getName());
//					
//					
//					gNeighbors.remove(neighbor);
//					//System.out.println ("after delete neighbor"+neighbor+" " +neighbor.getName());
//					////System.out.println (" gNeighbors is (toString): "+gNeighbors.toString());
//					 boolean inG=false;
//					 for (Node n:arrayG){
//						if (n==neighbor) {inG=true;break;}
//					 }
//						if (!inG) {
//							nOccurCounter.remove(neighbor);
//							//System.out.println ("after deleting neighbor from nOccurCounter"+neighbor+" " +neighbor.getName());
//							
//						}
//						else {
//							nOccurCounter.put(neighbor, 0);
//							//System.out.println ("after putting count to 0 for neighbor from nOccurCounter"+neighbor+" " +neighbor.getName());
//						}
//						
//					 
//					
//					}
//				else{
//					   nOccurCounter.put(neighbor, v-1);
//					   }
//			}
//			gNeighbors.add(nodeToDel);
//		//	System.out.println ("----Ended deleting from array the node " +arrayG[arrayGindex]+"----");
//		}
//		
//		@SuppressWarnings("unchecked")
//		private void deleteNode (Node nodeToDel, @SuppressWarnings("rawtypes") BalancedOrderStatisticTree gNeighbors, Node[]arrayG, Map<Node,Integer> nOccurCounter) throws Exception{
//			//System.out.println ("----Deleting from array the node " +nodeToDel+"----");
//			if (nodeToDel==null){
//				throw new Exception("Error in deleteNode. nodeToDel is null:"+nodeToDel);
//
//			}
//			for (Node neighbor : network.getNeighborList (nodeToDel)){
//				
//				
//				// substract -1 to the counter in map for node or delete it
//				Integer v= nOccurCounter.get(neighbor);
//				if ((v==null)||(v<=0)){
//					
//					throw new Exception ("Exception: the node "+ neighbor+"doesn't exist in the neighbors' hashmap or has a negative occurances count.");
//					
//					}
//				else if(v==1){
//					
//					//System.out.println ("before deleting neighbor"+neighbor+" " +neighbor.getName());
//					
//					
//					gNeighbors.remove(neighbor);
//					//System.out.println ("after delete neighbor"+neighbor+" " +neighbor.getName());
//					////System.out.println (" gNeighbors is (toString): "+gNeighbors.toString());
//					 boolean inG=false;
//					 for (Node n:arrayG){
//						if (n==neighbor) {inG=true;break;}
//					 }
//						if (!inG) {
//							nOccurCounter.remove(neighbor);
//							//System.out.println ("after deleting neighbor from nOccurCounter"+neighbor+" " +neighbor.getName());
//							
//						}
//						else {
//							nOccurCounter.put(neighbor, 0);
//							//System.out.println ("after putting count to 0 for neighbor from nOccurCounter"+neighbor+" " +neighbor.getName());
//						}
//						
//					 
//					
//					}
//				else{
//					   nOccurCounter.put(neighbor, v-1);
//					   }
//			}
//			gNeighbors.add(nodeToDel);
//		//	System.out.println ("----Ended deleting from array the node " +arrayG[arrayGindex]+"----");
//		}
//		
//		@SuppressWarnings("unchecked")
//		private void addNode (Node nodeToAdd, @SuppressWarnings("rawtypes") BalancedOrderStatisticTree gNeighbors, Node[]arrayG, Map<Node,Integer> nOccurCounter){
//		//System.out.println("-----Adding to array node"+nodeToAdd+" "+nodeToAdd.getName()+"------");
//			if (nodeToAdd != null){
//				arrayG[arrayGindex]=nodeToAdd;
//				gNeighbors.remove(nodeToAdd);
//				Integer v=nOccurCounter.get(nodeToAdd);
//				if (v==null){
//					nOccurCounter.put(nodeToAdd,0);
//					//System.out.println("nodeToAdd " +nodeToAdd+ "wasn't in nOccurCounter. I put it there."); 
//					}
//				
//			//	System.out.println("Hey after Node"+nodeToAdd+" in arrayG");
//			}
//				
//			
//			// add NodeToAdd's neighbors
//			for (Node neighbor : network.getNeighborList (arrayG[arrayGindex])){
//				
//				Integer v= nOccurCounter.get(neighbor);
//				if (v==null){
//					nOccurCounter.put(neighbor,1);
//					
//					gNeighbors.add(neighbor);
//					//System.out.println("Inserting neighbor"+ neighbor+" "+neighbor.getName());
//					
//					}
//				else{
//					   nOccurCounter.put(neighbor, v+1);
//					 //  System.out.println("Incrementing in nOccurCounter neighbor"+ neighbor+" "+neighbor.getName());
//					   }
//			}
//			
//			
//			//System.out.println("-----Ended adding to array node"+nodeToAdd+" "+nodeToAdd.getName()+"------");
//		//	System.out.println("g before arrayGindex +"+arrayGindex+"is:");
//		/*	int i=0;
//			while ( i<=arrayGindex){
//				System.out.println(arrayG[i]+" " + arrayG[i].getName() );
//				i++;
//			}
//			*/
//			
//			return;
//			
//		}
		/**
		 * Recursive greedy search function. Called from runGreedySearch() to a
		 * recursive set of calls to greedily identify high scoring networks. The
		 * idea for this search is that we make a recursive call for each addition
		 * of a node from the neighborhood. At each stage we check to see if we have
		 * found a higher scoring network, and if so, store it in one of the global
		 * variables. You know how in the Wonder Twins, one of them turned into an
		 * elephant and the other turned into a bucket of water? This function is
		 * like the elephant.
		 * 
		 * @param depth
		 *            The remaining depth allowed for this greed search.
		 * @param component
		 *            The current component we are branching from.
		 * @param lastAdded
		 *            The last node added.
		 * @param removableNodes
		 *            Nodes that can be removed. 
		 */
		private boolean runGreedySearchRecursiveConstantSize(int depth, Component component,
				 HashSet <Node> removableNodes) {
			boolean globalExtremumFound=false;
			boolean nodeAdded=false;
			//double oldScore = component.getScore();
			
			//remove lowest scoring node of removableNodes from component
		Iterator<Node> rIter=removableNodes.iterator();
		while (rIter.hasNext()&& !nodeAdded){
			Node toDel=rIter.next();
			component.removeNode(toDel);
			removableNodes.remove(toDel);
			Node predecessor = (Node) node2Predecessor.get(toDel);
			int dependentCount = ((Integer) node2DependentCount
					.get(predecessor)).intValue();
			dependentCount -= 1;
			if (dependentCount == 0) {
				removableNodes.add(predecessor);
			} // end of if ()
			else {
				node2DependentCount.put(predecessor, new Integer(
						dependentCount));
			} // end of else
			
			
			double toDelPval=toDel.getPvalue();
			
			//if exists, add a better scoring node and re-launch program on the new network
			Iterator<Node> neighIter=component.neighborhood.iterator();
			while( neighIter.hasNext() && !nodeAdded){
				
				Node candidateToAdd=neighIter.next();
				if ((candidateToAdd.getPvalue() <toDelPval)&&withinMaxDepth.contains(candidateToAdd)
						&& !component.contains(candidateToAdd)){
					component.addNode(candidateToAdd);
					removableNodes.add(candidateToAdd);
					//TODO: update removableNodes, depth?, dependentCount
					//TODO: fix : node2Predecessor.put(candidateToAdd, value);// if I do not know how to get value, 
					//I can just delete predecessor 2 structures +removable nodes and check whether component is connected after node is deleted (include csplugins....util 
					//and cf MetropolisHastings Sampling: ScoreTests ST=new ScoreTests(network);isConnected=ST.isConnected(arrayG);
					nodeAdded=true;
					
					
					runGreedySearchRecursiveConstantSize(depth, component, removableNodes);
				}
				
			}
			if (!nodeAdded){
				component.addNode(toDel);
				removableNodes.add(toDel);}
			
		}
		if (!nodeAdded){
			
			globalExtremumFound=true;
			
		//TODO: return component
		}	
		return globalExtremumFound;}
		
//			boolean improved = false;
//			// score this component, check and see if the global top scores should
//			// be updated, if we have found a better score, then return true
//			if (component.getScore() > bestScore) {
//				depth = search_depth;
//				improved = true;
//				bestScore = component.getScore();
//			}
//
//			if (depth > 0) //&& ((component.getNodes()).size()<sizeSubnets))
//			{
//				// if depth > 0, otherwise we are out of depth and the recursive
//				// calls will end
//				// Get an iterator of nodes which are next to the
//				Iterator nodeIt = network.getNeighborList(lastAdded).iterator(); 
//				boolean anyCallImproved = false;
//				removableNodes.remove(lastAdded);
//				int dependentCount = 0;
//				while (nodeIt.hasNext()) {
//					Node newNeighbor = (Node) nodeIt.next();
//					// this node is only a new neighbor if it is not currently
//					// in the component.
//					if (withinMaxDepth.contains(newNeighbor)
//							&& !component.contains(newNeighbor)) {
//						component.addNode(newNeighbor);
//						removableNodes.add(newNeighbor);
//						boolean thisCallImproved = runGreedySearchRecursiveConstantSize(
//								depth - 1, component, newNeighbor, removableNodes);
//						if (!thisCallImproved) {
//							component.removeNode(newNeighbor);
//							removableNodes.remove(newNeighbor);
//						} // end of if ()
//						else {
//							dependentCount += 1;
//							anyCallImproved = true;
//							node2Predecessor.put(newNeighbor, lastAdded);
//						} // end of else
//					} // end of if ()
//				}
//				improved |= anyCallImproved;
//				if (dependentCount > 0) {
//					removableNodes.remove(lastAdded);
//					node2DependentCount.put(lastAdded, new Integer(dependentCount));
//				} // end of if ()
//
//			}
//			return improved;
//		}

		private void runGreedyRemovalSearch(Component component,
				HashSet removableNodes) {
			LinkedList list = new LinkedList(removableNodes);
			while (!list.isEmpty()) {
				Node current = (Node) list.removeFirst();
				component.removeNode(current);
				double score = component.getScore();
				if (score > bestScore) {
					bestScore = score;
					Node predecessor = (Node) node2Predecessor.get(current);
					int dependentCount = ((Integer) node2DependentCount
							.get(predecessor)).intValue();
					dependentCount -= 1;
					if (dependentCount == 0) {
						removableNodes.add(predecessor);
					} // end of if ()
					else {
						node2DependentCount.put(predecessor, new Integer(
								dependentCount));
					} // end of else

				} // end of if ()
				else {
					component.addNode(current);
				} // end of else

			} // end of while ()

		}
	}


