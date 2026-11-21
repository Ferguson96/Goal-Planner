package com.osrsgoals.core;

import java.util.ArrayList;
import java.util.List;

/** One task of an achievement diary tier. See docs/diary-schema.md. */
public class DiaryTask
{
	public String id;
	public String description;
	public List<SkillRequirement> skills = new ArrayList<>();
	/** Quests that must be finished. */
	public List<String> quests = new ArrayList<>();
	/** Quests that only need to be started. */
	public List<String> questsStarted = new ArrayList<>();
	/** Items, combat level, kills, other diaries: shown to the player, not checked automatically. */
	public List<String> other = new ArrayList<>();
	public String notes;
}
