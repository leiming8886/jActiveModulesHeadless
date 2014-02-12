/*
 File: RankScaler.java

 Copyright (c) 2010, The Cytoscape Consortium (www.cytoscape.org)

 This library is free software; you can redistribute it and/or modify it
 under the terms of the GNU Lesser General Public License as published
 by the Free Software Foundation; either version 2.1 of the License, or
 any later version.

 This library is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. The software and
 documentation provided hereunder is on an "as is" basis, and the
 Institute for Systems Biology and the Whitehead Institute
 have no obligations to provide maintenance, support,
 updates, enhancements or modifications. In no event shall the
 Institute for Systems Biology and the Whitehead Institute
 be liable to any party for direct, indirect, special,
 incidental or consequential damages, including lost profits, arising
 out of the use of this software and its documentation, even if the
 Institute for Systems Biology and the Whitehead Institute
 have been advised of the possibility of such damage. See
 the GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package csplugins.jActiveModulesHeadless.util;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Used to scale a list of values to [a,b]
 */
class RankScaler extends AbstractScaler {
	private final long negativeZeroBitsLong = Double.doubleToRawLongBits(-0.0);
	private final int negativeZeroBitsInt = Float.floatToRawIntBits(-0.0f);

	public double[] scale(final double values[], final double a, final double b)
			throws IllegalArgumentException {
		if (values.length < 2)
			throw new IllegalArgumentException(
					"need at least 2 values for scaling!");
		if (a >= b)
			throw new IllegalArgumentException("bad bounds!");

		final double sortedValues[] = values.clone();
		Arrays.sort(sortedValues);

		final HashMap<Double, Double> origValueToRankValueMap = new HashMap<Double, Double>();
		final double stepSize = (b - a) / values.length;
		double currentValue = sortedValues[0];
		double sum = stepSize / 2.0;
		double count = 1.0;
		for (int i = 1; i < values.length; ++i) {
			final double currentRankValue = stepSize * (0.5 + i);
			if (sortedValues[i] == currentValue
					&& ((Double.doubleToRawLongBits(sortedValues[i]) | Double
							.doubleToRawLongBits(currentValue)) != negativeZeroBitsLong)) {
				++count;
				sum += currentRankValue;
			} else {
				origValueToRankValueMap.put(currentValue, sum / count);
				currentValue = sortedValues[i];
				sum = currentRankValue;
				count = 1.0;
			}
		}
		origValueToRankValueMap.put(sortedValues[values.length - 1], sum
				/ count);

		final double[] scaledValues = new double[values.length];
		for (int i = 0; i < values.length; ++i)
			scaledValues[i] = origValueToRankValueMap.get(values[i]);

		return scaledValues;
	}

	public float[] scale(final float values[], final float a, final float b)
			throws IllegalArgumentException {
		if (values.length < 2)
			throw new IllegalArgumentException(
					"need at least 2 values for scaling!");
		if (a >= b)
			throw new IllegalArgumentException("bad bounds!");

		final float sortedValues[] = values.clone();
		Arrays.sort(sortedValues);

		final HashMap<Float, Float> origValueToRankValueMap = new HashMap<Float, Float>();
		final float stepSize = (b - a) / values.length;
		float currentValue = sortedValues[0];
		float sum = stepSize / 2.0f;
		float count = 1.0f;
		for (int i = 1; i < values.length; ++i) {
			final float currentRankValue = stepSize * (0.5f + i);
			if (sortedValues[i] == currentValue
					&& ((Float.floatToRawIntBits(sortedValues[i]) | Float
							.floatToRawIntBits(currentValue)) != negativeZeroBitsInt)) {
				++count;
				sum += currentRankValue;
			} else {
				origValueToRankValueMap.put(currentValue, sum / count);
				currentValue = sortedValues[i];
				sum = currentRankValue;
				count = 1.0f;
			}
		}
		origValueToRankValueMap.put(sortedValues[values.length - 1], sum
				/ count);

		final float[] scaledValues = new float[values.length];
		for (int i = 0; i < values.length; ++i)
			scaledValues[i] = origValueToRankValueMap.get(values[i]);

		return scaledValues;
	}
}
