package com.osrsgoals.core;

/** A rare drop from a boss. See docs/boss-schema.md. */
public class BossDrop
{
	public String item;
	/** The wiki's fraction as "n/d", e.g. "1/512". */
	public String rarity;
	/** n / d as a decimal. */
	public double chance;
	public String notes;
}
