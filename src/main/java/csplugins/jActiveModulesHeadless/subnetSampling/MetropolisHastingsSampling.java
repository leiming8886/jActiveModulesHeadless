package csplugins.jActiveModulesHeadless.subnetSampling;

import java.util.*;

import csplugins.jActiveModulesHeadless.data.ActivePathFinderParameters;
import csplugins.jActiveModulesHeadless.networkUtils.Network;
import csplugins.jActiveModulesHeadless.networkUtils.Node;
import csplugins.jActiveModulesHeadless.tests.ScoreTests;


	
public class MetropolisHastingsSampling {

	Random rand;
	private Network network;
	ActivePathFinderParameters apfParams;
	private int arrayGindex=0;
	
	
	
	public MetropolisHastingsSampling(Network Network, ActivePathFinderParameters apfParams) {
		
		System.out.println("Hey  MetropolisHastingsSampling constructor");
		this.apfParams = apfParams;


		if (Network == null || Network.getNodeCount() == 0) {
			throw new IllegalArgumentException("Please select a network");
		}

		
		this.network = Network;
		
		network.generateNeighborList();
		
		rand = new Random();
		
	}
	/**
	 * Returns a pseudo-random number between min and max, inclusive.
	 * The difference between min and max can be at most
	 * <code>Integer.MAX_VALUE - 1</code>.
	 *
	 * @param min Minimum value
	 * @param max Maximum value.  Must be greater than min.
	 * @return Integer between min and max, inclusive.
	 * @see java.util.Random#nextInt(int)
	 */
	public int randInt(int min, int max) {
			
	    // nextInt is normally exclusive of the top value,
	    // so add 1 to make it inclusive
	    int randomNum = rand.nextInt((max - min) + 1) + min;
	
	    return randomNum;
		}
	
		
	@SuppressWarnings("unchecked")
	private void deleteNode (Node nodeToDel, @SuppressWarnings("rawtypes") BalancedOrderStatisticTree gNeighbors, Node[]arrayG, Map<Node,Integer> nOccurCounter) throws Exception{
		System.out.println ("----Deleting from array the node " +nodeToDel+"----");
		if (nodeToDel==null){
			System.out.println("Error in deleteNode. nodeToDel is null:"+nodeToDel);
			return;
		}
		for (Node neighbor : network.getNeighborList (nodeToDel)){
			
			
			// substract -1 to the counter in map for node or delete it
			Integer v= nOccurCounter.get(neighbor);
			if ((v==null)||(v<=0)){
				System.out.println();
				throw new Exception ("Exception: the node "+ neighbor+"doesn't exist in the neighbors' hashmap or has a negative occurances count.");
				
				}
			else if(v==1){
				//System.out.println (" gNeighbors is (toString): "+gNeighbors.toString());
				System.out.println ("before deleting neighbor"+neighbor+" " +neighbor.getName());
				
				
				gNeighbors.remove(neighbor);
				//System.out.println ("after delete neighbor"+neighbor+" " +neighbor.getName());
				//System.out.println (" gNeighbors is (toString): "+gNeighbors.toString());
				 nOccurCounter.remove(neighbor);
				
				}
			else{
				   nOccurCounter.put(neighbor, v-1);
				   }
		}
		gNeighbors.add(nodeToDel);
		System.out.println ("----Ended deleting from array the node " +arrayG[arrayGindex]+"----");
	}
	
	@SuppressWarnings("unchecked")
	private void addNode (Node nodeToAdd, @SuppressWarnings("rawtypes") BalancedOrderStatisticTree gNeighbors, Node[]arrayG, Map<Node,Integer> nOccurCounter){
		System.out.println("-----Adding to array node"+nodeToAdd+" "+nodeToAdd.getName()+"------");
		if (nodeToAdd != null){
			arrayG[arrayGindex]=nodeToAdd;
			gNeighbors.remove(nodeToAdd);
			System.out.println("Hey after Node"+nodeToAdd+" in arrayG");
		}
		else{
			
			System.out.println("[ERROR]: nodeToAdd=null");
			return;
		}
			
		
		// add NodeToAdd's neighbors
		for (Node neighbor : network.getNeighborList (arrayG[arrayGindex])){
			
			Integer v= nOccurCounter.get(neighbor);
			if (v==null){
				nOccurCounter.put(neighbor,1);
				System.out.println("Inserting neighbor"+ neighbor+" "+neighbor.getName());
				gNeighbors.add(neighbor);
				//System.out.println("Hey after insert neighbor");
				
				}
			else{
				   nOccurCounter.put(neighbor, v+1);
				   }
		}
		
		
		System.out.println("-----Ended adding to array node"+nodeToAdd+" "+nodeToAdd.getName()+"------");
		System.out.println("g before arrayGindex is:");
		int i=0;
		while ( i<=arrayGindex){
			System.out.println(arrayG[i]+" " + arrayG[i].getName() );
			i++;
		}
		
		return;
		
	}
	private Node whichNodeToAdd(@SuppressWarnings("rawtypes") BalancedOrderStatisticTree gNeighbors, Node[] arrayG){
		
		Node nodeToAdd=null;
		
		if (gNeighbors.size()<=0){
			System.out.println ("Error during initialisation of g: The graph g seems to not have any neighbors. "
					+ "The counter of neighbors is equal to: "+gNeighbors.size()+ ". g is:");
					arrayG.toString();
			return null;
			}
		else {
			
			
			
			
				int nodeIndex = randInt(0,gNeighbors.size()-1);
				
				nodeToAdd= (Node)gNeighbors.select(nodeIndex);
									
	
				return nodeToAdd;
		}
	}
	
