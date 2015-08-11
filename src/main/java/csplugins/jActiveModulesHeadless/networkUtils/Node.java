package csplugins.jActiveModulesHeadless.networkUtils;

/*
 * #%L
 * Cytoscape Model Impl (model-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2008 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */



public class Node implements Comparable<Node>{
	
	
	private Network nestedNet;
	private final Long suid;
	private double pvalue;
	private String nodeName;
	
	public Node() {
		suid = SUIDFactory.getNextSUID();
		nestedNet = null;
		pvalue = 0.0;
		nodeName = "";
	}
	
	public Node(String name) {
		suid = SUIDFactory.getNextSUID();
		nestedNet = null;
		pvalue = 0.0;
		nodeName = name;
	}

	final public Long getSUID() {
		return suid;
	}

	public String getName()
	{
		return nodeName;
	}
	public void setPvalue(double pvalue)
	{
		this.pvalue = pvalue;
	}
	
	public double getPvalue()
	{
		return pvalue;
	}
	
	public int hashCode() {
		final int prime = 17;
		int result = 1;
		result = prime * result + (int) (suid ^ (suid >>> 32));
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (! (obj instanceof Node))
			return false;
		Node other = (Node) obj;
		return (suid == other.suid);
	}
	/**
	 * @see org.cytoscape.model.CyNode#getNetworkPointer()
	 */
	public synchronized Network getNetworkPointer() {
		return nestedNet;
	}

	/**
	 * @see org.cytoscape.model.CyNode#setNetworkPointer(CyNetwork)
	 */
	public void setNetworkPointer(final Network n) {
		final Network orig; 
	
		synchronized (this) {
			orig = nestedNet;
			if (n == nestedNet)
				return;
			else
				nestedNet = n;
		}

		
	}
	
//	public int compareTo(Object other){
//	    long this_suid,other_suid;
//	    this_suid = this.suid;
//	    other_suid = ((Node)other).suid;
//	    if(this_suid < other_suid){
//	      return 1;
//	    }
//	    else if(this_suid > other_suid){
//	      return -1;
//	    }
//	    else{
//	      return 0;
//	    }
//		
//	  }
	
	public String toString() {
		return "Node suid: " + getSUID();
	}

	@Override
	public int compareTo(Node o) {
		 long this_suid,other_suid;
		    this_suid = this.suid;
		    other_suid = ((Node)o).suid;
		    if(this_suid < other_suid){
		      return 1;
		    }
		    else if(this_suid > other_suid){
		      return -1;
		    }
		    else{
		      return 0;
		    }
			
	}
}
