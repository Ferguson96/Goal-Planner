package com.osrsgoals.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** The randomizer: picks the next goal to work on. */
public final class Wheel
{
	private Wheel()
	{
	}

	public static final class Entry
	{
		public final Goal goal;
		public final GoalStatus status;

		public Entry(Goal goal, GoalStatus status)
		{
			this.goal = goal;
			this.status = status;
		}
	}

	/** Goals that are not finished (and, optionally, that you can start now). */
	public static List<Entry> candidates(List<Entry> entries, boolean onlyDoable)
	{
		List<Entry> out = new ArrayList<>();
		for (Entry e : entries)
		{
			if (e.status.complete)
			{
				continue;
			}
			if (onlyDoable && !e.status.doableNow)
			{
				continue;
			}
			out.add(e);
		}
		return out;
	}

	static double weight(Entry e, WeightMode mode)
	{
		switch (mode)
		{
			case PRIORITY:
				return Math.max(1, Math.min(5, e.goal.priority));
			case QUICKEST:
				// Unknown times get a small fixed chance so they still come up now and then.
				return e.status.hoursRemaining == null ? 0.1 : 1.0 / (e.status.hoursRemaining + 1.0);
			default:
				return 1.0;
		}
	}

	/** Picks one goal at random, or null if nothing qualifies. */
	public static Entry pick(List<Entry> entries, WeightMode mode, boolean onlyDoable, Random random)
	{
		List<Entry> pool = candidates(entries, onlyDoable);
		if (pool.isEmpty())
		{
			return null;
		}
		double total = 0;
		for (Entry e : pool)
		{
			total += weight(e, mode);
		}
		double roll = random.nextDouble() * total;
		for (Entry e : pool)
		{
			roll -= weight(e, mode);
			if (roll < 0)
			{
				return e;
			}
		}
		return pool.get(pool.size() - 1);
	}
}
