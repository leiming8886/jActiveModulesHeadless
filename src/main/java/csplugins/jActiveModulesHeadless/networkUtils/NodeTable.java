package csplugins.jActiveModulesHeadless.networkUtils;

import java.util.*;


import csplugins.jActiveModulesHeadless.networkUtils.*;

public class NodeTable {
	
	private Map<Node, Row> nMap;
	private int numCols;
	
	public NodeTable()
	{
		this.nMap = new HashMap<Node, Row>(10000);
		numCols = 0;
	}
	
	public void setNumColumns(int size)
	{
		numCols = size;
	}
	
	public int getNumColumns()
	{
		return numCols;
	}
	
	public Collection<Row> getAllRows()
	{
		return nMap.values();
	}
	
	public Row getRow(Node node)
	{
		return nMap.get(node);
		
	}
	
	public void addRow(Node node)
	{
		if(nMap.get(node) == null)
		{
			Row row = new Row(node);
			nMap.put(node, row);
		}
		
	}
	
	public void randomizeTable()
	{
		List<Node> nodesList = new ArrayList<Node> (nMap.keySet());
		int size = nodesList.size();
		int k;
		Row tempRow;
		Random rand = new Random();
		
		for (int l=size;l>0;l--){
	        k = rand.nextInt()%l;//random number between 0 and l(excluded).

	        
	        if (k!=(l-1)){//exchange node row [k] and node row [l-1] in nMap.
	        	tempRow =  nMap.get(nodesList.get(k));
	        	nMap.put(nodesList.get(k), nMap.get(nodesList.get(l-1)));
	        	nMap.put(nodesList.get(l-1), tempRow);

	            
	        }
	    }
		
	}
}

