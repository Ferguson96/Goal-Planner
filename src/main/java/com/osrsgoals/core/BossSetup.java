package com.osrsgoals.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A recommended gear and inventory setup from the wiki (e.g. budget, mid-level, max). */
public class BossSetup
{
	public String name;
	public String source;
	/** Slot name (HEAD, WEAPON, ...) to item names; first is the wiki's best choice, the rest are alternatives. */
	public Map<String, List<String>> equipment = new LinkedHashMap<>();
	public List<SetupItem> inventory = new ArrayList<>();
	public String notes;
}