	/** Implementation of Metropolis Hastings algorithm to sample one subnetwork of sike k, using t as the burn-in period
	**/
	public Node[] SampleknodeSubnet (int k, int t, Network network){
		
		System.out.println("Entering SampleknodeSubnet");
		final List<Node> nodeList = network.getNodeList();
		
		//We need to take an element of nodelist.
		
		
		int nodeIndex = randInt(0, nodeList.size()-1);
		Node nodeToAdd=nodeList.get(nodeIndex);
		
		System.out.println("seedNode:"+nodeToAdd);

		//creating a first subgraph: we take  a random seednode , compute the neighbors of this graph, add neighbor+ compute its neighbors and so on until right quantity of nodes added to g.
		
		Node[] arrayG = new Node[k];
		
		
		@SuppressWarnings("rawtypes")
		BalancedOrderStatisticTree gNeighbors = new BalancedOrderStatisticTree();
		
		Map<Node,Integer> nOccurCounter= new HashMap<Node,Integer>();//number of time a neighbor occurs in neighbor lists of nodes of g.
		
		
		boolean seedNode=true;
		
		
		arrayGindex=0;
		
		
		while (arrayGindex < k){
			//searching for next node to add
			if (!seedNode){

				nodeToAdd=whichNodeToAdd(gNeighbors, arrayG);
				}
			else seedNode=false;//in the next while we will not be dealing with seedNode any more
			
			
			//adding node
				addNode(nodeToAdd, gNeighbors, arrayG, nOccurCounter);
				
				++arrayGindex;
				
			}
			
			

		
			
			
		
		System.out.println("Hey graph1created");
		
//Initialisation finished.
		try{return SampleknodeSubnet ( k, t, network, arrayG, nOccurCounter, gNeighbors);}
		catch (Exception e){
			System.out.println(e);
			return null;
		}
	}
	
	
	
	
	@SuppressWarnings({ "rawtypes" })
	private Node[] SampleknodeSubnet (int k, int t, Network network, Node[] arrayG, Map<Node,Integer> nOccurCounter, BalancedOrderStatisticTree gNeighbors) throws Exception{
		int numNeighborsOfOldG=	gNeighbors.size();
		// starting the burnout counter:
		ScoreTests ST=new ScoreTests(network);
		while (t>0) {
			
			//deleting a node arrayG[arrayGindex] in g
			boolean isConnected=false;
			//Set<Node> setG = new HashSet<Node>(Arrays.asList(arrayG));
			Node nodeToDel=null;
			Node nodeToAdd=null;
			while (!isConnected){
				arrayGindex = randInt(0,k-1);
				nodeToDel=arrayG[arrayGindex];
				nodeToAdd=whichNodeToAdd(gNeighbors, arrayG);
				arrayG[arrayGindex]=nodeToAdd;
				isConnected=ST.isConnected(arrayG);
				System.out.println("When node" +nodeToDel+ "deleted, and node "+nodeToAdd+"added in subgraph, isConnected is"+ isConnected );
				//returning to the array before adding or deleting any node:
				arrayG[arrayGindex]=nodeToDel;
			}
			System.out.println("nodeToDel:"+nodeToDel);
			try{
			    deleteNode (nodeToDel,  gNeighbors,  arrayG, nOccurCounter);
			    }
			catch (Exception e){
				throw new Exception(e);
				
			}
			addNode(nodeToAdd, gNeighbors, arrayG, nOccurCounter);
			
			
			
			
			

			
 // deciding whether to stay with oldG or pass to g
	        
	       
	        double alpha=Math.random();
			if (numNeighborsOfOldG/gNeighbors.size() <alpha){
        		//returning everything back to old:
				System.out.println("g is going back to old");
        		
        		//deleting NodeToAdd
        		try{
        			deleteNode(nodeToAdd, gNeighbors, arrayG, nOccurCounter);
        		}
        		catch (Exception e){
        			throw new Exception(e);
				
        		}
        		//adding NodeToDel
        		addNode(nodeToDel, gNeighbors, arrayG, nOccurCounter);
        		
        		System.out.println("g is back to old");
        	}
	        else{
	        	System.out.println("g has changed");
	        }
			System.out.println ("g is:");
					int i=0;
					while ( i<arrayG.length){
						System.out.println (arrayG[i]+ "  "+arrayG[i].getName());
						i++;
					};
	        
			t = t-1;
		}
		System.out.println("Hey returning g");	
		return arrayG;
		
	
	}
}



	
	
	

