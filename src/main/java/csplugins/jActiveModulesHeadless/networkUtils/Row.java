package csplugins.jActiveModulesHeadless.networkUtils;

import java.util.HashMap;
import java.util.Map;





public class Row {
	
	private String name;
	
	private long suid;
	
	private double data [];
	
	public Row(Node node)
	{
		name = node.getName();
		suid = node.getSUID();
		
		data = null;
	}
	
	public void setDataSize(int size)
	{
		data = new double[size];
	}
	
	public void setDataColumn( int index, double value)
	{
		if(index <data.length )
			data[index] = value;
	}
	
	public double[] getData()
	{
		return data;
	}
	
	public double getDataColumn(int index)
	{
		double result = 0;
		if(index <data.length )
			result = data[index] ;
		
		return result;
	}
	
	public int getDataSize()
	{
		if( data == null)
			return 0;
		else
			return data.length;
	}
	
	public String getName()
	{
		return name;
	}
	
}

