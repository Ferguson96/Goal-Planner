package com.osrsgoals.core;

import java.util.ArrayList;
import java.util.List;

/** What the wiki says is needed for something (same shape as a diary task's requirements). */
public class RequirementSet
{
	public List<SkillRequirement> skills = new ArrayList<>();
	/** Quests that must be finished. */
	public List<String> quests = new ArrayList<>();
	/** Quests that only need to be started. */
	public List<String> questsStarted = new ArrayList<>();
	/** Items, combat level, kill counts, unlocks: shown to the player, not checked automatically. */
	public List<String> other = new ArrayList<>();
}
