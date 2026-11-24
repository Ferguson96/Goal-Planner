package com.osrsgoals.core;

import java.util.ArrayList;
import java.util.List;

/**
 * One way of training a skill, loaded from src/main/resources/methods/&lt;skill&gt;.json.
 * See docs/method-schema.md for the meaning of every field. Rates are listed per level
 * (the way the OSRS Wiki publishes them) and the calculator interpolates between them.
 */
public class TrainingMethod
{
	public String id;
	/** Matches RuneLite's Skill enum name, e.g. "FISHING". */
	public String skill;
	public String name;
	/** TRAINING, MINIGAME, QUEST, etc. Free text for filtering later. */
	public String category = "TRAINING";
	public int minLevel = 1;
	/** Optional: the method is only listed for levels below this (null = no limit). */
	public Integer maxLevel;
	public List<String> requirements = new ArrayList<>();
	public List<RateBreakpoint> rates = new ArrayList<>();
	/** Wiki page the numbers came from. */
	public String source;
	/** Date (yyyy-mm-dd) the numbers were read from the wiki. */
	public String retrieved;
	public String notes;
	/** Optional: XP gained per action (one arrow, one lap, one potion). Null when it varies. */
	public Double xpEach;
	/** Optional: what one action is called, in the plural (e.g. "broad arrows", "laps"). */
	public String unit;

	/** How many actions it takes to gain this much XP, or null if the method has no fixed XP per action. */
	public Long actionsFor(long xp)
	{
		if (xpEach == null || xpEach <= 0 || xp <= 0)
		{
			return null;
		}
		return (long) Math.ceil(xp / xpEach);
	}
}
