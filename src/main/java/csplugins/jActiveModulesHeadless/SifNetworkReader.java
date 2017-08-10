package csplugins.jActiveModulesHeadless;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;



import csplugins.jActiveModulesHeadless.networkUtils.*;

public class SifNetworkReader {
	
	private Map<String, Node> nMap;
	
	public SifNetworkReader()
	{
		this.nMap = new HashMap<String, Node>(10000);
	}
	
	
	public Network readNetwork(File networkFile, String delimiter)
	{
		Network newNetwork = null;
		String networkCols[] = null;
		String interac = "";
		int i =0;
		NodeTable table;
		Node sourceNode,targetNode;
		
		if(!networkFile.exists())
			return newNetwork;
		
		newNetwork = new Network();
		table = newNetwork.getNodeTable();
		try {
            BufferedReader br = new BufferedReader(new FileReader(networkFile));
            String line = null;
            while ((line = br.readLine()) != null) {
            	networkCols = line.split(delimiter);
            	//System.out.println("network cols: " + networkCols.length);
            	
            	if (line.trim().length() <= 0)
    				continue;
            	try {
    				final Interaction itr = new Interaction(line, delimiter);
    				interac = itr.getType();
    				createEdge(itr, newNetwork, table);
    			} catch (Exception e) {
    				// Simply ignore invalid lines.
    				continue;
    			}
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
		finally{
			newNetwork.setInteractionType(interac);
		}
		
		if((newNetwork.getEdgeList().size() == 0) || (newNetwork.getAllNodes().size() == 0))
			return null;
		
		return newNetwork;
	}
	
	private void createEdge(final Interaction itr, final Network subNetwork, final NodeTable table) {
		Node sourceNode = nMap.get(itr.getSource());
		if (sourceNode == null) {
			sourceNode =  new Node(itr.getSource());
			table.addRow(sourceNode);
			nMap.put(itr.getSource(), sourceNode);
		}

		for (final String target : itr.getTargets()) {
			Node targetNode = nMap.get(target);
			if (targetNode == null) {
				targetNode = new Node(target);
				table.addRow(targetNode);
				nMap.put(target, targetNode);
			}
			
			// Add the sourceNode and targetNode to subNetwork
			if (!subNetwork.containsNode(sourceNode)){
				subNetwork.addNode(sourceNode);				
			}
			if (!subNetwork.containsNode(targetNode)){
				subNetwork.addNode(targetNode);				
			}
			
			subNetwork.addEdge(sourceNode, targetNode, true);
		}
	}
	
	final class Interaction {
		private String source;
		private List<String> targets;
		private String interactionType;

		Interaction(final String rawText, final String delimiter) {
			final StringTokenizer strtok = new StringTokenizer(rawText, delimiter);
			int counter = 0;
			targets = new ArrayList<String>();

			while (strtok.hasMoreTokens()) {
				if (counter == 0)
					source = strtok.nextToken().trim();
				else if (counter == 1)
					interactionType = strtok.nextToken().trim();
				else
					targets.add(strtok.nextToken().trim());

				counter++;
			}
		}

		/**
		 * @return The source node identifier string.
		 */
		final String getSource() {
			return source;
		}

		/**
		 * @return The interaction type string.
		 */
		String getType() {
			return interactionType;
		}

		/**
		 * @return The array of target node identifier strings.
		 */
		List<String> getTargets() {
			return targets;
		}

	}
}

