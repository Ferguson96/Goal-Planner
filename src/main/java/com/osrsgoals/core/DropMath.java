package com.osrsgoals.core;

/** Drop-rate maths: expected kills, and how lucky or unlucky you have to be. */
public final class DropMath
{
	private DropMath()
	{
	}

	/** Average number of kills for one drop (1 / chance). */
	public static double expectedKills(double chance)
	{
		return chance > 0 ? 1.0 / chance : Double.POSITIVE_INFINITY;
	}

	/** Probability of at least one drop within the given number of kills. */
	public static double probabilityWithin(double chance, long kills)
	{
		if (chance <= 0 || kills <= 0)
		{
			return 0;
		}
		if (chance >= 1)
		{
			return 1;
		}
		return -Math.expm1(kills * Math.log1p(-chance));
	}

	/** Kills needed to have at least one drop with the given confidence (0..1), e.g. 0.9 for "90% of players". */
	public static long killsForConfidence(double chance, double confidence)
	{
		if (confidence <= 0)
		{
			return 0;
		}
		if (chance <= 0)
		{
			return Long.MAX_VALUE;
		}
		if (chance >= 1)
		{
			return 1;
		}
		double c = Math.min(confidence, 0.999999);
		return (long) Math.ceil(Math.log1p(-c) / Math.log1p(-chance));
	}

	/** Hours for a number of kills at a kills-per-hour rate; null if the rate is unknown. */
	public static Double hoursFor(double kills, Double killsPerHour)
	{
		if (killsPerHour == null || killsPerHour <= 0)
		{
			return null;
		}
		return kills / killsPerHour;
	}
}
