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

import java.util.Map;


public class Edge  {
	final private Node source;
	final private Node target;
	final private boolean directed;
	private final Long suid;
	
	/**
	 * A String column created by default for every CyEdge that
	 * holds the interaction description of the edge. 
	 */
	String INTERACTION = "interaction";

	/**
	 * The Type enum is used by methods in {@link CyNetwork} to restrict
	 * the edges that match a query. 
	 */
	public enum Type {

		/**
		 * matches only undirected edges
		 */
		UNDIRECTED,

		/**
		 * matches either undirected edges or directed edges that end with this node</li>
		 */
		INCOMING,

		/**
		 * matches either undirected edges or directed edges that start with this node</li>
		 */
		OUTGOING,

		/**
		 * matches directed edges regardless of whether this node is the source or the target
		 */
		DIRECTED,

		/**
	 	 * matches any edge
		 */
		ANY;
	}

	Edge( Node src, Node tgt, boolean dir, long ind) {
		suid = SUIDFactory.getNextSUID();
		source = src;
		target = tgt;
		directed = dir;
	}

	final public Long getSUID() {
		return suid;
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
		Edge other = (Edge) obj;
		return (suid == other.suid);
	}

	/**
	 * @see org.cytoscape.model.CyEdge#getSource()
	 */
	
	public Node getSource() {
		return source;
	}

	/**
	 * @see org.cytoscape.model.CyEdge#getTarget()
	 */
	
	public Node getTarget() {
		return target;
	}

	/**
	 * @see org.cytoscape.model.CyEdge#isDirected()
	 */
	
	public boolean isDirected() {
		return directed;
	}

	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("source: ");
		sb.append(source.toString());
		sb.append("  target: ");
		sb.append(target.toString());
		sb.append("  directed: ");
		sb.append(Boolean.toString(directed));

		return sb.toString();
	}
}
