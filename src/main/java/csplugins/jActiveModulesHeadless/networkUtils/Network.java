package csplugins.jActiveModulesHeadless.networkUtils;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;



/**
 * This class is a CySubnetwork Implementation without tables
 */
 public class Network  {

	// Unique ID for this
	private final Long suid;

	private final Map<Long, NodePointer> nodePointers;
	private final Map<Long, EdgePointer> edgePointers;
	
	private final Map<String, Node> nodeMap;

	private final AtomicInteger nodeCount;
	private final AtomicInteger edgeCount;
	
	private Map<Node,List<Node>> neighborsMap;

	private NodePointer firstNode;
	
	private NodeTable nodeTable;
	
	private String name;
	
	private double score;

	public Network() {
		this.suid = SUIDFactory.getNextSUID();

		nodeCount = new AtomicInteger(0);
		edgeCount = new AtomicInteger(0);
		firstNode = null;
		nodePointers = new ConcurrentHashMap<Long, NodePointer>();
		edgePointers = new ConcurrentHashMap<Long, EdgePointer>();
		nodeMap = new HashMap<String, Node>();
		nodeTable = new NodeTable();
		name = "";
		score = 0.0;
		neighborsMap = null;
	}

	/**
	 * This is an constant and stateless.
	 */
	public Long getSUID() {
		return suid;
	}

	public NodeTable getNodeTable() {
		return nodeTable;
	}
	
	public Row getRow(Node node) {
		return nodeTable.getRow(node);
	}
	
	public Node getNode(String nodeName) {
		return nodeMap.get(nodeName);
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String networkName) {
		name = networkName;
	}
	
	public double getScore() {
		return score;
	}
	
	public void setScore(double score) {
		this.score = score;
	}
	
	public Collection<Node> getAllNodes() {
		return nodeMap.values();
	}
	/**
	 * Atomic & thread-safe.
	 */
	public int getNodeCount() {
		return nodeCount.get();
	}

	/**
	 * Atomic & thread-safe.
	 */
	public int getEdgeCount() {
		return edgeCount.get();
	}

	public Edge getEdge(final long e) {
		final EdgePointer ep = edgePointers.get(e);
		if (ep != null)
			return ep.Edge;
		else
			return null;
	}

	public Node getNode(final long n) {
		final NodePointer np = nodePointers.get(n);
		if (np != null)
			return np.Node;
		else
			return null;
	}

	
	public List<Node> getNodeList() {
		final List<Node> ret = new ArrayList<Node>(nodeCount.get());
		int numRemaining = nodeCount.get();
		NodePointer node = firstNode;

		while (numRemaining > 0) {
			// possible NPE here if the linked list isn't constructed correctly
			// this is the correct behavior
			final Node toAdd = node.Node;
			node = node.nextNode;
			ret.add(toAdd);
			numRemaining--;
		}

		return ret;
	}
	
	
	public List<Edge> getEdgeList() {
		final List<Edge> ret = new ArrayList<Edge>(edgeCount.get());
		EdgePointer edge = null;

		int numRemaining = edgeCount.get();
		NodePointer node = firstNode;
		while (numRemaining > 0) {
			final Edge retEdge;

			if (edge != null) {
				retEdge = edge.Edge;
			} else {
				for (edge = node.firstOutEdge; edge == null; node = node.nextNode, edge = node.firstOutEdge)
					;

				node = node.nextNode;
				retEdge = edge.Edge;
			}

			edge = edge.nextOutEdge;
			numRemaining--;

			ret.add(retEdge);
		}

		return ret;
	}

	public List<Node> getNeighborList(final Node n, final Edge.Type e) {
		if (!containsNode(n))
			return Collections.emptyList();

		final NodePointer np = getNodePointer(n);
		final List<Node> ret = new ArrayList<Node>(countEdges(np, e));
		final Iterator<EdgePointer> it = edgesAdjacent(np, e);
		while (it.hasNext()) {
			final EdgePointer edge = it.next();
			final long neighborIndex = np.index ^ edge.source.index ^ edge.target.index;
			ret.add(getNode(neighborIndex));
		}

		return ret;
	}
	
	public List<Node> getNeighborList(final Node n) {

		List<Node> tempList = null;
		if(neighborsMap != null)
			tempList = neighborsMap.get(n);
		
		if(tempList == null)
			return getNeighborList(n, Edge.Type.ANY);
		else
			return neighborsMap.get(n);
	}
	
	public void generateNeighborList()
	{
		Node nodeTemp;
		neighborsMap = new HashMap<Node,List<Node>>(nodeCount.get());
		Iterator<Node> nodeIterator = getNodeList().iterator();
		
		while(nodeIterator.hasNext()){
			nodeTemp = (Node) nodeIterator.next();
			neighborsMap.put(nodeTemp, getNeighborList(nodeTemp,Edge.Type.ANY));
		}
		
	}

	public List<Edge> getAdjacentEdgeList(final Node n, final Edge.Type e) {
		if (!containsNode(n))
			return Collections.emptyList();

		final NodePointer np = getNodePointer(n);
		final List<Edge> ret = new ArrayList<Edge>(countEdges(np, e));
		final Iterator<EdgePointer> it = edgesAdjacent(np, e);

		while (it.hasNext()) {
			ret.add(it.next().Edge);
		}

		return ret;
	}

	public Iterable<Edge> getAdjacentEdgeIterable(final Node n, final Edge.Type e) {
		if (!containsNode(n))
			return Collections.emptyList();

		final NodePointer np = getNodePointer(n);
		return new IterableEdgeIterator(edgesAdjacent(np, e));
	}

	private final class IterableEdgeIterator implements Iterator<Edge>, Iterable<Edge> {
		private final Iterator<EdgePointer> epIterator;

		IterableEdgeIterator(final Iterator<EdgePointer> epIterator) {
			this.epIterator = epIterator;
		}

		public Edge next() {
			return epIterator.next().Edge;
		}

		public boolean hasNext() {
			return epIterator.hasNext();
		}

		public void remove() {
			epIterator.remove();
		}

		public Iterator<Edge> iterator() {
			return this;
		}
	}

	
	public List<Edge> getConnectingEdgeList(final Node src, final Node trg, final Edge.Type e) {
		if (!containsNode(src))
			return Collections.emptyList();

		if (!containsNode(trg))
			return Collections.emptyList();

		final NodePointer srcP = getNodePointer(src);
		final NodePointer trgP = getNodePointer(trg);

		final List<Edge> ret = new ArrayList<Edge>(Math.min(countEdges(srcP, e), countEdges(trgP, e)));
		final Iterator<EdgePointer> it = edgesConnecting(srcP, trgP, e);

		while (it.hasNext())
			ret.add(it.next().Edge);

		return ret;
	}

	/**
	 * IMPORTANT: this is not protected by synchronized because caller always
	 * uses lock.
	 */
	private final Node addNodeInternal(final Node node) {
		// node already exists in this network
		if (containsNode(node))
			return node;

		NodePointer n = new NodePointer(node);
		nodePointers.put(node.getSUID(), n);
		nodeCount.incrementAndGet();
		firstNode = n.insert(firstNode);
		nodeMap.put(node.getName(), node);

		return node;
	}

	private final boolean removeNodesInternal(final Collection<Node> nodes) {
		if (nodes == null || nodes.isEmpty())
			return false;

		boolean madeChanges = false;
		synchronized (this) {
			for (Node n : nodes) {
				if (!containsNode(n))
					continue;

				// remove adjacent edges from network
				removeEdgesInternal(getAdjacentEdgeList(n, Edge.Type.ANY));

				final NodePointer node = (NodePointer) nodePointers.get(n.getSUID());
				nodePointers.remove(n.getSUID());
				firstNode = node.remove(firstNode);

				nodeCount.decrementAndGet();
				madeChanges = true;
			}
		}
		return madeChanges;
	}

	private final Edge addEdgeInternal(final Node s, final Node t, final boolean directed, final Edge edge) {

		final EdgePointer e;

		synchronized (this) {
			// here we check with possible sub node, not just root node
			if (!containsNode(s))
				throw new IllegalArgumentException("source node is not a member of this network");

			// here we check with possible sub node, not just root node
			if (!containsNode(t))
				throw new IllegalArgumentException("target node is not a member of this network");

			// edge already exists in this network
			if (containsEdge(edge))
				return edge;

			final NodePointer source = getNodePointer(s);
			final NodePointer target = getNodePointer(t);

			e = new EdgePointer(source, target, directed, edge);

			edgePointers.put(edge.getSUID(), e);
			edgeCount.incrementAndGet();
		}
		return edge;
	}

	private final boolean removeEdgesInternal(final Collection<Edge> edges) {
		if (edges == null || edges.isEmpty())
			return false;

		boolean madeChanges = false;
		synchronized (this) {
			for (Edge edge : edges) {
				if (!containsEdge(edge))
					continue;

				final EdgePointer e = (EdgePointer) edgePointers.get(edge.getSUID());
				edgePointers.remove(edge.getSUID());

				e.remove();

				edgeCount.decrementAndGet();
				madeChanges = true;
			}
		}

		return madeChanges;
	}

	
	public boolean containsNode(final Node node) {
		if (node == null)
			return false;

		final NodePointer thisNode;

		//synchronized (this) {
			thisNode = (NodePointer) nodePointers.get(node.getSUID());
		//}

		if (thisNode == null)
			return false;

		return thisNode.Node.equals(node);
	}

	
	public boolean containsEdge(final Edge edge) {
		if (edge == null)
			return false;

		final EdgePointer thisEdge;

		//synchronized (this) {
			thisEdge = (EdgePointer) edgePointers.get(edge.getSUID());
		//}

		if (thisEdge == null)
			return false;

		return thisEdge.Edge.equals(edge);
	}

	
	
	public boolean containsEdge(final Node n1, final Node n2) {
		// System.out.println("private containsEdge");
		if (!containsNode(n1)) {
			// System.out.println("private containsEdge doesn't contain node1 "
			// + inId);
			return false;
		}

		if (!containsNode(n2)) {
			// System.out.println("private containsEdge doesn't contain node2 "
			// + inId);
			return false;
		}

		final Iterator<EdgePointer> it = edgesConnecting(getNodePointer(n1), getNodePointer(n2), Edge.Type.ANY);

		return it.hasNext();
	}

	private final Iterator<EdgePointer> edgesAdjacent(final NodePointer n, final Edge.Type edgeType) {
		assert (n != null);

		final EdgePointer[] edgeLists;

		final boolean incoming = assessIncoming(edgeType);
		final boolean outgoing = assessOutgoing(edgeType);
		final boolean undirected = assessUndirected(edgeType);

		if (undirected || (outgoing && incoming))
			edgeLists = new EdgePointer[] { n.firstOutEdge, n.firstInEdge };
		else if (outgoing) // Cannot also be incoming.
			edgeLists = new EdgePointer[] { n.firstOutEdge, null };
		else if (incoming) // Cannot also be outgoing.
			edgeLists = new EdgePointer[] { null, n.firstInEdge };
		else
			// All boolean input parameters are false - can never get here!
			edgeLists = new EdgePointer[] { null, null };

		final int inEdgeCount = countEdges(n, edgeType);
		// System.out.println("edgesAdjacent edgeCount: " + inEdgeCount);

		return new Iterator<EdgePointer>() {
			private int numRemaining = inEdgeCount;
			private int edgeListIndex = -1;
			private EdgePointer edge;

			public boolean hasNext() {
				return numRemaining > 0;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}

			public EdgePointer next() {
				// get the first non-null edgePointer
				while (edge == null)
					edge = edgeLists[++edgeListIndex];

				long returnIndex = -1;

				// look at outgoing edges
				if (edgeListIndex == 0) {
					// go to the next edge if the current edge is NOT either
					// directed when we want outgoing or undirected when we
					// want undirected
					while ((edge != null) && !((outgoing && edge.directed) || (undirected && !edge.directed))) {
						edge = edge.nextOutEdge;

						// we've hit the last edge in the list
						// so increment edgeListIndex so we go to
						// incoming, set edge, and break
						if (edge == null) {
							edge = edgeLists[++edgeListIndex];
							break;
						}
					}

					// if we have a non-null outgoing edge set the
					// edge and return values
					// since edgeListIndex is still for outgoing we'll
					// just directly to the return
					if ((edge != null) && (edgeListIndex == 0)) {
						returnIndex = edge.index;
						edge = edge.nextOutEdge;
					}
				}

				// look at incoming edges
				if (edgeListIndex == 1) {

					// Important NOTE!!!
					// Possible null pointer exception here if numRemaining,
					// i.e. edgeCount is wrong. However, this is probably the
					// correct behavior since it means the linked lists are
					// messed up and there isn't a graceful way to deal.

					// go to the next edge if the edge is a self edge AND
					// either directed when we're looking for outgoing or
					// undirected when we're looking for undirected
					// OR
					// go to the next edge if the current edge is NOT either
					// directed when we want incoming or undirected when we
					// want undirected
					while (((edge.source.index == edge.target.index) && ((outgoing && edge.directed) || (undirected && !edge.directed)))
							|| !((incoming && edge.directed) || (undirected && !edge.directed))) {
						edge = edge.nextInEdge;
					}

					returnIndex = edge.index;
					edge = edge.nextInEdge;
				}

				numRemaining--;
				return (EdgePointer) edgePointers.get(returnIndex);
			}
		};
	}

	private final Iterator<EdgePointer> edgesConnecting(final NodePointer node0, final NodePointer node1, final Edge.Type et) {
		assert (node0 != null);
		assert (node1 != null);

		final Iterator<EdgePointer> theAdj;
		final long nodeZero;
		final long nodeOne;

		// choose the smaller iterator
		if (countEdges(node0, et) <= countEdges(node1, et)) {
			// System.out.println("edgesConnecting fewer edges node0: " +
			// node0.index);
			theAdj = edgesAdjacent(node0, et);
			nodeZero = node0.index;
			nodeOne = node1.index;
		} else {
			// System.out.println("edgesConnecting fewer edges node1: " +
			// node1.index);
			theAdj = edgesAdjacent(node1, et);
			nodeZero = node1.index;
			nodeOne = node0.index;
		}

		return new Iterator<EdgePointer>() {
			private long nextIndex = -1;

			private void ensureComputeNext() {
				if (nextIndex != -1) {
					return;
				}

				while (theAdj.hasNext()) {
					final EdgePointer e = theAdj.next();

					if (nodeOne == (nodeZero ^ e.source.index ^ e.target.index)) {
						nextIndex = e.index;

						return;
					}
				}

				nextIndex = -2;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}

			public boolean hasNext() {
				ensureComputeNext();

				return (nextIndex >= 0);
			}

			public EdgePointer next() {
				ensureComputeNext();

				final long returnIndex = nextIndex;
				nextIndex = -1;

				return (EdgePointer) edgePointers.get(returnIndex);
			}
		};
	}

	private final boolean assessUndirected(final Edge.Type e) {
		return ((e == Edge.Type.UNDIRECTED) || (e == Edge.Type.ANY));
	}

	private final boolean assessIncoming(final Edge.Type e) {
		return ((e == Edge.Type.DIRECTED) || (e == Edge.Type.ANY) || (e == Edge.Type.INCOMING));
	}

	private final boolean assessOutgoing(final Edge.Type e) {
		return ((e == Edge.Type.DIRECTED) || (e == Edge.Type.ANY) || (e == Edge.Type.OUTGOING));
	}

	private final int countEdges(final NodePointer n, final Edge.Type edgeType) {
		assert (n != null);
		final boolean undirected = assessUndirected(edgeType);
		final boolean incoming = assessIncoming(edgeType);
		final boolean outgoing = assessOutgoing(edgeType);

		// System.out.println("countEdges un: " + undirected + " in: " +
		// incoming + " out: " + outgoing);

		int tentativeEdgeCount = 0;

		if (outgoing) {
			// System.out.println("  countEdges outgoing: " + n.outDegree);
			tentativeEdgeCount += n.outDegree;
		}

		if (incoming) {
			// System.out.println("  countEdges incoming: " + n.inDegree);
			tentativeEdgeCount += n.inDegree;
		}

		if (undirected) {
			// System.out.println("  countEdges undirected: " + n.undDegree);
			tentativeEdgeCount += n.undDegree;
		}

		if (outgoing && incoming) {
			// System.out.println("  countEdges out+in MINUS: " + n.selfEdges);
			tentativeEdgeCount -= n.selfEdges;
		}

		 //System.out.println("  countEdges final: " + tentativeEdgeCount);
		return tentativeEdgeCount;
	}

	private final NodePointer getNodePointer(final Node node) {
		assert (node != null);
		return (NodePointer) nodePointers.get(node.getSUID());
	}

	
	public boolean equals(final Object o) {
		if (!(o instanceof Network))
			return false;

		final Network ag = (Network) o;

		return ag.suid.longValue() == this.suid.longValue();
	}

	
	public int hashCode() {
		return (int) (suid.longValue() ^ (suid.longValue() >>> 32));
	}


	private final class NodePointer {
		final Node Node;
		final long index;

		NodePointer nextNode;
		NodePointer prevNode;
		EdgePointer firstOutEdge;
		EdgePointer firstInEdge;

		// The number of directed edges whose source is this node.
		int outDegree;

		// The number of directed edges whose target is this node.
		int inDegree;

		// The number of undirected edges which touch this node.
		int undDegree;

		// The number of directed self-edges on this node.
		int selfEdges;

		NodePointer(final Node cyn) {
			Node = cyn;
			index = cyn.getSUID();

			outDegree = 0;
			inDegree = 0;
			undDegree = 0;
			selfEdges = 0;

			firstOutEdge = null;
			firstInEdge = null;
		}

		NodePointer insert(final NodePointer next) {
			nextNode = next;
			if (next != null)
				next.prevNode = this;
			// return instead of:
			// next = this;
			return this;
		}

		NodePointer remove(final NodePointer first) {
			NodePointer ret = first;
			if (prevNode != null)
				prevNode.nextNode = nextNode;
			else
				ret = nextNode;

			if (nextNode != null)
				nextNode.prevNode = prevNode;

			nextNode = null;
			prevNode = null;
			firstOutEdge = null;
			firstInEdge = null;

			return ret;
		}
	}

	private static final class EdgePointer {
		final Edge Edge;
		final long index;
		final boolean directed;
		final NodePointer source;
		final NodePointer target;

		EdgePointer nextOutEdge;
		EdgePointer prevOutEdge;
		EdgePointer nextInEdge;
		EdgePointer prevInEdge;

		EdgePointer(final NodePointer s, final NodePointer t, final boolean dir, final Edge edge) {
			source = s;
			target = t;
			directed = dir;
			Edge = edge;
			index = edge.getSUID();

			nextOutEdge = null;
			prevOutEdge = null;

			nextInEdge = null;
			prevInEdge = null;

			insertSelf();
		}

		private void insertSelf() {

			nextOutEdge = source.firstOutEdge;

			if (source.firstOutEdge != null)
				source.firstOutEdge.prevOutEdge = this;

			source.firstOutEdge = this;

			nextInEdge = target.firstInEdge;

			if (target.firstInEdge != null)
				target.firstInEdge.prevInEdge = this;

			target.firstInEdge = this;

			if (directed) {
				source.outDegree++;
				target.inDegree++;
			} else {
				source.undDegree++;
				target.undDegree++;
			}

			// Self-edge
			if (source == target) {
				if (directed) {
					source.selfEdges++;
				} else {
					source.undDegree--;
				}
			}
		}

		void remove() {

			if (prevOutEdge != null)
				prevOutEdge.nextOutEdge = nextOutEdge;
			else
				source.firstOutEdge = nextOutEdge;

			if (nextOutEdge != null)
				nextOutEdge.prevOutEdge = prevOutEdge;

			if (prevInEdge != null)
				prevInEdge.nextInEdge = nextInEdge;
			else
				target.firstInEdge = nextInEdge;

			if (nextInEdge != null)
				nextInEdge.prevInEdge = prevInEdge;

			if (directed) {
				source.outDegree--;
				target.inDegree--;
			} else {
				source.undDegree--;
				target.undDegree--;
			}

			// Self-edge.
			if (source == target) {
				if (directed) {
					source.selfEdges--;
				} else {
					source.undDegree--;
				}
			}

			nextOutEdge = null; // ?? wasn't here in DynamicGraph
			prevOutEdge = null;
			nextInEdge = null;
			prevInEdge = null;
		}

	}


	
	public Node addNode() {
		final Node ret;
		synchronized (this) {
			ret=addNodeInternal(null);
		}

		return ret;
	}

	
	public boolean addNode(final Node node) {
		if (node == null)
			throw new NullPointerException("node is null");

		synchronized (this) {
			if (containsNode(node))
				return false;

			addNodeInternal(node);
		}

		return true;
	}

	
	public Edge addEdge(final Node source, final Node target, final boolean isDirected) {
		// important that it's edgeAdd and not addEdge
		final Edge ret;
			
		synchronized (this) {
			// then add the resulting Edge to this network
			ret = new Edge(source, target,isDirected, 0);
			addEdgeInternal(source,target,isDirected,ret);
		}

		return ret;
	}

	
	public boolean addEdge(final Edge edge) {
		if (edge == null)
			throw new NullPointerException("edge is null");

		synchronized (this) {
			if (containsEdge(edge))
				return false;

			// This will:
			// -- add the node if it doesn't already exist
			// -- do nothing if the node does exist
			// -- throw an exception if the node isn't part of the root network
			addNode(edge.getSource());
			addNode(edge.getTarget());

			// add edge
			addEdgeInternal(edge.getSource(),edge.getTarget(),edge.isDirected(),edge);
		}

		return true;
	}

	
	
	public boolean removeNodes(final Collection<Node> nodes) {
		if ( nodes == null || nodes.isEmpty() )
			return false;

		boolean ret = removeNodesInternal(nodes);

		return ret;
	}
	
	
	public boolean removeNodesKeepConnections(final Collection<Node> nodes) {
		if ( nodes == null || nodes.isEmpty() )
			return false;

		Node[] neighboors;
		for (Node n : nodes) {
			if (!containsNode(n))
				continue;
			neighboors = getNeighborList(n,Edge.Type.ANY).toArray(new Node[0]);
			System.out.println("Removing node: " + n.getName() + " neighbors size: " + neighboors.length);
			for(int i = 0; i <neighboors.length;i++ )
			{
				for(int j = (i+1); j <neighboors.length;j++)
				{
					if(!containsEdge(neighboors[i],neighboors[j]))
						addEdge(neighboors[i],neighboors[j],false);
				}
			}
		}
		boolean ret = removeNodesInternal(nodes);

		return ret;
	}

	
	public boolean removeEdges(final Collection<Edge> edges) {
		if ( edges == null || edges.isEmpty() )
			return false;

		boolean ret = removeEdgesInternal(edges);

		return ret;
	}

	
	
	
	public void dispose() {
	}

	
	


}
