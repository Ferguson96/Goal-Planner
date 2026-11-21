package com.osrsgoals.core;

/** One skill-level requirement of a diary task. */
public class SkillRequirement
{
	/** RuneLite Skill enum name, e.g. "FISHING". */
	public String skill;
	public int level;
	/** True only if the wiki says this level can be boosted. */
	public boolean boostable;
}
