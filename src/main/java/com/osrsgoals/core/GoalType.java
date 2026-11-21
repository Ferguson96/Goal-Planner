package com.osrsgoals.core;

public enum GoalType
{
	SKILL("Skill level"),
	DIARY("Diary tier"),
	BOSS_KILLS("Boss kills"),
	DROP("Boss drop");

	private final String label;

	GoalType(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
