package com.osrsgoals.core;

public enum DiaryTier
{
	EASY("Easy"),
	MEDIUM("Medium"),
	HARD("Hard"),
	ELITE("Elite");

	private final String label;

	DiaryTier(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
