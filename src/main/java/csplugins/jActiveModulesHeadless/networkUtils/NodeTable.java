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
}

