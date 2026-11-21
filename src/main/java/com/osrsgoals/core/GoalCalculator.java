package com.osrsgoals.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Turns "I am at X XP and want level N" into a ranked list of methods with time and cost.
 * Pure Java (no RuneLite), so it can be unit tested anywhere.
 *
 * Time is worked out one level at a time, using the rate for the level you are at while you
 * earn that slice of XP, because wiki rates rise as you level.
 */
public final class GoalCalculator
{
	private GoalCalculator()
	{
	}

	/** XP still needed to reach targetLevel from currentXp (never negative). */
	public static long xpNeeded(int currentXp, int targetLevel)
	{
		long gap = (long) XpTable.xpForLevel(targetLevel) - currentXp;
		return Math.max(0, gap);
	}

	public static MethodResult evaluate(TrainingMethod method, int currentXp, int targetLevel, Playstyle style)
	{
		int currentLevel = XpTable.levelForXp(currentXp);
		long needed = xpNeeded(currentXp, targetLevel);

		if (currentLevel < method.minLevel)
		{
			return new MethodResult(method, false, "Needs level " + method.minLevel, needed,
				null, null, null, null);
		}

		if (method.maxLevel != null && currentLevel >= method.maxLevel)
		{
			return new MethodResult(method, false, "Only for levels below " + method.maxLevel, needed,
				null, null, null, null);
		}

		Function<RateBreakpoint, Double> xpField = xpField(style);
		Function<RateBreakpoint, Double> gpField = p -> p.gpPerHour;

		double hours = 0;
		double gp = 0;
		boolean hoursKnown = true;
		boolean gpKnown = true;

		for (int level = currentLevel; level < targetLevel; level++)
		{
			long from = Math.max(currentXp, XpTable.xpForLevel(level));
			long slice = (long) XpTable.xpForLevel(level + 1) - from;
			if (slice <= 0)
			{
				continue;
			}

			Double xpRate = (method.maxLevel != null && level >= method.maxLevel)
				? null : rateAt(method.rates, level, xpField);
			if (xpRate == null || xpRate <= 0)
			{
				hoursKnown = false;
				gpKnown = false;
				break;
			}
			double sliceHours = slice / xpRate;
			hours += sliceHours;

			Double gpRate = rateAt(method.rates, level, gpField);
			if (gpRate == null)
			{
				gpKnown = false;
			}
			else if (gpKnown)
			{
				gp += gpRate * sliceHours;
			}
		}

		Double hoursResult = hoursKnown ? hours : null;
		Double xpPerHour = (hoursKnown && hours > 0) ? needed / hours : null;
		Double gpTotal = (hoursKnown && gpKnown) ? gp : null;
		Double gpPerHour = (gpTotal != null && hours > 0) ? gpTotal / hours : null;
		return new MethodResult(method, true, null, needed, xpPerHour, hoursResult, gpPerHour, gpTotal);
	}

	/** Evaluates every method; usable ones first, then by the chosen sort, unknowns last. */
	public static List<MethodResult> rank(List<TrainingMethod> methods, int currentXp, int targetLevel,
		Playstyle style, SortMode sort)
	{
		List<MethodResult> results = new ArrayList<>();
		for (TrainingMethod method : methods)
		{
			results.add(evaluate(method, currentXp, targetLevel, style));
		}

		Comparator<MethodResult> byEligible = Comparator.comparing(r -> !r.eligible);
		Comparator<MethodResult> bySort;
		if (sort == SortMode.MOST_PROFIT)
		{
			bySort = Comparator
				.comparing((MethodResult r) -> r.netGpTotal == null)
				.thenComparing(r -> r.netGpTotal == null ? 0.0 : -r.netGpTotal);
		}
		else
		{
			bySort = Comparator
				.comparing((MethodResult r) -> r.hoursNeeded == null)
				.thenComparing(r -> r.hoursNeeded == null ? 0.0 : r.hoursNeeded);
		}
		results.sort(byEligible.thenComparing(bySort).thenComparing(r -> r.method.name));
		return results;
	}

	/**
	 * Linear interpolation of a rate between the listed levels. Below the first listed level or above
	 * the last, the nearest listed value is used (no extrapolation). Null if no level lists the field.
	 */
	static Double rateAt(List<RateBreakpoint> points, int level, Function<RateBreakpoint, Double> field)
	{
		RateBreakpoint lower = null;
		RateBreakpoint upper = null;
		for (RateBreakpoint p : points)
		{
			Double value = field.apply(p);
			if (value == null)
			{
				continue;
			}
			if (p.level <= level && (lower == null || p.level > lower.level))
			{
				lower = p;
			}
			if (p.level >= level && (upper == null || p.level < upper.level))
			{
				upper = p;
			}
		}
		if (lower == null && upper == null)
		{
			return null;
		}
		if (lower == null)
		{
			return field.apply(upper);
		}
		if (upper == null || upper.level == lower.level)
		{
			return field.apply(lower);
		}
		double fraction = (double) (level - lower.level) / (upper.level - lower.level);
		return field.apply(lower) + fraction * (field.apply(upper) - field.apply(lower));
	}

	static Function<RateBreakpoint, Double> xpField(Playstyle style)
	{
		switch (style)
		{
			case AFK:
				return p -> p.afk;
			case ACTIVE:
				return p -> p.active;
			default:
				return p ->
				{
					if (p.afk == null)
					{
						return p.active;
					}
					if (p.active == null)
					{
						return p.afk;
					}
					return Math.max(p.afk, p.active);
				};
		}
	}

	/** Formats hours as "12h 30m". */
	public static String formatHours(double hours)
	{
		long totalMinutes = Math.round(hours * 60);
		return (totalMinutes / 60) + "h " + (totalMinutes % 60) + "m";
	}
}
