package com.osrsgoals.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Plans a route to a level. Unlike GoalCalculator (one method for the whole way), the fastest path
 * may switch methods as you level, e.g. "78-80 with A, then 80-82 with B".
 */
public final class Planner
{
	private Planner()
	{
	}

	public static final class Segment
	{
		public final TrainingMethod method;
		public final int fromLevel;
		public final int toLevel;
		public final double hours;
		/** GP gained (positive) or spent (negative) over this stretch; null if the wiki lists none. */
		public final Double gp;

		Segment(TrainingMethod method, int fromLevel, int toLevel, double hours, Double gp)
		{
			this.method = method;
			this.fromLevel = fromLevel;
			this.toLevel = toLevel;
			this.hours = hours;
			this.gp = gp;
		}
	}

	public static final class PathResult
	{
		public final List<Segment> segments;
		/** False if some level has no method with a wiki rate. */
		public final boolean complete;
		public final long xpNeeded;
		public final double hours;
		public final Double gpTotal;
		/** The level at which no usable method was found, or 0. */
		public final int blockedAtLevel;

		PathResult(List<Segment> segments, boolean complete, long xpNeeded, double hours, Double gpTotal,
			int blockedAtLevel)
		{
			this.segments = segments;
			this.complete = complete;
			this.xpNeeded = xpNeeded;
			this.hours = hours;
			this.gpTotal = gpTotal;
			this.blockedAtLevel = blockedAtLevel;
		}
	}

	/** The best single method (first in the ranking that you can use and that has a known time), or null. */
	public static MethodResult best(List<TrainingMethod> methods, int currentXp, int targetLevel, Playstyle style,
		SortMode sort)
	{
		for (MethodResult r : GoalCalculator.rank(methods, currentXp, targetLevel, style, sort))
		{
			if (r.eligible && r.hoursNeeded != null)
			{
				return r;
			}
		}
		return null;
	}

	/** Picks the fastest usable method for every level on the way and merges the stretches. */
	public static PathResult fastestPath(List<TrainingMethod> methods, int currentXp, int targetLevel,
		Playstyle style)
	{
		int currentLevel = XpTable.levelForXp(currentXp);
		long needed = GoalCalculator.xpNeeded(currentXp, targetLevel);

		List<Segment> segments = new ArrayList<>();
		TrainingMethod open = null;
		int openFrom = 0;
		int openTo = 0;
		double openHours = 0;
		double openGp = 0;
		boolean openGpKnown = true;
		double totalHours = 0;
		boolean complete = true;
		int blockedAt = 0;

		for (int level = currentLevel; level < targetLevel; level++)
		{
			long from = Math.max(currentXp, XpTable.xpForLevel(level));
			long slice = (long) XpTable.xpForLevel(level + 1) - from;
			if (slice <= 0)
			{
				continue;
			}

			TrainingMethod pick = null;
			double pickHours = Double.MAX_VALUE;
			Double pickGp = null;
			for (TrainingMethod method : methods)
			{
				if (method.minLevel > level || (method.maxLevel != null && level >= method.maxLevel))
				{
					continue;
				}
				Double rate = GoalCalculator.rateAt(method.rates, level, GoalCalculator.xpField(style));
				if (rate == null || rate <= 0)
				{
					continue;
				}
				double hours = slice / rate;
				if (hours < pickHours)
				{
					pick = method;
					pickHours = hours;
					Double gpRate = GoalCalculator.rateAt(method.rates, level, p -> p.gpPerHour);
					pickGp = gpRate == null ? null : gpRate * hours;
				}
			}

			if (pick == null)
			{
				complete = false;
				blockedAt = level;
				break;
			}

			if (pick != open)
			{
				if (open != null)
				{
					segments.add(new Segment(open, openFrom, openTo, openHours, openGpKnown ? openGp : null));
				}
				open = pick;
				openFrom = level;
				openHours = 0;
				openGp = 0;
				openGpKnown = true;
			}
			openTo = level + 1;
			openHours += pickHours;
			if (pickGp == null)
			{
				openGpKnown = false;
			}
			else
			{
				openGp += pickGp;
			}
			totalHours += pickHours;
		}
		if (open != null)
		{
			segments.add(new Segment(open, openFrom, openTo, openHours, openGpKnown ? openGp : null));
		}

		Double gpTotal = null;
		if (complete)
		{
			double sum = 0;
			boolean known = true;
			for (Segment s : segments)
			{
				if (s.gp == null)
				{
					known = false;
				}
				else
				{
					sum += s.gp;
				}
			}
			gpTotal = known ? sum : null;
		}
		return new PathResult(segments, complete, needed, totalHours, gpTotal, blockedAt);
	}
}
