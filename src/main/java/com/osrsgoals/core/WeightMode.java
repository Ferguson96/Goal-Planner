package com.osrsgoals.core;

/** How the wheel favours some goals over others. */
public enum WeightMode
{
	EQUAL("Equal chance"),
	PRIORITY("By priority (1-5)"),
	QUICKEST("Favour quickest");

	private final String label;

	WeightMode(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
