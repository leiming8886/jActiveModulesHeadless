package csplugins.jActiveModulesHeadless;

import java.io.*;
import java.util.HashMap;
import java.util.Map;



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
            	if(networkCols.length != 3)
    		    	return null;
            	
            	if(i == 0)
            	{
            		interac = networkCols[1];
            		i++;
            	}
            	Node sourceNode = nMap.get(networkCols[0]);
        		if (sourceNode == null) {
        			sourceNode =  new Node(networkCols[0]);
        			table.addRow(sourceNode);
        			nMap.put(networkCols[0], sourceNode);
        			newNetwork.addNode(sourceNode);
        		}
        		
        		Node targetNode = nMap.get(networkCols[2]);
        		if (targetNode == null) {
        			targetNode =  new Node(networkCols[2]);
        			table.addRow(targetNode);
        			nMap.put(networkCols[2], targetNode);
        			newNetwork.addNode(targetNode);
        		}
            	
        		newNetwork.addEdge(sourceNode, targetNode, true);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
		finally{
			newNetwork.setInteractionType(interac);
		}
		
		return newNetwork;
	}
}

