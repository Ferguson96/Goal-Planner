package com.osrsgoals.core;

public enum SortMode
{
	FASTEST("Fastest"),
	MOST_PROFIT("Most profit / least cost");

	private final String label;

	SortMode(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
