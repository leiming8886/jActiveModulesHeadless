package csplugins.jActiveModulesHeadless;

import java.io.*;
import java.util.HashMap;
import java.util.Map;



import csplugins.jActiveModulesHeadless.networkUtils.*;

public class DataReader {
	
	private Map<String, Node> nMap;
	
	public DataReader()
	{
		this.nMap = new HashMap<String, Node>(10000);
	}
	
	
	public int readData(Network network, File dataFile, String delimiter)
	{
		String dataCols[];
		int lineNum = 0;
		int dataSize = 0;
		NodeTable table;
		Node node;
		
		table = network.getNodeTable();
		
		if(table == null)
			return dataSize;
		
		try {
            BufferedReader br = new BufferedReader(new FileReader(dataFile));
            String line = null;
            while ((line = br.readLine()) != null) {
            	dataCols = line.split(delimiter);
            	if(lineNum == 0)
            	{
	            	if(dataCols.length < 2)
	    		    	return dataSize;
	            	dataSize = dataCols.length-1;
	            	System.out.println("datSize: " +  dataSize);
             	}
            	else
            	{
            		
            		node = network.getNode(dataCols[0]);
            		
            		if( node != null)
            		{
            			//System.out.println("name: " + dataCols[0] + " value: " + dataCols[1]);
            			Row newRow = table.getRow(node);
            			newRow.setDataSize(dataSize);
            			for(int i =0;i< Math.min(dataCols.length -1 ,dataSize);i++)
            			{
            				newRow.setDataColumn(i, Double.parseDouble(dataCols[i+1]));
            				//System.out.println("name: " + dataCols[0] + " value: " + newRow.getDataColumn(i));
            			}
            			
            		}
            	}
        		
        		lineNum++;
            	
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return 0;
        }

		return dataSize;
	}
}

