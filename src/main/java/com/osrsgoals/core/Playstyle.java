package com.osrsgoals.core;

public enum Playstyle
{
	/** Use the best rate listed for either style. */
	BEST("Best available"),
	AFK("AFK"),
	ACTIVE("Active");

	private final String label;

	Playstyle(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
