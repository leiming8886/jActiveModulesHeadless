/*
  File: AbstractScaler.java

  Copyright (c) 2010, The Cytoscape Consortium (www.cytoscape.org)

  This library is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as published
  by the Free Software Foundation; either version 2.1 of the License, or
  any later version.

  This library is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
  MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
  documentation provided hereunder is on an "as is" basis, and the
  Institute for Systems Biology and the Whitehead Institute
  have no obligations to provide maintenance, support,
  updates, enhancements or modifications.  In no event shall the
  Institute for Systems Biology and the Whitehead Institute
  be liable to any party for direct, indirect, special,
  incidental or consequential damages, including lost profits, arising
  out of the use of this software and its documentation, even if the
  Institute for Systems Biology and the Whitehead Institute
  have been advised of the possibility of such damage.  See
  the GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
*/
package csplugins.jActiveModulesHeadless.util;


import java.util.AbstractCollection;


/**
 *  Used to scale a list of values to [a,b]
 */
public abstract class AbstractScaler implements Scaler {
	public abstract double[] scale(final double values[], final double a, final double b) throws IllegalArgumentException;

	public final double[] scale(final AbstractCollection<Double> values, final double a,
				    final double b) throws IllegalArgumentException
	{
		// Convert the collection to an array:
		final double[] array = new double[values.size()];
		int i = 0;
		for (final Double d : values)
			array[i++] = d;

		return scale(array, a, b);
	}

	public abstract float[] scale(final float values[], final float a, final float b) throws IllegalArgumentException;

	public final float[] scale(final AbstractCollection<Float> values, final float a,
				   final float b) throws IllegalArgumentException
	{
		// Convert the collection to an array:
		final float[] array = new float[values.size()];
		int i = 0;
		for (final Float f : values)
			array[i++] = f;

		return scale(array, a, b);
	}
}
