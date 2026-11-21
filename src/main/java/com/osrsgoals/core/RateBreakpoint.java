package com.osrsgoals.core;

/**
 * XP per hour a method gives at one Fishing/etc. level, as listed on the OSRS Wiki.
 * Either value may be null if the wiki doesn't list that playstyle.
 */
public class RateBreakpoint
{
	public int level;
	public Double afk;
	public Double active;
	/** GP per hour at this level (negative = costs money), at the wiki's prices when it was read. */
	public Double gpPerHour;
}
