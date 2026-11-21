package com.osrsgoals.core;

/**
 * The standard OSRS experience table. Pure Java (no RuneLite dependency) so it can be tested anywhere.
 */
public final class XpTable
{
	public static final int MAX_LEVEL = 99;
	public static final int MAX_XP = 200_000_000;

	private static final int[] XP_FOR_LEVEL = new int[MAX_LEVEL + 1];

	static
	{
		double points = 0;
		for (int level = 1; level <= MAX_LEVEL; level++)
		{
			XP_FOR_LEVEL[level] = (int) Math.floor(points / 4.0);
			points += Math.floor(level + 300.0 * Math.pow(2.0, level / 7.0));
		}
	}

	private XpTable()
	{
	}

	/** Total XP needed to reach the given level (1..99). */
	public static int xpForLevel(int level)
	{
		if (level < 1 || level > MAX_LEVEL)
		{
			throw new IllegalArgumentException("Level must be between 1 and " + MAX_LEVEL + ": " + level);
		}
		return XP_FOR_LEVEL[level];
	}

	/** The level a given total XP corresponds to (capped at 99). */
	public static int levelForXp(int xp)
	{
		int level = 1;
		while (level < MAX_LEVEL && xp >= XP_FOR_LEVEL[level + 1])
		{
			level++;
		}
		return level;
	}
}
