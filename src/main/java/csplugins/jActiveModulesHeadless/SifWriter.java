package csplugins.jActiveModulesHeadless;

/*
 * #%L
 * Cytoscape IO Impl (io-impl)
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

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.List;


import csplugins.jActiveModulesHeadless.networkUtils.*;


public class SifWriter  {

	private static final String DEFAULT_INTERACTION = "-";
	private static final String ENCODING = "UTF-8";

	private final CharsetEncoder encoder;
	private final OutputStream outputStream;
	private final Network network;

	public SifWriter(final OutputStream outputStream, final Network network) {
		

		this.outputStream = outputStream;
		this.network = network;
		if(Charset.isSupported(ENCODING)) {
			// UTF-8 is supported by system
			this.encoder = Charset.forName(ENCODING).newEncoder();
		} else {
			// Use default.
			System.out.println("UTF-8 is not supported by this system.  This can be a problem for non-English annotations.");
			this.encoder = Charset.defaultCharset().newEncoder();
		}
	}

	public void writeSif() throws Exception {
		

		System.out.println("Encoding = " + encoder.charset());
		final OutputStreamWriter writer = new OutputStreamWriter(outputStream, encoder);

		final String lineSep = System.getProperty("line.separator");
		final List<Node> nodeList = network.getNodeList();

		for (Node node : nodeList) {
			
			
			final String sourceName = node.getName();
			final List<Edge> edges = network.getAdjacentEdgeList(node, Edge.Type.ANY);
			if(sourceName == null || sourceName.length() == 0)
				throw new IllegalStateException("This network contains null or empty node name.");
			

			if (edges.size() == 0) {
				writer.write(sourceName + lineSep);
			} else {
				for (final Edge edge : edges) {

					if (node == edge.getSource()) { 
						// Do only for outgoing edges
						final Node target = edge.getTarget();
						final String targetName = target.getName();
						if(targetName == null || targetName.length() == 0)
							throw new IllegalStateException("This network contains null or empty node name.");
						
						String interactionName = DEFAULT_INTERACTION;

						writer.write(sourceName);
						writer.write("\t");
						writer.write(interactionName);
						writer.write("\t");
						writer.write(targetName);
						writer.write(lineSep);
					}
					writer.flush();
				}
			}
		}

		writer.close();
		outputStream.close();
	}

	
}
