package com.osrsgoals.core;

/** A goal the player has set. Saved as JSON, so keep fields simple. */
public class Goal
{
	public String id;
	public GoalType type;
	public String label;
	/** SKILL: RuneLite skill name, e.g. "FISHING". */
	public String skill;
	public int targetLevel;
	/** DIARY: region id and tier name. */
	public String diaryId;
	public String tier;
	/** BOSS_KILLS and DROP. */
	public String bossId;
	public int targetKills;
	/** DROP: item name. */
	public String item;
	/** 1 (low) to 5 (high); used by the wheel. */
	public int priority = 3;
	/** Manually marked finished (used for drops). */
	public boolean done;

	public static Goal skill(String skill, int level)
	{
		Goal g = new Goal();
		g.type = GoalType.SKILL;
		g.skill = skill;
		g.targetLevel = level;
		g.label = Text.pretty(skill) + " to level " + level;
		return g;
	}

	public static Goal diary(String diaryId, String diaryName, DiaryTier tier)
	{
		Goal g = new Goal();
		g.type = GoalType.DIARY;
		g.diaryId = diaryId;
		g.tier = tier.name();
		g.label = tier + " " + diaryName;
		return g;
	}

	public static Goal bossKills(String bossId, String bossName, int kills)
	{
		Goal g = new Goal();
		g.type = GoalType.BOSS_KILLS;
		g.bossId = bossId;
		g.targetKills = kills;
		g.label = kills + " " + bossName + " kills";
		return g;
	}

	public static Goal drop(String bossId, String bossName, String item)
	{
		Goal g = new Goal();
		g.type = GoalType.DROP;
		g.bossId = bossId;
		g.item = item;
		g.label = item + " (" + bossName + ")";
		return g;
	}

	/** Identifies the same goal even if it is added twice. */
	public String key()
	{
		return type + "|" + nz(skill) + "|" + targetLevel + "|" + nz(diaryId) + "|" + nz(tier) + "|"
			+ nz(bossId) + "|" + targetKills + "|" + nz(item);
	}

	private static String nz(String s)
	{
		return s == null ? "" : s;
	}
}
